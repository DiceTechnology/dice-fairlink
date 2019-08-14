package technology.dice.dicefairlink.discovery.tags;

import java.util.Set;

public interface TagFilter {
  Set<String> listExcludedInstances(ExclusionTag tag);
}
