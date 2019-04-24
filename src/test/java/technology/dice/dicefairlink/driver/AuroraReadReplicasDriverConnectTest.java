/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.driver;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDSAsync;
import com.amazonaws.services.rds.AmazonRDSAsyncClient;
import com.amazonaws.services.rds.AmazonRDSAsyncClientBuilder;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterMember;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
public class AuroraReadReplicasDriverConnectTest {

  private static final ListTagsForResourceResult EMPTY_TAG_RESULT =
      new ListTagsForResourceResult().withTagList();
  private static final String VALID_JDBC_URL =
      "jdbc:auroraro:postresql://aa:123/db?param1=123&param2=true&param3=abc";
  private static final String VALID_LOW_JDBC_URL =
      "jdbc:postresql://replica-1-ro:123/db?param1=123&param2=true&param3=abc";
  private static final String VALID_ENDPOINT_ADDRESS = "replica-1-ro";

  @Test
  @PrepareForTest({
    DriverManager.class,
    Regions.class,
    AmazonRDSAsyncClient.class,
    AmazonRDSAsyncClientBuilder.class,
    AuroraReadReplicasDriver.class
  })
  public void canConnectToValidUrlBasicAuth() throws Exception {
    final String stubInstanceId = "123";

    final Properties validProperties = new Properties();
    validProperties.put("replicaPollInterval", "1");
    validProperties.put("auroraDiscoveryAuthMode", "basic");
    validProperties.put("auroraDiscoveryKeyId", "TestAwsKey123");
    validProperties.put("auroraDiscoverKeySecret", "TestAwsSecret123");
    validProperties.put("auroraClusterRegion", "eu-west-1");

    final AmazonRDSAsyncClientBuilder mockAmazonRDSAsyncClientBuilder =
        mock(AmazonRDSAsyncClientBuilder.class);
    final AmazonRDSAsync mockAmazonRDSAsync = mock(AmazonRDSAsync.class);
    final DescribeDBClustersResult mockDescribeDBClustersResult =
        mock(DescribeDBClustersResult.class);
    final DBCluster mockDbCluster = mock(DBCluster.class);
    final DBClusterMember mockDbClusterMember = mock(DBClusterMember.class);
    final DescribeDBInstancesResult mockDbInstancesResult = mock(DescribeDBInstancesResult.class);
    final DBInstance mockDbInstance = mock(DBInstance.class);
    final Endpoint mockEndpoint = mock(Endpoint.class);
    final Driver mockDriver = mock(Driver.class);

    PowerMock.mockStatic(DriverManager.class);
    PowerMockito.doNothing().doThrow(Exception.class).when(DriverManager.class);
    EasyMock.expect(DriverManager.getDriver(VALID_LOW_JDBC_URL)).andReturn(mockDriver);
    PowerMock.replay(DriverManager.class);

    PowerMockito.mockStatic(AmazonRDSAsyncClient.class);
    PowerMockito.when(AmazonRDSAsyncClient.asyncBuilder())
        .thenReturn(mockAmazonRDSAsyncClientBuilder);
    Mockito.when(mockAmazonRDSAsyncClientBuilder.withRegion(Regions.EU_WEST_1.getName()))
        .thenReturn(mockAmazonRDSAsyncClientBuilder);
    Mockito.when(mockAmazonRDSAsyncClientBuilder.withCredentials(any(AWSCredentialsProvider.class)))
        .thenReturn(mockAmazonRDSAsyncClientBuilder);
    Mockito.when(mockAmazonRDSAsyncClientBuilder.build()).thenReturn(mockAmazonRDSAsync);
    Mockito.when(mockAmazonRDSAsync.describeDBClusters(any(DescribeDBClustersRequest.class)))
        .thenReturn(mockDescribeDBClustersResult);
    Mockito.when(mockDescribeDBClustersResult.getDBClusters())
        .thenReturn(Arrays.asList(mockDbCluster));
    Mockito.when(mockDbCluster.getDBClusterMembers())
        .thenReturn(Arrays.asList(mockDbClusterMember));
    Mockito.when(mockDbClusterMember.isClusterWriter()).thenReturn(false);
    Mockito.when(mockDbClusterMember.getDBInstanceIdentifier()).thenReturn(stubInstanceId);
    Mockito.when(mockAmazonRDSAsync.describeDBInstances(any(DescribeDBInstancesRequest.class)))
        .thenReturn(mockDbInstancesResult);
    Mockito.when(mockDbInstancesResult.getDBInstances()).thenReturn(Arrays.asList(mockDbInstance));
    Mockito.when(mockDbInstance.getEndpoint()).thenReturn(mockEndpoint);
    Mockito.when(mockDbInstance.getDBInstanceStatus()).thenReturn("available");
    Mockito.when(mockEndpoint.getAddress()).thenReturn(VALID_ENDPOINT_ADDRESS);
    Mockito.when(mockAmazonRDSAsync.listTagsForResource(any())).thenReturn(EMPTY_TAG_RESULT);

    final StepByStepExecutor stepByStepExecutor = new StepByStepExecutor(1);
    AuroraReadReplicasDriver auroraReadReplicasDriver =
        new AuroraReadReplicasDriver(() -> stepByStepExecutor);
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, validProperties);
    stepByStepExecutor.step();

    Mockito.verify(mockDbClusterMember, times(2))
        .isClusterWriter(); // 2 - because of: Init step + execution steps in stepByStepExecutor
    Mockito.verify(mockDbClusterMember, times(2)).getDBInstanceIdentifier();
    Mockito.verify(mockDbInstance, times(2)).getEndpoint();
    Mockito.verify(mockEndpoint, times(2)).getAddress();

    PowerMock.verifyAll();
  }
}
