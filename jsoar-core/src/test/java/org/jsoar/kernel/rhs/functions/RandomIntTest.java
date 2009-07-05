/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2009
 */
package org.jsoar.kernel.rhs.functions;


import static org.junit.Assert.*;

import java.util.Random;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;

/**
 * @author ray
 */
public class RandomIntTest extends JSoarTest
{
    @Test public void testExpectedName()
    {
        final RandomInt rf = new RandomInt(new Random());
        assertEquals("random-int", rf.getName());
    }

    @Test public void testPositiveUpperBoundIsHonored() throws Exception
    {
        final Random random = new Random();
        final RandomInt ri = new RandomInt(random);
        
        for(int i = 0; i < 5000; ++i)
        {
            final Symbol result = ri.execute(rhsFuncContext, Symbols.asList(syms, 10));
            assertNotNull(result);
            assertNotNull(result.asInteger());
            final int value = result.asInteger().getValue();
            assertTrue(value >= 0 && value < 10);
        }
    }
    
    @Test public void testNegativeUpperBoundIsHonored() throws Exception
    {
        final Random random = new Random();
        final RandomInt ri = new RandomInt(random);
        
        for(int i = 0; i < 5000; ++i)
        {
            final Symbol result = ri.execute(rhsFuncContext, Symbols.asList(syms, -10));
            assertNotNull(result);
            assertNotNull(result.asInteger());
            final int value = result.asInteger().getValue();
            assertTrue(value <= 0 && value > -10);
        }
    }
    
}
