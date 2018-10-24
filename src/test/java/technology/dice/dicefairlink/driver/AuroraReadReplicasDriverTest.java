/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.driver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.Test;

public class AuroraReadReplicasDriverTest {

  private static final String VALID_JDBC_URL =
      "jdbc:auroraro:mysql://aa:123/db?param1=123&param2=true&param3=abc";

  private AuroraReadDriver auroraReadReplicasDriver;
  @Before
  public void setUp() {
    auroraReadReplicasDriver =
        new AuroraReadDriver(AuroraReadDriver.DRIVER_PROTOCOL_RO, AuroraReadDriver.ONLY_READ_REPLICAS, new ScheduledThreadPoolExecutor(1));
  }

  @Test(expected = SQLException.class)
  public void throwsOnAcceptsURL_nullString() throws Exception {
    auroraReadReplicasDriver.acceptsURL(null);
  }

  @Test
  public void canAcceptsURL_emptyString() throws Exception {
    boolean retunedValue = auroraReadReplicasDriver.acceptsURL("");
    assertThat(retunedValue).isEqualTo(false);
  }

  @Test
  public void canAcceptsURL_validString() throws Exception {
    boolean retunedValue = auroraReadReplicasDriver.acceptsURL(VALID_JDBC_URL);
    assertThat(retunedValue).isEqualTo(true);
  }

  @Test(expected = NullPointerException.class)
  public void failToConnectToValidUrl_nullProperties() throws Exception {
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, null); // last call must throw
  }

  @Test(expected = RuntimeException.class)
  public void failToConnectToValidUrl_emptyProperties_andNoRegionAvailable() throws Exception {
    final Properties emptyProperties = new Properties();
    auroraReadReplicasDriver.connect(VALID_JDBC_URL, emptyProperties); // last call must throw
  }
}
