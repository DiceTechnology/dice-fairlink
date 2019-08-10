/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.BaseReadReplicasFinder;
import technology.dice.dicefairlink.discovery.awsapi.ReplicasFinderFactory;
import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuroraReadonlyEndpoint {
  private static final Logger LOGGER = Logger.getLogger(AuroraReadonlyEndpoint.class.getName());
  private RandomisedCyclicIterator<String> replicas;
  private final AtomicReference<String> lastReplica = new AtomicReference<>();

  public AuroraReadonlyEndpoint(
      URI uri, FairlinkConfiguration fairlinkConfiguration, ScheduledExecutorService executor) {

    BaseReadReplicasFinder finder =
        ReplicasFinderFactory.getFinder(
            fairlinkConfiguration,
            uri,
            discoveredReplicas -> {
              replicas = discoveredReplicas;
              if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    String.format(
                        "Retrieved [%s] read replicas for cluster identified by [%s] with. List will be refreshed in [%s]",
                        replicas.size(),
                        uri.getHost(),
                        fairlinkConfiguration.getReplicaPollInterval()));
              }
            });
    replicas = finder.init();
    LOGGER.log(
        Level.INFO,
        String.format(
            "Initialised driver for cluster identified by [%s] with [%s] read replicas. List will be refreshed every [%s]",
            uri.getHost(), replicas.size(), fairlinkConfiguration.getReplicaPollInterval()));
    executor.scheduleAtFixedRate(
        finder,
        fairlinkConfiguration.getReplicaPollInterval().getSeconds(),
        fairlinkConfiguration.getReplicaPollInterval().getSeconds(),
        TimeUnit.SECONDS);
  }

  public String getNextReplica() {
    String nextReplica = replicas.next();
    if (nextReplica != null && nextReplica.equals(lastReplica.get())) {
      nextReplica = replicas.next();
    }
    lastReplica.set(nextReplica);
    return nextReplica;
  }
}
