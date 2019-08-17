/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.driver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FairlinkConnectionString {
  private static final String DRIVER_PROTOCOL = "fairlink";
  private static final Pattern driverPattern =
      Pattern.compile("jdbc:" + DRIVER_PROTOCOL + ":(?<delegate>[^:]*):(?<uri>.*\\/\\/.+)");
  private static final String JDBC_PREFIX = "jdbc";
  private final String delegateProtocol;
  private final String fairlinKUri;
  private final URI delegateUri;
  private final Properties properties;

  public FairlinkConnectionString(String connectionString, Properties properties)
      throws URISyntaxException {
    this.fairlinKUri = connectionString;
    this.properties = properties;

    Matcher matcher = driverPattern.matcher(connectionString);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          connectionString + " is not a valid fairlink connection string");
    }
    this.delegateProtocol = matcher.group("delegate");
    this.delegateUri = new URI(this.delegateProtocol + ":" + matcher.group("uri"));
  }

  public String getDelegateProtocol() {
    return delegateProtocol;
  }

  public String delegateConnectionString() {
    return JDBC_PREFIX + ":" + this.delegateUri.toASCIIString();
  }

  public String delegateConnectionString(String forHost) throws URISyntaxException {
    final URI portExtractor = new URI(JDBC_PREFIX + "://" + forHost);
    return new URI(
            JDBC_PREFIX + ":" + delegateUri.getScheme(),
            delegateUri.getUserInfo(),
            portExtractor.getHost(),
            portExtractor.getPort() == -1 ? delegateUri.getPort() : portExtractor.getPort(),
            delegateUri.getPath(),
            delegateUri.getQuery(),
            delegateUri.getFragment())
        .toASCIIString();
  }

  public String getHost() {
    return delegateUri.getHost();
  }

  public String getFairlinkUri() {
    return this.fairlinKUri;
  }

  public Properties getProperties() {
    return properties;
  }

  public static boolean accepts(String url) {
    Matcher matcher = driverPattern.matcher(url);
    return matcher.matches();
  }
}
