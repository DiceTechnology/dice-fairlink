package technology.dice.dicefairlink.discovery.members;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.members.awsapi.AwsApiReplicasFinder;
import technology.dice.dicefairlink.discovery.members.sql.SqlReplicasFinder;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.sql.Driver;

public class ReplicasFinderFactory {
  public static MemberFinderMethod getFinder(
      FairlinkConfiguration configuration,
      FairlinkConnectionString fairlinkConnectionString,
      Driver driverForDelegate) {
    switch (configuration.getReplicasDiscoveryMode()) {
      case RDS_API:
        return new AwsApiReplicasFinder(configuration, fairlinkConnectionString);
      case SQL_MYSQL:
        return new SqlReplicasFinder(configuration, fairlinkConnectionString, driverForDelegate);
      default:
        throw new IllegalArgumentException(
            configuration.getReplicasDiscoveryMode().name() + "is not a valid discovery mode");
    }
  }
}
