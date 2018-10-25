/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.driver;

import static org.mockito.ArgumentMatchers.any;

import static org.powermock.api.mockito.PowerMockito.mock;
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
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Properties;
import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.Collections;
import technology.dice.dicefairlink.driver.AuroraReadReplicasDriverConnectTest.StepByStepExecutor;

@RunWith(PowerMockRunner.class)
public class AuroraReadReplicasDriverSimulateClusterRecoveryTest {

  private static final String VALID_JDBC_URL =
      "jdbc:auroraro:mysql://aa:123/db?param1=123&param2=true&param3=abc";
  private static final String VALID_JDBC_CLUSTER_RO_ENDPOINT_URL =
      "jdbc:mysql://aa-ro:123/db?param1=123&param2=true&param3=abc";
  private static final String VALID_ENDPOINT_ADDRESS_A = "replica-1-ro";
  private static final String VALID_ENDPOINT_ADDRESS_B = "replica-2-ro";

  @Test
  @PrepareForTest({
    DriverManager.class,
    Regions.class,
    AmazonRDSAsyncClient.class,
    AmazonRDSAsyncClientBuilder.class,
    AuroraReadOnlyDriver.class
  })
  public void canConnectToValidUrlBasicAuth_whenOnlyMasterIsAvailableThenListOfReplicasChanges() throws Exception {
    final String stubInstanceId_A = "123";
    final String stubInstanceId_B = "345";

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
    final DBClusterMember mockDbClusterMember_A = mock(DBClusterMember.class);
    final DBClusterMember mockDbClusterMember_B = mock(DBClusterMember.class);
    final DescribeDBInstancesResult mockDbInstancesResult_A = mock(DescribeDBInstancesResult.class);
    final DescribeDBInstancesResult mockDbInstancesResult_B = mock(DescribeDBInstancesResult.class);
    final DBInstance mockDbInstance_A = mock(DBInstance.class);
    final DBInstance mockDbInstance_B = mock(DBInstance.class);
    final Endpoint mockEndpoint_A = mock(Endpoint.class);
    final Endpoint mockEndpoint_B = mock(Endpoint.class);
    final Driver mockMySqlDriver = mock(Driver.class);

    PowerMock.mockStatic(DriverManager.class);
    DriverManager.registerDriver(EasyMock.anyObject(AuroraReadOnlyDriver.class));
    PowerMock.expectLastCall();
    EasyMock.expect(DriverManager.getDriver(VALID_JDBC_CLUSTER_RO_ENDPOINT_URL)).andReturn(mockMySqlDriver); // once driver is decided for the delegated URL type (be it MySQL, PostgreSQL etc) it will not change.
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
        .thenReturn(Collections.emptyList())
        .thenReturn(Collections.emptyList())
        .thenReturn(Arrays.asList(mockDbClusterMember_A, mockDbClusterMember_B));

    Mockito.when(mockDbClusterMember_A.isClusterWriter()).thenReturn(false);
    Mockito.when(mockDbClusterMember_A.getDBInstanceIdentifier()).thenReturn(stubInstanceId_A);
    Mockito.when(mockDbClusterMember_B.isClusterWriter()).thenReturn(false);
    Mockito.when(mockDbClusterMember_B.getDBInstanceIdentifier()).thenReturn(stubInstanceId_B);

    Mockito.when(mockAmazonRDSAsync.describeDBInstances(Mockito.any(DescribeDBInstancesRequest.class)))
        .thenReturn(mockDbInstancesResult_A)
        .thenReturn(mockDbInstancesResult_B);
    Mockito.when(mockDbInstancesResult_A.getDBInstances())
        .thenReturn(Arrays.asList(mockDbInstance_A));
    Mockito.when(mockDbInstancesResult_B.getDBInstances())
        .thenReturn(Arrays.asList(mockDbInstance_B));

    Mockito.when(mockDbInstance_A.getEndpoint()).thenReturn(mockEndpoint_A);
    Mockito.when(mockDbInstance_A.getDBInstanceStatus()).thenReturn("available");
    Mockito.when(mockDbInstance_B.getEndpoint()).thenReturn(mockEndpoint_B);
    Mockito.when(mockDbInstance_B.getDBInstanceStatus()).thenReturn("available");
    Mockito.when(mockEndpoint_A.getAddress()).thenReturn(VALID_ENDPOINT_ADDRESS_A);
    Mockito.when(mockEndpoint_B.getAddress()).thenReturn(VALID_ENDPOINT_ADDRESS_B);

    Mockito.when(mockDbCluster.getReaderEndpoint())
        .thenReturn(VALID_JDBC_CLUSTER_RO_ENDPOINT_URL);

    final StepByStepExecutor stepByStepExecutor = new StepByStepExecutor(1);
    AuroraReadOnlyDriver auroraReadReplicasDriver =
        new AuroraReadOnlyDriver(stepByStepExecutor);
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, validProperties);
    stepByStepExecutor.step();
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, validProperties);
    stepByStepExecutor.step();
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, validProperties);

    PowerMock.verifyAll();
  }
}
