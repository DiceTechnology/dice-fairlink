package technology.dice.dicefairlink.discovery.tags;

public class ExclusionTag {
  private final String key;
  private final String value;

  public ExclusionTag(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
