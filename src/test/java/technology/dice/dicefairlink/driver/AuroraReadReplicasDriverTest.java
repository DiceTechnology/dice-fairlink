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

package technology.dice.dicefairlink.driver;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import java.sql.SQLException;
import java.util.Properties;

public class AuroraReadReplicasDriverTest {

  private static final String VALID_JDBC_URL = "jdbc:mysql:auroraro://aa:123/db?param1=123&param2=true&param3=abc";
  private static final String VALID_LOW_JDBC_URL = "jdbc:mysql://aa:123/db?param1=123&param2=true&param3=abc";

  @Test(expected = SQLException.class)
  public void throwsOnAcceptsURL_nullString() throws Exception {
    AuroraReadReplicasDriver auroraReadReplicasDriver = new AuroraReadReplicasDriver();
    auroraReadReplicasDriver.acceptsURL(null);
  }

  @Test
  public void canAcceptsURL_emptyString() throws Exception {
    AuroraReadReplicasDriver auroraReadReplicasDriver = new AuroraReadReplicasDriver();
    boolean retunedValue = auroraReadReplicasDriver.acceptsURL("");
    assertThat(retunedValue).isEqualTo(false);
  }

  @Test
  public void canAcceptsURL_validString() throws Exception {
    AuroraReadReplicasDriver auroraReadReplicasDriver = new AuroraReadReplicasDriver();
    boolean retunedValue = auroraReadReplicasDriver.acceptsURL(VALID_JDBC_URL);
    assertThat(retunedValue).isEqualTo(true);
  }

  @Test(expected = NullPointerException.class)
  public void failToConnectToValidUrl_nullProperties() throws Exception {
    AuroraReadReplicasDriver auroraReadReplicasDriver = new AuroraReadReplicasDriver();
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, null);
  }

  @Test(expected = SQLException.class)
  public void failToConnectToValidUrl_emptyProperties() throws Exception {
    AuroraReadReplicasDriver auroraReadReplicasDriver = new AuroraReadReplicasDriver();
    final Properties emptyProperties = new Properties();
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, emptyProperties);
  }

  @Test
  public void testGetMajorVersion() {
  }

  @Test
  public void testGetMinorVersion() {
  }

  @Test
  public void testGetParentLogger() {
  }

  @Test
  public void testGetPropertyInfo() {
  }

  @Test
  public void testJdbcCompliant() {
  }

}
