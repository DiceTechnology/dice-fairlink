/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CyclicIterator<T> implements SizedIterator<T> {
  private final List<T> elements;
  private volatile Iterator<T> iterator;

  protected CyclicIterator(Collection<? extends T> collection) {
    this.elements = Collections.unmodifiableList(new ArrayList(collection));
    this.iterator = this.elements.iterator();
  }

  @Override
  public boolean hasNext() {
    return !elements.isEmpty();
  }

  @Override
  public int size() {
    return elements.size();
  }

  @Override
  public synchronized T next() {
    if (!iterator.hasNext()) {
      iterator = elements.iterator();
      if (!iterator.hasNext()) {
        throw new NoSuchElementException();
      }
    }
    T next = iterator.next();
    return next;
  }
}
