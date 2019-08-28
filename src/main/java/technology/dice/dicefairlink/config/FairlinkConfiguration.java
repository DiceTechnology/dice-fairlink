/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FairlinkConfiguration {
  private static final Logger LOGGER = Logger.getLogger(FairlinkConfiguration.class.getName());
  private static final int MAX_START_DELAY = 10;
  public static final String AWS_AUTH_MODE_PROPERTY_NAME = "auroraDiscoveryAuthMode";
  public static final String AWS_BASIC_CREDENTIALS_KEY = "auroraDiscoveryKeyId";
  public static final String AWS_BASIC_CREDENTIALS_SECRET = "auroraDiscoverKeySecret";
  public static final String REPLICA_POLL_INTERVAL_PROPERTY_NAME = "replicaPollInterval";
  public static final String TAGS_INTERVAL_PROPERTY_NAME = "tagsPollInterval";
  public static final String REPLICA_ENDPOINT_TEMPLATE = "replicaEndpointTemplate";
  public static final String DISCOVERY_MODE_PROPERTY_NAME = "discoveryMode";
  public static final String VALIDATE_CONNECTION = "validateConnection";
  public static final String CLUSTER_REGION = "auroraClusterRegion";
  public static final String FALLBACK_ENDPOINT = "fallbackEndpoint";
  private static final Duration DEFAULT_POLLER_INTERVAL = Duration.ofSeconds(30);
  private static final Duration DEFAULT_TAG_POLL_INTERVAL = Duration.ofMinutes(2);
  private static final String MYSQL = "mysql";
  private static final String AWS_ENDPOINT_OVERRIDE = "awsEndpointOverride";
  private final Region auroraClusterRegion;
  private final Optional<String> replicaEndpointTemplate;
  private final Optional<String> fallbackEndpoint;
  private final Optional<String> awsEndpointOverride;
  private final AwsCredentialsProvider awsCredentialsProvider;
  private final Duration replicaPollInterval;
  private final ReplicasDiscoveryMode replicasDiscoveryMode;
  private final Map<String, String> env;
  private final boolean validateConnection;
  private final Duration tagsPollerInterval;

  public FairlinkConfiguration(Properties properties, Map<String, String> env) {
    this.env = env;
    this.auroraClusterRegion = this.resolveRegion(properties);
    this.awsCredentialsProvider = this.awsAuth(properties);
    this.tagsPollerInterval = this.resolveTagPollerInterval(properties);
    this.replicaPollInterval = this.resolvePollerInterval(properties);
    this.replicasDiscoveryMode = this.resolveDiscoveryMode(properties);
    this.replicaEndpointTemplate = this.resolveReplicaEndpointTemplate(properties);
    this.validateConnection = this.resolveValidationConnection(properties);
    this.fallbackEndpoint = this.resolveFallbackEndpoint(properties);
    this.awsEndpointOverride = this.resolveAwsEndpointOverride(properties);
    this.validateConfiguration();
  }

  private Optional<String> resolveAwsEndpointOverride(Properties properties) {
    return Optional.ofNullable(properties.getProperty(AWS_ENDPOINT_OVERRIDE));
  }

  private Optional<String> resolveFallbackEndpoint(Properties properties) {
    return Optional.ofNullable(properties.getProperty(FALLBACK_ENDPOINT));
  }

  private Optional<String> resolveReplicaEndpointTemplate(Properties properties) {
    return Optional.ofNullable(properties.getProperty(REPLICA_ENDPOINT_TEMPLATE));
  }

  private boolean resolveValidationConnection(Properties properties) {
    return Optional.ofNullable(Boolean.parseBoolean(properties.getProperty(VALIDATE_CONNECTION)))
        .orElse(Boolean.TRUE);
  }

  private void validateConfiguration() {
    if (this.replicasDiscoveryMode == ReplicasDiscoveryMode.AWS_API) {
      this.validateAwsApiDiscovery();
    } else {
      this.validateSqlDiscovery();
    }

    this.replicaEndpointTemplate.orElseThrow(
        () ->
            new IllegalStateException(
                "Replica endpoint template mandatory. It is used for tag exclusion discovery and if an SQL discovery mode is selected"));
  }

  private void validateSqlDiscovery() {}

  private void validateAwsApiDiscovery() {}

  private ReplicasDiscoveryMode resolveDiscoveryMode(Properties properties) {
    return ReplicasDiscoveryMode.fromStringInsensitive(
            properties.getProperty(
                DISCOVERY_MODE_PROPERTY_NAME, ReplicasDiscoveryMode.AWS_API.name()))
        .orElse(ReplicasDiscoveryMode.AWS_API);
  }

  private AwsCredentialsProvider awsAuth(Properties properties) {
    AwsApiDiscoveryAuthMode authMode =
        AwsApiDiscoveryAuthMode.fromStringInsensitive(
                properties.getProperty(
                    AWS_AUTH_MODE_PROPERTY_NAME, AwsApiDiscoveryAuthMode.DEFAULT_CHAIN.name()))
            .orElse(AwsApiDiscoveryAuthMode.DEFAULT_CHAIN);
    LOGGER.log(Level.FINE, "authMode: {0}", authMode);
    switch (authMode) {
      case BASIC:
        String key = properties.getProperty(AWS_BASIC_CREDENTIALS_KEY);
        String secret = properties.getProperty(AWS_BASIC_CREDENTIALS_SECRET);
        if (key == null || secret == null) {
          throw new IllegalStateException(
              String.format(
                  "For basic authentication both [%s] and [%s] must both be set",
                  AWS_BASIC_CREDENTIALS_KEY, AWS_BASIC_CREDENTIALS_SECRET));
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(key, secret));
      case ENVIRONMENT:
        return EnvironmentVariableCredentialsProvider.create();
      default:
        // DEFAULT_CHAIN
        return DefaultCredentialsProvider.create();
    }
  }

  private Region resolveRegion(final Properties properties) {
    final Optional<String> propertyRegion =
        Optional.ofNullable(properties.getProperty(CLUSTER_REGION));
    LOGGER.log(Level.FINE, "Region from property: {0}", propertyRegion);
    return propertyRegion
        .map(r -> Region.of(r))
        .orElseGet(
            () -> {
              final String envRegion = env.get("AWS_DEFAULT_REGION");
              LOGGER.log(Level.FINE, "Region from environment: {0}", envRegion);
              if (envRegion != null) {
                return Region.of(envRegion);
              }
              throw new IllegalStateException(
                  "Region is mandatory for exclusion tag discovery and replica discovery on AWS_API mode");
            });
  }

  private Duration resolvePollerInterval(Properties properties) {
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

  private Duration resolveTagPollerInterval(Properties properties) {
    try {
      return Duration.ofSeconds(
          Integer.parseInt(properties.getProperty(TAGS_INTERVAL_PROPERTY_NAME)));
    } catch (IllegalArgumentException | NullPointerException e) {
      LOGGER.warning(
          String.format(
              "No or invalid tags polling interval specified. Using default tags poll interval of %s",
              DEFAULT_TAG_POLL_INTERVAL));
      return DEFAULT_TAG_POLL_INTERVAL;
    }
  }

  public Duration randomBoundDelay() {
    return Duration.ofMillis(
        new Float(ThreadLocalRandom.current().nextFloat() * MAX_START_DELAY * 1000).longValue());
  }

  public Duration getReplicaPollInterval() {
    return replicaPollInterval;
  }

  public Duration getTagsPollerInterval() {
    return tagsPollerInterval;
  }

  public Optional<String> getAwsEndpointOverride() {
    return awsEndpointOverride;
  }

  public AwsCredentialsProvider getAwsCredentialsProvider() {
    return awsCredentialsProvider;
  }

  public Optional<String> getFallbackEndpoint() {
    return fallbackEndpoint;
  }

  public Region getAuroraClusterRegion() {
    return auroraClusterRegion;
  }

  public String hostname(String fromDbIdentifier) {
    return String.format(replicaEndpointTemplate.get(), fromDbIdentifier);
  }

  public boolean isValidateConnection() {
    return validateConnection;
  }

  public ReplicasDiscoveryMode getReplicasDiscoveryMode() {
    return replicasDiscoveryMode;
  }

  public boolean isDiscoveryModeValidForDelegate(String delegateProtocol) {
    return this.getReplicasDiscoveryMode() != ReplicasDiscoveryMode.SQL_MYSQL
        || delegateProtocol.equalsIgnoreCase(MYSQL);
  }
}
