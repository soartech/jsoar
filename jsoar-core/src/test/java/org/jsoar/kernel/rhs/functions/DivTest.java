/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;

/** @author ray */
public class DivTest extends JSoarTest {

  @Test
  public void testDiv() throws Exception {
    for (int i = -100; i < 100; ++i) {
      for (int j = -100; j < 100; ++j) {
        if (j != 0) {
          validateDiv(i, j);
        }
      }
    }
  }

  @Test(expected = RhsFunctionException.class)
  public void testDivThrowsExceptionOnDivideByZero() throws Exception {
    validateDiv(1, 0);
  }

  private void validateDiv(int a, int b) throws Exception {
    final Div div = new Div();
    final Symbol r = div.execute(rhsFuncContext, Symbols.asList(syms, a, b));
    assertEquals(String.format("(div %d %d)", a, b), a / b, r.asInteger().getValue());
  }
}
