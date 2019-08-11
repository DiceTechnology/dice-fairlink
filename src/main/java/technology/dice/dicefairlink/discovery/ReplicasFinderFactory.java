package technology.dice.dicefairlink.discovery;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.awsapi.AwsApiReplicasFinder;

public class ReplicasFinderFactory {
  public static AwsApiReplicasFinder getFinder(
      FairlinkConfiguration configuration, String host, DiscoveryCallback callback) {
    switch (configuration.getReplicasDiscoveryMode()) {
      case RDS_API:
        return new AwsApiReplicasFinder(
            host,
            configuration.getAwsCredentialsProvider(),
            configuration.getAuroraClusterRegion(),
            callback);
      case SQL:
        return null;
      default:
        throw new IllegalArgumentException(
            configuration.getReplicasDiscoveryMode().name() + "is not a valid discovery mode");
    }
  }
}
