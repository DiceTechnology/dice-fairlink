package technology.dice.dicefairlink.config;

import java.util.Arrays;
import java.util.Optional;

public enum ReplicasDiscoveryMode {
  AWS_API,
  SQL_MYSQL;

  public static Optional<ReplicasDiscoveryMode> fromStringInsensitive(String candidate) {
    return Arrays.stream(ReplicasDiscoveryMode.values())
        .filter(mode -> mode.toString().equalsIgnoreCase(candidate))
        .findAny();
  }
}
