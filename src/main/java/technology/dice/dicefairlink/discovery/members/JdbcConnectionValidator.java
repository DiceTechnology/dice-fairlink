package technology.dice.dicefairlink.discovery.members;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;

public class JdbcConnectionValidator implements ReplicaValidator {
  private final Driver driver;

  public JdbcConnectionValidator(Driver driver) {
    this.driver = driver;
  }

  @Override
  public boolean isValid(String host, Properties properties) {
    try (Connection c = driver.connect(host, properties)) {
      c.createStatement().executeQuery("SELECT 1");
    } catch (Exception e) {
      return false;
    }
    return true;
  }
}
