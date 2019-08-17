/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.tags.awsapi;

import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesRequest;
import software.amazon.awssdk.services.resourcegroupstaggingapi.paginators.GetResourcesIterable;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.tags.ExclusionTag;
import technology.dice.dicefairlink.discovery.tags.TagFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ResourceGroupApiTagDiscovery implements TagFilter {
  private static final Logger LOGGER =
      Logger.getLogger(ResourceGroupApiTagDiscovery.class.getName());
  private static final String RDS_DB_INSTANCE_FILTER = "rds:db";

  private final ResourceGroupsTaggingApiClient client;
  private final Collection<String> typeFilter;

  public ResourceGroupApiTagDiscovery(FairlinkConfiguration fairlinkConfiguration) {
    this.client =
        ResourceGroupsTaggingApiClient.builder()
            .region(fairlinkConfiguration.getAuroraClusterRegion())
            .credentialsProvider(fairlinkConfiguration.getAwsCredentialsProvider())
            .build();
    this.typeFilter = new ArrayList<>(1);
    this.typeFilter.add(RDS_DB_INSTANCE_FILTER);
  }

  @Override
  public Set<String> listExcludedInstances(ExclusionTag tags) {
    try {
      GetResourcesRequest request =
          GetResourcesRequest.builder()
              .resourceTypeFilters(this.typeFilter)
              .tagFilters(
                  software.amazon.awssdk.services.resourcegroupstaggingapi.model.TagFilter.builder()
                      .key(tags.getKey())
                      .values(tags.getValue())
                      .build())
              .build();
      final GetResourcesIterable resourcesPaginator = client.getResourcesPaginator(request);
      final Set<String> excludedDbInstances =
          resourcesPaginator.stream()
              .flatMap(
                  p ->
                      p.resourceTagMappingList().stream()
                          .map(
                              e -> e.resourceARN().substring(e.resourceARN().lastIndexOf(":") + 1)))
              .collect(Collectors.toSet());
      LOGGER.fine(
          "Found "
              + excludedDbInstances.size()
              + " excluded replica"
              + (excludedDbInstances.size() != 1 ? "s" : "")
              + " in the account, across all clusters");
      return excludedDbInstances;
    } catch (Exception e) {
      LOGGER.log(
          Level.SEVERE,
          "Failed to obtain excluded instances. All instances assumed not to be excluded",
          e);
      return new HashSet<>(0);
    }
  }
}
