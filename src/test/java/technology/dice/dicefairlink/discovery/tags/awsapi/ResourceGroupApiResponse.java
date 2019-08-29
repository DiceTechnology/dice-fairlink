package technology.dice.dicefairlink.discovery.tags.awsapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ResourceGroupApiResponse {
  public static class Resource {
    public static class Tag {
      private final String key;
      private final String value;

      public Tag(String key, String value) {
        this.key = key;
        this.value = value;
      }

      @JsonProperty("Key")
      public String getKey() {
        return key;
      }

      @JsonProperty("Value")
      public String getValue() {
        return value;
      }
    }

    private final String resourceARN;
    private final List<Tag> tags;

    public Resource(String resourceARN, List<Tag> tags) {
      this.resourceARN = resourceARN;
      this.tags = tags;
    }

    @JsonProperty("ResourceARN")
    public String getResourceARN() {
      return resourceARN;
    }

    @JsonProperty("Tags")
    public List<Tag> getTags() {
      return tags;
    }
  }

  private final List<Resource> taggedResources;
  private final String taginationToken;

  public ResourceGroupApiResponse(List<Resource> taggedResources, String taginationToken) {
    this.taggedResources = taggedResources;
    this.taginationToken = taginationToken;
  }

  @JsonProperty("ResourceTagMappingList")
  public List<Resource> getTaggedResources() {
    return taggedResources;
  }

  @JsonProperty("PaginationToken")
  public String getTaginationToken() {
    return taginationToken;
  }
}
