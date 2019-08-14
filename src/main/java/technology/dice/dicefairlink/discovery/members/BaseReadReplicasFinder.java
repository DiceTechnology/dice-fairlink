/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.tags.ExclusionTag;
import technology.dice.dicefairlink.discovery.tags.ExclusionTagFinderFactory;
import technology.dice.dicefairlink.discovery.tags.TagFilter;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;
import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class BaseReadReplicasFinder implements Runnable {
  private static final String EXCLUSION_TAG_KEY = "Fairlink-Exclude";
  private static final Logger LOGGER = Logger.getLogger(BaseReadReplicasFinder.class.getName());
  private final ReplicasDiscoveryCallback callback;
  private final FairlinkConfiguration fairlinkConfiguration;
  private String fallbackReadOnlyEndpoint;
  protected final FairlinkConnectionString fairlinkConnectionString;
  protected final Driver driverForDelegate;
  protected final TagFilter tagFilter;
  protected Collection<String> excludedInstanceIds = new HashSet<>(0);

  public BaseReadReplicasFinder(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkConnectionString fairlinkConnectionString,
      ScheduledExecutorService tagsPollingExecutor,
      ReplicasDiscoveryCallback callback)
      throws SQLException {
    this.callback = callback;
    this.fairlinkConnectionString = fairlinkConnectionString;
    this.fairlinkConfiguration = fairlinkConfiguration;
    this.tagFilter = ExclusionTagFinderFactory.getTagFilter(fairlinkConfiguration);
    this.driverForDelegate =
        DriverManager.getDriver(fairlinkConnectionString.delegateConnectionString());
    tagsPollingExecutor.scheduleAtFixedRate(
        () ->
            excludedInstanceIds =
                tagFilter.listExcludedInstances(
                    new ExclusionTag(EXCLUSION_TAG_KEY, Boolean.TRUE.toString())),
        1,
        fairlinkConfiguration.getTagsPollerInterval().getSeconds(),
        TimeUnit.SECONDS);
  }

  protected abstract ClusterInfo discoverCluster();

  private final RandomisedCyclicIterator<String> discoverReplicas() {
    long before = System.currentTimeMillis();
    final ClusterInfo clusterInfo = discoverCluster();
    fallbackReadOnlyEndpoint = clusterInfo.getReadonlyEndpoint();
    final Set<String> filteredReplicas =
        clusterInfo.getReplicas().stream()
            .filter(
                db ->
                    (!this.fairlinkConfiguration.isValidateConnection())
                        || this.validateConnection(db))
            .filter(db -> !excludedInstanceIds.contains(db))
            .collect(Collectors.toSet());
    final RandomisedCyclicIterator<String> result =
        filteredReplicas.isEmpty()
            ? RandomisedCyclicIterator.of(clusterInfo.getReadonlyEndpoint())
            : RandomisedCyclicIterator.of(filteredReplicas);
    long after = System.currentTimeMillis();
    LOGGER.info(
        "Updated list of replicas in "
            + (after - before)
            + " ms. Found "
            + filteredReplicas.size()
            + " good, active replicas (validation "
            + (fairlinkConfiguration.isValidateConnection() ? "" : "NOT " + "done)")
            + ". Next update in "
            + this.fairlinkConfiguration.getReplicaPollInterval());
    return result;
  }

  private boolean validateConnection(String s) {
    try (Connection c =
        driverForDelegate.connect(
            fairlinkConnectionString.delegateConnectionString(s),
            fairlinkConnectionString.getProperties())) {
      c.createStatement().executeQuery("SELECT 1");
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public final RandomisedCyclicIterator<String> init() {
    return this.discoverReplicas();
  }

  protected String getFallbackReadOnlyEndpoint() {
    return fallbackReadOnlyEndpoint;
  }

  public FairlinkConfiguration getFairlinkConfiguration() {
    return fairlinkConfiguration;
  }

  @Override
  public final void run() {
    this.callback.callback(this.discoverReplicas());
  }
}
