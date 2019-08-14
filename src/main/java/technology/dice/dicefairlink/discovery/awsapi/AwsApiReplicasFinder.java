/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.awsapi;

import com.amazonaws.services.rds.AmazonRDSAsync;
import com.amazonaws.services.rds.AmazonRDSAsyncClient;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBClusterMember;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBClustersRequest;
import com.amazonaws.services.rds.model.DescribeDBClustersResult;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.BaseReadReplicasFinder;
import technology.dice.dicefairlink.discovery.ClusterInfo;
import technology.dice.dicefairlink.discovery.DiscoveryCallback;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AwsApiReplicasFinder extends BaseReadReplicasFinder {
  private static final Logger LOGGER = Logger.getLogger(AwsApiReplicasFinder.class.getName());
  private static final String ACTIVE_STATUS = "available";
  private static final String EXCLUSION_TAG_KEY = "Fairlink-Exclude";
  private final String clusterId;
  private final AmazonRDSAsync client;

  public AwsApiReplicasFinder(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkConnectionString fairlinkConnectionString,
      DiscoveryCallback callback)
      throws SQLException {
    super(fairlinkConfiguration, fairlinkConnectionString, callback);
    this.clusterId = fairlinkConnectionString.getHost();
    LOGGER.log(Level.INFO, "Cluster ID: {0}", fairlinkConnectionString.getHost());
    LOGGER.log(Level.INFO, "AWS Region: {0}", fairlinkConfiguration.getAuroraClusterRegion());
    this.client =
        AmazonRDSAsyncClient.asyncBuilder()
            .withRegion(fairlinkConfiguration.getAuroraClusterRegion().getName())
            .withCredentials(fairlinkConfiguration.getAwsCredentialsProvider())
            .build();
  }

  private Optional<DBCluster> describeCluster() {
    DescribeDBClustersResult describeDBClustersResult =
        client.describeDBClusters(
            new DescribeDBClustersRequest().withDBClusterIdentifier(this.clusterId));
    return describeDBClustersResult.getDBClusters().stream().findFirst();
  }

  @Override
  protected ClusterInfo discoverCluster() {
    Optional<DBCluster> dbClusterOptional = this.describeCluster();
    if (!dbClusterOptional.isPresent()) {
      throw new RuntimeException(
          String.format("Could not find exactly one cluster with cluster id [%s]", this.clusterId));
    }
    DBCluster cluster = dbClusterOptional.get();
    List<String> readerUrls = replicaMembersOf(cluster);

    return new ClusterInfo(cluster.getReaderEndpoint(), readerUrls);
  }

  private List<String> replicaMembersOf(DBCluster cluster) {
    List<DBClusterMember> readReplicas =
        cluster.getDBClusterMembers().stream()
            .filter(member -> !member.isClusterWriter())
            .collect(Collectors.toList());
    List<String> urls = new ArrayList<>(readReplicas.size());
    for (DBClusterMember readReplica : readReplicas) {
      try {
        // the only functionally relevant branch of this iteration's branch is the final "else"
        // (replica has an endpoint
        // and is ACTIvE_STATUS. . All the other cases are for logging/visibility purposes only
        final String dbInstanceIdentifier = readReplica.getDBInstanceIdentifier();
        LOGGER.log(
            Level.FINE,
            String.format(
                "Found read replica in cluster [%s]: [%s])", clusterId, dbInstanceIdentifier));

        DescribeDBInstancesResult describeDBInstancesResult =
            client.describeDBInstances(
                new DescribeDBInstancesRequest().withDBInstanceIdentifier(dbInstanceIdentifier));
        if (describeDBInstancesResult.getDBInstances().size() != 1) {
          LOGGER.log(
              Level.WARNING,
              String.format(
                  "Got [%s] database instances for identifier [%s] (member of cluster [%s]). This is unexpected. Skipping.",
                  describeDBInstancesResult.getDBInstances().size(),
                  dbInstanceIdentifier,
                  clusterId));
        } else {
          DBInstance readerInstance = describeDBInstancesResult.getDBInstances().get(0);

          boolean excluded;
          try {
            final ListTagsForResourceResult listTagsForResourceResult =
                client.listTagsForResource(
                    new ListTagsForResourceRequest()
                        .withResourceName(readerInstance.getDBInstanceArn()));

            excluded =
                listTagsForResourceResult.getTagList().stream()
                    .anyMatch(
                        tag ->
                            tag.getKey().equals(EXCLUSION_TAG_KEY)
                                && Boolean.parseBoolean(tag.getValue()));
          } catch (Exception e) {
            excluded = false;
            LOGGER.log(
                Level.WARNING,
                String.format(
                    "Error fetching tags for replica [%s] of cluster [%]. Will consider it not excluded.",
                    dbInstanceIdentifier, clusterId),
                e);
          }
          Endpoint endpoint = readerInstance.getEndpoint();
          if (!ACTIVE_STATUS.equalsIgnoreCase(readerInstance.getDBInstanceStatus())) {
            LOGGER.warning(
                String.format(
                    "Found [%s] as a replica for [%s] but its status is [%s]. Only replicas with status of [%s] are accepted. Skipping",
                    dbInstanceIdentifier,
                    clusterId,
                    readerInstance.getDBInstanceStatus(),
                    ACTIVE_STATUS));
          } else if (excluded) {
            LOGGER.info(
                String.format(
                    "Found [%s] as a replica for [%s] but it's marked as excluded by the presence of the tag [%s] with value 'true'. Skipping",
                    dbInstanceIdentifier, clusterId, EXCLUSION_TAG_KEY));
          } else if (endpoint == null) {
            LOGGER.log(
                Level.WARNING,
                String.format(
                    "Found [%s] as a replica for [%s] but it does not have a reachable address. Maybe it is still being created. Skipping",
                    dbInstanceIdentifier, clusterId));
          } else {
            final String endPointAddress = endpoint.getAddress();
            LOGGER.log(
                Level.FINE,
                String.format(
                    "Accepted instance with id [%s] with URL=[%s] to cluster [%s]",
                    dbInstanceIdentifier, endPointAddress, clusterId));
            urls.add(endPointAddress);
          }
        }
      } catch (Exception ex) {
        LOGGER.log(
            Level.SEVERE,
            String.format(
                "Got exception when processing [%s] member of [%s]. Skipping.",
                readReplica.getDBInstanceIdentifier(), clusterId),
            ex);
      }
    }
    return urls;
  }
}
