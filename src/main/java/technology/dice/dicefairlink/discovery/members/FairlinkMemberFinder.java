/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.tags.ExclusionTag;
import technology.dice.dicefairlink.discovery.tags.TagFilter;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;
import technology.dice.dicefairlink.iterators.SizedIteratorBuilder;

import java.sql.Connection;
import java.sql.Driver;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FairlinkMemberFinder {
  private static final String EXCLUSION_TAG_KEY = "Fairlink-Exclude";
  private static final Logger LOGGER = Logger.getLogger(FairlinkMemberFinder.class.getName());
  private final FairlinkConfiguration fairlinkConfiguration;
  private final MemberFinderMethod memberFinder;
  private final Driver driverForDelegate;
  private final SizedIteratorBuilder<String> iteratorBuilder;
  private String fallbackReadOnlyEndpoint;
  protected final FairlinkConnectionString fairlinkConnectionString;
  protected final TagFilter tagFilter;
  protected Collection<String> excludedInstanceIds = new HashSet<>(0);

  public FairlinkMemberFinder(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkConnectionString fairlinkConnectionString,
      ScheduledExecutorService tagsPollingExecutor,
      TagFilter excludedInstancesFinder,
      MemberFinderMethod memberFinder,
      SizedIteratorBuilder<String> stringSizedIteratorBuilder,
      Driver driverForDelegate) {
    this.fairlinkConnectionString = fairlinkConnectionString;
    this.fairlinkConfiguration = fairlinkConfiguration;
    this.tagFilter = excludedInstancesFinder;
    this.memberFinder = memberFinder;
    this.driverForDelegate = driverForDelegate;
    final Duration startJitter = fairlinkConfiguration.randomBoundDelay();
    this.iteratorBuilder = stringSizedIteratorBuilder;
    LOGGER.info("Starting excluded members discovery with " + startJitter + " delay.");
    tagsPollingExecutor.scheduleAtFixedRate(
        () ->
            excludedInstanceIds =
                tagFilter.listExcludedInstances(
                    new ExclusionTag(EXCLUSION_TAG_KEY, Boolean.TRUE.toString())),
        startJitter.getSeconds(),
        fairlinkConfiguration.getTagsPollerInterval().getSeconds(),
        TimeUnit.SECONDS);
  }

  public final Iterator<String> discoverReplicas() {
    long before = System.currentTimeMillis();
    final ClusterInfo clusterInfo = this.memberFinder.discoverCluster();
    fallbackReadOnlyEndpoint = clusterInfo.getReadonlyEndpoint();
    final Set<String> filteredReplicas =
        clusterInfo.getReplicas().stream()
            .filter(
                db ->
                    (!this.fairlinkConfiguration.isValidateConnection())
                        || this.validateConnection(db))
            .filter(db -> !excludedInstanceIds.contains(db))
            .collect(Collectors.toSet());
    final Iterator<String> result =
        filteredReplicas.isEmpty()
            ? this.iteratorBuilder.from(clusterInfo.getReadonlyEndpoint())
            : this.iteratorBuilder.from(filteredReplicas);
    long after = System.currentTimeMillis();
    LOGGER.info(
        "Updated list of replicas in "
            + (after - before)
            + " ms. Found "
            + filteredReplicas.size()
            + " good, active, non-excluded replica"
            + (filteredReplicas.size() != 1 ? "s" : "")
            + " (validation "
            + (fairlinkConfiguration.isValidateConnection() ? "" : "NOT ")
            + "done). Next update in "
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

  public final Iterator<String> init() {
    final Iterator<String> replicasIterator = this.discoverReplicas();
    LOGGER.log(
        Level.INFO,
        String.format(
            "Initialised driver for cluster identified by [%s]. List will be refreshed every [%s]",
            fairlinkConnectionString.getHost(), fairlinkConfiguration.getReplicaPollInterval()));
    return replicasIterator;
  }

  protected String getFallbackReadOnlyEndpoint() {
    return fallbackReadOnlyEndpoint;
  }

  public FairlinkConfiguration getFairlinkConfiguration() {
    return fairlinkConfiguration;
  }
}
