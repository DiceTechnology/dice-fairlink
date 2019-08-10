/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import technology.dice.dicefairlink.discovery.AwsApiReplicasFinder;
import technology.dice.dicefairlink.discovery.BaseReadReplicasFinder;
import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;

import java.time.Duration;
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
      String clusterId,
      AWSCredentialsProvider credentialsProvider,
      Duration pollerInterval,
      Region region,
      ScheduledExecutorService executor) {

    BaseReadReplicasFinder finder =
        new AwsApiReplicasFinder(
            clusterId,
            credentialsProvider,
            region,
            discoveredReplicas -> {
              replicas = discoveredReplicas;
              if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    String.format(
                        "Retrieved [%s] read replicas for cluster id [%s] with. List will be refreshed in [%s] seconds",
                        replicas.size(), clusterId, pollerInterval.getSeconds()));
              }
            });
    replicas = finder.init();
    LOGGER.log(
        Level.INFO,
        String.format(
            "Initialised driver for cluster id [%s] with [%s] read replicas. List will be refreshed every [%s] seconds",
            clusterId, replicas.size(), pollerInterval.getSeconds()));
    executor.scheduleAtFixedRate(
        finder, pollerInterval.getSeconds(), pollerInterval.getSeconds(), TimeUnit.SECONDS);
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
