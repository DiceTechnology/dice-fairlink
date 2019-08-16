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
import technology.dice.dicefairlink.support.discovery.tags.FailingExcludedReplicasFinder;
import technology.dice.dicefairlink.support.discovery.tags.FixedSetExcludedReplicasFinder;
import technology.dice.dicefairlink.support.iterators.TestCyclicIterator;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class FairlinkMemberFinderTest {
  private StepByStepExecutor exclusionTagsExecutor;

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

  private Set<String> addDomain(Set<String> ids, Properties properties) {
    return ids.stream()
        .map(id -> String.format(properties.getProperty("replicaEndpointTemplate"), id))
        .collect(Collectors.toSet());
  }

  private String addDomain(String id, Properties properties) {
    return this.addDomain(ImmutableSet.of(id), properties).iterator().next();
  }

  @Test
  public void allGood() throws URISyntaxException {
    FairlinkMemberFinder underTest =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            new FixedSetExcludedReplicasFinder(ImmutableList.of()),
            new FixedSetReplicasFinder("my-fallback.domain.com", baseReplicaList()),
            strings -> TestCyclicIterator.of(strings),
            (host, properties) -> true);
    this.exclusionTagsExecutor.step();
    final SizedIterator<String> result = underTest.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(4, result.size());
    Assert.assertEquals(
        this.addDomain(baseReplicaList(), this.baseTestProperties()),
        ((TestCyclicIterator) result).getElements());
  }

  @Test
  public void exclusions() throws URISyntaxException {
    FairlinkMemberFinder underTest =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            new FixedSetExcludedReplicasFinder(ImmutableSet.of("replica1", "replica3")),
            new FixedSetReplicasFinder(
                "my-fallback.domain.com",
                ImmutableSet.of("replica1", "replica2", "replica3", "replica4")),
            strings -> TestCyclicIterator.of(strings),
            (host, properties) -> true);
    this.exclusionTagsExecutor.step();
    final SizedIterator<String> result = underTest.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(2, result.size());
    Assert.assertEquals(
        this.addDomain(ImmutableSet.of("replica2", "replica4"), this.baseTestProperties()),
        ((TestCyclicIterator) result).getElements());
  }

  @Test
  public void badReplica() throws URISyntaxException {
    FairlinkMemberFinder underTest =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            new FixedSetExcludedReplicasFinder(ImmutableSet.of()),
            new FixedSetReplicasFinder(
                "my-fallback.domain.com",
                ImmutableSet.of("replica1", "replica2", "replica3", "replica4")),
            strings -> TestCyclicIterator.of(strings),
            (host, properties) ->
                !host.equalsIgnoreCase(
                    "jdbc:fairlinktestdriver://"
                        + this.addDomain("replica1", this.baseTestProperties())));
    this.exclusionTagsExecutor.step();
    final SizedIterator<String> result = underTest.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(3, result.size());
    Assert.assertEquals(
        this.addDomain(
            ImmutableSet.of("replica2", "replica3", "replica4"), this.baseTestProperties()),
        ((TestCyclicIterator) result).getElements());
  }

  @Test
  public void replicaAdded() throws URISyntaxException {
    final FixedSetReplicasFinder fixedSetReplicasFinder =
        new FixedSetReplicasFinder(
            "my-fallback.domain.com", ImmutableSet.of("replica1", "replica2"));
    FairlinkMemberFinder underTest =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            new FixedSetExcludedReplicasFinder(ImmutableSet.of()),
            fixedSetReplicasFinder,
            strings -> TestCyclicIterator.of(strings),
            (host, properties) -> true);
    this.exclusionTagsExecutor.step();
    final SizedIterator<String> result = underTest.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(2, result.size());
    Assert.assertEquals(
        this.addDomain(ImmutableSet.of("replica1", "replica2"), this.baseTestProperties()),
        ((TestCyclicIterator) result).getElements());

    fixedSetReplicasFinder.updateReplicas(ImmutableSet.of("replica1", "replica2", "replica3"));
    final SizedIterator<String> afterAddingReplica = underTest.discoverReplicas();
    Assert.assertTrue(afterAddingReplica instanceof TestCyclicIterator);
    Assert.assertEquals(3, afterAddingReplica.size());
    Assert.assertEquals(
        this.addDomain(
            ImmutableSet.of("replica1", "replica2", "replica3"), this.baseTestProperties()),
        ((TestCyclicIterator) afterAddingReplica).getElements());
  }

  @Test
  public void replicaRemoved() throws URISyntaxException {
    final FixedSetReplicasFinder fixedSetReplicasFinder =
        new FixedSetReplicasFinder(
            "my-fallback.domain.com", ImmutableSet.of("replica1", "replica2"));
    FairlinkMemberFinder underTest =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            new FixedSetExcludedReplicasFinder(ImmutableSet.of()),
            fixedSetReplicasFinder,
            strings -> TestCyclicIterator.of(strings),
            (host, properties) -> true);
    this.exclusionTagsExecutor.step();
    final SizedIterator<String> result = underTest.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(2, result.size());
    Assert.assertEquals(
        this.addDomain(ImmutableSet.of("replica1", "replica2"), this.baseTestProperties()),
        ((TestCyclicIterator) result).getElements());

    fixedSetReplicasFinder.updateReplicas(ImmutableSet.of("replica1"));
    final SizedIterator<String> afterAddingReplica = underTest.discoverReplicas();
    Assert.assertTrue(afterAddingReplica instanceof TestCyclicIterator);
    Assert.assertEquals(1, afterAddingReplica.size());
    Assert.assertEquals(
        this.addDomain(ImmutableSet.of("replica1"), this.baseTestProperties()),
        ((TestCyclicIterator) afterAddingReplica).getElements());
  }

  @Test
  public void exclusionAdded() throws URISyntaxException {
    final FixedSetExcludedReplicasFinder exclusionsFinder =
        new FixedSetExcludedReplicasFinder(ImmutableList.of());
    FairlinkMemberFinder underTest =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            exclusionsFinder,
            new FixedSetReplicasFinder(
                "my-fallback.domain.com",
                ImmutableSet.of("replica1", "replica2", "replica3", "replica4")),
            strings -> TestCyclicIterator.of(strings),
            (host, properties) -> true);
    this.exclusionTagsExecutor.step();
    final SizedIterator<String> result = underTest.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(4, result.size());
    Assert.assertEquals(
        this.addDomain(baseReplicaList(), this.baseTestProperties()),
        ((TestCyclicIterator) result).getElements());

    exclusionsFinder.updateExclusions(ImmutableSet.of("replica1", "replica4"));
    this.exclusionTagsExecutor.step();

    final SizedIterator<String> afterAddingReplica = underTest.discoverReplicas();
    Assert.assertTrue(afterAddingReplica instanceof TestCyclicIterator);
    Assert.assertEquals(2, afterAddingReplica.size());
    Assert.assertEquals(
        this.addDomain(ImmutableSet.of("replica2", "replica3"), this.baseTestProperties()),
        ((TestCyclicIterator) afterAddingReplica).getElements());
  }

  @Test(expected = RuntimeException.class)
  public void discoveryExceptionNoFallback() throws URISyntaxException {
    FairlinkMemberFinder underTest =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            new FixedSetExcludedReplicasFinder(ImmutableList.of()),
            () -> {
              throw new RuntimeException("boom");
            },
            strings -> TestCyclicIterator.of(strings),
            (host, properties) -> true);
    this.exclusionTagsExecutor.step();
    underTest.discoverReplicas();
  }

  @Test
  public void discoveryExceptionFallback() throws URISyntaxException {
    FairlinkMemberFinder underTest =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            new FixedSetExcludedReplicasFinder(ImmutableList.of()),
            new FailingReplicasFinder("my-fallback.domain.com", baseReplicaList(), 1),
            strings -> TestCyclicIterator.of(strings),
            (host, properties) -> true);
    this.exclusionTagsExecutor.step();
    final SizedIterator<String> result = underTest.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(4, result.size());
    Assert.assertEquals(
        this.addDomain(baseReplicaList(), this.baseTestProperties()),
        ((TestCyclicIterator) result).getElements());

    final SizedIterator<String> secondResult = underTest.discoverReplicas();
    Assert.assertTrue(secondResult instanceof TestCyclicIterator);
    Assert.assertEquals(1, secondResult.size());
    Assert.assertEquals(
        ImmutableSet.of("my-fallback.domain.com"),
        ((TestCyclicIterator) secondResult).getElements());
  }

  @Test
  public void exclusionDiscoveryException() throws URISyntaxException {
    FairlinkMemberFinder underTest =
        new FairlinkMemberFinder(
            new FairlinkConfiguration(this.baseTestProperties(), new HashMap<>()),
            new FairlinkConnectionString(
                "jdbc:auroraro:fairlinktestdriver://my-fallback.domain.com",
                this.baseTestProperties()),
            this.exclusionTagsExecutor,
            new FailingExcludedReplicasFinder(ImmutableSet.of("replica1", "replica3"), 1),
            new FixedSetReplicasFinder(
                "my-fallback.domain.com",
                ImmutableSet.of("replica1", "replica2", "replica3", "replica4")),
            strings -> TestCyclicIterator.of(strings),
            (host, properties) -> true);
    this.exclusionTagsExecutor.step();
    final SizedIterator<String> result = underTest.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(2, result.size());
    Assert.assertEquals(
        this.addDomain(ImmutableSet.of("replica2", "replica4"), this.baseTestProperties()),
        ((TestCyclicIterator) result).getElements());

    this.exclusionTagsExecutor.step();
    final SizedIterator<String> secondResult = underTest.discoverReplicas();

    Assert.assertTrue(result instanceof TestCyclicIterator);
    Assert.assertEquals(4, secondResult.size());
    Assert.assertEquals(
        this.addDomain(
            ImmutableSet.of("replica1", "replica2", "replica3", "replica4"),
            this.baseTestProperties()),
        ((TestCyclicIterator) secondResult).getElements());
  }
}
