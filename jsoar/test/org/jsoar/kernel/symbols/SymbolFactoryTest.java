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
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author ray
 */
public class SymbolFactoryTest
{
    private SymbolFactory syms;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        syms = new SymbolFactory();
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
        Identifier s = syms.make_new_identifier('s', (short) 1);
        assertNotNull(s);
        assertEquals('S', s.name_letter);
        assertEquals(1, s.name_number);
        assertEquals(1, s.level);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.find_identifier(s.name_letter, s.name_number));
        
        // Make another id and make sure the id increments
        s = syms.make_new_identifier('s', (short) 4);
        assertNotNull(s);
        assertEquals('S', s.name_letter);
        assertEquals(2, s.name_number);
        assertEquals(4, s.level);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.find_identifier(s.name_letter, s.name_number));
    }

    @Test
    public void testMakeFloatConstant()
    {
        FloatConstant s = syms.make_float_constant(3.14);
        assertNotNull(s);
        assertEquals(3.14, s.value);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.find_float_constant(s.value));
        assertSame(s, syms.make_float_constant(s.value));
    }

    @Test
    public void testMakeIntConstant()
    {
        IntConstant s = syms.make_int_constant(99);
        assertNotNull(s);
        assertEquals(99, s.value);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.find_int_constant(s.value));
        assertSame(s, syms.make_int_constant(s.value));
    }
    
    @Test
    public void testMakeNewSymConstant()
    {
        SymConstant s = syms.make_sym_constant("A sym constant");
        assertNotNull(s);
        assertEquals("A sym constant", s.name);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.find_sym_constant(s.name));
        assertSame(s, syms.make_sym_constant(s.name));
    }
    
    @Test
    public void testGenerateNewSymConstant()
    {
        SymConstant a0 = syms.make_sym_constant("A0");
        SymConstant a1 = syms.make_sym_constant("A1");
        
        ByRef<Integer> number = ByRef.create(0);
        SymConstant a2 = syms.generate_new_sym_constant("A", number);
        assertNotNull(a2);
        assertNotSame(a0, a2);
        assertNotSame(a1, a2);
        assertEquals("A2", a2.name);
        assertEquals(3, number.value);
    }
    
    @Test
    @Ignore
    public void testGarbageCollectedSymbolsAreRemovedFromCache()
    {
        // TODO: Implement this functionality using WeakReference
        for(int i = 0; i < 1000; ++i)
        {
            syms.make_int_constant(i);
        }
        System.gc();
        for(int i = 0; i < 1000; ++i)
        {
            assertNull(syms.find_int_constant(i));
        }
    }
}
