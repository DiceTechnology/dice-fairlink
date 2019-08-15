package technology.dice.dicefairlink.discovery.members.sql;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SqlReplicasFinder implements MemberFinderMethod {

  private final String FIND_NODES_QUERY =
      "select server_id, if(session_id = 'MASTER_SESSION_ID',"
          + "'WRITER', 'READER') as role from "
          + "information_schema.replica_host_status;";
  private final Driver driverForDelegate;
  private final FairlinkConnectionString fairlinkConnectionString;
  private final FairlinkConfiguration fairlinkConfiguration;

  public SqlReplicasFinder(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkConnectionString fairlinkConnectionString,
      Driver driverForDelegate) {
    this.driverForDelegate = driverForDelegate;
    this.fairlinkConnectionString = fairlinkConnectionString;
    this.fairlinkConfiguration = fairlinkConfiguration;
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
                  String.format(
                      this.fairlinkConfiguration.getReplicaEndpointTemplate(),
                      resultSet.getString("server_id"))));
        }
      }
    } catch (SQLException e) {
      // TODO: handle me
      e.printStackTrace();
    }
    return instances;
  }

  @Override
  public ClusterInfo discoverCluster() {
    return new ClusterInfo(
        this.fairlinkConnectionString.getFairlinkUri(),
        this.findReplicas().stream()
            .filter(databaseInstance -> databaseInstance.getRole() == DatabaseInstanceRole.READER)
            .map(
                e ->
                    String.format(
                        this.fairlinkConfiguration.getReplicaEndpointTemplate(), e.getId()))
            .collect(Collectors.toSet()));
  }
}
