/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */ package technology.dice.dicefairlink.discovery;

import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;

@FunctionalInterface
public interface DiscoveryCallback {
  void callback(RandomisedCyclicIterator<String> replicas);
}
