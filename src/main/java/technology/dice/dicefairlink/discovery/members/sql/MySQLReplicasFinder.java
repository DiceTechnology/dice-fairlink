/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members.sql;

import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MySQLReplicasFinder implements MemberFinderMethod {
  private static final Logger LOGGER = Logger.getLogger(MySQLReplicasFinder.class.getName());
  private static final Set<DatabaseInstance> EMPTY_SET =
      Collections.unmodifiableSet(new HashSet<>(0));
  private static final String DEFAULT_INFORMATION_SCHEMA_NAME = "information_schema";
  private static final String FIND_NODES_QUERY_TEMPLATE =
      "select server_id, if(session_id =    'MASTER_SESSION_ID',"
          + "'WRITER', 'READER') as role from "
          + "%s.replica_host_status;";
  private final Driver driverForDelegate;
  private final FairlinkConnectionString fairlinkConnectionString;
  private final String informationSchemaName;

  public MySQLReplicasFinder(
      FairlinkConnectionString fairlinkConnectionString,
      Driver driverForDelegate,
      String informationSchemaName) {
    this.driverForDelegate = driverForDelegate;
    this.fairlinkConnectionString = fairlinkConnectionString;
    this.informationSchemaName =
        Optional.ofNullable(informationSchemaName).orElse(DEFAULT_INFORMATION_SCHEMA_NAME);
  }

  protected Set<DatabaseInstance> findReplicas() {
    Set<DatabaseInstance> instances = new HashSet<>();
    try (final Connection c =
            this.driverForDelegate.connect(
                fairlinkConnectionString.delegateConnectionString(),
                fairlinkConnectionString.getProperties());
        final ResultSet resultSet =
            c.createStatement()
                .executeQuery(
                    String.format(FIND_NODES_QUERY_TEMPLATE, this.informationSchemaName))) {
      while (resultSet.next()) {
        instances.add(
            new DatabaseInstance(
                DatabaseInstanceRole.valueOf(resultSet.getString("role")),
                resultSet.getString("server_id")));
      }
    } catch (Exception e) {
      LOGGER.log(
          Level.SEVERE,
          "Failed to obtain cluster members due to exception. Returning empty set",
          e);
      return EMPTY_SET;
    }
    return Collections.unmodifiableSet(instances);
  }

  @Override
  public ClusterInfo discoverCluster() {
    return new ClusterInfo(
        this.fairlinkConnectionString.delegateConnectionString(),
        this.findReplicas().stream()
            .filter(databaseInstance -> databaseInstance.getRole() == DatabaseInstanceRole.READER)
            .map(DatabaseInstance::getId)
            .collect(Collectors.toSet()));
  }
}
