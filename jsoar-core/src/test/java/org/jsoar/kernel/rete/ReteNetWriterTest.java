package org.jsoar.kernel.rete;

import org.junit.Test;

public class ReteNetWriterTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfContextIsNull() {
    new ReteNetWriter(null);
  }

}
