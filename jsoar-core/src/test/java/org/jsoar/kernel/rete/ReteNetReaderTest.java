package org.jsoar.kernel.rete;

import org.junit.Test;

public class ReteNetReaderTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfContextIsNull() {
    new ReteNetReader(null);
  }
}
