package technology.dice.dicefairlink.config;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FairlinkConfiguration {
  private static final Logger LOGGER = Logger.getLogger(FairlinkConfiguration.class.getName());
  public static final String AWS_AUTH_MODE_PROPERTY_NAME = "auroraDiscoveryAuthMode";
  public static final String AWS_BASIC_CREDENTIALS_KEY = "auroraDiscoveryKeyId";
  public static final String AWS_BASIC_CREDENTIALS_SECRET = "auroraDiscoverKeySecret";
  public static final String REPLICA_POLL_INTERVAL_PROPERTY_NAME = "replicaPollInterval";
  public static final String CLUSTER_REGION = "auroraClusterRegion";

  private static final Duration DEFAULT_POLLER_INTERVAL = Duration.ofSeconds(30);
  private final Optional<Region> auroraClusterRegion;
  private final AWSCredentialsProvider awsCredentialsProvider;
  private final Duration replicaPollInterval;
  private final ReplicasDiscoveryMode replicasDiscoveryMode;
  private final Map<String, String> env;

  public FairlinkConfiguration(String url, Properties properties) {
    this(url, properties, System.getenv());
  }

  public FairlinkConfiguration(String url, Properties properties, Map<String, String> env) {
    this.env = env;
    this.auroraClusterRegion = Optional.ofNullable(this.resolveRegion(properties));
    this.awsCredentialsProvider = this.awsAuth(properties);
    this.replicaPollInterval = this.resolvePollerInterval(properties);
    this.replicasDiscoveryMode = null;
  }

  private AWSCredentialsProvider awsAuth(Properties properties) {
    AwsApiDiscoveryAuthMode authMode =
        AwsApiDiscoveryAuthMode.fromStringInsensitive(
                properties.getProperty(AWS_AUTH_MODE_PROPERTY_NAME, "default_chain"))
            .orElse(AwsApiDiscoveryAuthMode.DEFAULT_CHAIN);
    LOGGER.log(Level.FINE, "authMode: {0}", authMode);
    switch (authMode) {
      case BASIC:
        String key = properties.getProperty(AWS_BASIC_CREDENTIALS_KEY);
        String secret = properties.getProperty(AWS_BASIC_CREDENTIALS_SECRET);
        if (key == null || secret == null) {
          throw new RuntimeException(
              String.format(
                  "For basic authentication both [%s] and [%s] must both be set",
                  AWS_BASIC_CREDENTIALS_KEY, AWS_BASIC_CREDENTIALS_SECRET));
        }
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(key, secret));
      case ENVIRONMENT:
        if (LOGGER.isLoggable(Level.FINE)) {
          logAwsAccessKeys();
        }
        return new EnvironmentVariableCredentialsProvider();
      default:
        // DEFAULT_CHAIN
        return DefaultAWSCredentialsProviderChain.getInstance();
    }
  }

  private Region resolveRegion(final Properties properties) {
    final String propertyRegion = properties.getProperty(CLUSTER_REGION);
    LOGGER.log(Level.FINE, "Region from property: {0}", propertyRegion);
    if (propertyRegion != null) {
      return RegionUtils.getRegion(propertyRegion);
    }

    final String envRegion = this.env.get("AWS_DEFAULT_REGION");
    LOGGER.log(Level.FINE, "Region from environment: {0}", envRegion);
    if (envRegion != null) {
      return RegionUtils.getRegion(envRegion);
    }
    throw new RuntimeException(
        "Region is null. Please either provide property ["
            + CLUSTER_REGION
            + "] or set the environment variable [AWS_DEFAULT_REGION]");
  }

  private void logAwsAccessKeys() {
    final String accessKey =
        getDualEnvironmentVariable(
            SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR,
            SDKGlobalConfiguration.ALTERNATE_ACCESS_KEY_ENV_VAR);
    final String secretKey =
        getDualEnvironmentVariable(
            SDKGlobalConfiguration.SECRET_KEY_ENV_VAR,
            SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR);
    LOGGER.log(
        Level.FINE,
        String.format(
            "accessKey: %s**",
            accessKey != null && accessKey.length() > 4 ? accessKey.substring(0, 3) : ""));
    LOGGER.log(
        Level.FINE,
        String.format(
            "secretKey: %s**",
            secretKey != null && secretKey.length() > 4 ? secretKey.substring(0, 3) : ""));
  }

  private String getDualEnvironmentVariable(
      final String primaryVarName, final String secondaryVarName) {
    final String primaryVal = this.env.get(primaryVarName);
    if (primaryVal == null) {
      return this.env.get(secondaryVarName);
    }
    return primaryVal;
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

  public Duration getReplicaPollInterval() {
    return replicaPollInterval;
  }

  public AWSCredentialsProvider getAwsCredentialsProvider() {
    return awsCredentialsProvider;
  }

  public Region getAuroraClusterRegion() {
    return auroraClusterRegion.get();
  }
}
