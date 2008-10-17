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
public class StringSymbolImplTest
{

    /**
     * Test method for {@link org.jsoar.kernel.symbols.StringSymbolImpl#formatTo(java.util.Formatter, int, int, int)}.
     */
    @Test
    public void testFormatTo()
    {
        SymbolFactoryImpl syms = new SymbolFactoryImpl();
        
        assertEquals("|S1|", String.format("%s", syms.createString("S1")));
        assertEquals("S1", String.format("%#s", syms.createString("S1")));
        assertEquals("s1", String.format("%s", syms.createString("s1")));
        assertEquals("s1", String.format("%#s", syms.createString("s1")));
        assertEquals("|this has spaces in it|", String.format("%s", syms.createString("this has spaces in it")));
        assertEquals("|thisHasCapsInIt|", String.format("%s", syms.createString("thisHasCapsInIt")));
        assertEquals("thisHasCapsInIt", String.format("%#s", syms.createString("thisHasCapsInIt")));
        assertEquals("thisisalllowercase", String.format("%s", syms.createString("thisisalllowercase")));
        assertEquals("|<v>|", String.format("%s", syms.createString("<v>")));
        assertEquals("<v>", String.format("%#s", syms.createString("<v>")));
    }

}
