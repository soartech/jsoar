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
    
    @Test
    public void testFormatToWhenStringContainsPercent()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        
        final StringSymbol s = syms.createString("%y");
        assertEquals("%y", s.toString());
        assertEquals("|%y|", String.format("%s", s));
        assertEquals("%y", String.format("%#s", s));
    }
}
