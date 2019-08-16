package technology.dice.dicefairlink.discovery.tags;

import java.util.Set;

@FunctionalInterface
public interface TagFilter {
  Set<String> listExcludedInstances(ExclusionTag tag);
}
