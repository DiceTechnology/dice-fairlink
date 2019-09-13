package technology.dice.dicefairlink.discovery.members.sql;

import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PostgresSQLReplicasFinder implements MemberFinderMethod {
    private static final Logger LOGGER = Logger.getLogger(PostgresSQLReplicasFinder.class.getName());
    private static final Set<DatabaseInstance> EMPTY_SET =
        Collections.unmodifiableSet(new HashSet<>(0));

    private static final String FIND_NODES_QUERY =
        "select server_id, "
        + "case when session_id = 'MASTER_SESSION_ID' then 'WRITER' else 'READER' end "
        + "as role from aurora_replica_status();";

    private final Driver driverForDelegate;
    private final FairlinkConnectionString fairlinkConnectionString;

    public PostgresSQLReplicasFinder(FairlinkConnectionString fairlinkConnectionString, Driver driverForDelegate) {
        this.fairlinkConnectionString = fairlinkConnectionString;
        this.driverForDelegate = driverForDelegate;
    }

    protected Set<DatabaseInstance> findReplicas() {
        Set<DatabaseInstance> instances = new HashSet<>();
        try (final Connection c =
                this.driverForDelegate.connect(
                    fairlinkConnectionString.delegateConnectionString(),
                    fairlinkConnectionString.getProperties());
            final ResultSet resultSet =
                c.createStatement()
                    .executeQuery(FIND_NODES_QUERY)) {
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
