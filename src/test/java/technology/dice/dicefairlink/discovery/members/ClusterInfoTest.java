package technology.dice.dicefairlink.discovery.members;

import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

public class ClusterInfoTest {
  @Test
  public void hashcode() {
    ClusterInfo c1 = new ClusterInfo("a", ImmutableSet.of("r1", "r2"));
    ClusterInfo c2 = new ClusterInfo("b", ImmutableSet.of("r3", "r4"));
    ClusterInfo c3 = new ClusterInfo("a", ImmutableSet.of("r1", "r2"));
    Assert.assertEquals(c1.hashCode(), c3.hashCode());
    Assert.assertNotEquals(c1.hashCode(), c2.hashCode());
  }

  @Test
  public void equalz() {
    ClusterInfo c1 = new ClusterInfo("a", ImmutableSet.of("r1", "r2"));
    ClusterInfo c2 = new ClusterInfo("b", ImmutableSet.of("r3", "r4"));
    ClusterInfo c3 = new ClusterInfo("a", ImmutableSet.of("r1", "r2"));
    ClusterInfo c4 = new ClusterInfo("a", ImmutableSet.of("r1", "r4"));
    Assert.assertTrue(c1.equals(c3));
    Assert.assertTrue(c1.equals(c1));
    Assert.assertFalse(c1.equals(c2));
    Assert.assertFalse(c1.equals(c4));
    Assert.assertFalse(c1.equals("not eequals"));
    Assert.assertFalse(c1.equals(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void ctorEmptyReadOnlyEndpoint() {
    new ClusterInfo("", ImmutableSet.of("r1", "r2"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void ctorNullReadOnlyEndpoint() {
    new ClusterInfo(null, ImmutableSet.of("r1", "r2"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void ctorNullListOfReplicas() {
    new ClusterInfo("a", null);
  }
}
