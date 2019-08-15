package technology.dice.dicefairlink.iterators;

import java.util.Collection;

public class RandomisedCyclicIteatorBuilder<T> implements SizedIteratorBuilder<T> {
  @Override
  public SizedIterator<T> from(Collection<T> collection) {
    return RandomisedCyclicIterator.of(collection);
  }
}
