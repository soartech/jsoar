/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 9, 2010
 */
package org.jsoar.kernel.memory;


import static org.junit.Assert.*;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class DummyWmeTest
{
    private SymbolFactory syms;

    @Before
    public void setUp() throws Exception
    {
        syms = new SymbolFactoryImpl();
    }

    @Test
    public void testGetIdentiferAttrAndValue()
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
    public void testGetTimetagAlwaysReturnsMinusOne()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createDouble(3.14159);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertEquals(-1, wme.getTimetag());
    }
    
    @Test
    public void testGetPreferencesAlwaysReturnsAnEmptyIterator()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createDouble(3.14159);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertFalse(wme.getPreferences().hasNext());
    }
    
    @Test
    public void testIsAcceptableAlwaysReturnsFalse()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createDouble(3.14159);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertFalse(wme.isAcceptable());
    }
    
    @Test
    public void testAlternateFormatTo()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createInteger(3);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertEquals("(T1 ^attr 3)\n", String.format("%#s", wme));
    }
    
    @Test
    public void testFormatTo()
    {
        final Identifier id = syms.createIdentifier('T');
        final Symbol attr = syms.createString("attr");
        final Symbol value = syms.createInteger(3);
        
        final DummyWme wme = new DummyWme(id, attr, value);
        assertEquals("(-1: T1 ^attr 3)\n", String.format("%s", wme));
    }
}
