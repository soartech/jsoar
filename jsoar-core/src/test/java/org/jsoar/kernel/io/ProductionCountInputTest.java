package org.jsoar.kernel.io;

import static org.mockito.Mockito.mock;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.Symbol;
import org.junit.Test;

public class ProductionCountInputTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfAgentIsNull() {
    new ProductionCountInput(null);
  }

}
