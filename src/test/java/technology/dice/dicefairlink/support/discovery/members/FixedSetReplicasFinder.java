package technology.dice.dicefairlink.support.discovery.members;

import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;

import java.util.Set;

public class FixedSetReplicasFinder implements MemberFinderMethod {
  private Set<String> replicas;
  private final String fallbackEndpoint;

  public FixedSetReplicasFinder(String fallbackEndpoint, Set<String> replicas) {
    this.fallbackEndpoint = fallbackEndpoint;
    this.replicas = replicas;
  }

  public void updateReplicas(Set<String> replicas) {
    this.replicas = replicas;
  }

  @Override
  public ClusterInfo discoverCluster() {
    return new ClusterInfo(this.fallbackEndpoint, this.replicas);
  }
}
