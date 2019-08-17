package technology.dice.dicefairlink.support.discovery.members;

import technology.dice.dicefairlink.discovery.members.ClusterInfo;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FailingReplicasFinder extends FixedSetReplicasFinder {
  private final AtomicInteger discoveriesUntilFailure;

  public FailingReplicasFinder(String fallbackEndpoint, Set<String> replicas, int failAfter) {
    super(fallbackEndpoint, replicas);
    this.discoveriesUntilFailure = new AtomicInteger(failAfter);
  }

  @Override
  public ClusterInfo discoverCluster() {
    final int left = this.discoveriesUntilFailure.decrementAndGet();
    if (left < 0) {
      throw new RuntimeException("Programmed exception set number of executions");
    }
    return super.discoverCluster();
  }
}
