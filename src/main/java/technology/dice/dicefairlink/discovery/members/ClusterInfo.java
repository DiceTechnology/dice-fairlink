/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members;

import java.util.Objects;
import java.util.Set;

public final class ClusterInfo {
  private final String readonlyEndpoint;
  private final Set<String> replicas;

  public ClusterInfo(String readonlyEndpoint, Set<String> replicas) {
    if ("".equals(readonlyEndpoint) || readonlyEndpoint == null) {
      throw new IllegalArgumentException("Read only endpoint must not be null");
    }
    if (replicas == null) {
      throw new IllegalArgumentException("Set of replicas must not be mull");
    }
    this.readonlyEndpoint = readonlyEndpoint;
    this.replicas = replicas;
  }

  public String getReadonlyEndpoint() {
    return readonlyEndpoint;
  }

  public Set<String> getReplicas() {
    return replicas;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ClusterInfo)) {
      return false;
    }
    ClusterInfo that = (ClusterInfo) o;
    return Objects.equals(getReadonlyEndpoint(), that.getReadonlyEndpoint())
        && Objects.equals(getReplicas(), that.getReplicas());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getReadonlyEndpoint(), getReplicas());
  }
}
