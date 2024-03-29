/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 9, 2010
 */
package org.jsoar.kernel.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class DummyWmeTest
{
    private SymbolFactory syms;
    
    @BeforeEach
    void setUp() throws Exception
    {
        syms = new SymbolFactoryImpl();
    }
    
    @Test
    void testGetIdentiferAttrAndValue()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createDouble(3.14159);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertSame(id, wme.getIdentifier());
        assertSame(attr, wme.getAttribute());
        assertSame(value, wme.getValue());
    }
    
    @Test
    void testGetTimetagAlwaysReturnsMinusOne()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createDouble(3.14159);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertEquals(-1, wme.getTimetag());
    }
    
    @Test
    void testGetPreferencesAlwaysReturnsAnEmptyIterator()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createDouble(3.14159);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertFalse(wme.getPreferences().hasNext());
    }
    
    @Test
    void testIsAcceptableAlwaysReturnsFalse()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createDouble(3.14159);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertFalse(wme.isAcceptable());
    }
    
    @Test
    void testAlternateFormatTo()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createInteger(3);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertEquals("(T1 ^attr 3)\n", String.format("%#s", wme));
    }
    
    @Test
    void testFormatTo()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createInteger(3);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertEquals("(-1: T1 ^attr 3)\n", String.format("%s", wme));
    }
}
