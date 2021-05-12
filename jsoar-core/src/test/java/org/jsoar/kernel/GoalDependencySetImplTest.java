package org.jsoar.kernel;

import org.junit.Test;

public class GoalDependencySetImplTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfGoalIsNull() {
    new GoalDependencySetImpl(null);
  }
}
