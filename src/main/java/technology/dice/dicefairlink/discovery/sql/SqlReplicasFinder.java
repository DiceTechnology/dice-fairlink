package technology.dice.dicefairlink.discovery.sql;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.BaseReadReplicasFinder;
import technology.dice.dicefairlink.discovery.ClusterInfo;
import technology.dice.dicefairlink.discovery.DiscoveryCallback;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SqlReplicasFinder extends BaseReadReplicasFinder {

  private final String FIND_NODES_QUERY =
      "select server_id, if(session_id = 'MASTER_SESSION_ID',"
          + "'WRITER', 'READER') as role from "
          + "information_schema.replica_host_status;";
  private final String replicaEndpointTemplate;

  public SqlReplicasFinder(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkConnectionString fairlinkConnectionString,
      DiscoveryCallback callback)
      throws SQLException {
    super(fairlinkConfiguration, fairlinkConnectionString, callback);
    this.replicaEndpointTemplate = fairlinkConfiguration.getReplicaEndpointTemplate();
  }

  protected List<DatabaseInstance> findReplicas() {
    List<DatabaseInstance> instances = new ArrayList<>();
    try (final Connection c =
        this.driverForDelegate.connect(
            fairlinkConnectionString.delegateConnectionString(),
            fairlinkConnectionString.getProperties())) {
      try (final ResultSet resultSet = c.createStatement().executeQuery(FIND_NODES_QUERY)) {
        while (resultSet.next()) {
          instances.add(
              new DatabaseInstance(
                  DatabaseInstanceRole.valueOf(
                      DatabaseInstanceRole.class, resultSet.getString("role")),
                  resultSet.getString("server_id")));
        }
      }
    } catch (SQLException e) {
      // TODO: handle me
      e.printStackTrace();
    }
    return instances;
  }

  @Override
  protected ClusterInfo discoverCluster() {
    return new ClusterInfo(
        this.fairlinkConnectionString.getFairlinkUri(),
        this.findReplicas().stream()
            .filter(databaseInstance -> databaseInstance.getRole() == DatabaseInstanceRole.READER)
            .map(e -> String.format(this.replicaEndpointTemplate, e.getId()))
            .collect(Collectors.toSet()));
  }
}
