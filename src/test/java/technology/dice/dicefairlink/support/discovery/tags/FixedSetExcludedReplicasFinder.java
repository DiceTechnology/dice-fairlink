package technology.dice.dicefairlink.support.discovery.tags;

import technology.dice.dicefairlink.discovery.tags.ExclusionTag;
import technology.dice.dicefairlink.discovery.tags.TagFilter;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class FixedSetExcludedReplicasFinder implements TagFilter {
  private Collection<String> exclusions;

  public FixedSetExcludedReplicasFinder(Collection<String> exclusions) {
    this.exclusions = exclusions;
  }

  public void updateExclusions(Collection<String> replicas) {
    this.exclusions = replicas;
  }

  @Override
  public Set<String> listExcludedInstances(ExclusionTag tag) {
    return this.exclusions.stream().collect(Collectors.toSet());
  }
}
