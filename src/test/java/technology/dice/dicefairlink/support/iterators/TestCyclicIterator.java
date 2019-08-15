package technology.dice.dicefairlink.support.iterators;

import technology.dice.dicefairlink.iterators.CyclicIterator;

import java.util.Collection;

public class TestCyclicIterator<T> extends CyclicIterator<T> {
  private final Collection<? extends T> collection;

  protected TestCyclicIterator(Collection<? extends T> collection) {
    super(collection);
    this.collection = collection;
  }

  public static <T> CyclicIterator<T> of(Collection<? extends T> collection) {
    return new TestCyclicIterator<>(collection);
  }

  public Collection<? extends T> getElements() {
    return this.collection;
  }
}
