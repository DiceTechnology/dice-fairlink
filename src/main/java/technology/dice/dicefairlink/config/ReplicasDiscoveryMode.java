package technology.dice.dicefairlink.config;

import java.util.Optional;

public enum ReplicasDiscoveryMode {
  AWS_API,
  SQL_MYSQL;

  public static Optional<ReplicasDiscoveryMode> fromStringInsensitive(String candidate) {
    return Optional.of(ReplicasDiscoveryMode.valueOf(candidate.toUpperCase()));
  }
}
