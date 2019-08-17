/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
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
