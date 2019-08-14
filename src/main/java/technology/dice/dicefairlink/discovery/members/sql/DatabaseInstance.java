package technology.dice.dicefairlink.discovery.members.sql;

public class DatabaseInstance {
  private final DatabaseInstanceRole role;
  private final String id;

  public DatabaseInstance(DatabaseInstanceRole role, String id) {
    this.role = role;
    this.id = id;
  }

  public DatabaseInstanceRole getRole() {
    return role;
  }

  public String getId() {
    return id;
  }
}
