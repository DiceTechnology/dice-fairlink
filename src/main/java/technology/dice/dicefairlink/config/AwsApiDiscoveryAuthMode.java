/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.config;

import java.util.Arrays;
import java.util.Optional;

public enum AwsApiDiscoveryAuthMode {
  BASIC,
  ENVIRONMENT,
  DEFAULT_CHAIN;

  public static Optional<AwsApiDiscoveryAuthMode> fromStringInsensitive(String candidate) {
    return Arrays.stream(AwsApiDiscoveryAuthMode.values())
        .filter(mode -> mode.toString().equalsIgnoreCase(candidate))
        .findAny();
  }
}
