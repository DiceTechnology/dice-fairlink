package technology.dice.dicefairlink.support.discovery.members;

import technology.dice.dicefairlink.discovery.members.MemberFinder;
import technology.dice.dicefairlink.iterators.SizedIterator;
import technology.dice.dicefairlink.support.iterators.TestCyclicIterator;

import java.util.Collection;

public class FixedMemberFinder implements MemberFinder {
  private Collection<String> members;

  public FixedMemberFinder(Collection<String> members) {
    this.members = members;
  }

  public void updateMembers(Collection<String> replicas) {
    this.members = replicas;
  }

  @Override
  public SizedIterator<String> discoverReplicas() {
    return TestCyclicIterator.of(this.members);
  }
}
