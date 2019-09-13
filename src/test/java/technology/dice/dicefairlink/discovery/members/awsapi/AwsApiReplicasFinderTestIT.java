package technology.dice.dicefairlink.discovery.members.awsapi;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;

public class AwsApiReplicasFinderTestIT {
  // Here to capture the http wire traffic. Not automatically ran as part of CI
//  @Test
  public void makeDescribeClusterApiCall() {
    final RdsClient client =
        RdsClient.builder()
            .region(Region.EU_WEST_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build();

    client.describeDBClusters(
        DescribeDbClustersRequest.builder().dbClusterIdentifier("sample-cluster").build());
  }
}
