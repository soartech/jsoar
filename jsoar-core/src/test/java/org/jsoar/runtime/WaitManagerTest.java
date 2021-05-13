package org.jsoar.runtime;

import org.junit.Test;

public class WaitManagerTest {

  @Test(expected = IllegalArgumentException.class)
  public void testAttachThrowsExceptionIfAgentIsNull() {
    WaitManager manager = new WaitManager();
    manager.attach(null);
  }

}
