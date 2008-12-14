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

import java.io.File;

import org.jsoar.util.ByRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class SymbolFactoryImplTest
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
        assertEquals('S', s.getNameLetter());
        assertEquals(1, s.getNameNumber());
        assertEquals(1, s.level);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findIdentifier(s.getNameLetter(), s.getNameNumber()));
        
        // Make another id and make sure the id increments
        s = syms.make_new_identifier('s', (short) 4);
        assertNotNull(s);
        assertEquals('S', s.getNameLetter());
        assertEquals(2, s.getNameNumber());
        assertEquals(4, s.level);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findIdentifier(s.getNameLetter(), s.getNameNumber()));
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
        StringSymbol a2 = syms.generateUniqueString("A", number);
        assertNotNull(a2);
        assertNotSame(a0, a2);
        assertNotSame(a1, a2);
        assertEquals("A2", a2.getValue());
        assertEquals(3, number.value.intValue());
    }
    
    @Test
    public void testCreateJavaSymbol()
    {
        File f = new File(System.getProperty("user.dir"));
        JavaSymbol js = syms.findJavaSymbol(f);
        assertNull(js);
        js = syms.createJavaSymbol(f);
        assertNotNull(js);
        assertEquals(f, js.getValue());
    }
    
    @Test
    public void testNullJavaSymbol()
    {
        JavaSymbol js = syms.findJavaSymbol(null);
        assertNotNull(js);
        assertNull(js.getValue());
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
