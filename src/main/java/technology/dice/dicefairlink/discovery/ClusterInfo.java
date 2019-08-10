package technology.dice.dicefairlink.discovery;

import java.util.Collection;

public final class ClusterInfo {
  private final String readonlyEndpoint;
  private final Collection<String> replicas;

  public ClusterInfo(String readonlyEndpoint, Collection<String> replicas) {
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

  public Collection<String> getReplicas() {
    return replicas;
  }
}
