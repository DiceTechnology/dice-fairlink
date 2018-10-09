/*
 * The MIT License
 *
 * Copyright 2018 Andrey Lebedenko (andrey.lebedenko@img.com).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package technology.dice.dicefairlink.iterators;

import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CyclicIteratorTest {

  private final String elemetPrefix = "TEST_";

  @Test(expected = NoSuchElementException.class)
  public void canCallOfFromEmptyList() {
    CyclicIterator cyclicIterator = CyclicIterator.of(Collections.EMPTY_LIST);

    Assertions.assertThat(cyclicIterator).isNotNull();

    assertThat(cyclicIterator.hasNext()).isEqualTo(true); // fixme should be proper hasNext
    cyclicIterator.next(); // final step throws
  }

  @Test(expected = NoSuchElementException.class)
  public void canCallOfFromEmptySet() {
    CyclicIterator cyclicIterator = CyclicIterator.of(Collections.EMPTY_SET);

    Assertions.assertThat(cyclicIterator).isNotNull();

    assertThat(cyclicIterator.hasNext()).isEqualTo(true); // fixme should be proper hasNext
    cyclicIterator.next(); // final step throws
  }

  @Test
  public void canOperateWithListOfSingleElement() {
    final String singleElement = elemetPrefix + "_1";

    final List<String> listOfStrings = new LinkedList<>();
    listOfStrings.add(singleElement);

    CyclicIterator<String> cyclicIterator = CyclicIterator.<String>of(listOfStrings);

    assertThat(cyclicIterator).isNotNull();
    for(int cycle = 0; cycle < ThreadLocalRandom.current().nextInt(10, 100); cycle++) {
      assertThat(cyclicIterator.hasNext()).isEqualTo(true);
      assertThat(cyclicIterator.next()).isEqualTo(singleElement);
    }
  }

  @Test
  public void canOperateWithSetOfSingleElement() {
    final String singleElement = elemetPrefix + "_1";

    Set<String> setOfString = new LinkedHashSet<>();
    setOfString.add(singleElement);

    CyclicIterator<String> cyclicIterator = CyclicIterator.<String>of(setOfString);

    assertThat(cyclicIterator).isNotNull();
    for(int cycle = 0; cycle < ThreadLocalRandom.current().nextInt(10, 100); cycle++) {
      assertThat(cyclicIterator.hasNext()).isEqualTo(true);
      assertThat(cyclicIterator.next()).isEqualTo(singleElement);
    }
  }

  @Test
  public void canOperateWithListOfMultipleIdenticalElements() {
    final String singleElement = elemetPrefix + "_1";
    final int size = ThreadLocalRandom.current().nextInt(100, 1000);
    final List<String> listOfStrings = new LinkedList<>();

    for(int i = 0; i < size; i++) {
      listOfStrings.add(singleElement);
    }

    CyclicIterator<String> cyclicIterator = CyclicIterator.<String>of(listOfStrings);


    assertThat(cyclicIterator).isNotNull();
    for(int cycle = 0; cycle < ThreadLocalRandom.current().nextInt(10, 100); cycle++) {
      for(int i = 0; i < size; i++) {
        assertThat(cyclicIterator.hasNext()).isEqualTo(true);
        assertThat(cyclicIterator.next()).isEqualTo(singleElement);
      }
    }
  }

  @Test
  public void canOperateWithSetOfMultipleElement() {
    final int size = ThreadLocalRandom.current().nextInt(100, 1000);
    final Set<String> setOfStrings = new LinkedHashSet<>();

    for(int i = 0; i < size; i++) {
      setOfStrings.add(elemetPrefix + i);
    }

    CyclicIterator<String> cyclicIterator = CyclicIterator.<String>of(setOfStrings);

    assertThat(cyclicIterator).isNotNull();

    for(int cycle = 0; cycle < ThreadLocalRandom.current().nextInt(10, 100); cycle++) {
      for(int i = 0; i < size; i++) {
        assertThat(cyclicIterator.hasNext()).isEqualTo(true);
        assertThat(cyclicIterator.next()).isEqualTo(elemetPrefix + i);
      }
    }
  }

}
