/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2009
 */
package org.jsoar.kernel.rhs.functions;


import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;

import java.util.ArrayList;
import java.util.Random;

/**
 * @author ray
 */
public class RandomFloatTest extends JSoarTest
{
    public void testExpectedName()
    {
        final RandomFloat rf = new RandomFloat(new Random());
        assertEquals("random-float", rf.getName());
    }
    
    public void testRandomFloat() throws Exception
    {
        final RandomFloat rf = new RandomFloat(new Random());
        for(int i = 0; i < 5000; ++i)
        {
            final Symbol result = rf.execute(rhsFuncContext, new ArrayList<Symbol>());
            assertNotNull(result);
            assertNotNull(result.asDouble());
            final double value = result.asDouble().getValue();
            assertTrue(value >= 0.0 && value < 1.0);
        }
    }
}
