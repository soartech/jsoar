/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 9, 2009
 */
package org.jsoar.kernel.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jsoar.JSoarTest;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class SymbolsTest extends JSoarTest
{
    @Test
    void testCreateWithNewIdCreatesANewIdentifier()
    {
        final Symbol s = Symbols.create(syms, Symbols.NEW_ID);
        assertNotNull(s.asIdentifier());
    }
    
    @Test
    void testCreateWithSymbolReturnsTheSymbol()
    {
        final IntegerSymbol i = syms.createInteger(42);
        assertSame(i, Symbols.create(syms, i));
    }
    
    @Test
    void testCreateWithCharReturnsString()
    {
        final Symbol s = Symbols.create(syms, 'X');
        assertEquals("X", s.asString().getValue());
    }
    
    @Test
    void testCreateWithDoubleReturnsDouble()
    {
        final Symbol s = Symbols.create(syms, 98.765);
        assertEquals(98.765, s.asDouble().getValue(), 0.000001);
    }
    
    @Test
    void testCreateWithFloatReturnsDouble()
    {
        final Symbol s = Symbols.create(syms, 98.765f);
        assertEquals(98.765, s.asDouble().getValue(), 0.000001);
    }
    
    @Test
    void testCreateWithIntegerReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, 98);
        assertEquals(98, s.asInteger().getValue());
    }
    
    @Test
    void testCreateWithLongReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, 98L);
        assertEquals(98, s.asInteger().getValue());
    }
    
    @Test
    void testCreateWithShortReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, (short) 98);
        assertEquals(98, s.asInteger().getValue());
    }
    
    @Test
    void testCreateWithByteReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, (byte) 34);
        assertEquals(34, s.asInteger().getValue());
    }
    
    @Test
    void testCreateWithAtomicIntegerReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, new AtomicInteger(42));
        assertEquals(42, s.asInteger().getValue());
    }
    
    @Test
    void testCreateWithAtomicLongReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, new AtomicLong(43));
        assertEquals(43, s.asInteger().getValue());
    }
    
    @Test
    void testCreateWithNullReturnsJavaSymbol()
    {
        final JavaSymbol s = Symbols.create(syms, null).asJava();
        assertNotNull(s);
        assertNull(s.getValue());
    }
    
    @Test
    void testCreateWithOtherReturnsJavaSymbol()
    {
        final File f = new File("testCreateWithOtherReturnsJavaSymbol");
        final JavaSymbol s = Symbols.create(syms, f).asJava();
        assertNotNull(s);
        assertSame(f, s.getValue());
    }
    
    @Test
    void testValueOfIdentifierReturnsIdentifier()
    {
        final Identifier id = syms.createIdentifier('X');
        assertSame(id, Symbols.valueOf(id));
    }
    
    @Test
    void testValueOfInteger()
    {
        final IntegerSymbol s = syms.createInteger(42);
        assertEquals(42L, Symbols.valueOf(s));
    }
    
    @Test
    void testValueOfDouble()
    {
        final DoubleSymbol s = syms.createDouble(3.14159);
        assertEquals(3.14159, (Double) Symbols.valueOf(s), 0.0001);
    }
    
    @Test
    void testValueOfString()
    {
        final StringSymbol s = syms.createString("testValueOfString");
        assertEquals("testValueOfString", Symbols.valueOf(s));
    }
    
    @Test
    void testValueOfNonNullJavaSymbol()
    {
        final File f = new File("testValueOfJavaSymbol");
        final JavaSymbol s = syms.createJavaSymbol(f);
        assertSame(f, Symbols.valueOf(s));
    }
    
    @Test
    void testValueOfNullJavaSymbol()
    {
        final JavaSymbol s = syms.createJavaSymbol(null);
        assertNull(Symbols.valueOf(s));
    }
    
    @Test
    void testGetFirstLetterOfEmptyStringReturnsZ()
    {
        assertEquals('Z', Symbols.getFirstLetter(""));
    }
    
    @Test
    void testGetFirstLetterOfNonAlphabeticStringReturnsZ()
    {
        assertEquals('Z', Symbols.getFirstLetter("1234"));
    }
    
    @Test
    void testGetFirstLetterOfIntegerReturnsZ()
    {
        assertEquals('Z', Symbols.getFirstLetter(5678));
    }
    
    @Test
    void testGetFirstLetterOfDoubleReturnsZ()
    {
        assertEquals('Z', Symbols.getFirstLetter(3.14159));
    }
    
    @Test
    void testGetFirstLetterOfAlphabeticString()
    {
        assertEquals('C', Symbols.getFirstLetter("Cat"));
    }
    
}
