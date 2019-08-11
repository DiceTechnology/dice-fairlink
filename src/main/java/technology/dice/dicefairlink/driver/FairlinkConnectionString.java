package technology.dice.dicefairlink.driver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FairlinkConnectionString {
  private static final String DRIVER_PROTOCOL = "auroraro";
  private static final Pattern driverPattern =
      Pattern.compile("jdbc:" + DRIVER_PROTOCOL + ":(?<delegate>[^:]*):(?<uri>.*\\/\\/.+)");
  private static final String JDBC_PREFIX = "jdbc";
  private final String delegateProtocol;
  private final String fairlinKUri;
  private final URI delegateUri;
  private final String prefix;

  public FairlinkConnectionString(String connectionString) throws URISyntaxException {
    this.fairlinKUri = connectionString;

    Matcher matcher = driverPattern.matcher(connectionString);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          connectionString + "is not a valid fairlink connection string");
    }
    this.delegateProtocol = matcher.group("delegate");
    this.prefix = String.format("%s:%s", JDBC_PREFIX, delegateProtocol);
    this.delegateUri = new URI(this.delegateProtocol + ":" + matcher.group("uri"));
  }

  public String getDelegateProtocol() {
    return delegateProtocol;
  }

  public String delegateConnectionString() throws URISyntaxException {
    return JDBC_PREFIX + ":" + this.delegateUri.toASCIIString();
  }

  public String delegateConnectionString(String forHost) throws URISyntaxException {
    return new URI(
            JDBC_PREFIX + ":" + delegateUri.getScheme(),
            delegateUri.getUserInfo(),
            forHost,
            delegateUri.getPort(),
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
}
