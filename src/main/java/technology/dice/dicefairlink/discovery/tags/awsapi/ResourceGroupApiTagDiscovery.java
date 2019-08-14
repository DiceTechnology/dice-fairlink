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
  private final String replicaEndpointTemplate;

  public ResourceGroupApiTagDiscovery(FairlinkConfiguration fairlinkConfiguration) {
    this.client =
        ResourceGroupsTaggingApiClient.builder()
            .region(fairlinkConfiguration.getAuroraClusterRegion())
            .credentialsProvider(fairlinkConfiguration.getAwsCredentialsProvider())
            .build();
    this.typeFilter = new ArrayList<>(1);
    this.typeFilter.add(RDS_DB_INSTANCE_FILTER);
    this.replicaEndpointTemplate = fairlinkConfiguration.getReplicaEndpointTemplate();
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
      return resourcesPaginator.stream()
          .flatMap(
              p ->
                  p.resourceTagMappingList().stream()
                      .map(e -> e.resourceARN().substring(e.resourceARN().lastIndexOf(":") + 1)))
          .map(id -> String.format(this.replicaEndpointTemplate, id))
          .collect(Collectors.toSet());
    } catch (Exception e) {
      LOGGER.log(
          Level.SEVERE,
          "Failed to obtain excluded instances. All instances assumed not to be excluded",
          e);
      return new HashSet<>(0);
    }
  }
}
