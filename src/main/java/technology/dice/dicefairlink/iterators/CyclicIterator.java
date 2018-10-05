/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CyclicIterator<T> implements Iterator<T> {
  private final Collection<T> elements;
  private Iterator<T> iterator;

  private CyclicIterator(Collection<? extends T> collection) {
    this.elements = new ArrayList<>(collection.size());
    this.elements.addAll(collection);
    this.iterator = this.elements.iterator();
  }

  public static <T> CyclicIterator<T> of(Collection<? extends T> collection) {
    return new CyclicIterator<>(collection);
  }

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public synchronized T next() {
    if (!this.iterator.hasNext()) {
      this.iterator = this.elements.iterator();
      if (!this.iterator.hasNext()) {
        throw new NoSuchElementException();
      }
    }
    T next = this.iterator.next();
    return next;
  }
}
