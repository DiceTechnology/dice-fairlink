package technology.dice.dicefairlink.driver;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;
import technology.dice.dicefairlink.StepByStepExecutor;
import technology.dice.dicefairlink.discovery.members.sql.SqlReplicasFinderTest;
import technology.dice.dicefairlink.iterators.CyclicIterator;
import technology.dice.dicefairlink.iterators.SizedIterator;
import technology.dice.dicefairlink.support.discovery.tags.FixedSetExcludedReplicasFinder;
import technology.dice.dicefairlink.support.iterators.TestCyclicIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AuroraReadReplicasDriverEndToEndTest {
  @Rule public MySQLContainer master = new MySQLContainer();
  @Rule public MySQLContainer slave1 = new MySQLContainer();
  @Rule public MySQLContainer slave2 = new MySQLContainer();

  private void runScript(MySQLContainer container, String path) throws IOException, SQLException {
    this.runScript(container, path, ImmutableMap.of());
  }

  private void runScript(MySQLContainer container, String path, Map<String, String> replacing)
      throws IOException, SQLException {
    Properties p = new Properties();
    p.setProperty("allowMultiQueries", "true");
    p.setProperty("user", container.getUsername());
    p.setProperty("password", container.getPassword());
    final InputStream resourceAsStream =
        SqlReplicasFinderTest.class.getClassLoader().getResourceAsStream(path);
    String schemaSql =
        CharStreams.toString(new InputStreamReader(resourceAsStream, Charsets.UTF_8));
    for (Map.Entry<String, String> replace : replacing.entrySet()) {
      schemaSql = schemaSql.replace(replace.getKey(), replace.getValue());
    }
    Connection conn = DriverManager.getConnection(container.getJdbcUrl(), p);

    conn.createStatement().executeUpdate(schemaSql);
    conn.close();
  }

  @Before
  public void before() throws IOException, SQLException {
    this.runScript(master, "technology/dice/dicefairlink/discovery/members/sql/schema.sql");
    this.runScript(slave1, "technology/dice/dicefairlink/discovery/members/sql/schema.sql");
    this.runScript(slave2, "technology/dice/dicefairlink/discovery/members/sql/schema.sql");
    this.runScript(
        master,
        "technology/dice/dicefairlink/discovery/members/sql/3replicasPorts.sql",
        ImmutableMap.of(
            "__PORT_SLAVE_1__",
            slave1.getFirstMappedPort().toString(),
            "__PORT_SLAVE_2__",
            slave2.getFirstMappedPort().toString()));
    this.runScript(master, "technology/dice/dicefairlink/discovery/members/sql/masterData.sql");
    this.runScript(slave1, "technology/dice/dicefairlink/discovery/members/sql/slave1Data.sql");
    this.runScript(slave2, "technology/dice/dicefairlink/discovery/members/sql/slave2Data.sql");
  }

  private StepByStepExecutor exclusionTagsExecutor;
  private StepByStepExecutor memberDiscoveryExecutor;

  private Properties baseTestProperties() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("discoveryMode", "SQL_MYSQL");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s");
    p.setProperty("validateConnection", "true");
    p.setProperty("_fairlinkMySQLSchemaOverride", "test");
    p.setProperty("user", master.getUsername());
    p.setProperty("password", master.getPassword());
    return p;
  }

  @Before
  public void setup() {
    this.exclusionTagsExecutor = new StepByStepExecutor(1);
    this.memberDiscoveryExecutor = new StepByStepExecutor(1);
  }

  @Test
  public void f() throws SQLException {
    CyclicIterator<String>[] i = new CyclicIterator[1];
    AuroraReadReplicasDriver underTest =
        new AuroraReadReplicasDriver(
            () -> memberDiscoveryExecutor,
            () -> exclusionTagsExecutor,
            new FixedSetExcludedReplicasFinder(ImmutableSet.of("replica1", "replica3")),
            null,
            null,
            new Function<Collection<String>, SizedIterator<String>>() {
              @Nullable
              @Override
              public SizedIterator<String> apply(@Nullable Collection<String> strings) {
                final CyclicIterator<String> iterator = TestCyclicIterator.of(strings);
                i[0] = iterator;
                return iterator;
              }
            });

    List<Data> foundData = readTwice(underTest);
    Assert.assertEquals(2, i[0].size());
    Assert.assertEquals(4, foundData.size());
    Assert.assertTrue(
        foundData.stream()
            .filter(d -> d.getB().equalsIgnoreCase("SLAVE1"))
            .findFirst()
            .isPresent());
    Assert.assertTrue(
        foundData.stream()
            .filter(d -> d.getB().equalsIgnoreCase("SLAVE2"))
            .findFirst()
            .isPresent());

    final Connection masterConnection =
        DriverManager.getConnection(master.getJdbcUrl(), baseTestProperties());
    masterConnection
        .createStatement()
        .executeUpdate("DELETE from replica_host_status WHERE SESSION_ID='another-sesion2'");
    masterConnection.close();

    memberDiscoveryExecutor.step();
    foundData = readTwice(underTest);

    Assert.assertEquals(1, i[0].size());
    Assert.assertEquals(4, foundData.size());
    Assert.assertEquals(
        2, foundData.stream().filter(d -> d.getB().equalsIgnoreCase("SLAVE1")).count());
    Assert.assertFalse(
        foundData.stream()
            .filter(d -> d.getB().equalsIgnoreCase("SLAVE2"))
            .findFirst()
            .isPresent());
  }

  private List<Data> readTwice(AuroraReadReplicasDriver underTest) throws SQLException {
    List<Data> foundData = new ArrayList();
    Connection connect =
        underTest.connect(
            master.getJdbcUrl().replace("mysql", "auroraro:mysql"), this.baseTestProperties());
    ResultSet resultSet = connect.createStatement().executeQuery("SELECT * FROM data");
    while (resultSet.next()) {
      Data data = new Data(resultSet.getString("a"), resultSet.getString("b"));
      foundData.add(data);
    }
    connect.close();
    resultSet.close();
    connect =
        underTest.connect(
            master.getJdbcUrl().replace("mysql", "auroraro:mysql"), this.baseTestProperties());
    resultSet = connect.createStatement().executeQuery("SELECT * FROM data");
    while (resultSet.next()) {
      Data data = new Data(resultSet.getString("a"), resultSet.getString("b"));
      foundData.add(data);
    }

    connect.close();
    resultSet.close();

    return foundData;
  }
}
