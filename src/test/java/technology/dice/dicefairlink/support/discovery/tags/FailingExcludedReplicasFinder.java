package technology.dice.dicefairlink.support.discovery.tags;

import technology.dice.dicefairlink.discovery.tags.ExclusionTag;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class FailingExcludedReplicasFinder extends FixedSetExcludedReplicasFinder {
  private final AtomicInteger discoveriesUntilFailure;

  public FailingExcludedReplicasFinder(Collection<String> exclusions, int failAfter) {
    super(exclusions);
    this.discoveriesUntilFailure = new AtomicInteger(failAfter);
  }

  @Override
  public Set<String> listExcludedInstances(ExclusionTag tag) {
    final int left = this.discoveriesUntilFailure.decrementAndGet();
    if (left < 0) {
      throw new RuntimeException("Programmed exception set number of executions");
    }
    return super.listExcludedInstances(tag);
  }
}
