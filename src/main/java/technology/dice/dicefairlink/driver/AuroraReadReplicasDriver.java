/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.driver;

import software.amazon.awssdk.regions.Region;
import technology.dice.dicefairlink.AuroraReadonlyEndpoint;
import technology.dice.dicefairlink.ParsedUrl;
import technology.dice.dicefairlink.config.FairlinkConfiguration;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuroraReadReplicasDriver implements Driver {
  private static final Logger LOGGER = Logger.getLogger(AuroraReadReplicasDriver.class.getName());
  private final Map<String, Driver> delegates = new HashMap<>();
  private final Map<String, AuroraReadonlyEndpoint> auroraClusters = new HashMap<>();

  private final Supplier<ScheduledExecutorService> discoveryExecutor;
  private final Supplier<ScheduledExecutorService> tagPollExecutor;

  static {
    try {
      DriverManager.registerDriver(new AuroraReadReplicasDriver());
      LOGGER.fine("AuroraReadReplicasDriver is now registered.");
    } catch (Exception e) {
      throw new RuntimeException("Can't register driver!", e);
    }
  }

  public AuroraReadReplicasDriver() {
    this(() -> Executors.newScheduledThreadPool(1), () -> Executors.newScheduledThreadPool(1));
  }

  public AuroraReadReplicasDriver(
      final Supplier<ScheduledExecutorService> discoveryExecutor,
      final Supplier<ScheduledExecutorService> tagPollExecutor) {
    LOGGER.fine("Starting...");
    this.discoveryExecutor = discoveryExecutor;
    this.tagPollExecutor = tagPollExecutor;
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    if (url == null) {
      throw new SQLException("Url must not be null");
    }
    boolean matches = FairlinkConnectionString.accepts(url);
    LOGGER.info(String.format("Accepting URL: [%s] : %s", url, matches));
    return matches;
  }

  /** {@inheritDoc} */
  @Override
  public Connection connect(final String url, final Properties properties) throws SQLException {
    final Optional<ParsedUrl> parsedUrlOptional = parseUrlAndCacheDriver(url, properties);
    final ParsedUrl parsedUrl =
        parsedUrlOptional.orElseThrow(
            () -> new SQLException(String.format("Invalid url: [%s]", url)));
    // TODO if our info about replica is wrong (say, instance is down), then following 'connect'
    // will throw, and we must re-query Aurora Cluster and try again once.
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

  private Optional<ParsedUrl> parseUrlAndCacheDriver(final String url, final Properties properties)
      throws SQLException {
    LOGGER.log(Level.FINE, "URI: {0}", url);
    try {

      FairlinkConnectionString fairlinkConnectionString =
          new FairlinkConnectionString(url, properties);

      if (!this.auroraClusters.containsKey(fairlinkConnectionString.getFairlinkUri())) {
        FairlinkConfiguration fairlinkConfiguration =
            new FairlinkConfiguration(properties, System.getenv());
        LOGGER.log(
            Level.FINE, "Delegate driver: {0}", fairlinkConnectionString.getDelegateProtocol());
        LOGGER.log(Level.FINE, "Driver URI: {0}", fairlinkConnectionString.getFairlinkUri());
        final Region region = fairlinkConfiguration.getAuroraClusterRegion();
        LOGGER.log(Level.FINE, "Region: {0}", region);
        // because AWS credentials, region and poll interval properties
        // are only processed once per uri, the driver does not support dynamically changing them
        final AuroraReadonlyEndpoint roEndpoint =
            new AuroraReadonlyEndpoint(
                fairlinkConnectionString,
                fairlinkConfiguration,
                discoveryExecutor.get(),
                tagPollExecutor.get());

        LOGGER.log(Level.FINE, "RO url: {0}", fairlinkConnectionString.getHost());
        if (!fairlinkConfiguration.isDiscoveryModeValidForDelegate(
            fairlinkConnectionString.getDelegateProtocol())) {
          throw new IllegalStateException(
              fairlinkConfiguration.getReplicasDiscoveryMode()
                  + " is not a valid discovery node for underlying protocol "
                  + fairlinkConnectionString.getDelegateProtocol());
        }
        this.auroraClusters.put(fairlinkConnectionString.getFairlinkUri(), roEndpoint);
      }

      this.addDriverForDelegate(
          fairlinkConnectionString.getDelegateProtocol(),
          fairlinkConnectionString.delegateConnectionString());
      final String nextReplica =
          auroraClusters.get(fairlinkConnectionString.getFairlinkUri()).getNextReplica();
      LOGGER.fine(
          String.format(
              "Obtained [%s] for the next replica to use for cluster [%s]",
              nextReplica, fairlinkConnectionString.getHost()));
      final String delegatedReplicaUri =
          fairlinkConnectionString.delegateConnectionString(nextReplica);

      LOGGER.log(Level.FINE, "URI to connect to: {0}", delegatedReplicaUri);

      return Optional.of(
          new ParsedUrl(fairlinkConnectionString.getDelegateProtocol(), delegatedReplicaUri));
    } catch (URISyntaxException | NoSuchElementException | IllegalArgumentException e) {
      LOGGER.log(Level.SEVERE, "Can not get replicas for cluster URI: " + url, e);
      return Optional.empty();
    }
  }

  private void addDriverForDelegate(String delegate, final String stringURI) throws SQLException {
    if (!this.delegates.containsKey(delegate)) {
      this.delegates.put(delegate, DriverManager.getDriver(stringURI));
    }
  }
}
