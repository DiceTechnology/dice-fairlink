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
import technology.dice.dicefairlink.iterators.SizedIterator;

import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FairlinkMemberFinder {
  private static final ExclusionTag EXCLUSION_TAG = new ExclusionTag("Fairlink-Exclude", "true");
  private static final Logger LOGGER = Logger.getLogger(FairlinkMemberFinder.class.getName());
  private final FairlinkConfiguration fairlinkConfiguration;
  private final MemberFinderMethod memberFinder;
  private final ReplicaValidator replicaValidator;
  private final Function<Collection<String>, SizedIterator<String>> iteratorBuilder;
  protected final FairlinkConnectionString fairlinkConnectionString;
  protected final TagFilter tagFilter;
  protected Collection<String> excludedInstanceIds = new HashSet<>(0);

  public FairlinkMemberFinder(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkConnectionString fairlinkConnectionString,
      ScheduledExecutorService tagsPollingExecutor,
      TagFilter excludedInstancesFinder,
      MemberFinderMethod memberFinder,
      Function<Collection<String>, SizedIterator<String>> stringSizedIteratorBuilder,
      ReplicaValidator replicaValidator) {
    this.fairlinkConnectionString = fairlinkConnectionString;
    this.fairlinkConfiguration = fairlinkConfiguration;
    this.tagFilter = excludedInstancesFinder;
    this.memberFinder = memberFinder;
    this.replicaValidator = replicaValidator;
    final Duration startJitter = fairlinkConfiguration.randomBoundDelay();
    this.iteratorBuilder = stringSizedIteratorBuilder;
    LOGGER.info("Starting excluded members discovery with " + startJitter + " delay.");
    tagsPollingExecutor.scheduleAtFixedRate(
        () -> excludedInstanceIds = tagFilter.listExcludedInstances(EXCLUSION_TAG),
        startJitter.getSeconds(),
        fairlinkConfiguration.getTagsPollerInterval().getSeconds(),
        TimeUnit.SECONDS);
  }

  public final SizedIterator<String> discoverReplicas() {
    long before = System.currentTimeMillis();
    final ClusterInfo clusterInfo = this.memberFinder.discoverCluster();
    final Set<String> filteredReplicas =
        clusterInfo.getReplicas().stream()
            .filter(
                dbIdentifier ->
                    (!this.fairlinkConfiguration.isValidateConnection())
                        || this.validate(fairlinkConfiguration.hostname(dbIdentifier)))
            .filter(db -> !excludedInstanceIds.contains(db))
            .map(dbIdentifier -> fairlinkConfiguration.hostname(dbIdentifier))
            .collect(Collectors.toSet());
    final SizedIterator<String> result =
        filteredReplicas.isEmpty()
            ? this.iteratorBuilder.apply(this.setOf(clusterInfo.getReadonlyEndpoint()))
            : this.iteratorBuilder.apply(filteredReplicas);
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

  private Set<String> setOf(String entry) {
    Set<String> set = new HashSet<>(1);
    set.add(entry);
    return set;
  }

  private boolean validate(String host) {
    try {
      return this.replicaValidator.isValid(
          fairlinkConnectionString.delegateConnectionString(host),
          fairlinkConnectionString.getProperties());
    } catch (URISyntaxException e) {
      return false;
    }
  }

  public final Iterator<String> init() {
    this.excludedInstanceIds = this.tagFilter.listExcludedInstances(EXCLUSION_TAG);
    final SizedIterator<String> replicasIterator = this.discoverReplicas();
    LOGGER.log(
        Level.INFO,
        String.format(
            "Initialised driver for cluster identified by [%s with [%d] replicas]. List will be refreshed every [%s]",
            fairlinkConnectionString.getHost(),
            replicasIterator.size(),
            fairlinkConfiguration.getReplicaPollInterval()));
    return replicasIterator;
  }
}
