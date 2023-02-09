/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2009
 */
package org.jsoar.kernel.rhs.functions;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Random;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class RandFloatTest extends JSoarTest
{
    @Test public void testExpectedName()
    {
        final RandFloat rf = new RandFloat(new Random());
        assertEquals("rand-float", rf.getName());
    }
    
    @Test public void testRandomFloat() throws Exception
    {
        final RandFloat rf = new RandFloat(new Random());
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
