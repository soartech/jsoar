package org.jsoar.kernel;

import org.junit.Test;

public class DeciderTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrownExceptionIfAgentIsNull() {
    new Decider(null);
  }
}
