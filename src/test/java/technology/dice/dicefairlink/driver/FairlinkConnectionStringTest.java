package technology.dice.dicefairlink.driver;

import org.junit.Assert;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Properties;

public class FairlinkConnectionStringTest {
  @Test
  public void valid() throws URISyntaxException {
    String connString =
        "jdbc:fairlink:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc";
    final Properties properties = new Properties();
    properties.setProperty("a", "b");
    final FairlinkConnectionString underTest = new FairlinkConnectionString(connString, properties);
    Assert.assertEquals(connString, underTest.getFairlinkUri());
    Assert.assertEquals("fairlinktestdriver", underTest.getDelegateProtocol());
    Assert.assertEquals("aa", underTest.getHost());
    Assert.assertEquals(
        "jdbc:fairlinktestdriver://anotherHost:123/db?param1=123&param2=true&param3=abc",
        underTest.delegateConnectionString("anotherHost"));
    Assert.assertEquals(
        "jdbc:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc",
        underTest.delegateConnectionString());
    Assert.assertEquals(1, underTest.getProperties().size());
    Assert.assertEquals("b", underTest.getProperties().getProperty("a"));
  }

  @Test
  public void validHostInDifferentPort() throws URISyntaxException {
    String connString =
        "jdbc:fairlink:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc";
    final Properties properties = new Properties();
    properties.setProperty("a", "b");
    final FairlinkConnectionString underTest = new FairlinkConnectionString(connString, properties);
    Assert.assertEquals(connString, underTest.getFairlinkUri());
    Assert.assertEquals("fairlinktestdriver", underTest.getDelegateProtocol());
    Assert.assertEquals("aa", underTest.getHost());
    Assert.assertEquals(
        "jdbc:fairlinktestdriver://anotherHost:999/db?param1=123&param2=true&param3=abc",
        underTest.delegateConnectionString("anotherHost:999"));
    Assert.assertEquals(
        "jdbc:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc",
        underTest.delegateConnectionString());
    Assert.assertEquals(1, underTest.getProperties().size());
    Assert.assertEquals("b", underTest.getProperties().getProperty("a"));
  }

  @Test
  public void accepts() {
    Assert.assertTrue(
        FairlinkConnectionString.accepts(
            "jdbc:fairlink:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc"));
  }

  @Test
  public void doesNotAccept() {
    Assert.assertFalse(
        FairlinkConnectionString.accepts(
            "jdbc:something:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc"));
    Assert.assertFalse(
        FairlinkConnectionString.accepts(
            "jdbc:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void ctorBadConnString() throws URISyntaxException {
    new FairlinkConnectionString(
        "jdbc:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc", new Properties());
  }
}
