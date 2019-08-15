package technology.dice.dicefairlink.iterators;

import java.util.Iterator;

public interface SizedIterator<T> extends Iterator<T> {
  int size();
}
