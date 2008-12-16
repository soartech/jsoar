/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;

/**
 * @author ray
 */
public class IntRhsFunctionTest extends JSoarTest
{

    @Test
    public void testConvertString() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, "99"));
        assertEquals(99, result.asInteger().getValue());
    }
    @Test
    public void testConvertDouble() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, 3.14159));
        assertEquals(3, result.asInteger().getValue());
    }
}
