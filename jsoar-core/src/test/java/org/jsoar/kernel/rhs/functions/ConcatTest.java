/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;

/** @author ray */
public class ConcatTest extends JSoarTest {

  @Test
  public void testConstructor() {
    Concat concat = new Concat();
    assertEquals("concat", concat.getName());
    assertEquals(0, concat.getMinArguments());
    assertEquals(Integer.MAX_VALUE, concat.getMaxArguments());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConcatThrowsExceptionIfArgumentsIsNull() throws RhsFunctionException {
    Concat.concat(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExecuteThrowsExceptionIfContextIsNull() throws RhsFunctionException {
    Concat c = new Concat();
    c.execute(null, List.of(mock(Symbol.class)));
  }

  @Test
  public void testExecute() throws Exception {
    // Given a instance of production concat
    Concat concat = new Concat();
    // And a list of symbols
    List<Symbol> arguments = Symbols.asList(syms, "a", "b", "c  D  e>", "<>", 1, 2, 3);

    // When execute concat production
    Symbol result = concat.execute(rhsFuncContext, arguments);

    // Then result matches concatenated list of symbols
    assertEquals("abc  D  e><>123", result.asString().getValue());
  }

  @Test
  public void testExecuteIfArgumentsEmpty() throws Exception {
    // Given a instance of production concat
    Concat concat = new Concat();
    // And a empty list of symbols
    List<Symbol> arguments = Symbols.asList(syms);

    // When execute concat production
    Symbol result = concat.execute(rhsFuncContext, arguments);

    // Then result matches concatenated list of symbols
    assertEquals("", result.asString().getValue());
  }
}
