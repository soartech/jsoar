/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;

/** @author ray */
public class StrLenTest extends JSoarTest {

  @Test
  public void testExecute() throws Exception {
    StrLen strlen = new StrLen();

    String s = "";
    for (int i = 1; i < 100; ++i) {
      s += "B";
      assertEquals(
          i, strlen.execute(rhsFuncContext, Symbols.asList(syms, s)).asInteger().getValue());
    }
  }
}
