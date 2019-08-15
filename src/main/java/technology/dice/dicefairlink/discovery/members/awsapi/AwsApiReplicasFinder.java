/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members.awsapi;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBCluster;
import software.amazon.awssdk.services.rds.model.DBClusterMember;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.Endpoint;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AwsApiReplicasFinder implements MemberFinderMethod {
  private static final Logger LOGGER = Logger.getLogger(AwsApiReplicasFinder.class.getName());
  private static final String ACTIVE_STATUS = "available";
  private final String clusterId;
  private final RdsClient client;

  public AwsApiReplicasFinder(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkConnectionString fairlinkConnectionString) {

    this.clusterId = fairlinkConnectionString.getHost();
    LOGGER.log(Level.INFO, "Cluster ID: {0}", fairlinkConnectionString.getHost());
    LOGGER.log(Level.INFO, "AWS Region: {0}", fairlinkConfiguration.getAuroraClusterRegion());
    this.client =
        RdsClient.builder()
            .region(fairlinkConfiguration.getAuroraClusterRegion())
            .credentialsProvider(fairlinkConfiguration.getAwsCredentialsProvider())
            .build();
  }

  private Optional<DBCluster> describeCluster() {
    final DescribeDbClustersResponse describeDbClustersResponse =
        client.describeDBClusters(
            DescribeDbClustersRequest.builder().dbClusterIdentifier(this.clusterId).build());
    return describeDbClustersResponse.dbClusters().stream().findFirst();
  }

  @Override
  public ClusterInfo discoverCluster() {
    Optional<DBCluster> dbClusterOptional = this.describeCluster();
    if (!dbClusterOptional.isPresent()) {
      throw new RuntimeException(
          String.format("Could not find exactly one cluster with cluster id [%s]", this.clusterId));
    }
    DBCluster cluster = dbClusterOptional.get();
    List<String> readerUrls = replicaMembersOf(cluster);

    return new ClusterInfo(cluster.readerEndpoint(), readerUrls);
  }

  private List<String> replicaMembersOf(DBCluster cluster) {
    List<DBClusterMember> readReplicas =
        cluster.dbClusterMembers().stream()
            .filter(member -> !member.isClusterWriter())
            .collect(Collectors.toList());
    List<String> urls = new ArrayList<>(readReplicas.size());
    for (DBClusterMember readReplica : readReplicas) {
      try {
        // the only functionally relevant branch of this iteration's branch is the final "else"
        // (replica has an endpoint
        // and is ACTIvE_STATUS. . All the other cases are for logging/visibility purposes only
        final String dbInstanceIdentifier = readReplica.dbInstanceIdentifier();
        LOGGER.log(
            Level.FINE,
            String.format(
                "Found read replica in cluster [%s]: [%s])", clusterId, dbInstanceIdentifier));

        DescribeDbInstancesResponse describeDBInstancesResult =
            client.describeDBInstances(
                DescribeDbInstancesRequest.builder()
                    .dbInstanceIdentifier(dbInstanceIdentifier)
                    .build());
        if (describeDBInstancesResult.dbInstances().size() != 1) {
          LOGGER.log(
              Level.WARNING,
              String.format(
                  "Got [%s] database instances for identifier [%s] (member of cluster [%s]). This is unexpected. Skipping.",
                  describeDBInstancesResult.dbInstances().size(), dbInstanceIdentifier, clusterId));
        } else {
          DBInstance readerInstance = describeDBInstancesResult.dbInstances().get(0);

          Endpoint endpoint = readerInstance.endpoint();
          if (!ACTIVE_STATUS.equalsIgnoreCase(readerInstance.dbInstanceStatus())) {
            LOGGER.warning(
                String.format(
                    "Found [%s] as a replica for [%s] but its status is [%s]. Only replicas with status of [%s] are accepted. Skipping",
                    dbInstanceIdentifier,
                    clusterId,
                    readerInstance.dbInstanceStatus(),
                    ACTIVE_STATUS));
          } else if (endpoint == null) {
            LOGGER.log(
                Level.WARNING,
                String.format(
                    "Found [%s] as a replica for [%s] but it does not have a reachable address. Maybe it is still being created. Skipping",
                    dbInstanceIdentifier, clusterId));
          } else {
            final String endPointAddress = endpoint.address();
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
                readReplica.dbInstanceIdentifier(), clusterId),
            ex);
      }
    }
    return urls;
  }
}
