/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.driver;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import technology.dice.dicefairlink.AuroraReadonlyEndpoint;
import technology.dice.dicefairlink.DiscoveryAuthMode;
import technology.dice.dicefairlink.ParsedUrl;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuroraReadReplicasDriver implements java.sql.Driver {
  private static final Logger LOGGER = Logger.getLogger(AuroraReadReplicasDriver.class.getName());
  private static final Pattern driverPattern =
      Pattern.compile("jdbc:(?<delegate>[^:]*):(?<uri>auroraro:.*\\/\\/.+)");
  private static final Duration DEFAULT_POLLER_INTERVAL = Duration.ofSeconds(30);
  private static final String JDBC_PREX = "jdbc";
  private final Map<String, Driver> delegates = new HashMap<>();
  private final Map<URI, AuroraReadonlyEndpoint> auroraClusters = new HashMap<>();

  private static final String AWS_AUTH_MODE_PROPERTY_NAME = "auroraDiscoveryAuthMode";
  private static final String AWS_BASIC_CREDENTIALS_KEY = "auroraDiscoveryKeyId";
  private static final String AWS_BASIC_CREDENTIALS_SECRET = "auroraDiscoverKeySecret";
  private static final String REPLICA_POLL_INTERVAL_PROPERTY_NAME = "replicaPollInterval";
  private static final String CLUSTER_REGION = "auroraClusterRegion";

  static {
    try {
      DriverManager.registerDriver(new AuroraReadReplicasDriver());
    } catch (Exception e) {
      throw new RuntimeException("Can't register driver!", e);
    }
  }

  private AWSCredentialsProvider awsAuth(Properties properties) throws SQLException {
    DiscoveryAuthMode authMode =
        DiscoveryAuthMode.fromStringInsensitive(
                properties.getProperty(AWS_AUTH_MODE_PROPERTY_NAME, "environment"))
            .orElse(DiscoveryAuthMode.ENVIRONMENT);
    switch (authMode) {
      case BASIC:
        String key = properties.getProperty(AWS_BASIC_CREDENTIALS_KEY);
        String secret = properties.getProperty(AWS_BASIC_CREDENTIALS_SECRET);
        if (key == null || secret == null) {
          throw new SQLException(
              String.format(
                  "For basic authentication both [%s] and [%s] must both be set",
                  AWS_BASIC_CREDENTIALS_KEY, AWS_BASIC_CREDENTIALS_SECRET));
        }
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(key, secret));
      default:
        ENVIRONMENT:
        return new EnvironmentVariableCredentialsProvider();
    }
  }

  private Optional<ParsedUrl> parseUrlAndCacheDriver(String url, Properties properties)
      throws SQLException {
    Matcher matcher = driverPattern.matcher(url);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    String delegate = matcher.group("delegate");
    try {
      URI uri = new URI(matcher.group("uri"));
      if (!this.auroraClusters.containsKey(uri)) {
        // because AWS credentials, region and poll interval properties
        // are only processed once per uri, the driver does not support dynamically changing them
        AWSCredentialsProvider credentialsProvider = this.awsAuth(properties);
        Duration pollerInterval = DEFAULT_POLLER_INTERVAL;
        Regions region = null;
        try {
          region = Regions.fromName(properties.getProperty(CLUSTER_REGION));
          int replicaUpdateInterval =
              Integer.parseInt(properties.getProperty(REPLICA_POLL_INTERVAL_PROPERTY_NAME));
          pollerInterval = Duration.ofSeconds(replicaUpdateInterval);
        } catch (NumberFormatException | NullPointerException e) {
          LOGGER.info(
              String.format(
                  "No or invalid polling interval specified. Using default replica poll interval of %s",
                  DEFAULT_POLLER_INTERVAL));
        } catch (IllegalArgumentException e) {
          LOGGER.log(
              Level.SEVERE,
              String.format(
                  "Invalid value [%s] for region. Must be one of [%s]",
                  properties.getProperty(CLUSTER_REGION),
                  Arrays.stream(Regions.values())
                      .map(r -> r.getName())
                      .collect(Collectors.joining(","))));
          return Optional.empty();
        }
        AuroraReadonlyEndpoint ro =
            new AuroraReadonlyEndpoint(uri.getHost(), credentialsProvider, pollerInterval, region);
        this.auroraClusters.put(uri, ro);
      }

      String nextReplica = this.auroraClusters.get(uri).getNextReplica();
      LOGGER.fine(
          String.format(
              "Obtained [%s] for the next replica to use for cluster [%s]",
              nextReplica, uri.getHost()));
      URI connectURI =
          new URI(
              String.format("%s:%s", JDBC_PREX, delegate),
              uri.getUserInfo(),
              nextReplica,
              uri.getPort(),
              uri.getPath(),
              uri.getQuery(),
              uri.getFragment());
      if (!this.delegates.containsKey(delegate)) {
        this.delegates.put(delegate, DriverManager.getDriver(connectURI.toASCIIString()));
      }
      return Optional.of(new ParsedUrl(delegate, connectURI.toASCIIString()));
    } catch (URISyntaxException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    if (url == null) {
      throw new SQLException("Url must not be null");
    }
    return driverPattern.matcher(url).matches();
  }

  /** {@inheritDoc} */
  @Override
  public Connection connect(String url, Properties properties) throws SQLException {
    Optional<ParsedUrl> parsedUrlOptional = this.parseUrlAndCacheDriver(url, properties);
    if (!parsedUrlOptional.isPresent()) {
      throw new SQLException("Invalid url: '" + url + "'");
    }
    ParsedUrl parsedUrl = parsedUrlOptional.get();
    return delegates
        .get(parsedUrl.getDelegateProtocol())
        .connect(parsedUrl.getDelegateUrl(), properties);
  }

  /** {@inheritDoc} */
  @Override
  public int getMajorVersion() {
    return Constants.AURORA_RO_MAJOR_VERSION;
  }

  /** {@inheritDoc} */
  @Override
  public int getMinorVersion() {
    return Constants.AURORA_RO_MINOR_VERSION;
  }

  /** {@inheritDoc} */
  @Override
  public Logger getParentLogger() {
    return LOGGER.getParent();
  }

  /** {@inheritDoc} */
  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) {
    return new DriverPropertyInfo[0];
  }

  /** {@inheritDoc} */
  @Override
  public boolean jdbcCompliant() {
    return false;
  }
}
