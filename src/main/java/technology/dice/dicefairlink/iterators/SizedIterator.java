/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.iterators;

import java.util.Iterator;

public interface SizedIterator<T> extends Iterator<T> {
  int size();
}
