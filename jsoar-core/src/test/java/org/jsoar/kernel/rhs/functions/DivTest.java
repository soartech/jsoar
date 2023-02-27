/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class DivTest extends JSoarTest
{
    
    @Test
    void testDiv() throws Exception
    {
        for(int i = -100; i < 100; ++i)
        {
            for(int j = -100; j < 100; ++j)
            {
                if(j != 0)
                {
                    validateDiv(i, j);
                }
            }
        }
    }
    
    @Test()
    public void testDivThrowsExceptionOnDivideByZero()
    {
        assertThrows(RhsFunctionException.class, () -> validateDiv(1, 0));
    }
    
    private void validateDiv(int a, int b) throws Exception
    {
        final Div div = new Div();
        final Symbol r = div.execute(rhsFuncContext, Symbols.asList(syms, a, b));
        assertEquals(a / b, r.asInteger().getValue(), String.format("(div %d %d)", a, b));
    }
}
