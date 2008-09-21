/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 20, 2008
 */
package org.jsoar.kernel.memory;

import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
import org.junit.Test;

/**
 * @author ray
 */
public class PreferenceTest extends JSoarTest
{

    /**
     * Test method for {@link org.jsoar.kernel.memory.Preference#formatTo(java.util.Formatter, int, int, int)}.
     */
    @Test
    public void testFormatTo()
    {
        Preference p = new Preference(PreferenceType.BINARY_PARALLEL_PREFERENCE_TYPE, 
                                      syms.make_new_identifier('S', 0),
                                      syms.make_sym_constant("superstate"),
                                      syms.make_sym_constant("nil"),
                                      syms.make_float_constant(3.14));
        
        assertEquals("(S1 ^superstate nil & 3.14)\n", String.format("%s", p));
        
        p.o_supported = true;
        assertEquals("(S1 ^superstate nil & 3.14  :O )\n", String.format("%s", p));
        
        // TODO test all other preference types :(
    }

}
