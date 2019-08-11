package technology.dice.dicefairlink.discovery;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.DiscoveryCallback;
import technology.dice.dicefairlink.discovery.awsapi.AwsApiReplicasFinder;

import java.net.URI;

public class ReplicasFinderFactory {
  public static AwsApiReplicasFinder getFinder(
      FairlinkConfiguration configuration, URI url, DiscoveryCallback callback) {
    switch (configuration.getReplicasDiscoveryMode()) {
      case RDS_API:
        return new AwsApiReplicasFinder(
            url.getHost(),
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
