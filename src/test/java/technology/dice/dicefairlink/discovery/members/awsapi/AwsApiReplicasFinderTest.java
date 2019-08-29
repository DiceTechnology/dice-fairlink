package technology.dice.dicefairlink.discovery.members.awsapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Properties;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.members.ClusterInfo;
import technology.dice.dicefairlink.driver.FairlinkConnectionString;

public class AwsApiReplicasFinderTest {
  @Rule public WireMockRule wireMockRule = new WireMockRule(11342);

  private Properties baseTestProperties() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("auroraDiscoveryAuthMode", "basic");
    p.setProperty("auroraDiscoveryKeyId", "keyId");
    p.setProperty("auroraDiscoverKeySecret", "keySecret");
    p.setProperty("discoveryMode", "AWS_API");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    p.setProperty("awsEndpointOverride", "http://localhost:11342");
    return p;
  }

  @Test
  public void withMembers() throws URISyntaxException, IOException {
    AwsApiReplicasFinder underTest =
        new AwsApiReplicasFinder(
            new FairlinkConfiguration(this.baseTestProperties(), ImmutableMap.of()),
            new FairlinkConnectionString(
                "jdbc:fairlink:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc",
                this.baseTestProperties()));

    String response =
        CharStreams.toString(
            new InputStreamReader(
                AwsApiReplicasFinderTest.class
                    .getClassLoader()
                    .getResourceAsStream(
                        "technology/dice/dicefairlink/discovery/members/awsapi/withMembers.xml"),
                Charsets.UTF_8));

    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(200).withBody(response)));

    final ClusterInfo actual = underTest.discoverCluster();
    Assert.assertEquals(
        new ClusterInfo(
            "cluster-reader-endpoint",
            ImmutableSet.of("my-db-cluster-agd-3", "my-db-cluster-agd-2")),
        actual);
  }

  @Test
  public void withoutMembers() throws URISyntaxException, IOException {
    AwsApiReplicasFinder underTest =
        new AwsApiReplicasFinder(
            new FairlinkConfiguration(this.baseTestProperties(), ImmutableMap.of()),
            new FairlinkConnectionString(
                "jdbc:fairlink:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc",
                this.baseTestProperties()));

    String response =
        CharStreams.toString(
            new InputStreamReader(
                AwsApiReplicasFinderTest.class
                    .getClassLoader()
                    .getResourceAsStream(
                        "technology/dice/dicefairlink/discovery/members/awsapi/withoutMembers.xml"),
                Charsets.UTF_8));

    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(200).withBody(response)));

    final ClusterInfo actual = underTest.discoverCluster();
    Assert.assertEquals(
        new ClusterInfo("sample-cluster.reader-endpoint", ImmutableSet.of()), actual);
  }

  @Test(expected = RuntimeException.class)
  public void serverError() throws URISyntaxException {
    AwsApiReplicasFinder underTest =
        new AwsApiReplicasFinder(
            new FairlinkConfiguration(this.baseTestProperties(), ImmutableMap.of()),
            new FairlinkConnectionString(
                "jdbc:fairlink:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc",
                this.baseTestProperties()));

    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(500)));

    underTest.discoverCluster();
  }

  @Test(expected = RuntimeException.class)
  public void clusterNotFound() throws URISyntaxException, IOException {
    AwsApiReplicasFinder underTest =
        new AwsApiReplicasFinder(
            new FairlinkConfiguration(this.baseTestProperties(), ImmutableMap.of()),
            new FairlinkConnectionString(
                "jdbc:fairlink:fairlinktestdriver://aa:123/db?param1=123&param2=true&param3=abc",
                this.baseTestProperties()));

    String response =
        CharStreams.toString(
            new InputStreamReader(
                AwsApiReplicasFinderTest.class
                    .getClassLoader()
                    .getResourceAsStream(
                        "technology/dice/dicefairlink/discovery/members/awsapi/emptyResponse.xml"),
                Charsets.UTF_8));

    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(200).withBody(response)));

    underTest.discoverCluster();
  }
}
