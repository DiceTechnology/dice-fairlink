package technology.dice.dicefairlink;

import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.support.discovery.members.FixedMemberFinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class AuroraReadonlyEndpointTest {
  private Properties baseTestProperties() {
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
    return p;
  }

  @Test
  public void multipleReplicas() {
    AuroraReadonlyEndpoint underTest =
        new AuroraReadonlyEndpoint(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FixedMemberFinder(ImmutableSet.of("r1", "r2", "r3")),
            new StepByStepExecutor(1));
    Assert.assertEquals("r1", underTest.getNextReplica());
    Assert.assertEquals("r2", underTest.getNextReplica());
    Assert.assertEquals("r3", underTest.getNextReplica());
    Assert.assertEquals("r1", underTest.getNextReplica());
  }

  @Test
  public void oneReplica() {
    AuroraReadonlyEndpoint underTest =
        new AuroraReadonlyEndpoint(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FixedMemberFinder(ImmutableSet.of("r1")),
            new StepByStepExecutor(1));
    Assert.assertEquals("r1", underTest.getNextReplica());
    Assert.assertEquals("r1", underTest.getNextReplica());
    Assert.assertEquals("r1", underTest.getNextReplica());
  }

  @Test
  public void triesToSkipRepeated() {
    AuroraReadonlyEndpoint underTest =
        new AuroraReadonlyEndpoint(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FixedMemberFinder(ImmutableList.of("r1", "r1", "r2")),
            new StepByStepExecutor(1));
    Assert.assertEquals("r1", underTest.getNextReplica());
    Assert.assertEquals("r2", underTest.getNextReplica());
  }

  @Test
  public void nullEntry() {
    ArrayList acceptsNulls = new ArrayList(3);
    acceptsNulls.add(null);
    acceptsNulls.add(null);
    acceptsNulls.add("r3");
    AuroraReadonlyEndpoint underTest =
        new AuroraReadonlyEndpoint(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FixedMemberFinder(acceptsNulls),
            new StepByStepExecutor(1));
    Assert.assertNull(underTest.getNextReplica());
    Assert.assertNull(underTest.getNextReplica());
    Assert.assertEquals("r3", underTest.getNextReplica());
  }
}
