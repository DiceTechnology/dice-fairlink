package technology.dice.dicefairlink.discovery.members.sql;

import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SqlReplicasFinder implements MemberFinderMethod {
  private static final Logger LOGGER = Logger.getLogger(SqlReplicasFinder.class.getName());
  private static final Set<DatabaseInstance> EMPTY_SET = new HashSet<>(0);
  private final String FIND_NODES_QUERY =
      "select server_id, if(session_id = 'MASTER_SESSION_ID',"
          + "'WRITER', 'READER') as role from "
          + "information_schema.replica_host_status;";
  private final Driver driverForDelegate;
  private final FairlinkConnectionString fairlinkConnectionString;

  public SqlReplicasFinder(
      FairlinkConnectionString fairlinkConnectionString, Driver driverForDelegate) {
    this.driverForDelegate = driverForDelegate;
    this.fairlinkConnectionString = fairlinkConnectionString;
  }

  protected Set<DatabaseInstance> findReplicas() {
    Set<DatabaseInstance> instances = new HashSet<>();
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
    } catch (Exception e) {
      LOGGER.log(
          Level.SEVERE,
          "Failed to obtain cluster members due to exception. Returning empty set",
          e);
      return EMPTY_SET;
    }
    return instances;
  }

  @Override
  public ClusterInfo discoverCluster() {
    return new ClusterInfo(
        this.fairlinkConnectionString.delegateConnectionString(),
        this.findReplicas().stream()
            .filter(databaseInstance -> databaseInstance.getRole() == DatabaseInstanceRole.READER)
            .map(DatabaseInstance::getId)
            .collect(Collectors.toSet()));
  }
}
