/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 13, 2008
 */
package org.jsoar.kernel.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class StringSymbolImplTest
{
    private SymbolFactoryImpl syms;
    
    @BeforeEach
    void setUp()
    {
        syms = new SymbolFactoryImpl();
    }
    
    @Test
    void includeBarsWhenStringLooksLikeId()
    {
        assertEquals("|S1|", String.format("%s", syms.createString("S1")));
        assertEquals("S1", String.format("%#s", syms.createString("S1")));
    }
    
    @Test
    void includeBarsWhenStringLooksLikeInteger()
    {
        assertEquals("|12345|", String.format("%s", syms.createString("12345")));
        assertEquals("12345", String.format("%#s", syms.createString("12345")));
    }
    
    @Test
    void includeBarsWhenStringHasSpaces()
    {
        assertEquals("|this has spaces in it|", String.format("%s", syms.createString("this has spaces in it")));
        assertEquals("this has spaces in it", String.format("%#s", syms.createString("this has spaces in it")));
    }
    
    @Test
    void includeBarsWhenStringHasParens()
    {
        assertEquals("|()|", String.format("%s", syms.createString("()")));
        assertEquals("()", String.format("%#s", syms.createString("()")));
    }
    
    @Test
    void includBarsWhenStringLooksLikeFloat()
    {
        assertEquals("|3.14|", String.format("%s", syms.createString("3.14")));
        assertEquals("3.14", String.format("%#s", syms.createString("3.14")));
    }
    
    @Test
    void noBarsForSingleCharacters()
    {
        assertEquals("A", String.format("%s", syms.createString("A")));
        assertEquals("A", String.format("%#s", syms.createString("A")));
        
        assertEquals("b", String.format("%s", syms.createString("b")));
        assertEquals("b", String.format("%#s", syms.createString("b")));
    }
    
    @Test
    void noBarsForNormalStrings()
    {
        assertEquals("s1", String.format("%s", syms.createString("s1")));
        assertEquals("s1", String.format("%#s", syms.createString("s1")));
        assertEquals("thisHasCapsInIt", String.format("%s", syms.createString("thisHasCapsInIt")));
        assertEquals("thisHasCapsInIt", String.format("%#s", syms.createString("thisHasCapsInIt")));
        assertEquals("thisisalllowercase", String.format("%s", syms.createString("thisisalllowercase")));
    }
    
    @Test
    void includeBarsWhenStringLooksLikeVariable()
    {
        assertEquals("|<v>|", String.format("%s", syms.createString("<v>")));
        assertEquals("<v>", String.format("%#s", syms.createString("<v>")));
    }
    
    @Test
    void testFormatToWhenStringContainsPercent()
    {
        final StringSymbol s = syms.createString("%y");
        assertEquals("%y", s.toString());
        assertEquals("|%y|", String.format("%s", s));
        assertEquals("%y", String.format("%#s", s));
    }
}
