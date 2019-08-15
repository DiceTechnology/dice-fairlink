package technology.dice.dicefairlink.iterators;

import java.util.Arrays;
import java.util.Collection;

public interface SizedIteratorBuilder<T> {
  SizedIterator<T> from(Collection<T> collection);

  default SizedIterator<T> from(T... elements) {
    return this.from(Arrays.asList(elements));
  }
}
