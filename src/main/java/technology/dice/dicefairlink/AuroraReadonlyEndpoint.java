/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
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
import technology.dice.dicefairlink.iterators.CyclicIterator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AuroraReadonlyEndpoint {
  private static final Logger LOGGER = Logger.getLogger(AuroraReadonlyEndpoint.class.getName());
  private static final String ACTIVE_STATUS = "available";
  private final Duration pollerInterval;
  private CyclicIterator<String> replicas;
  private String readOnlyEndpoint;

  public AuroraReadonlyEndpoint(
      String clusterId,
      AWSCredentialsProvider credentialsProvider,
      Duration pollerInterval,
      Regions region,
      ScheduledExecutorService executor) {
    AuroraReplicasFinder finder = new AuroraReplicasFinder(clusterId, credentialsProvider, region);
    this.pollerInterval = pollerInterval;
    finder.init();
    executor.scheduleAtFixedRate(
        finder, pollerInterval.getSeconds(), pollerInterval.getSeconds(), TimeUnit.SECONDS);
  }

  public String getNextReplica() {
    try {
      return this.replicas.next();
    } catch (NoSuchElementException e) {
      LOGGER.warning(
          String.format(
              "Could not find any read replicas. Returning the read only endpoint ([%s]) to fallback on Aurora balancing",
              this.readOnlyEndpoint));
      return readOnlyEndpoint;
    }
  }

  public class AuroraReplicasFinder implements Runnable {
    private final AmazonRDSAsync client;
    private final String clusterId;

    public AuroraReplicasFinder(
        String clusterId, AWSCredentialsProvider credentialsProvider, Regions region) {
      this.clusterId = clusterId;
      this.client =
          AmazonRDSAsyncClient.asyncBuilder()
              .withRegion(region)
              .withCredentials(credentialsProvider)
              .build();
    }

    private Optional<DBCluster> describeCluster() {
      DescribeDBClustersResult describeDBClustersResult =
          client.describeDBClusters(
              new DescribeDBClustersRequest().withDBClusterIdentifier(this.clusterId));
      return describeDBClustersResult.getDBClusters().stream().findFirst();
    }

    private List<String> replicaMembersOf(DBCluster cluster) {
      List<DBClusterMember> readReplicas =
          cluster
              .getDBClusterMembers()
              .stream()
              .filter(member -> !member.isClusterWriter())
              .collect(Collectors.toList());
      List<String> urls = new ArrayList<>(readReplicas.size());
      for (DBClusterMember readReplica : readReplicas) {
        LOGGER.log(
            Level.FINE,
            String.format(
                "Found read replica in cluster [%s]: [%s])",
                clusterId, readReplica.getDBInstanceIdentifier()));

        DescribeDBInstancesResult describeDBInstancesResult =
            client.describeDBInstances(
                new DescribeDBInstancesRequest()
                    .withDBInstanceIdentifier(readReplica.getDBInstanceIdentifier()));
        if (describeDBInstancesResult.getDBInstances().size() != 1) {
          LOGGER.log(
              Level.WARNING,
              String.format(
                  "Got [%s] database instances for identifier [%s] (member of cluster [%s]). This is unexpected. Skipping.",
                  describeDBInstancesResult.getDBInstances().size(),
                  readReplica.getDBInstanceIdentifier(),
                  clusterId));
        } else {
          DBInstance readerInstance = describeDBInstancesResult.getDBInstances().get(0);
          Endpoint endpoint = readerInstance.getEndpoint();
          if (!ACTIVE_STATUS.equalsIgnoreCase(readerInstance.getDBInstanceStatus())) {
            LOGGER.warning(
                String.format(
                    "Found [%s] as a replica for [%s] but its status is [%s]. Only replicas with status of [%s] are accepted. Skipping",
                    readReplica.getDBInstanceIdentifier(),
                    clusterId,
                    readerInstance.getDBInstanceStatus(),
                    ACTIVE_STATUS));
          } else if (endpoint == null) {
            LOGGER.warning(
                String.format(
                    "Found [%s] as a replica for [%s] but it does not have a reachable address. Maybe it is still being created. Skipping",
                    readReplica.getDBInstanceIdentifier(), clusterId));

          } else {
            LOGGER.log(
                Level.FINE,
                String.format(
                    "Accepted instance with id [%s] with URL=[%s] to cluster [%s]",
                    readReplica.getDBInstanceIdentifier(), endpoint.getAddress(), clusterId));
            urls.add(endpoint.getAddress());
          }
        }
      }
      return urls;
    }

    @Override
    public void run() {
      try {
        Optional<DBCluster> dbClusterOptional = this.describeCluster();
        if (!dbClusterOptional.isPresent()) {
          LOGGER.warning(
              String.format(
                  "Could not retrieve cluster information for cluster [%s]. Will fallback to [%s] until individual members can be retrieved again",
                  clusterId, readOnlyEndpoint));
          return;
        }
        List<String> readerUrls =
            dbClusterOptional.map(cluster -> replicaMembersOf(cluster)).orElse(new ArrayList<>(0));
        replicas = CyclicIterator.of(readerUrls);
        if (readerUrls.size() == 0) {
          LOGGER.warning(
              String.format(
                  "No read replicas found for cluster [%s]. Will fallback to [%s] until individual members can be retrieved again",
                  clusterId, readOnlyEndpoint));
        }
        LOGGER.info(
            String.format(
                "Retrieved [%s] read replicas for cluster id [%s] with. List will be refreshed in [%s] seconds",
                readerUrls.size(), clusterId, pollerInterval.getSeconds()));
      } catch (Exception e) {
        LOGGER.log(
            Level.SEVERE,
            String.format(
                "Exception while refreshing list of read replicas from cluster [%s]. Skipping",
                clusterId),
            e);
      }
    }

    public void init() {
      Optional<DBCluster> dbClusterOptional = this.describeCluster();
      if (!dbClusterOptional.isPresent()) {
        throw new RuntimeException(
            String.format(
                "Could not find exactly one cluster with cluster id [%s]", this.clusterId));
      }
      DBCluster cluster = dbClusterOptional.get();
      readOnlyEndpoint = cluster.getReaderEndpoint();
      List<String> readerUrls = replicaMembersOf(cluster);
      replicas = CyclicIterator.of(readerUrls);
      LOGGER.info(
          String.format(
              "Initialized driver for cluster id [%s] with [%s] read replicas. List will be refreshed every [%s] seconds",
              clusterId, readerUrls.size(), pollerInterval.getSeconds()));
    }
  }
}
