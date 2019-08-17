/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members;

import java.util.Properties;

@FunctionalInterface
public interface ReplicaValidator {
  boolean isValid(String host, Properties properties);
}
