/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
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
import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;

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
  private RandomisedCyclicIterator<String> replicas;
  private String readOnlyEndpoint;

  public AuroraReadonlyEndpoint(
      String clusterId,
      AWSCredentialsProvider credentialsProvider,
      Duration pollerInterval,
      Region region,
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
      LOGGER.log(
          Level.WARNING,
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
        String clusterId, AWSCredentialsProvider credentialsProvider, Region region) {
      this.clusterId = clusterId;
      LOGGER.log(Level.INFO, "Cluster ID: {0}", clusterId);
      LOGGER.log(Level.INFO, "AWS Region: {0}", region);
      this.client =
          AmazonRDSAsyncClient.asyncBuilder()
              .withRegion(region.getName())
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
          Endpoint endpoint = readerInstance.getEndpoint();
          if (!ACTIVE_STATUS.equalsIgnoreCase(readerInstance.getDBInstanceStatus())) {
            LOGGER.warning(
                String.format(
                    "Found [%s] as a replica for [%s] but its status is [%s]. Only replicas with status of [%s] are accepted. Skipping",
                    dbInstanceIdentifier,
                    clusterId,
                    readerInstance.getDBInstanceStatus(),
                    ACTIVE_STATUS));
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
      }
      return urls;
    }

    @Override
    public void run() {
      try {
        Optional<DBCluster> dbClusterOptional = this.describeCluster();
        if (!dbClusterOptional.isPresent()) {
          LOGGER.log(
              Level.WARNING,
              String.format(
                  "Could not retrieve cluster information for cluster [%s]. Will fallback to [%s] until individual members can be retrieved again",
                  clusterId, readOnlyEndpoint));
          return;
        }
        List<String> readerUrls =
            dbClusterOptional.map(cluster -> replicaMembersOf(cluster)).orElse(new ArrayList<>(0));
        if (replicas.hasSameContent(readerUrls)) {
          return;
        }
        if (readerUrls.isEmpty()) {
          LOGGER.log(
              Level.WARNING,
              "No read replicas found for cluster [{0}]. Will fallback to [{1}] until individual members can be retrieved again",
              new Object[]{clusterId, readOnlyEndpoint});
        }
        replicas = RandomisedCyclicIterator.of(readerUrls);
        if (LOGGER.isLoggable(Level.FINE)) {
          LOGGER.log(
              Level.FINE,
              String.format(
                  "Retrieved [%s] read replicas for cluster id [%s] with. List will be refreshed in [%s] seconds",
                  readerUrls.size(), clusterId, pollerInterval.getSeconds()));
        }
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
      replicas = RandomisedCyclicIterator.of(readerUrls);
      LOGGER.log(
          Level.INFO,
          String.format(
              "Initialized driver for cluster id [%s] with [%s] read replicas. List will be refreshed every [%s] seconds",
              clusterId, readerUrls.size(), pollerInterval.getSeconds()));
    }
  }
}
