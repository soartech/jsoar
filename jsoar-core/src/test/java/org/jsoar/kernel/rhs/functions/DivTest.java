/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;

/** @author ray */
public class DivTest extends JSoarTest {

  @Test
  public void testCreateDivProduction() {
    // When creating new div production
    Div production = new Div();

    // Then name of production is get-url
    assertEquals("div", production.getName());
    // And production requires 2 mandatory argument
    assertEquals(2, production.getMinArguments());
    assertEquals(2, production.getMaxArguments());
  }

  @Test(expected = RhsFunctionException.class)
  public void testExecuteThrowsExceptionIfDividendIsNotInteger() throws RhsFunctionException {
    // Given a div production
    Div div = new Div();
    // And dividend is string
    // And divider is integer
    List<Symbol> arguments = Symbols.asList(syms, "DIVIDENT", 1);

    // When executing production
    // Then exception is thrown
    div.execute(mock(RhsFunctionContext.class), arguments);
  }

  @Test(expected = RhsFunctionException.class)
  public void testExecuteThrowsExceptionIfDividerIsNotInteger() throws RhsFunctionException {
    // Given a div production
    Div div = new Div();
    // And dividend is integer
    // And divider is string
    List<Symbol> arguments = Symbols.asList(syms, 1, "DIVIDER");

    // When executing production
    // Then exception is thrown
    div.execute(mock(RhsFunctionContext.class), arguments);
  }

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
