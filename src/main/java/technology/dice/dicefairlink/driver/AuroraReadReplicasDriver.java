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
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuroraReadReplicasDriver implements Driver {

  public static final String AWS_AUTH_MODE_PROPERTY_NAME = "auroraDiscoveryAuthMode";
  public static final String AWS_BASIC_CREDENTIALS_KEY = "auroraDiscoveryKeyId";
  public static final String AWS_BASIC_CREDENTIALS_SECRET = "auroraDiscoverKeySecret";
  public static final String REPLICA_POLL_INTERVAL_PROPERTY_NAME = "replicaPollInterval";
  public static final String CLUSTER_REGION = "auroraClusterRegion";

  private static final Logger LOGGER = Logger.getLogger(AuroraReadReplicasDriver.class.getName());
  private static final String DRIVER_PROTOCOL = "auroraro";
  private static final Pattern driverPattern =
      Pattern.compile("jdbc:" + DRIVER_PROTOCOL + ":(?<delegate>[^:]*):(?<uri>.*\\/\\/.+)");
  private static final Duration DEFAULT_POLLER_INTERVAL = Duration.ofSeconds(30);
  private static final String JDBC_PREFIX = "jdbc";
  private final Map<String, Driver> delegates = new HashMap<>();
  private final Map<URI, AuroraReadonlyEndpoint> auroraClusters = new HashMap<>();

  private final ScheduledExecutorService executor;
  private final AtomicReference<String> lastReplica = new AtomicReference<>();

  static {
    try {
      DriverManager.registerDriver(
          new AuroraReadReplicasDriver(new ScheduledThreadPoolExecutor(1)));
      LOGGER.fine("AuroraReadReplicasDriver is now registered.");
    } catch (Exception e) {
      throw new RuntimeException("Can't register driver!", e);
    }
  }

  public AuroraReadReplicasDriver(final ScheduledExecutorService executor) {
    LOGGER.fine("Starting...");
    this.executor = executor;
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    if (url == null) {
      throw new SQLException("Url must not be null");
    }
    final boolean matches = driverPattern.matcher(url).matches();
    LOGGER.info(String.format("Accepting URL: [%s] : %s", url, matches));
    return matches;
  }

  /** {@inheritDoc} */
  @Override
  public Connection connect(final String url, final Properties properties) throws SQLException {
    try {
      final Optional<ParsedUrl> parsedUrlOptional = parseUrlAndCacheDriver(url, properties);
      final ParsedUrl parsedUrl =
          parsedUrlOptional.orElseThrow(
              () -> new SQLException(String.format("Invalid url: [%s]", url)));
      // TODO if our info about replica is wrong (say, instance is down), then following 'connect'
      // will throw, and we must re-query Aurora Cluster and try again once.
      return delegates
          .get(parsedUrl.getDelegateProtocol())
          .connect(parsedUrl.getDelegateUrl(), properties);
    } catch (URISyntaxException ex) {
      throw new SQLException(ex);
    }
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

  private Region getRegion(final Properties properties) {
    final String propertyRegion = properties.getProperty(CLUSTER_REGION);
    LOGGER.log(Level.FINE, "Region from property: {0}", propertyRegion);
    if (propertyRegion != null) {
      return RegionUtils.getRegion(propertyRegion);
    }

    final String commandLinveVariableRegion = System.getProperty(CLUSTER_REGION);
    LOGGER.log(Level.FINE, "Region from command line variable: {0}", commandLinveVariableRegion);
    if (commandLinveVariableRegion != null) {
      return RegionUtils.getRegion(commandLinveVariableRegion);
    }

    final String envRegion = System.getenv("AWS_DEFAULT_REGION");
    LOGGER.log(Level.FINE, "Region from environment: {0}", envRegion);
    if (envRegion != null) {
      return RegionUtils.getRegion(envRegion);
    }
    throw new RuntimeException(
        "Region is null. Please either provide property ["
            + CLUSTER_REGION
            + "] or set the environment variable [AWS_DEFAULT_REGION]");
  }

  private AWSCredentialsProvider awsAuth(Properties properties) throws SQLException {
    DiscoveryAuthMode authMode =
        DiscoveryAuthMode.fromStringInsensitive(
                properties.getProperty(AWS_AUTH_MODE_PROPERTY_NAME, "environment"))
            .orElse(DiscoveryAuthMode.ENVIRONMENT);
    LOGGER.log(Level.FINE, "authMode: {0}", authMode);
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
        // Too sensitive. Uncomment for debug only.
        // if (LOGGER.isLoggable(Level.FINE)) {
        // logAwsAccessKeys();
        // }
        return new EnvironmentVariableCredentialsProvider();
    }
  }

  // Too sensitive. Uncomment for debug only.
  // private void logAwsAccessKeys() {
  //   final String accessKey =
  //       getDualEnvironmentVariable(ACCESS_KEY_ENV_VAR, ALTERNATE_ACCESS_KEY_ENV_VAR);
  //   final String secretKey =
  //       getDualEnvironmentVariable(SECRET_KEY_ENV_VAR, ALTERNATE_SECRET_KEY_ENV_VAR);
  //   LOGGER.log(Level.FINE, "accessKey: {0}", accessKey);
  //   LOGGER.log(Level.FINE, "secretKey: {0}", secretKey);
  // }
  // private String getDualEnvironmentVariable(final String primaryVarName,
  //   final String secondaryVarName) {
  //   final String primaryVal = System.getenv(primaryVarName);
  //   if (primaryVal == null) {
  //     return System.getenv(secondaryVarName);
  //   }
  //   return primaryVal;
  // }
  private Optional<ParsedUrl> parseUrlAndCacheDriver(final String url, final Properties properties)
      throws SQLException, URISyntaxException {
    LOGGER.log(Level.FINE, "URI: {0}", url);
    Matcher matcher = driverPattern.matcher(url);
    if (!matcher.matches()) {
      LOGGER.log(Level.INFO, "URI not supported [{0}]. Returning empty.", url);
      return Optional.empty();
    }
    String delegate = matcher.group("delegate");
    LOGGER.log(Level.FINE, "Delegate driver: {0}", delegate);
    final String clusterURI = DRIVER_PROTOCOL + ":" + matcher.group("uri");
    try {
      URI uri = new URI(clusterURI);
      LOGGER.log(Level.FINE, "Driver URI: {0}", uri);
      final Region region = getRegion(properties);
      LOGGER.log(Level.FINE, "Region: {0}", region);

      if (!this.auroraClusters.containsKey(uri)) {
        // because AWS credentials, region and poll interval properties
        // are only processed once per uri, the driver does not support dynamically changing them
        final Duration pollerInterval = getPollerInterval(properties);
        final AWSCredentialsProvider credentialsProvider = awsAuth(properties);
        final AuroraReadonlyEndpoint roEndpoint =
            new AuroraReadonlyEndpoint(
                uri.getHost(), credentialsProvider, pollerInterval, region, executor);

        LOGGER.log(Level.FINE, "RO url: {0}", uri.getHost());
        this.auroraClusters.put(uri, roEndpoint);
      }

      final String nextReplica = getNextReplica(auroraClusters.get(uri));
      LOGGER.fine(
          String.format(
              "Obtained [%s] for the next replica to use for cluster [%s]",
              nextReplica, uri.getHost()));
      final String delegatedReplicaUri =
          new URI(
                  String.format("%s:%s", JDBC_PREFIX, delegate),
                  uri.getUserInfo(),
                  nextReplica,
                  uri.getPort(),
                  uri.getPath(),
                  uri.getQuery(),
                  uri.getFragment())
              .toASCIIString();
      LOGGER.log(Level.INFO, "URI to connect to: {0}", delegatedReplicaUri);

      addDriverForDelegate(delegate, delegatedReplicaUri);

      return Optional.of(new ParsedUrl(delegate, delegatedReplicaUri));
    } catch (URISyntaxException e) {
      LOGGER.log(Level.SEVERE, "Can not use URI: " + clusterURI, e);
      return Optional.empty();
    }
  }

  private void addDriverForDelegate(String delegate, final String stringURI) throws SQLException {
    if (!this.delegates.containsKey(delegate)) {
      this.delegates.put(delegate, DriverManager.getDriver(stringURI));
    }
  }

  private synchronized String getNextReplica(final AuroraReadonlyEndpoint replicaEndPoint) {
    String nextReplica = replicaEndPoint.getNextReplica();
    if (nextReplica.equals(lastReplica.get())) {
      nextReplica = replicaEndPoint.getNextReplica();
    }
    lastReplica.set(nextReplica);
    return nextReplica;
  }

  private Duration getPollerInterval(Properties properties) {
    try {
      return Duration.ofSeconds(
          Integer.parseInt(properties.getProperty(REPLICA_POLL_INTERVAL_PROPERTY_NAME)));
    } catch (IllegalArgumentException | NullPointerException e) {
      LOGGER.warning(
          String.format(
              "No or invalid polling interval specified. Using default replica poll interval of %s",
              DEFAULT_POLLER_INTERVAL));
      return DEFAULT_POLLER_INTERVAL;
    }
  }
}
