/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.tags;

import java.util.Set;

@FunctionalInterface
public interface TagFilter {
  Set<String> listExcludedInstances(ExclusionTag tag);
}
