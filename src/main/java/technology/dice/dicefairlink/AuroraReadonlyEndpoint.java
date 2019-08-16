/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.members.FairlinkMemberFinder;

import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuroraReadonlyEndpoint {
  private static final Logger LOGGER = Logger.getLogger(AuroraReadonlyEndpoint.class.getName());
  private Iterator<String> replicas;
  private final AtomicReference<String> lastReplica = new AtomicReference<>();

  public AuroraReadonlyEndpoint(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkMemberFinder fairlinkMemberFinder,
      ScheduledExecutorService replicaDiscoveryExecutor) {

    replicas = fairlinkMemberFinder.init();
    final Duration startJitter = fairlinkConfiguration.randomBoundDelay();
    LOGGER.log(Level.INFO, "Starting cluster member discovery with {} delay.", startJitter);
    replicaDiscoveryExecutor.scheduleAtFixedRate(
        () -> replicas = fairlinkMemberFinder.discoverReplicas(),
        fairlinkConfiguration.getReplicaPollInterval().plus(startJitter).getSeconds(),
        fairlinkConfiguration.getReplicaPollInterval().getSeconds(),
        TimeUnit.SECONDS);
  }

  public String getNextReplica() {
    String nextReplica = replicas.next();
    LOGGER.finer("Obtained replica: " + nextReplica);
    if (nextReplica != null && nextReplica.equals(lastReplica.get())) {
      nextReplica = replicas.next();
    }
    lastReplica.set(nextReplica);
    return nextReplica;
  }
}
