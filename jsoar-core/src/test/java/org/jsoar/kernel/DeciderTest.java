package org.jsoar.kernel;

import javax.swing.event.DocumentEvent;
import org.junit.Test;

public class DeciderTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrownExceptionIfAgentIsNull() {
    new Decider(null);
  }

}
