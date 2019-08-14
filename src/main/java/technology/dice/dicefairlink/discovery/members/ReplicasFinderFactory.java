package technology.dice.dicefairlink.discovery.members;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.members.awsapi.AwsApiReplicasFinder;
import technology.dice.dicefairlink.discovery.members.sql.SqlReplicasFinder;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;

public class ReplicasFinderFactory {
  public static BaseReadReplicasFinder getFinder(
      FairlinkConfiguration configuration,
      FairlinkConnectionString fairlinkConnectionString,
      ScheduledExecutorService tagsPollingExecutor,
      ReplicasDiscoveryCallback callback)
      throws SQLException {
    switch (configuration.getReplicasDiscoveryMode()) {
      case RDS_API:
        return new AwsApiReplicasFinder(
            configuration, fairlinkConnectionString, tagsPollingExecutor, callback);
      case SQL_MYSQL:
        try {
          return new SqlReplicasFinder(
              configuration, fairlinkConnectionString, tagsPollingExecutor, callback);
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      default:
        throw new IllegalArgumentException(
            configuration.getReplicasDiscoveryMode().name() + "is not a valid discovery mode");
    }
  }
}
