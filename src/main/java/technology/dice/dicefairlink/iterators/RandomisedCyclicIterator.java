/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class RandomisedCyclicIterator<T> extends CyclicIterator<T> {

  protected RandomisedCyclicIterator(Collection<? extends T> collection) {
    super(shuffle(collection));
  }

  private static <T> Collection<? extends T> shuffle(Collection<? extends T> collection) {
    final ArrayList<? extends T> copy = new ArrayList<>(collection);
    Collections.shuffle(copy, ThreadLocalRandom.current());
    return copy;
  }

  public static <T> RandomisedCyclicIterator<T> of(Collection<? extends T> collection) {
    return new RandomisedCyclicIterator<>(collection);
  }
}
