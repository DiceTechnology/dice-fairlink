/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink;

import java.util.Arrays;
import java.util.Optional;

public enum DiscoveryAuthMode {
  BASIC,
  ENVIRONMENT;

  public static Optional<DiscoveryAuthMode> fromStringInsensitive(String candidate) {
    return Arrays.stream(DiscoveryAuthMode.values())
        .filter(mode -> mode.toString().equalsIgnoreCase(candidate))
        .findAny();
  }
}
