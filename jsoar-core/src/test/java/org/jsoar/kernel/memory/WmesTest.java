/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 21, 2010
 */
package org.jsoar.kernel.memory;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Before;
import org.junit.Test;

public class WmesTest
{
    private SymbolFactory syms;
    private MockFactory factory;
    private static class Triple
    {
        final Identifier id;
        final Symbol attr;
        final Symbol value;
        public Triple(Identifier id, Symbol attr, Symbol value)
        {
            this.id = id;
            this.attr = attr;
            this.value = value;
        }
    }
    private class MockFactory implements WmeFactory<Void>
    {
        final List<Triple> triples = new ArrayList<Triple>();
        
        @Override
        public Void addWme(Identifier id, Symbol attr, Symbol value)
        {
            triples.add(new Triple(id, attr, value));
            return null;
        }

        @Override
        public SymbolFactory getSymbols()
        {
            return syms;
        }
    }
    
    @Before
    public void setUp()
    {
        syms = new SymbolFactoryImpl();
        factory = new MockFactory();
    }

    @Test
    public void testCreateLinkedListWithEmptyListReturnsEmptyIdentifier()
    {
        final Identifier result = Wmes.createLinkedList(factory, Collections.emptyList().iterator());
        assertNotNull(result);
        assertEquals(0, factory.triples.size());
    }
    
    @Test
    public void testCreateLinkedListWithOneEntry()
    {
        final Identifier result = Wmes.createLinkedList(factory, Arrays.asList("first").iterator());
        assertNotNull(result);
        assertEquals(1, factory.triples.size());
        verifyTriple(0, result, "value", "first");
    }
    
    @Test
    public void testCreateLinkedListWithTwoEntries()
    {
        @SuppressWarnings("unchecked")
        final Identifier result = Wmes.createLinkedList(factory, Arrays.asList("first", 99).iterator());
        assertNotNull(result);
        assertEquals(3, factory.triples.size());
        verifyTriple(0, result, "value", "first");
        verifyTriple(1, result, "next", null);
        verifyTriple(2, factory.triples.get(1).value.asIdentifier(), "value", 99);
    }
    
    private void verifyTriple(int index, Identifier id, Object attr, Object value)
    {
        assertSame(id, factory.triples.get(index).id);
        assertSame(Symbols.create(syms, attr), factory.triples.get(index).attr);
        if(value != null)
        {
            assertSame(Symbols.create(syms, value), factory.triples.get(index).value);
        }
        else
        {
            assertTrue(factory.triples.get(index).value instanceof Identifier);
        }
    }
}
