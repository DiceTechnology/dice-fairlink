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
import technology.dice.dicefairlink.discovery.members.FairlinkMemberFinder;
import technology.dice.dicefairlink.discovery.members.JdbcConnectionValidator;
import technology.dice.dicefairlink.discovery.members.MemberFinderMethod;
import technology.dice.dicefairlink.discovery.members.ReplicaValidator;
import technology.dice.dicefairlink.discovery.members.awsapi.AwsApiReplicasFinder;
import technology.dice.dicefairlink.discovery.members.sql.MySQLReplicasFinder;
import technology.dice.dicefairlink.discovery.tags.TagFilter;
import technology.dice.dicefairlink.discovery.tags.awsapi.ResourceGroupApiTagDiscovery;
import technology.dice.dicefairlink.iterators.RandomisedCyclicIterator;
import technology.dice.dicefairlink.iterators.SizedIterator;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuroraReadReplicasDriver implements Driver {
  private static final Logger LOGGER = Logger.getLogger(AuroraReadReplicasDriver.class.getName());
  private final Map<String, Driver> delegates = new HashMap<>();
  private final Map<String, AuroraReadonlyEndpoint> auroraClusters = new HashMap<>();

  private final Supplier<ScheduledExecutorService> discoveryExecutor;
  private final Supplier<ScheduledExecutorService> tagPollExecutor;
  private final Optional<TagFilter> tagFilter;
  private final Optional<FairlinkMemberFinder> fairlinkMemberFinder;
  private final Optional<Function<Collection<String>, SizedIterator<String>>> sizedIteratorBuilder;
  private final Optional<ReplicaValidator> replicaValidator;

  static {
    try {
      DriverManager.registerDriver(new AuroraReadReplicasDriver());
      LOGGER.fine("AuroraReadReplicasDriver is now registered.");
    } catch (Exception e) {
      throw new RuntimeException("Can't register driver!", e);
    }
  }

  public AuroraReadReplicasDriver() {
    this(
        () -> Executors.newScheduledThreadPool(1),
        () -> Executors.newScheduledThreadPool(1),
        null,
        null,
        null,
        null);
  }

  public AuroraReadReplicasDriver(
      final Supplier<ScheduledExecutorService> discoveryExecutor,
      final Supplier<ScheduledExecutorService> tagPollExecutor,
      final TagFilter tagFilter,
      final FairlinkMemberFinder memberFinder,
      final ReplicaValidator replicaValidator,
      final Function<Collection<String>, SizedIterator<String>> iteratorBuilder) {
    LOGGER.fine("Starting...");
    this.discoveryExecutor = discoveryExecutor;
    this.tagPollExecutor = tagPollExecutor;
    this.tagFilter = Optional.ofNullable(tagFilter);
    this.replicaValidator = Optional.ofNullable(replicaValidator);
    this.fairlinkMemberFinder = Optional.ofNullable(memberFinder);
    this.sizedIteratorBuilder = Optional.ofNullable(iteratorBuilder);
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

    if (!parsedUrlOptional.isPresent()) {
      return null;
    }
    return delegates
        .get(parsedUrlOptional.get().getDelegateProtocol())
        .connect(parsedUrlOptional.get().getDelegateUrl(), properties);
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

        this.addDriverForDelegate(
            fairlinkConnectionString.getDelegateProtocol(),
            fairlinkConnectionString.delegateConnectionString());

        final AuroraReadonlyEndpoint roEndpoint =
            new AuroraReadonlyEndpoint(
                fairlinkConfiguration,
                this.fairlinkMemberFinder.orElseGet(
                    () ->
                        newMemberFinder(
                            fairlinkConnectionString, fairlinkConfiguration, properties)),
                this.discoveryExecutor.get());

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

    } catch (URISyntaxException e) {
      LOGGER.log(Level.FINE, "Can not get replicas for cluster URI: " + url, e);
      return Optional.empty();
    } catch (NoSuchElementException | IllegalArgumentException e) {
      LOGGER.log(Level.SEVERE, "Can not get replicas for cluster URI: " + url, e);
      return Optional.empty();
    }
  }

  private FairlinkMemberFinder newMemberFinder(
      FairlinkConnectionString fairlinkConnectionString,
      FairlinkConfiguration fairlinkConfiguration,
      Properties properties) {
    return new FairlinkMemberFinder(
        fairlinkConfiguration,
        fairlinkConnectionString,
        this.tagPollExecutor.get(),
        this.tagFilter.orElseGet(() -> new ResourceGroupApiTagDiscovery(fairlinkConfiguration)),
        this.newMemberFinderMethod(
            fairlinkConfiguration,
            fairlinkConnectionString,
            this.delegates.get(fairlinkConnectionString.getDelegateProtocol()),
            properties),
        this.sizedIteratorBuilder.orElse(strings -> RandomisedCyclicIterator.of(strings)),
        this.replicaValidator.orElse(
            new JdbcConnectionValidator(
                this.delegates.get(fairlinkConnectionString.getDelegateProtocol()))));
  }

  private MemberFinderMethod newMemberFinderMethod(
      FairlinkConfiguration fairlinkConfiguration,
      FairlinkConnectionString fairlinkConnectionString,
      Driver driver,
      Properties properties) {
    switch (fairlinkConfiguration.getReplicasDiscoveryMode()) {
      case AWS_API:
        return new AwsApiReplicasFinder(fairlinkConfiguration, fairlinkConnectionString);
      case SQL_MYSQL:
        return new MySQLReplicasFinder(
            fairlinkConnectionString,
            driver,
            properties.getProperty("_fairlinkMySQLSchemaOverride"));
      default:
        throw new IllegalArgumentException(
            fairlinkConfiguration.getReplicasDiscoveryMode().name()
                + "is not a valid discovery mode");
    }
  }

  private void addDriverForDelegate(String delegate, final String stringURI) throws SQLException {
    if (!this.delegates.containsKey(delegate)) {
      this.delegates.put(delegate, DriverManager.getDriver(stringURI));
    }
  }
}
