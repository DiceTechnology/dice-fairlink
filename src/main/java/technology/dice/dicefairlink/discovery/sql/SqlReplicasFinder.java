package technology.dice.dicefairlink.discovery.sql;

import technology.dice.dicefairlink.discovery.BaseReadReplicasFinder;
import technology.dice.dicefairlink.discovery.ClusterInfo;
import technology.dice.dicefairlink.discovery.DiscoveryCallback;
import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;

public class SqlReplicasFinder extends BaseReadReplicasFinder {
  public SqlReplicasFinder(DiscoveryCallback callback) {
    super(callback);
  }

  @Override
  public RandomisedCyclicIterator<String> discoverReplicas() {
    return null;
  }

  @Override
  protected ClusterInfo discoverCluster() {
    return null;
  }
}
