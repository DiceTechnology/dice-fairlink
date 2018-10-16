/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.iterators;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

public class CyclicIteratorTest {
  private static final String ELEMENT_PREFIX = "TEST_";

  @Test(expected = NoSuchElementException.class)
  public void canCallOfFromEmptyList() {
    RandomisedCyclicIterator cyclicIterator = RandomisedCyclicIterator.of(Collections.EMPTY_LIST);

    Assertions.assertThat(cyclicIterator).isNotNull();

    assertThat(cyclicIterator.hasNext()).isEqualTo(false);
    cyclicIterator.next(); // final step throws
  }

  @Test(expected = NoSuchElementException.class)
  public void canCallOfFromEmptySet() {
    RandomisedCyclicIterator cyclicIterator = RandomisedCyclicIterator.of(Collections.EMPTY_SET);

    Assertions.assertThat(cyclicIterator).isNotNull();

    assertThat(cyclicIterator.hasNext()).isEqualTo(false);
    cyclicIterator.next(); // final step throws
  }

  @Test
  public void canOperateWithListOfSingleElement() {
    final String singleElement = ELEMENT_PREFIX + "_1";

    final List<String> listOfStrings = new LinkedList<>();
    listOfStrings.add(singleElement);

    RandomisedCyclicIterator<String> cyclicIterator =
        RandomisedCyclicIterator.<String>of(listOfStrings);

    assertThat(cyclicIterator).isNotNull();
    for (int cycle = 0; cycle < ThreadLocalRandom.current().nextInt(10, 100); cycle++) {
      assertThat(cyclicIterator.hasNext()).isEqualTo(true);
      assertThat(cyclicIterator.next()).isEqualTo(singleElement);
    }
  }

  @Test
  public void canOperateWithSetOfSingleElement() {
    final String singleElement = ELEMENT_PREFIX + "_1";

    Set<String> setOfString = new LinkedHashSet<>();
    setOfString.add(singleElement);

    RandomisedCyclicIterator<String> cyclicIterator =
        RandomisedCyclicIterator.<String>of(setOfString);

    assertThat(cyclicIterator).isNotNull();
    for (int cycle = 0; cycle < ThreadLocalRandom.current().nextInt(10, 100); cycle++) {
      assertThat(cyclicIterator.hasNext()).isEqualTo(true);
      assertThat(cyclicIterator.next()).isEqualTo(singleElement);
    }
  }

  @Test
  public void canOperateWithListOfMultipleIdenticalElements() {
    final String singleElement = ELEMENT_PREFIX + "_1";
    final int size = ThreadLocalRandom.current().nextInt(100, 1000);
    final List<String> listOfStrings = new LinkedList<>();

    for (int i = 0; i < size; i++) {
      listOfStrings.add(singleElement);
    }

    RandomisedCyclicIterator<String> cyclicIterator =
        RandomisedCyclicIterator.<String>of(listOfStrings);

    assertThat(cyclicIterator).isNotNull();
    for (int cycle = 0; cycle < ThreadLocalRandom.current().nextInt(10, 100); cycle++) {
      for (int i = 0; i < size; i++) {
        assertThat(cyclicIterator.hasNext()).isEqualTo(true);
        assertThat(cyclicIterator.next()).isEqualTo(singleElement);
      }
    }
  }
}
