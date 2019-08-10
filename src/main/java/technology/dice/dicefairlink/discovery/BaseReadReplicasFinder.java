/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery;

import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;

public abstract class BaseReadReplicasFinder implements Runnable {
  private final DiscoveryCallback callback;
  protected String fallbackReadOnlyEndpoint;

  public BaseReadReplicasFinder(DiscoveryCallback callback) {
    this.callback = callback;
  }

  public abstract RandomisedCyclicIterator<String> discoverReplicas();

  protected abstract ClusterInfo discoverCluster();

  public RandomisedCyclicIterator<String> init() {
    final ClusterInfo clusterInfo = discoverCluster();
    return clusterInfo.getReplicas().isEmpty()
        ? RandomisedCyclicIterator.of(clusterInfo.getReadonlyEndpoint())
        : RandomisedCyclicIterator.of(clusterInfo.getReplicas());
  }

  @Override
  public final void run() {
    this.callback.callback(this.discoverReplicas());
  }
}
