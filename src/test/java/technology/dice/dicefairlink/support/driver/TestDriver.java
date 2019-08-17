package technology.dice.dicefairlink.support.driver;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class TestDriver implements Driver {
  private static final Logger LOGGER = Logger.getLogger(TestDriver.class.getName());

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    return null;
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return url.contains("fairlinktestdriver");
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    return new DriverPropertyInfo[0];
  }

  @Override
  public int getMajorVersion() {
    return 0;
  }

  @Override
  public int getMinorVersion() {
    return 0;
  }

  @Override
  public boolean jdbcCompliant() {
    return false;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return LOGGER.getParent();
  }
}
