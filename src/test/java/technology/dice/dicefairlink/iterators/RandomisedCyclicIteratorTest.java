/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.iterators;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

public class RandomisedCyclicIteratorTest {

  private static final int MIN_SIZE = 10;
  private static final int MAX_SIZE = 1_000;
  private static final String ELEMENT_PREFIX = "TEST_";

  private int numberOfElementsToTest = -1;

  @Rule public TestName testName = new TestName();

  @Before
  public void setUp() {
    numberOfElementsToTest = ThreadLocalRandom.current().nextInt(MIN_SIZE, MAX_SIZE);
  }

  @Test(expected = NoSuchElementException.class)
  public void canCallOfFromEmptyList() {
    RandomisedCyclicIterator cyclicIterator = RandomisedCyclicIterator.of(Collections.EMPTY_LIST);

    Assertions.assertThat(cyclicIterator).isNotNull();

    assertThat(cyclicIterator.hasNext()).isFalse();
    cyclicIterator.next(); // final step throws
  }

  @Test(expected = NoSuchElementException.class)
  public void canCallOfFromEmptySet() {
    RandomisedCyclicIterator cyclicIterator = RandomisedCyclicIterator.of(Collections.EMPTY_SET);

    Assertions.assertThat(cyclicIterator).isNotNull();

    assertThat(cyclicIterator.hasNext()).isFalse();
    cyclicIterator.next(); // final step throws
  }

  @Test
  public void canOperateWithListOfSingleElement() {
    final String singleElement = ELEMENT_PREFIX + "_1";

    final List<String> listOfStrings = new LinkedList<>();
    listOfStrings.add(singleElement);

    RandomisedCyclicIterator<String> cyclicIterator = RandomisedCyclicIterator.of(listOfStrings);

    assertThat(cyclicIterator).isNotNull();
    for (int cycle = 0; cycle < numberOfElementsToTest; cycle++) {
      assertThat(cyclicIterator.hasNext()).isTrue();
      assertThat(cyclicIterator.next()).isEqualTo(singleElement);
    }
  }

  @Test
  public void canOperateWithListOfMultipleIdenticalElements() {
    final String singleElement = ELEMENT_PREFIX + "_1";
    final List<String> listOfStrings = new LinkedList<>();

    for (int i = 0; i < numberOfElementsToTest; i++) {
      listOfStrings.add(singleElement);
    }

    RandomisedCyclicIterator<String> cyclicIterator = RandomisedCyclicIterator.of(listOfStrings);

    assertThat(cyclicIterator).isNotNull();
    for (int cycle = 0; cycle < numberOfElementsToTest; cycle++) {
      for (int i = 0; i < numberOfElementsToTest; i++) {
        assertThat(cyclicIterator.hasNext()).isTrue();
        assertThat(cyclicIterator.next()).isEqualTo(singleElement);
      }
    }
  }
}
