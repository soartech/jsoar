/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 28, 2010
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;

/** @author ray */
public class FormatRhsFunctionTest extends JSoarTest {
  private final FormatRhsFunction func = new FormatRhsFunction();

  @Test
  public void testCanFormatAStringWithNoArguments() throws Exception {
    checkResult("hello, world", func.execute(rhsFuncContext, Symbols.asList(syms, "hello, world")));
  }

  @Test
  public void testFormatWithSeveralArgs() throws Exception {
    checkResult(
        "S1: hi 987 3.140000 null",
        func.execute(
            rhsFuncContext,
            Symbols.asList(
                syms, "%s: %s %d %f %s", syms.createIdentifier('S'), "hi", 987, 3.14, null)));
  }

  @Test(expected = RhsFunctionException.class)
  public void testFormatThrowsExceptionWhenGivenAnInvalidFormatSpecifier() throws Exception {
    func.execute(rhsFuncContext, Symbols.asList(syms, "%d", "hello"));
  }

  private void checkResult(String expected, Symbol result) {
    assertNotNull(result);
    assertTrue("Result should be a string symbol", result.asString() != null);
    assertEquals(expected, result.asString().getValue());
  }
}
