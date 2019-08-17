package technology.dice.dicefairlink.discovery.members;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;

import java.util.Properties;

public class JdbcConnectionValidatorTest {
  @Rule public MySQLContainer mysql = new MySQLContainer();

  private Properties baseTestProperties() {
    Properties p = new Properties();
    p.setProperty("user", mysql.getUsername());
    p.setProperty("password", mysql.getPassword());
    return p;
  }

  @Test
  public void validConnection() {
    JdbcConnectionValidator underTest = new JdbcConnectionValidator(mysql.getJdbcDriverInstance());
    Assert.assertTrue(underTest.isValid(mysql.getJdbcUrl(), baseTestProperties()));
  }

  @Test
  public void inValidConnection() {
    JdbcConnectionValidator underTest = new JdbcConnectionValidator(mysql.getJdbcDriverInstance());
    Assert.assertFalse(underTest.isValid(mysql.getJdbcUrl(), new Properties()));
  }
}
