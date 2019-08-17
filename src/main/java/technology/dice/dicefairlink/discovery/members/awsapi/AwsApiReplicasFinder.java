/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members.awsapi;

import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBCluster;
import software.amazon.awssdk.services.rds.model.DBClusterMember;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbClustersResponse;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.Filter;
import software.amazon.awssdk.services.rds.paginators.DescribeDBInstancesIterable;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AwsApiReplicasFinder implements MemberFinderMethod {
  private static final Logger LOGGER = Logger.getLogger(AwsApiReplicasFinder.class.getName());
  private static final String ACTIVE_STATUS = "available";
  private static final Set<String> EMPTY_SET = Collections.unmodifiableSet(new HashSet<>(0));
  public static final String DB_CLUSTER_ID_FILTER = "db-cluster-id";
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
    try {
      DescribeDbInstancesRequest request =
          DescribeDbInstancesRequest.builder()
              .filters(
                  Filter.builder()
                      .name(DB_CLUSTER_ID_FILTER)
                      .values(cluster.dbClusterIdentifier())
                      .build())
              .build();
      final Optional<DBClusterMember> writer =
          cluster.dbClusterMembers().stream().filter(member -> member.isClusterWriter()).findAny();
      final DescribeDBInstancesIterable describeDbInstancesResponses =
          client.describeDBInstancesPaginator(request);
      final Set<String> replicaIds =
          describeDbInstancesResponses.stream()
              .flatMap(
                  dbInstances ->
                      dbInstances.dbInstances().stream()
                          .filter(
                              dbInstance ->
                                  dbInstance.dbInstanceStatus().equalsIgnoreCase(ACTIVE_STATUS))
                          .filter(
                              dbInstance ->
                                  !writer
                                      .map(
                                          w ->
                                              w.dbInstanceIdentifier()
                                                  .equalsIgnoreCase(
                                                      dbInstance.dbInstanceIdentifier()))
                                      .orElse(false))
                          .map(dbInstance -> dbInstance.dbInstanceIdentifier()))
              .collect(Collectors.toSet());

      return replicaIds;

    } catch (Exception e) {
      LOGGER.log(
          Level.SEVERE, "Failed to list cluster replicas. Returning an empty set of replicas", e);
      return EMPTY_SET;
    }
  }

  @Override
  public ClusterInfo discoverCluster() {
    final DBCluster cluster = this.describeCluster(this.clusterId);
    return new ClusterInfo(cluster.readerEndpoint(), replicaMembersOf(cluster));
  }
}
