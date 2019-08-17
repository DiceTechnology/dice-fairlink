package technology.dice.dicefairlink.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.time.Duration;
import java.util.Properties;

public class FairlinkConfigurationTest {
  @Test
  public void goodBasicCredentials() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "SQL_MYSQL");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    FairlinkConfiguration underTest = new FairlinkConfiguration(p, Maps.newHashMap());
    Assert.assertEquals(Region.EU_WEST_1, underTest.getAuroraClusterRegion());
    final StaticCredentialsProvider expectedCredentials =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("keyId", "keySecret"));
    Assert.assertTrue(underTest.getAwsCredentialsProvider() instanceof StaticCredentialsProvider);
    Assert.assertEquals("keyId", expectedCredentials.resolveCredentials().accessKeyId());
    Assert.assertEquals("keySecret", expectedCredentials.resolveCredentials().secretAccessKey());
    Assert.assertEquals(ReplicasDiscoveryMode.SQL_MYSQL, underTest.getReplicasDiscoveryMode());
    Assert.assertEquals(Duration.ofSeconds(10), underTest.getTagsPollerInterval());
    Assert.assertEquals(Duration.ofSeconds(5), underTest.getReplicaPollInterval());
    Assert.assertTrue(underTest.isValidateConnection());
    Assert.assertTrue(underTest.isDiscoveryModeValidForDelegate("mysql"));
    Assert.assertFalse(underTest.isDiscoveryModeValidForDelegate("postgresql"));
    Assert.assertEquals("replica1.rest-of-myhost.name", underTest.hostname("replica1"));
    final Duration jitter = underTest.randomBoundDelay();
    Assert.assertFalse(jitter.isNegative());
    Assert.assertTrue(jitter.compareTo(Duration.ofSeconds(10)) <= 0);
  }

  @Test
  public void goodBasicCredentialsRegionFromEnvironment() {
    Properties p = new Properties();
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "SQL_MYSQL");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    FairlinkConfiguration underTest =
        new FairlinkConfiguration(
            p, ImmutableMap.of("AWS_DEFAULT_REGION", Region.AP_NORTHEAST_1.toString()));
    Assert.assertEquals(Region.AP_NORTHEAST_1, underTest.getAuroraClusterRegion());
    final StaticCredentialsProvider expectedCredentials =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("keyId", "keySecret"));
    Assert.assertTrue(underTest.getAwsCredentialsProvider() instanceof StaticCredentialsProvider);
    Assert.assertEquals("keyId", expectedCredentials.resolveCredentials().accessKeyId());
    Assert.assertEquals("keySecret", expectedCredentials.resolveCredentials().secretAccessKey());
    Assert.assertEquals(ReplicasDiscoveryMode.SQL_MYSQL, underTest.getReplicasDiscoveryMode());
    Assert.assertEquals(Duration.ofSeconds(10), underTest.getTagsPollerInterval());
    Assert.assertEquals(Duration.ofSeconds(5), underTest.getReplicaPollInterval());
    Assert.assertTrue(underTest.isValidateConnection());
    Assert.assertTrue(underTest.isDiscoveryModeValidForDelegate("mysql"));
    Assert.assertFalse(underTest.isDiscoveryModeValidForDelegate("postgresql"));
    Assert.assertEquals("replica1.rest-of-myhost.name", underTest.hostname("replica1"));
    final Duration jitter = underTest.randomBoundDelay();
    Assert.assertFalse(jitter.isNegative());
    Assert.assertTrue(jitter.compareTo(Duration.ofSeconds(10)) <= 0);
  }

  @Test
  public void goodEnvironmentCredentials() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "environment");
    p.setProperty("discoveryMode", "AWS_API");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    FairlinkConfiguration underTest =
        new FairlinkConfiguration(
            p, ImmutableMap.of("AWS_SECRET_KEY", "key", "AWS_SECRET_ACCESS_KEY", "secret"));
    Assert.assertEquals(Region.EU_WEST_1, underTest.getAuroraClusterRegion());
    Assert.assertTrue(
        underTest.getAwsCredentialsProvider() instanceof EnvironmentVariableCredentialsProvider);
    Assert.assertEquals(ReplicasDiscoveryMode.AWS_API, underTest.getReplicasDiscoveryMode());
    Assert.assertEquals(Duration.ofSeconds(10), underTest.getTagsPollerInterval());
    Assert.assertEquals(Duration.ofSeconds(5), underTest.getReplicaPollInterval());
    Assert.assertTrue(underTest.isValidateConnection());
    Assert.assertTrue(underTest.isDiscoveryModeValidForDelegate("mysql"));
    Assert.assertTrue(underTest.isDiscoveryModeValidForDelegate("postgresql"));
    Assert.assertEquals("replica1.rest-of-myhost.name", underTest.hostname("replica1"));
    final Duration jitter = underTest.randomBoundDelay();
    Assert.assertFalse(jitter.isNegative());
    Assert.assertTrue(jitter.compareTo(Duration.ofSeconds(10)) <= 0);
  }

  @Test
  public void goodDefaultCredentials() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("discoveryMode", "SQL_MYSQL");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    FairlinkConfiguration underTest =
        new FairlinkConfiguration(
            p, ImmutableMap.of("AWS_SECRET_KEY", "key", "AWS_SECRET_ACCESS_KEY", "secret"));
    Assert.assertEquals(Region.EU_WEST_1, underTest.getAuroraClusterRegion());
    Assert.assertTrue(underTest.getAwsCredentialsProvider() instanceof DefaultCredentialsProvider);
    Assert.assertEquals(ReplicasDiscoveryMode.SQL_MYSQL, underTest.getReplicasDiscoveryMode());
    Assert.assertEquals(Duration.ofSeconds(10), underTest.getTagsPollerInterval());
    Assert.assertEquals(Duration.ofSeconds(5), underTest.getReplicaPollInterval());
    Assert.assertTrue(underTest.isValidateConnection());
    Assert.assertTrue(underTest.isDiscoveryModeValidForDelegate("mysql"));
    Assert.assertFalse(underTest.isDiscoveryModeValidForDelegate("postgresql"));
    Assert.assertEquals("replica1.rest-of-myhost.name", underTest.hostname("replica1"));
    final Duration jitter = underTest.randomBoundDelay();
    Assert.assertFalse(jitter.isNegative());
    Assert.assertTrue(jitter.compareTo(Duration.ofSeconds(10)) <= 0);
  }

  @Test(expected = IllegalStateException.class)
  public void basicWithoutKey() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "SQL_MYSQL");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    new FairlinkConfiguration(p, Maps.newHashMap());
  }

  @Test(expected = IllegalStateException.class)
  public void basicWithoutSecret() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("discoveryMode", "SQL_MYSQL");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    new FairlinkConfiguration(p, Maps.newHashMap());
  }

  @Test(expected = IllegalStateException.class)
  public void awsApiWithoutRegion() {
    Properties p = new Properties();
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "AWS_API");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    new FairlinkConfiguration(p, Maps.newHashMap());
  }

  @Test(expected = IllegalStateException.class)
  public void noReplicaTemplate() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "SQL_MYSQL");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("validateConnection", "true");
    new FairlinkConfiguration(p, Maps.newHashMap());
  }

  @Test
  public void defaultPollerDurationsAbsent() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "SQL_MYSQL");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    FairlinkConfiguration underTest = new FairlinkConfiguration(p, Maps.newHashMap());
    Assert.assertEquals(Duration.ofMinutes(2), underTest.getTagsPollerInterval());
    Assert.assertEquals(Duration.ofSeconds(30), underTest.getReplicaPollInterval());
  }

  @Test
  public void defaultPollerDurationsInvalid() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "SQL_MYSQL");
    p.setProperty("replicaPollInterval", "abc");
    p.setProperty("tagsPollInterval", "def");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    FairlinkConfiguration underTest = new FairlinkConfiguration(p, Maps.newHashMap());
    Assert.assertEquals(Duration.ofMinutes(2), underTest.getTagsPollerInterval());
    Assert.assertEquals(Duration.ofSeconds(30), underTest.getReplicaPollInterval());
  }
}
