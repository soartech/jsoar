/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2009
 */
package org.jsoar.kernel.rhs.functions;


import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;

import java.util.Random;

/**
 * @author ray
 */
public class RandomIntTest extends JSoarTest
{
    public void testExpectedName()
    {
        final RandomInt rf = new RandomInt(new Random());
        assertEquals("random-int", rf.getName());
    }

    public void testPositiveUpperBoundIsHonored() throws Exception
    {
        final Random random = new Random();
        final RandomInt ri = new RandomInt(random);
        
        for(int i = 0; i < 5000; ++i)
        {
            final Symbol result = ri.execute(rhsFuncContext, Symbols.asList(syms, 10));
            assertNotNull(result);
            assertNotNull(result.asInteger());
            final long value = result.asInteger().getValue();
            assertTrue(value >= 0 && value < 10);
        }
    }
    
    public void testNegativeUpperBoundIsHonored() throws Exception
    {
        final Random random = new Random();
        final RandomInt ri = new RandomInt(random);
        
        for(int i = 0; i < 5000; ++i)
        {
            final Symbol result = ri.execute(rhsFuncContext, Symbols.asList(syms, -10));
            assertNotNull(result);
            assertNotNull(result.asInteger());
            final long value = result.asInteger().getValue();
            assertTrue(value <= 0 && value > -10);
        }
    }
    
}
