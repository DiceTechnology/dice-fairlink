/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members.awsapi;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.awssdk.services.rds.model.DBCluster;
import software.amazon.awssdk.services.rds.model.DBClusterMember;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersResponse;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

public class AwsApiReplicasFinder implements MemberFinderMethod {
  private static final Logger LOGGER = Logger.getLogger(AwsApiReplicasFinder.class.getName());
  private final String clusterId;
  private final RdsClient client;

  public AwsApiReplicasFinder(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkConnectionString fairlinkConnectionString) {

    this.clusterId = fairlinkConnectionString.getHost();
    LOGGER.log(Level.INFO, "Cluster ID: {0}", fairlinkConnectionString.getHost());
    LOGGER.log(Level.INFO, "AWS Region: {0}", fairlinkConfiguration.getAuroraClusterRegion());
    final RdsClientBuilder clientBuilder =
        RdsClient.builder()
            .region(fairlinkConfiguration.getAuroraClusterRegion())
            .credentialsProvider(fairlinkConfiguration.getAwsCredentialsProvider());
    fairlinkConfiguration
        .getAwsEndpointOverride()
        .ifPresent(o -> clientBuilder.endpointOverride(URI.create(o)));
    this.client = clientBuilder.build();
  }

  private DBCluster describeCluster(String clusterId) {
    final DescribeDbClustersResponse describeDbClustersResponse =
        client.describeDBClusters(
            DescribeDbClustersRequest.builder().dbClusterIdentifier(clusterId).build());
    return describeDbClustersResponse.dbClusters().stream()
        .findFirst()
        .orElseThrow(
            () ->
                new RuntimeException(
                    String.format(
                        "Could not find exactly one cluster with cluster id [%s]", clusterId)));
  }

  private Set<String> replicaMembersOf(DBCluster cluster) {
    final Optional<DBClusterMember> writer =
        cluster.dbClusterMembers().stream().filter(member -> member.isClusterWriter()).findAny();
    final Set<String> replicaIds =
        cluster.dbClusterMembers().stream()
            .filter(dbClusterMember -> isDbWriter(writer, dbClusterMember))
            .map(dbClusterMember -> dbClusterMember.dbInstanceIdentifier())
            .collect(Collectors.toSet());

    return replicaIds;
  }

  private static boolean isDbWriter(Optional<DBClusterMember> writer, DBClusterMember dbInstance) {
    return !writer
        .map(w -> w.dbInstanceIdentifier().equalsIgnoreCase(dbInstance.dbInstanceIdentifier()))
        .orElse(false);
  }

  @Override
  public ClusterInfo discoverCluster() {
    final DBCluster cluster = this.describeCluster(this.clusterId);
    return new ClusterInfo(cluster.readerEndpoint(), replicaMembersOf(cluster));
  }
}
