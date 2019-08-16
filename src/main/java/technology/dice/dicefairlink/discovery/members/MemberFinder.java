package technology.dice.dicefairlink.discovery.members;

import technology.dice.dicefairlink.iterators.SizedIterator;

public interface MemberFinder {
  SizedIterator<String> discoverReplicas();

  default SizedIterator<String> init() {
    return this.discoverReplicas();
  }
}
