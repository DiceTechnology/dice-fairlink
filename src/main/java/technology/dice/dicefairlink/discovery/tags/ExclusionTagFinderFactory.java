package technology.dice.dicefairlink.discovery.tags;

import technology.dice.dicefairlink.config.FairlinkConfiguration;
import technology.dice.dicefairlink.discovery.tags.awsapi.ResourceGroupApiTagDiscovery;

public class ExclusionTagFinderFactory {
  public static TagFilter getTagFilter(FairlinkConfiguration fairlinkConfiguration) {
    return new ResourceGroupApiTagDiscovery(fairlinkConfiguration);
  }
}
