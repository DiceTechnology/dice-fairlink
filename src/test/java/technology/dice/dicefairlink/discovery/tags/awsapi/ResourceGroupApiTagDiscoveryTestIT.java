package technology.dice.dicefairlink.discovery.tags.awsapi;

import com.google.common.collect.ImmutableList;
import java.util.stream.Collectors;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.resourcegroupstaggingapi.ResourceGroupsTaggingApiClient;
import software.amazon.awssdk.services.resourcegroupstaggingapi.model.GetResourcesRequest;

public class ResourceGroupApiTagDiscoveryTestIT {

  // Here to capture the http wire traffic. Not automatically ran as part of CI
  @Test
  public void makeAPicall() {
    final ResourceGroupsTaggingApiClient client =
        ResourceGroupsTaggingApiClient.builder()
            .region(Region.EU_WEST_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build();

    GetResourcesRequest request =
        GetResourcesRequest.builder()
            .resourceTypeFilters(ImmutableList.of("rds:db"))
            .tagFilters(
                software.amazon.awssdk.services.resourcegroupstaggingapi.model.TagFilter.builder()
                    .key("Fairlink-Exclude")
                    .values("true")
                    .build())
            .build();

    client.getResourcesPaginator(request).stream().collect(Collectors.toSet());
  }
}
