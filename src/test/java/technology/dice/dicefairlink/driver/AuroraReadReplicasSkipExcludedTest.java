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

import java.sql.Driver;
import java.time.Duration;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
public class AuroraReadReplicasSkipExcludedTest {
  private static final String VALID_JDBC_URL =
      "jdbc:auroraro:mysql://aa:123/db?param1=123&param2=true&param3=abc";
  @Mock AmazonRDSAsyncClientBuilder mockAmazonRDSAsyncClientBuilder;
  @Mock private AmazonRDSAsync mockAmazonRDSAsync;
  @Mock private Driver mockMySqlDriver;

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

    Mockito.when(mockAmazonRDSAsync.describeDBClusters(any()))
        .thenReturn(
            new DescribeDBClustersResult()
                .withDBClusters(
                    new DBCluster()
                        .withDBClusterIdentifier("cluster1")
                        .withDBClusterMembers(
                            writer, readReplica1Member, readReplica2Member, readReplica3Member)));
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
  }

  @Test
  @PrepareForTest({
    AmazonRDSAsyncClient.class,
    AmazonRDSAsyncClientBuilder.class,
  })
  public void exclusionTagsObserved() {
    final StepByStepExecutor stepByStepExecutor = new StepByStepExecutor(1);
    final AuroraReadonlyEndpoint underTest =
        new AuroraReadonlyEndpoint(
            "cluster1",
            new AWSStaticCredentialsProvider(new BasicAWSCredentials("key", "secret")),
            Duration.ofSeconds(10),
            Region.getRegion(Regions.AP_NORTHEAST_1),
            stepByStepExecutor);

    final String oneEndpoint = underTest.getNextReplica();
    final String anotherEndpoint = underTest.getNextReplica();
    Assert.assertNotEquals(oneEndpoint, anotherEndpoint);
    Assert.assertNotEquals(oneEndpoint, "address2");
    Assert.assertNotEquals(anotherEndpoint, "address2");
  }
}
