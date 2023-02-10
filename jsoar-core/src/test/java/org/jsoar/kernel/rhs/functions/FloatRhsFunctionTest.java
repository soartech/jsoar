/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class FloatRhsFunctionTest extends JSoarTest
{
    
    @Test
    public void testConvertString() throws Exception
    {
        FloatRhsFunction f = new FloatRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, "3.14159"));
        assertEquals(3.14159, result.asDouble().getValue(), 0.0001);
    }
    
    @Test
    public void testConvertInt() throws Exception
    {
        FloatRhsFunction f = new FloatRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, 3));
        assertEquals(3.0, result.asDouble().getValue(), 0.0001);
    }
}
