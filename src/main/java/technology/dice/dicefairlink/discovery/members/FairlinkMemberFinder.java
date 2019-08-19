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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FairlinkMemberFinder implements MemberFinder {
  private static final Logger LOGGER = Logger.getLogger(FairlinkMemberFinder.class.getName());
  private static final ExclusionTag EXCLUSION_TAG = new ExclusionTag("Fairlink-Exclude", "true");
  private static final Set<String> EMPTY_SET = Collections.unmodifiableSet(new HashSet<>(0));

  private final FairlinkConfiguration fairlinkConfiguration;
  private final MemberFinderMethod memberFinder;
  private final ReplicaValidator replicaValidator;
  private final Function<Collection<String>, SizedIterator<String>> iteratorBuilder;
  protected final FairlinkConnectionString fairlinkConnectionString;
  protected final TagFilter tagFilter;
  protected Optional<String> fallbackEndpoint = Optional.empty();
  protected Collection<String> excludedInstanceIds =
      Collections.unmodifiableCollection(new HashSet<>(0));

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
        () -> excludedInstanceIds = safeExclusionsDiscovery(),
        startJitter.getSeconds(),
        fairlinkConfiguration.getTagsPollerInterval().getSeconds(),
        TimeUnit.SECONDS);
  }

  private Set<String> safeExclusionsDiscovery() {
    try {
      return tagFilter.listExcludedInstances(EXCLUSION_TAG);
    } catch (Exception e) {
      LOGGER.log(
          Level.SEVERE, "Could not discover exclusions; including all discovered instances", e);
      return EMPTY_SET;
    }
  }

  public final SizedIterator<String> discoverReplicas() {
    try {
      long before = System.currentTimeMillis();
      final ClusterInfo clusterInfo = this.memberFinder.discoverCluster();
      this.fallbackEndpoint =
          Optional.of(
              fairlinkConfiguration
                  .getFallbackEndpoint()
                  .orElse(clusterInfo.getReadonlyEndpoint()));
      final Set<String> filteredReplicas =
          clusterInfo.getReplicas().stream()
              .filter(db -> !excludedInstanceIds.contains(db))
              .filter(
                  dbIdentifier ->
                      (!this.fairlinkConfiguration.isValidateConnection())
                          || this.validate(fairlinkConfiguration.hostname(dbIdentifier)))
              .map(dbIdentifier -> fairlinkConfiguration.hostname(dbIdentifier))
              .collect(Collectors.toSet());
      final SizedIterator<String> result =
          filteredReplicas.isEmpty()
              ? this.iteratorBuilder.apply(
                  this.setOf(this.fallbackEndpoint.orElse(clusterInfo.getReadonlyEndpoint())))
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
              + "done). Excluded "
              + this.excludedInstanceIds.size()
              + " instance"
              + (this.excludedInstanceIds.size() != 1 ? "" : "s")
              + ". Next update in "
              + this.fairlinkConfiguration.getReplicaPollInterval());
      return result;
    } catch (Exception e) {
      LOGGER.log(
          Level.WARNING,
          "Error discovering cluster identified by ["
              + this.fairlinkConnectionString.getFairlinkUri()
              + "]. Will return fallback endpoint"
              + this.fallbackEndpoint
              + " if available",
          e);
      if (!this.fallbackEndpoint.isPresent()) {
        LOGGER.log(
            Level.SEVERE,
            "Fallback endpoint not available. This means the cluster has never been successfully discovered. This is probably a permanent error condition");
      }
      return fallbackEndpoint
          .map(fallbackEndpoint -> this.iteratorBuilder.apply(this.setOf(fallbackEndpoint)))
          .orElseThrow(
              () -> {
                LOGGER.log(
                    Level.SEVERE,
                    "Fallback endpoint not available. This means the cluster has never been successfully discovered. This is probably a permanent error condition");
                return new RuntimeException(
                    "Could not discover cluster identified by ["
                        + fairlinkConnectionString.getFairlinkUri()
                        + "] and a fallback reader endpoint is not available");
              });
    }
  }

  private Set<String> setOf(String entry) {
    Set<String> set = new HashSet<>(1);
    set.add(entry);
    return Collections.unmodifiableSet(set);
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

  public final SizedIterator<String> init() {
    this.excludedInstanceIds = safeExclusionsDiscovery();
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
