/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.discovery.members;

import technology.dice.dicefairlink.iterators.SizedIterator;

public interface MemberFinder {
  SizedIterator<String> discoverReplicas();

  default SizedIterator<String> init() {
    return this.discoverReplicas();
  }
}
