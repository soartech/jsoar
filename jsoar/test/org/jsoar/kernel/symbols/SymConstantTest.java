/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 13, 2008
 */
package org.jsoar.kernel.symbols;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author ray
 */
public class SymConstantTest
{

    /**
     * Test method for {@link org.jsoar.kernel.symbols.SymConstant#formatTo(java.util.Formatter, int, int, int)}.
     */
    @Test
    public void testFormatTo()
    {
        SymbolFactoryImpl syms = new SymbolFactoryImpl();
        
        assertEquals("|S1|", String.format("%s", syms.make_sym_constant("S1")));
        assertEquals("S1", String.format("%#s", syms.make_sym_constant("S1")));
        assertEquals("s1", String.format("%s", syms.make_sym_constant("s1")));
        assertEquals("s1", String.format("%#s", syms.make_sym_constant("s1")));
        assertEquals("|this has spaces in it|", String.format("%s", syms.make_sym_constant("this has spaces in it")));
        assertEquals("|thisHasCapsInIt|", String.format("%s", syms.make_sym_constant("thisHasCapsInIt")));
        assertEquals("thisHasCapsInIt", String.format("%#s", syms.make_sym_constant("thisHasCapsInIt")));
        assertEquals("thisisalllowercase", String.format("%s", syms.make_sym_constant("thisisalllowercase")));
        assertEquals("|<v>|", String.format("%s", syms.make_sym_constant("<v>")));
        assertEquals("<v>", String.format("%#s", syms.make_sym_constant("<v>")));
    }

}
