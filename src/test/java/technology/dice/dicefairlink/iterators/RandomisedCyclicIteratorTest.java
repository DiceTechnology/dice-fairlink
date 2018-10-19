/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.iterators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;

public class RandomisedCyclicIteratorTest {

  private static final int MIN_SIZE = 10;
  private static final int MAX_SIZE = 1_000;
  private static final String ELEMENT_PREFIX = "TEST_";

  private int numberOfElementsToTest = -1;

  @Before
  public void setUp() {
    numberOfElementsToTest = ThreadLocalRandom.current().nextInt(MIN_SIZE, MAX_SIZE);
  }

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

    RandomisedCyclicIterator<String> cyclicIterator
        = RandomisedCyclicIterator.<String>of(listOfStrings);

    assertThat(cyclicIterator).isNotNull();
    for (int cycle = 0; cycle < numberOfElementsToTest; cycle++) {
      assertThat(cyclicIterator.hasNext()).isEqualTo(true);
      assertThat(cyclicIterator.next()).isEqualTo(singleElement);
    }
  }

  @Test
  public void canOperateWithSetOfSingleElement() {
    final String singleElement = ELEMENT_PREFIX + "_1";

    Set<String> setOfString = new LinkedHashSet<>();
    setOfString.add(singleElement);

    RandomisedCyclicIterator<String> cyclicIterator
        = RandomisedCyclicIterator.<String>of(setOfString);

    assertThat(cyclicIterator).isNotNull();
    for (int cycle = 0; cycle < numberOfElementsToTest; cycle++) {
      assertThat(cyclicIterator.hasNext()).isEqualTo(true);
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

    RandomisedCyclicIterator<String> cyclicIterator
        = RandomisedCyclicIterator.<String>of(listOfStrings);

    assertThat(cyclicIterator).isNotNull();
    for (int cycle = 0; cycle < numberOfElementsToTest; cycle++) {
      for (int i = 0; i < numberOfElementsToTest; i++) {
        assertThat(cyclicIterator.hasNext()).isEqualTo(true);
        assertThat(cyclicIterator.next()).isEqualTo(singleElement);
      }
    }
  }

  @Test
  public void canCompareInetrnalCollectionToExternal_overSameCollectionShuffeled() {
    List<String> originalElements = new ArrayList<>(numberOfElementsToTest);
    for (int i = 1; i <= numberOfElementsToTest; i++) {
      originalElements.add(ELEMENT_PREFIX + i);
    }

    RandomisedCyclicIterator<String> cyclicIterator
        = RandomisedCyclicIterator.<String>of(originalElements);

    assertThat(cyclicIterator.hasSameContent(originalElements)).isEqualTo(true);
    Collections.shuffle(originalElements);
    assertThat(cyclicIterator.hasSameContent(originalElements)).isEqualTo(true);
  }

  @Test
  public void canCompareInetrnalCollectionToExternal_whenBothAreSameExceptOrderOfElements() {
    List<String> originalElements = new ArrayList<>(numberOfElementsToTest);
    for (int i = 1; i <= numberOfElementsToTest; i++) {
      originalElements.add(ELEMENT_PREFIX + i);
    }
    List<String> copyOfOriginalElements = new ArrayList<>(numberOfElementsToTest);
    copyOfOriginalElements.addAll(originalElements);

    RandomisedCyclicIterator<String> cyclicIterator
        = RandomisedCyclicIterator.<String>of(originalElements);

    assertThat(cyclicIterator.hasSameContent(copyOfOriginalElements)).isEqualTo(true);
  }

  @Test
  public void canCompareInetrnalCollectionToExternal_whenOneElementIsDifferent() {
    List<String> originalElements = new ArrayList<>(numberOfElementsToTest);
    for (int i = 1; i <= numberOfElementsToTest; i++) {
      originalElements.add(ELEMENT_PREFIX + i);
    }
    List<String> differentElements = new ArrayList<>(numberOfElementsToTest);
    differentElements.addAll(originalElements);
    differentElements.set(ThreadLocalRandom.current().nextInt(MIN_SIZE, numberOfElementsToTest), "DIFFERENT_ELEMENT");

    RandomisedCyclicIterator<String> cyclicIterator
        = RandomisedCyclicIterator.<String>of(originalElements);

    assertThat(cyclicIterator.hasSameContent(differentElements)).isEqualTo(false);
  }

  @Test
  public void canCompareInetrnalCollectionToExternal_whenDirrefentInSize() {
    List<String> originalElements = new ArrayList<>(numberOfElementsToTest);
    for (int i = 1; i <= numberOfElementsToTest; i++) {
      originalElements.add(ELEMENT_PREFIX + i);
    }
    List<String> differentElements = new ArrayList<>(numberOfElementsToTest);
    differentElements.addAll(originalElements);
    differentElements.remove(ThreadLocalRandom.current().nextInt(MIN_SIZE, numberOfElementsToTest));

    RandomisedCyclicIterator<String> cyclicIterator
        = RandomisedCyclicIterator.<String>of(originalElements);

    assertThat(cyclicIterator.hasSameContent(differentElements)).isEqualTo(false);
  }

  @Test
  public void canCompareInetrnalCollectionToExternal_whenCollectionIsNull() {
    List<String> originalElements = new ArrayList<>(numberOfElementsToTest);
    for (int i = 1; i <= numberOfElementsToTest; i++) {
      originalElements.add(ELEMENT_PREFIX + i);
    }

    RandomisedCyclicIterator<String> cyclicIterator
        = RandomisedCyclicIterator.<String>of(originalElements);

    assertThat(cyclicIterator.hasSameContent(null)).isEqualTo(false);
  }
}
