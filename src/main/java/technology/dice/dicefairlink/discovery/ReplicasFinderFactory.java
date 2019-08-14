package technology.dice.dicefairlink.discovery;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.awsapi.AwsApiReplicasFinder;
import technology.dice.dicefairlink.discovery.sql.SqlReplicasFinder;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.sql.SQLException;

public class ReplicasFinderFactory {
  public static BaseReadReplicasFinder getFinder(
      FairlinkConfiguration configuration,
      FairlinkConnectionString fairlinkConnectionString,
      DiscoveryCallback callback)
      throws SQLException {
    switch (configuration.getReplicasDiscoveryMode()) {
      case RDS_API:
        return new AwsApiReplicasFinder(configuration, fairlinkConnectionString, callback);
      case SQL_MYSQL:
        try {
          return new SqlReplicasFinder(configuration, fairlinkConnectionString, callback);
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      default:
        throw new IllegalArgumentException(
            configuration.getReplicasDiscoveryMode().name() + "is not a valid discovery mode");
    }
  }
}
