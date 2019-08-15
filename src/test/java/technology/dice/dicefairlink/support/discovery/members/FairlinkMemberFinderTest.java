package technology.dice.dicefairlink.support.discovery.members;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import technology.dice.dicefairlink.StepByStepExecutor;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.members.FairlinkMemberFinder;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;
import technology.dice.dicefairlink.iterators.SizedIterator;
import technology.dice.dicefairlink.support.discovery.tags.FixedSetExcludedReplicasFinder;
import technology.dice.dicefairlink.support.iterators.TestCyclicIterator;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class FairlinkMemberFinderTest {
  StepByStepExecutor exclusionTagsExecutor;

  private Properties baseTestProperties() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "irrelevant");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    return p;
  }

  private Set<String> baseReplicaList() {
    return ImmutableSet.of("replica1", "replica2", "replica3", "replica4");
  }

  @Before
  public void setup() {
    this.exclusionTagsExecutor = new StepByStepExecutor(1);
  }

  private Set<String> addDomain(Set<String> ids, String template) {
    return ids.stream().map(id -> String.format(template, id)).collect(Collectors.toSet());
  }

  @Test
  public void noExclusionsNoValidation() throws URISyntaxException {
    FairlinkMemberFinder f =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            new FixedSetExcludedReplicasFinder(ImmutableList.of()),
            new FixedSetReplicasFinder("my-fallback-endpoint", baseReplicaList()),
            strings -> TestCyclicIterator.of(strings),
            (host, properties) -> true);
    this.exclusionTagsExecutor.step();
    final SizedIterator<String> result = f.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(4, result.size());
    Assert.assertEquals(
        this.addDomain(
            baseReplicaList(), baseTestProperties().getProperty("replicaEndpointTemplate")),
        ((TestCyclicIterator) result).getElements());
  }

  //  @Test
  //  public void exclusionsNoValidation() throws URISyntaxException {
  //    FairlinkMemberFinder f =
  //        new FairlinkMemberFinder(
  //            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
  //            new FairlinkConnectionString(
  //                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
  //                this.baseTestProperties()),
  //            this.exclusionTagsExecutor,
  //            new FixedSetExcludedReplicasFinder(new ArrayList<>()),
  //            new FixedSetReplicasFinder(
  //                "my-fallback-endpoint", List.of("replica1", "replica2", "replica3",
  // "replica4")),
  //            strings -> CyclicIterator.of(strings),
  //            (host, properties) -> true);
  //    this.exclusionTagsExecutor.step();
  //    final SizedIterator<String> result = f.discoverReplicas();
  //    Assert.assertEquals(4, result.getSize());
  //  }
}
