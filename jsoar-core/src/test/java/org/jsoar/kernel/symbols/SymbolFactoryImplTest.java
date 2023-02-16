/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 30, 2008
 */
package org.jsoar.kernel.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.jsoar.util.ByRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class SymbolFactoryImplTest
{
    private SymbolFactoryImpl syms;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        syms = new SymbolFactoryImpl();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
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
        assertNotEquals(0, s.hash_id);
        assertSame(s, syms.findIdentifier(s.getNameLetter(), s.getNameNumber()));
        
        // Make another id and make sure the id increments
        s = syms.make_new_identifier('s', (short) 4);
        assertNotNull(s);
        assertEquals('S', s.getNameLetter());
        assertEquals(2, s.getNameNumber());
        assertEquals(4, s.level);
        assertNotEquals(0, s.hash_id);
        assertSame(s, syms.findIdentifier(s.getNameLetter(), s.getNameNumber()));
    }
    
    @Test
    public void testMakeFloatConstant()
    {
        DoubleSymbolImpl s = syms.createDouble(3.14);
        assertNotNull(s);
        assertEquals(3.14, s.getValue(), 0.0001);
        assertNotEquals(0, s.hash_id);
        assertSame(s, syms.findDouble(s.getValue()));
        assertSame(s, syms.createDouble(s.getValue()));
    }
    
    @Test
    public void testMakeIntConstant()
    {
        IntegerSymbolImpl s = syms.createInteger(99);
        assertNotNull(s);
        assertEquals(99, s.getValue());
        assertNotEquals(0, s.hash_id);
        assertSame(s, syms.findInteger(s.getValue()));
        assertSame(s, syms.createInteger(s.getValue()));
    }
    
    @Test
    public void testMakeLargeIntConstant()
    {
        IntegerSymbolImpl s = syms.createInteger(999999999999L);
        assertNotNull(s);
        assertEquals(999999999999L, s.getValue());
        assertNotEquals(0, s.hash_id);
        assertSame(s, syms.findInteger(s.getValue()));
        assertSame(s, syms.createInteger(s.getValue()));
    }
    
    @Test
    public void testMakeNewSymConstant()
    {
        StringSymbolImpl s = syms.createString("A sym constant");
        assertNotNull(s);
        assertEquals("A sym constant", s.getValue());
        assertNotEquals(0, s.hash_id);
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
    
    @Test
    public void testImportReturnsInputUnchangedIfItsAlreadyOwnedByFactory()
    {
        final IntegerSymbol s = syms.createInteger(99);
        assertSame(s, syms.importSymbol(s));
    }
    
    @Test
    public void testImportThrowsAnExceptionForIdentifiers()
    {
        final Identifier id = syms.createIdentifier('T');
        assertThrows(IllegalArgumentException.class, () -> syms.importSymbol(id));
    }
    
    @Test
    public void testImportThrowsAnExceptionForVariables()
    {
        final Variable id = syms.make_variable("foo");
        assertThrows(IllegalArgumentException.class, () -> syms.importSymbol(id));
    }
    
    @Test
    public void testCanImportStringsAcrossFactories()
    {
        final SymbolFactory other = new SymbolFactoryImpl();
        final StringSymbol i = syms.createString("test");
        final Symbol s = other.importSymbol(i);
        assertNotSame(i, s);
        assertEquals(i.getValue(), s.asString().getValue());
    }
    
    @Test
    public void testCanImportIntegersAcrossFactories()
    {
        final SymbolFactory other = new SymbolFactoryImpl();
        final IntegerSymbol i = syms.createInteger(12345);
        final Symbol s = other.importSymbol(i);
        assertNotSame(i, s);
        assertEquals(i.getValue(), s.asInteger().getValue());
    }
    
    @Test
    public void testCanImportDoublesAcrossFactories()
    {
        final SymbolFactory other = new SymbolFactoryImpl();
        final DoubleSymbol i = syms.createDouble(12345.9);
        final Symbol s = other.importSymbol(i);
        assertNotSame(i, s);
        assertEquals(i.getValue(), s.asDouble().getValue(), 0.000001);
    }
    
    @Test
    public void testCanImportJavaSymbolsAcrossFactories()
    {
        final SymbolFactory other = new SymbolFactoryImpl();
        final JavaSymbol i = syms.createJavaSymbol(new File("."));
        final Symbol s = other.importSymbol(i);
        assertNotSame(i, s);
        assertSame(i.getValue(), s.asJava().getValue());
    }
}
