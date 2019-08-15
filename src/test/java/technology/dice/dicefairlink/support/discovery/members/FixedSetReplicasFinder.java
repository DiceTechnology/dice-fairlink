package technology.dice.dicefairlink.support.discovery.members;

import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;

import java.util.Collection;

public class FixedSetReplicasFinder implements MemberFinderMethod {
  private Collection<String> replicas;
  private final String fallbackEndpoint;

  public FixedSetReplicasFinder(String fallbackEndpoint, Collection<String> replicas) {
    this.fallbackEndpoint = fallbackEndpoint;
    this.replicas = replicas;
  }

  public void updateReplicas(Collection<String> replicas) {
    this.replicas = replicas;
  }

  @Override
  public ClusterInfo discoverCluster() {
    return new ClusterInfo(this.fallbackEndpoint, this.replicas);
  }
}
