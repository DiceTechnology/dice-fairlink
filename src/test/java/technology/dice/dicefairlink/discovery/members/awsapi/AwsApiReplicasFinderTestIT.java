package technology.dice.dicefairlink.discovery.members.awsapi;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;

public class AwsApiReplicasFinderTestIT {

  @Before
  public void before() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    Logger rootLogger = LogManager.getLogManager().getLogger("");
    rootLogger.setLevel(Level.FINEST);
    for (Handler h : rootLogger.getHandlers()) {
      h.setLevel(Level.INFO);
    }
  }

  // Here to capture the http wire traffic. Not automatically ran as part of CI
  @Test
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
