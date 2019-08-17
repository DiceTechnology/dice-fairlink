/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.driver;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import technology.dice.dicefairlink.support.driver.TestDriver;

import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

public class AuroraReadReplicasDriverTest {
  private static final String VALID_JDBC_URL =
      "jdbc:fairlink:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc";

  @BeforeClass
  public static void setupClass() throws SQLException {
    DriverManager.registerDriver(new TestDriver());
  }

  @Test
  public void driverInterfaceLock() {
    final AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    Assert.assertFalse(underTest.jdbcCompliant());
    Assert.assertEquals(2, underTest.getMajorVersion());
    Assert.assertEquals(0, underTest.getMinorVersion());
    Assert.assertArrayEquals(
        new DriverPropertyInfo[] {}, underTest.getPropertyInfo(VALID_JDBC_URL, new Properties()));
    Assert.assertEquals(Logger.getLogger(""), underTest.getParentLogger());
  }

  @Test
  public void accepts() throws SQLException {
    final AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    Assert.assertTrue(underTest.acceptsURL(VALID_JDBC_URL));
  }

  @Test
  public void doesNotAccept() throws SQLException {
    final AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    Assert.assertFalse(
        underTest.acceptsURL(
            "jdbc:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc"));
  }

  @Test(expected = SQLException.class)
  public void throwsOnNullAccepts() throws SQLException {
    final AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    underTest.acceptsURL(null);
  }

  @Test(expected = SQLException.class)
  public void throwsOnAcceptsURL_nullString() throws Exception {
    AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    underTest.acceptsURL(null);
  }

  @Test
  public void canAcceptsURL_emptyString() throws Exception {
    AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    boolean retunedValue = underTest.acceptsURL("");
    assertThat(retunedValue).isEqualTo(false);
  }

  @Test
  public void refuses_vanillaJdbc() throws Exception {
    AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    boolean retunedValue =
        underTest.acceptsURL("jdbc:fairlinktestdriver://host:3306/id?useSSL=false");
    assertThat(retunedValue).isEqualTo(false);
  }

  @Test
  public void canAcceptsURL_validString() throws Exception {
    AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    boolean retunedValue = underTest.acceptsURL(VALID_JDBC_URL);
    assertThat(retunedValue).isEqualTo(true);
  }

  @Test(expected = NullPointerException.class)
  public void failToConnectToValidUrl_nullProperties() throws Exception {
    AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    underTest.connect(VALID_JDBC_URL, null); // last call must throw
  }

  @Test(expected = IllegalStateException.class)
  public void failToConnectToValidUrl_emptyProperties_andNoRegionAvailable() throws Exception {
    AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    underTest.connect(VALID_JDBC_URL, new Properties()); // last call must throw
  }

  @Test
  public void failedToConnectBadUrl() throws Exception {
    AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    Assert.assertNull(
        underTest.connect("jdbc:fairlinktestdriver://host:3306/id?useSSL=false", new Properties()));
  }

  @Test
  public void failedToConnectIncompatibleDiscoveryMode() throws Exception {
    final Properties properties = new Properties();
    properties.setProperty("discoveryMode", "SQL_MYSQL");
    properties.setProperty("auroraClusterRegion", "eu-west-1");
    properties.setProperty("replicaEndpointTemplate", "%s");

    AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    Assert.assertNull(
        underTest.connect(
            "jdbc:fairlink:fairlinktestdriver://host:3306/id?useSSL=false", properties));
  }

  @Test
  public void testWellformedConnectionstringWithMalformedHost() throws Exception {
    AuroraReadReplicasDriver underTest = new AuroraReadReplicasDriver();
    Assert.assertNull(underTest.connect("jdbc:fairlink:<>|/://host:3306/id?useSSL=false", new Properties()));
  }

  @Test
  public void staticLoad() throws ClassNotFoundException {
    Class.forName(AuroraReadReplicasDriver.class.getCanonicalName());
  }
}
