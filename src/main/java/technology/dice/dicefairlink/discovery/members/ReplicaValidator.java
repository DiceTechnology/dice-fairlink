package technology.dice.dicefairlink.discovery.members;

import java.util.Properties;

@FunctionalInterface
public interface ReplicaValidator {
  boolean isValid(String host, Properties properties);
}
