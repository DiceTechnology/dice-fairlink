package technology.dice.dicefairlink.driver;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDSAsync;
import com.amazonaws.services.rds.AmazonRDSAsyncClient;
import com.amazonaws.services.rds.AmazonRDSAsyncClientBuilder;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterMember;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import com.amazonaws.services.rds.model.Tag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import technology.dice.dicefairlink.AuroraReadonlyEndpoint;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.config.ReplicasDiscoveryMode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
public class AuroraReadReplicasSkipExcludedTest {
  @Mock AmazonRDSAsyncClientBuilder mockAmazonRDSAsyncClientBuilder;
  @Mock private AmazonRDSAsync mockAmazonRDSAsync;

  @Before
  public void before() {
    PowerMockito.mockStatic(AmazonRDSAsyncClient.class);
    PowerMockito.when(AmazonRDSAsyncClient.asyncBuilder())
        .thenReturn(mockAmazonRDSAsyncClientBuilder);
    PowerMockito.when(AmazonRDSAsyncClient.asyncBuilder())
        .thenReturn(mockAmazonRDSAsyncClientBuilder);
    Mockito.when(mockAmazonRDSAsyncClientBuilder.withCredentials(any(AWSCredentialsProvider.class)))
        .thenReturn(mockAmazonRDSAsyncClientBuilder);
    Mockito.when(mockAmazonRDSAsyncClientBuilder.withRegion(anyString()))
        .thenReturn(mockAmazonRDSAsyncClientBuilder);
    Mockito.when(mockAmazonRDSAsyncClientBuilder.build()).thenReturn(mockAmazonRDSAsync);

    final DBClusterMember writer =
        new DBClusterMember().withDBInstanceIdentifier("w").withIsClusterWriter(true);
    final DBClusterMember readReplica1Member =
        new DBClusterMember().withDBInstanceIdentifier("rr1").withIsClusterWriter(false);
    final DBClusterMember readReplica2Member =
        new DBClusterMember().withDBInstanceIdentifier("rr2").withIsClusterWriter(false);
    final DBClusterMember readReplica3Member =
        new DBClusterMember().withDBInstanceIdentifier("rr3").withIsClusterWriter(false);
    final DBClusterMember readReplica4Member =
        new DBClusterMember().withDBInstanceIdentifier("rr4").withIsClusterWriter(false);

    Mockito.when(mockAmazonRDSAsync.describeDBClusters(any()))
        .thenReturn(
            new DescribeDBClustersResult()
                .withDBClusters(
                    new DBCluster()
                        .withDBClusterIdentifier("cluster1")
                        .withReaderEndpoint("readOnlyEndpoint")
                        .withDBClusterMembers(
                            writer,
                            readReplica1Member,
                            readReplica2Member,
                            readReplica3Member,
                            readReplica4Member)));
    final DBInstance readReplicaInstance1 =
        new DBInstance()
            .withDBInstanceIdentifier("rr1")
            .withEndpoint(new Endpoint().withAddress("address1"))
            .withDBInstanceArn("rr1")
            .withDBInstanceStatus("available");
    final DBInstance readReplicaInstance2 =
        new DBInstance()
            .withDBInstanceIdentifier("rr2")
            .withEndpoint(new Endpoint().withAddress("address2"))
            .withDBInstanceArn("rr2")
            .withDBInstanceStatus("available");
    final DBInstance readReplicaInstance3 =
        new DBInstance()
            .withDBInstanceIdentifier("rr3")
            .withEndpoint(new Endpoint().withAddress("address3"))
            .withDBInstanceArn("rr3")
            .withDBInstanceStatus("available");
    final DBInstance readReplicaInstance4 =
        new DBInstance()
            .withDBInstanceIdentifier("rr4")
            .withEndpoint(new Endpoint().withAddress("address4"))
            .withDBInstanceArn("rr4")
            .withDBInstanceStatus("available");
    Mockito.when(
            mockAmazonRDSAsync.describeDBInstances(
                Mockito.eq(new DescribeDBInstancesRequest().withDBInstanceIdentifier("rr1"))))
        .thenReturn(new DescribeDBInstancesResult().withDBInstances(readReplicaInstance1));
    Mockito.when(
            mockAmazonRDSAsync.describeDBInstances(
                Mockito.eq(new DescribeDBInstancesRequest().withDBInstanceIdentifier("rr2"))))
        .thenReturn(new DescribeDBInstancesResult().withDBInstances(readReplicaInstance2));
    Mockito.when(
            mockAmazonRDSAsync.describeDBInstances(
                Mockito.eq(new DescribeDBInstancesRequest().withDBInstanceIdentifier("rr3"))))
        .thenReturn(new DescribeDBInstancesResult().withDBInstances(readReplicaInstance3));
    Mockito.when(
            mockAmazonRDSAsync.describeDBInstances(
                Mockito.eq(new DescribeDBInstancesRequest().withDBInstanceIdentifier("rr4"))))
        .thenReturn(new DescribeDBInstancesResult().withDBInstances(readReplicaInstance4));
    Mockito.when(
            mockAmazonRDSAsync.listTagsForResource(
                new ListTagsForResourceRequest().withResourceName("rr1")))
        .thenReturn(
            new ListTagsForResourceResult()
                .withTagList(new Tag().withKey("Fairlink-Exclude").withValue("false")));
    Mockito.when(
            mockAmazonRDSAsync.listTagsForResource(
                new ListTagsForResourceRequest().withResourceName("rr2")))
        .thenReturn(
            new ListTagsForResourceResult()
                .withTagList(new Tag().withKey("Fairlink-Exclude").withValue("true")));
    Mockito.when(
            mockAmazonRDSAsync.listTagsForResource(
                new ListTagsForResourceRequest().withResourceName("rr3")))
        .thenReturn(new ListTagsForResourceResult().withTagList(new ArrayList<>(0)));
    Mockito.when(
            mockAmazonRDSAsync.listTagsForResource(
                new ListTagsForResourceRequest().withResourceName("rr4")))
        .thenReturn(
            new ListTagsForResourceResult()
                .withTagList(new Tag().withKey("Another tag").withValue("another value")));
  }

  @Test
  @PrepareForTest({
    AmazonRDSAsyncClient.class,
    AmazonRDSAsyncClientBuilder.class,
  })
  public void exclusionTagsObserved() {
    final StepByStepExecutor stepByStepExecutor = new StepByStepExecutor(1);
    Properties p = new Properties();
    p.setProperty("", "");
    FairlinkConfiguration fairlinkConfiguration =
        new FairlinkConfiguration(
            Region.getRegion(Regions.AP_NORTHEAST_1),
            new AWSStaticCredentialsProvider(new BasicAWSCredentials("key", "secret")),
            Duration.ofSeconds(10),
            ReplicasDiscoveryMode.RDS_API,
            System.getenv());

    final AuroraReadonlyEndpoint underTest =
        new AuroraReadonlyEndpoint("cluster1", fairlinkConfiguration, stepByStepExecutor);

    Set<String> endpoints = new HashSet<>(3);
    endpoints.add(underTest.getNextReplica());
    endpoints.add(underTest.getNextReplica());
    endpoints.add(underTest.getNextReplica());

    Assert.assertEquals(3, endpoints.size());
    Assert.assertTrue(endpoints.contains("address1"));
    Assert.assertFalse(endpoints.contains("address2"));
    Assert.assertTrue(endpoints.contains("address3"));
    Assert.assertTrue(endpoints.contains("address4"));
  }
}
