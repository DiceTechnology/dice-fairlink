package technology.dice.dicefairlink.discovery;

import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;

@FunctionalInterface
public interface DiscoveryCallback {
  void callback(RandomisedCyclicIterator<String> replicas);
}
