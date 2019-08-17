/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink;

public class ParsedUrl {
  private final String delegateProtocol;
  private final String delegateUrl;

  public ParsedUrl(String delegateProtocol, String delegateUrl) {
    this.delegateProtocol = delegateProtocol;
    this.delegateUrl = delegateUrl;
  }

  public String getDelegateProtocol() {
    return delegateProtocol;
  }

  public String getDelegateUrl() {
    return delegateUrl;
  }
}
