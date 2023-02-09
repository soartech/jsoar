/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2009
 */
package org.jsoar.kernel.rhs.functions;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class RandIntTest extends JSoarTest
{
    @Test public void testExpectedName()
    {
        final RandInt rf = new RandInt(new Random());
        assertEquals("rand-int", rf.getName());
    }

    @Test public void testPositiveUpperBoundIsHonored() throws Exception
    {
        final Random random = new Random();
        final RandInt ri = new RandInt(random);
        
        for(int i = 0; i < 5000; ++i)
        {
            final Symbol result = ri.execute(rhsFuncContext, Symbols.asList(syms, 10));
            assertNotNull(result);
            assertNotNull(result.asInteger());
            final long value = result.asInteger().getValue();
            assertTrue(value >= 0 && value <= 10);
        }
    }
    
    @Test public void testNegativeUpperBoundIsHonored() throws Exception
    {
        final Random random = new Random();
        final RandInt ri = new RandInt(random);
        
        for(int i = 0; i < 5000; ++i)
        {
            final Symbol result = ri.execute(rhsFuncContext, Symbols.asList(syms, -10));
            assertNotNull(result);
            assertNotNull(result.asInteger());
            final long value = result.asInteger().getValue();
            assertTrue(value <= 0 && value >= -10);
        }
    }
    
}
