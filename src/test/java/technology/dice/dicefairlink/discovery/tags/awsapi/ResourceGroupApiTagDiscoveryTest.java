package technology.dice.dicefairlink.discovery.tags.awsapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Properties;
import java.util.Set;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.tags.ExclusionTag;
import technology.dice.dicefairlink.discovery.tags.awsapi.ResourceGroupApiResponse.Resource;
import technology.dice.dicefairlink.discovery.tags.awsapi.ResourceGroupApiResponse.Resource.Tag;

public class ResourceGroupApiTagDiscoveryTest {
  @Rule public WireMockRule wireMockRule = new WireMockRule(11342);
  private final ObjectMapper mapper = new ObjectMapper();

  private Properties baseTestProperties() {
    Properties p = new Properties();
    p.setProperty("auroraClusterRegion", "eu-west-1");
    p.setProperty("discoveryMode", "AWS_API");
    p.setProperty("replicaPollInterval", "5");
    p.setProperty("tagsPollInterval", "10");
    p.setProperty("replicaEndpointTemplate", "%s.rest-of-myhost.name");
    p.setProperty("validateConnection", "true");
    p.setProperty("awsEndpointOverride", "http://localhost:11342");
    return p;
  }

  @Test
  public void withExclusions() throws JsonProcessingException {
    ResourceGroupApiTagDiscovery underTest =
        new ResourceGroupApiTagDiscovery(
            new FairlinkConfiguration(this.baseTestProperties(), ImmutableMap.of()));

    stubFor(
        post(urlEqualTo("/"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        this.stringifyResponse(
                            new ResourceGroupApiResponse(
                                ImmutableList.of(
                                    new Resource(
                                        "arn1",
                                        ImmutableList.of(
                                            new Tag("k", "v"),
                                            new Tag("Fairlink-Exclude", "true")))),
                                "")))));

    final Set<String> actual =
        underTest.listExcludedInstances(new ExclusionTag("FairlinkConfiguration", "true"));
    Assert.assertEquals(ImmutableSet.of("arn1"), actual);
  }

  @Test
  public void noExclusions() throws JsonProcessingException {
    ResourceGroupApiTagDiscovery underTest =
        new ResourceGroupApiTagDiscovery(
            new FairlinkConfiguration(this.baseTestProperties(), ImmutableMap.of()));

    stubFor(
        post(urlEqualTo("/"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(
                        this.stringifyResponse(
                            new ResourceGroupApiResponse(ImmutableList.of(), "")))));

    final Set<String> actual =
        underTest.listExcludedInstances(new ExclusionTag("FairlinkConfiguration", "true"));
    Assert.assertEquals(ImmutableSet.of(), actual);
  }

  @Test
  public void exceptionAssumesEmpty() {
    ResourceGroupApiTagDiscovery underTest =
        new ResourceGroupApiTagDiscovery(
            new FairlinkConfiguration(this.baseTestProperties(), ImmutableMap.of()));

    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(500)));

    final Set<String> actual =
        underTest.listExcludedInstances(new ExclusionTag("FairlinkConfiguration", "true"));
    Assert.assertEquals(ImmutableSet.of(), actual);
  }

  @Test
  public void noInstancesFound() {
    ResourceGroupApiTagDiscovery underTest =
        new ResourceGroupApiTagDiscovery(
            new FairlinkConfiguration(this.baseTestProperties(), ImmutableMap.of()));

    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(404)));

    final Set<String> actual =
        underTest.listExcludedInstances(new ExclusionTag("FairlinkConfiguration", "true"));
    Assert.assertEquals(ImmutableSet.of(), actual);
  }

  private String stringifyResponse(ResourceGroupApiResponse response)
      throws JsonProcessingException {
    return this.mapper.writeValueAsString(response);
  }
}
