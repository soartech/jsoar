/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2008
 */
package org.jsoar.kernel.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jsoar.JSoarTest;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class WmeTest extends JSoarTest
{
    
    /**
     * Test method for {@link org.jsoar.kernel.memory.WmeImpl#formatTo(java.util.Formatter, int, int, int)}.
     */
    @Test
    void testFormatTo()
    {
        // Acceptable, with and without timetag
        WmeImpl w1 = new WmeImpl(syms.make_new_identifier('S', 0),
                syms.createString("superstate"),
                syms.createString("nil"), true, 99);
        assertEquals("(99: S1 ^superstate nil +)\n", String.format("%s", w1));
        assertEquals("(S1 ^superstate nil +)\n", String.format("%#s", w1));
        
        // Not-acceptable, with and without timetag
        WmeImpl w2 = new WmeImpl(syms.make_new_identifier('S', 1),
                syms.createString("value"),
                syms.createInteger(100), false, 2);
        assertEquals("(2: S2 ^value 100)\n", String.format("%s", w2));
        assertEquals("(S2 ^value 100)\n", String.format("%#s", w2));
        
    }
    
}
