package technology.dice.dicefairlink.discovery.members.sql;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;
import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySqlReplicasFinderTest {
  @Rule public JdbcDatabaseContainer mysql = new MySQLContainer();

  @Before
  public void before() throws IOException, SQLException {
    this.runScript("technology/dice/dicefairlink/discovery/members/sql/mysql/schema.sql");
  }

  private void runScript(String path) throws IOException, SQLException {
    Properties p = new Properties();
    p.setProperty("allowMultiQueries", "true");
    p.setProperty("user", mysql.getUsername());
    p.setProperty("password", mysql.getPassword());
    final InputStream resourceAsStream =
        MySqlReplicasFinderTest.class.getClassLoader().getResourceAsStream(path);
    final String schemaSql =
        CharStreams.toString(new InputStreamReader(resourceAsStream, Charsets.UTF_8));
    Connection conn = DriverManager.getConnection(mysql.getJdbcUrl(), p);

    conn.createStatement().executeUpdate(schemaSql);
    conn.close();
  }

  private Properties baseTestProperties() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "irrelevant");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    p.setProperty("user", mysql.getUsername());
    p.setProperty("password", mysql.getPassword());
    return p;
  }

  @Test
  public void noReplicas() throws URISyntaxException {
    MySQLReplicasFinder underTest =
        new MySQLReplicasFinder(
            new FairlinkConnectionString(
                mysql.getJdbcUrl().replace("mysql", "fairlink:mysql"), this.baseTestProperties()),
            mysql.getJdbcDriverInstance(),
            mysql.getDatabaseName());
    final ClusterInfo clusterInfo = underTest.discoverCluster();
    Assert.assertEquals(new ClusterInfo(mysql.getJdbcUrl(), ImmutableSet.of()), clusterInfo);
  }

  @Test
  public void withReplicas() throws URISyntaxException, IOException, SQLException {
    this.runScript("technology/dice/dicefairlink/discovery/members/sql/mysql/2replicas.sql");
    MySQLReplicasFinder underTest =
        new MySQLReplicasFinder(
            new FairlinkConnectionString(
                mysql.getJdbcUrl().replace("mysql", "fairlink:mysql"), this.baseTestProperties()),
            mysql.getJdbcDriverInstance(),
            mysql.getDatabaseName());
    final ClusterInfo actual = underTest.discoverCluster();
    final ClusterInfo expected = new ClusterInfo(mysql.getJdbcUrl(), ImmutableSet.of("replica"));
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noConnectionsResultsInEmptySet()
      throws URISyntaxException, IOException, SQLException {
    this.runScript("technology/dice/dicefairlink/discovery/members/sql/mysql/2replicas.sql");
    MySQLReplicasFinder underTest =
        new MySQLReplicasFinder(
            new FairlinkConnectionString(
                mysql.getJdbcUrl().replace("mysql", "fairlink:fairlinktestdriver"),
                this.baseTestProperties()),
            mysql.getJdbcDriverInstance(),
            mysql.getDatabaseName());
    final ClusterInfo actual = underTest.discoverCluster();
    final ClusterInfo expected =
        new ClusterInfo(
            mysql.getJdbcUrl().replace("mysql", "fairlinktestdriver"), ImmutableSet.of());
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noTableResultsInEmptySet() throws URISyntaxException, IOException, SQLException {
    this.runScript("technology/dice/dicefairlink/discovery/members/sql/mysql/2replicas.sql");
    MySQLReplicasFinder underTest =
        new MySQLReplicasFinder(
            new FairlinkConnectionString(
                mysql.getJdbcUrl().replace("mysql", "fairlink:mysql"), this.baseTestProperties()),
            mysql.getJdbcDriverInstance(),
            "i_do_not_exist");
    final ClusterInfo actual = underTest.discoverCluster();
    final ClusterInfo expected = new ClusterInfo(mysql.getJdbcUrl(), ImmutableSet.of());
    Assert.assertEquals(expected, actual);
  }
}
