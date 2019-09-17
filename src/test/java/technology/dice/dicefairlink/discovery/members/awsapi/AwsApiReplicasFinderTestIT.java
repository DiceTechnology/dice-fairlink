package technology.dice.dicefairlink.discovery.members.awsapi;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.Filter;

public class AwsApiReplicasFinderTestIT {
  // Here to capture the http wire traffic. Not automatically ran as part of CI
  @Test
  public void makeDescribeApiCalls() {
    final RdsClient client =
        RdsClient.builder()
            .region(Region.EU_WEST_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build();

    client.describeDBClusters(
        DescribeDbClustersRequest.builder()
            .dbClusterIdentifier("cluster")
            .build());

    client.describeDBInstances(
        DescribeDbInstancesRequest.builder()
            .filters(
                Filter.builder().name("db-cluster-id").values("cluster").build())
            .build());
  }
}
