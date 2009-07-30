/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 30, 2008
 */
package org.jsoar.kernel.symbols;


import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

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
    
    @Test
    public void testGetStringSymbols()
    {
        final StringSymbolImpl a = syms.createString("a");
        final StringSymbolImpl b = syms.createString("b");
        
        final List<StringSymbol> strings = syms.getSymbols(StringSymbol.class);
        assertNotNull(strings);
        assertEquals(2, strings.size());
        assertTrue(strings.contains(a));
        assertTrue(strings.contains(b));
    }
    
    @Test
    public void testGetIntegerSymbols()
    {
        final IntegerSymbolImpl a = syms.createInteger(2);
        final IntegerSymbolImpl b = syms.createInteger(3);
        
        final List<IntegerSymbol> values = syms.getSymbols(IntegerSymbol.class);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
    }
    @Test
    public void testGetDoubleSymbols()
    {
        final DoubleSymbolImpl a = syms.createDouble(2.2);
        final DoubleSymbolImpl b = syms.createDouble(3.3);
        
        final List<DoubleSymbol> values = syms.getSymbols(DoubleSymbol.class);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
    }
    @Test
    public void testGetVariableSymbols()
    {
        final Variable a = syms.make_variable("a");
        final Variable b = syms.make_variable("b");
        
        final List<Variable> values = syms.getSymbols(Variable.class);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
    }
    @Test
    public void testGetJavaSymbols()
    {
        final JavaSymbolImpl a = syms.createJavaSymbol(new File("hi"));
        final JavaSymbolImpl b = syms.createJavaSymbol(new File("bye"));
        final JavaSymbolImpl n = syms.createJavaSymbol(null);
        
        final List<JavaSymbol> values = syms.getSymbols(JavaSymbol.class);
        assertNotNull(values);
        assertEquals(3, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
        assertTrue(values.contains(n));
    }
    @Test
    public void testGetIdentifierSymbols()
    {
        final IdentifierImpl a = syms.createIdentifier('s');
        final IdentifierImpl b = syms.createIdentifier('i');
        
        final List<Identifier> values = syms.getSymbols(Identifier.class);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
    }
}
