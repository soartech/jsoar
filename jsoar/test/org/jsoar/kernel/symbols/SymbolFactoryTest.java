/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 30, 2008
 */
package org.jsoar.kernel.symbols;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.jsoar.util.ByRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class SymbolFactoryTest
{
    private SymbolFactoryImpl syms;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        syms = new SymbolFactoryImpl();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        syms = null;
    }
    
    @Test
    public void testMakeNewIdentifier()
    {
        IdentifierImpl s = syms.make_new_identifier('s', (short) 1);
        assertNotNull(s);
        assertEquals('S', s.name_letter);
        assertEquals(1, s.name_number);
        assertEquals(1, s.level);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findIdentifier(s.name_letter, s.name_number));
        
        // Make another id and make sure the id increments
        s = syms.make_new_identifier('s', (short) 4);
        assertNotNull(s);
        assertEquals('S', s.name_letter);
        assertEquals(2, s.name_number);
        assertEquals(4, s.level);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findIdentifier(s.name_letter, s.name_number));
    }

    @Test
    public void testMakeFloatConstant()
    {
        DoubleSymbolImpl s = syms.createDouble(3.14);
        assertNotNull(s);
        assertEquals(3.14, s.getValue(), 0.0001);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findDouble(s.getValue()));
        assertSame(s, syms.createDouble(s.getValue()));
    }

    @Test
    public void testMakeIntConstant()
    {
        IntegerSymbolImpl s = syms.createInteger(99);
        assertNotNull(s);
        assertEquals(99, s.getValue());
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findInteger(s.getValue()));
        assertSame(s, syms.createInteger(s.getValue()));
    }
    
    @Test
    public void testMakeNewSymConstant()
    {
        StringSymbolImpl s = syms.createString("A sym constant");
        assertNotNull(s);
        assertEquals("A sym constant", s.getValue());
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findString(s.getValue()));
        assertSame(s, syms.createString(s.getValue()));
    }
    
    @Test
    public void testGenerateNewSymConstant()
    {
        StringSymbolImpl a0 = syms.createString("A0");
        StringSymbolImpl a1 = syms.createString("A1");
        
        ByRef<Integer> number = ByRef.create(0);
        StringSymbolImpl a2 = syms.generate_new_sym_constant("A", number);
        assertNotNull(a2);
        assertNotSame(a0, a2);
        assertNotSame(a1, a2);
        assertEquals("A2", a2.getValue());
        assertEquals(3, number.value.intValue());
    }
    
    @Test
    public void testGarbageCollectedSymbolsAreRemovedFromCache()
    {
        for(int i = 0; i < 1000; ++i)
        {
            assertNotNull(syms.createInteger(i));
            assertNotNull(syms.createString(Integer.toString(i)));
        }
        // Why do I believe this test works? Because it fails if I remove the
        // call to the garbage collector here :)
        System.gc();
        for(int i = 0; i < 1000; ++i)
        {
            assertNull(syms.findInteger(i));
            assertNull(syms.findString(Integer.toString(i)));
        }
    }
}
