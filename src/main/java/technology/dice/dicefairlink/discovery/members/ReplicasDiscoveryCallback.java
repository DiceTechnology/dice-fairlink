/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */ package technology.dice.dicefairlink.discovery.members;

import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;

@FunctionalInterface
public interface ReplicasDiscoveryCallback {
  void callback(RandomisedCyclicIterator<String> replicas);
}
