package technology.dice.dicefairlink.discovery.members.sql;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
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

/**
 * @author Ryan Gardner
 * @date 9/13/19
 */
public class PostgresSqlReplicaFinderTest {
    @Rule public PostgreSQLContainer postgres = new PostgreSQLContainer();


    @Before
    public void before() throws IOException, SQLException {
      this.runScript("technology/dice/dicefairlink/discovery/members/sql/postgresql/schema.sql");
    }

    private void runScript(String path) throws IOException, SQLException {
      Properties p = new Properties();
      p.setProperty("allowMultiQueries", "true");
      p.setProperty("user", postgres.getUsername());
      p.setProperty("password", postgres.getPassword());
      final InputStream resourceAsStream =
              PostgresSqlReplicaFinderTest.class.getClassLoader().getResourceAsStream(path);
      final String schemaSql =
          CharStreams.toString(new InputStreamReader(resourceAsStream, Charsets.UTF_8));
      Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), p);

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
      p.setProperty("user", postgres.getUsername());
      p.setProperty("password", postgres.getPassword());
      return p;
    }

    @Test
    public void noReplicas() throws URISyntaxException {
      PostgresSQLReplicasFinder underTest =
          new PostgresSQLReplicasFinder(
              new FairlinkConnectionString(
                  postgres.getJdbcUrl().replace("postgresql", "fairlink:postgresql"), this.baseTestProperties()),
              postgres.getJdbcDriverInstance());
      final ClusterInfo clusterInfo = underTest.discoverCluster();
      Assert.assertEquals(new ClusterInfo(postgres.getJdbcUrl(), ImmutableSet.of()), clusterInfo);
    }

    @Test
    public void withReplicas() throws URISyntaxException, IOException, SQLException {
      this.runScript("technology/dice/dicefairlink/discovery/members/sql/postgresql/2replicas.sql");
      PostgresSQLReplicasFinder underTest =
          new PostgresSQLReplicasFinder(
              new FairlinkConnectionString(
                  postgres.getJdbcUrl().replace("postgresql", "fairlink:postgresql"), this.baseTestProperties()),
              postgres.getJdbcDriverInstance());
      final ClusterInfo actual = underTest.discoverCluster();
      final ClusterInfo expected = new ClusterInfo(postgres.getJdbcUrl(), ImmutableSet.of("replica"));
      Assert.assertEquals(expected, actual);
    }

    @Test
    public void noConnectionsResultsInEmptySet()
        throws URISyntaxException, IOException, SQLException {
      this.runScript("technology/dice/dicefairlink/discovery/members/sql/postgresql/2replicas.sql");
      PostgresSQLReplicasFinder underTest =
          new PostgresSQLReplicasFinder(
              new FairlinkConnectionString(
                  postgres.getJdbcUrl().replace("postgresql", "fairlink:fairlinktestdriver"),
                  this.baseTestProperties()),
              postgres.getJdbcDriverInstance());
      final ClusterInfo actual = underTest.discoverCluster();
      final ClusterInfo expected =
          new ClusterInfo(
              postgres.getJdbcUrl().replace("postgresql", "fairlinktestdriver"), ImmutableSet.of());
      Assert.assertEquals(expected, actual);
    }

    @Test
    public void noFunctionResultsInEmptySet() throws URISyntaxException, IOException, SQLException {
      this.runScript("technology/dice/dicefairlink/discovery/members/sql/postgresql/drop_function.sql");
      PostgresSQLReplicasFinder underTest =
          new PostgresSQLReplicasFinder(
              new FairlinkConnectionString(
                  postgres.getJdbcUrl().replace("postgresql", "fairlink:postgresql"), this.baseTestProperties()),
              postgres.getJdbcDriverInstance());
      final ClusterInfo actual = underTest.discoverCluster();
      final ClusterInfo expected = new ClusterInfo(postgres.getJdbcUrl(), ImmutableSet.of());
      Assert.assertEquals(expected, actual);
    }
}
