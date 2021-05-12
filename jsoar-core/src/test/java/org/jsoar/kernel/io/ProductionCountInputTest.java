package org.jsoar.kernel.io;


import org.junit.Test;

public class ProductionCountInputTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfAgentIsNull() {
    new ProductionCountInput(null);
  }
}
