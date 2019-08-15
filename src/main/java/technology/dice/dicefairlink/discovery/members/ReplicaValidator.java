package technology.dice.dicefairlink.discovery.members;

import java.util.Properties;

public interface ReplicaValidator {
  boolean isValid(String host, Properties properties);
}
