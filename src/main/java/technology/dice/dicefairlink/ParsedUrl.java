/*
 * Copyright (C) 2018 - present by Dice Technology Ltd.
 *
 * Please see distribution for license.
 */
package technology.dice.dicefairlink;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ParsedUrl)) {
      return false;
    }
    ParsedUrl parsedUrl = (ParsedUrl) o;
    return Objects.equals(getDelegateProtocol(), parsedUrl.getDelegateProtocol())
        && Objects.equals(getDelegateUrl(), parsedUrl.getDelegateUrl());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDelegateProtocol(), getDelegateUrl());
  }
}
