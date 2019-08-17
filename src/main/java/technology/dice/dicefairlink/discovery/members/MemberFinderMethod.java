/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members;

@FunctionalInterface
public interface MemberFinderMethod {
  ClusterInfo discoverCluster();
}
