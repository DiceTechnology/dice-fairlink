package technology.dice.dicefairlink.discovery.members;

@FunctionalInterface
public interface MemberFinderMethod {
  ClusterInfo discoverCluster();
}
