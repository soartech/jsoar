/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2009
 */
package org.jsoar.kernel.rhs.functions;


import junit.framework.Assert;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;

/**
 * @author ray
 */
public class ModTest extends JSoarTest
{

    public void testMod() throws Exception
    {
        for(int i = -100; i < 100; ++i)
        {
            for(int j = -100; j < 100; ++j)
            {
                if(j != 0)
                {
                    validateMod(i, j);
                }
            }
        }
    }
    
    public void testModThrowsExceptionOnDivideByZero() throws Exception
    {
        try {
            validateMod(1, 0);
        }catch(RhsFunctionException e){
            Assert.assertEquals("Attempt to divide (mod) by zero", e.getMessage());
        }
    }
    
    private void validateMod(int a, int b) throws Exception
    {
        final Mod mod = new Mod();
        final Symbol r = mod.execute(rhsFuncContext, Symbols.asList(syms, a, b));
        assertEquals(String.format("(mod %d %d)", a, b), a % b, r.asInteger().getValue());
    }
}
