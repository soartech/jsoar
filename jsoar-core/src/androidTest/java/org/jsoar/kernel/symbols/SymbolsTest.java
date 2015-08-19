/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 9, 2009
 */
package org.jsoar.kernel.symbols;

import org.jsoar.JSoarTest;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ray
 */
public class SymbolsTest extends JSoarTest
{
    public void testCreateWithNewIdCreatesANewIdentifier()
    {
        final Symbol s = Symbols.create(syms, Symbols.NEW_ID);
        assertNotNull(s.asIdentifier());
    }

    public void testCreateWithSymbolReturnsTheSymbol()
    {
        final IntegerSymbol i = syms.createInteger(42);
        assertSame(i, Symbols.create(syms, i));
    }
    
    public void testCreateWithCharReturnsString()
    {
        final Symbol s = Symbols.create(syms, 'X');
        assertEquals("X", s.asString().getValue());
    }
    
    public void testCreateWithDoubleReturnsDouble()
    {
        final Symbol s = Symbols.create(syms, 98.765);
        assertEquals(98.765, s.asDouble().getValue(), 0.000001);
    }
    
    public void testCreateWithFloatReturnsDouble()
    {
        final Symbol s = Symbols.create(syms, 98.765f);
        assertEquals(98.765, s.asDouble().getValue(), 0.000001);
    }
    
    public void testCreateWithIntegerReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, 98);
        assertEquals(98, s.asInteger().getValue());
    }
    public void testCreateWithLongReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, 98L);
        assertEquals(98, s.asInteger().getValue());
    }
    public void testCreateWithShortReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, (short) 98);
        assertEquals(98, s.asInteger().getValue());
    }
    public void testCreateWithByteReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, (byte) 34);
        assertEquals(34, s.asInteger().getValue());
    }
    public void testCreateWithAtomicIntegerReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, new AtomicInteger(42));
        assertEquals(42, s.asInteger().getValue());
    }
    public void testCreateWithAtomicLongReturnsInteger()
    {
        final Symbol s = Symbols.create(syms, new AtomicLong(43));
        assertEquals(43, s.asInteger().getValue());
    }
    
    public void testCreateWithNullReturnsJavaSymbol()
    {
        final JavaSymbol s = Symbols.create(syms, null).asJava();
        assertNotNull(s);
        assertNull(s.getValue());
    }
    
    public void testCreateWithOtherReturnsJavaSymbol()
    {
        final File f = new File("testCreateWithOtherReturnsJavaSymbol");
        final JavaSymbol s = Symbols.create(syms, f).asJava();
        assertNotNull(s);
        assertSame(f, s.getValue());
    }
    
    public void testValueOfIdentifierReturnsIdentifier()
    {
        final Identifier id = syms.createIdentifier('X');
        assertSame(id, Symbols.valueOf(id));
    }
    
    public void testValueOfInteger()
    {
        final IntegerSymbol s = syms.createInteger(42);
        assertEquals(42L, Symbols.valueOf(s));
    }
    
    public void testValueOfDouble()
    {
        final DoubleSymbol s = syms.createDouble(3.14159);
        assertEquals(3.14159, (Double) Symbols.valueOf(s), 0.0001);
    }
    
    public void testValueOfString()
    {
        final StringSymbol s = syms.createString("testValueOfString");
        assertEquals("testValueOfString", Symbols.valueOf(s));
    }
    
    public void testValueOfNonNullJavaSymbol()
    {
        final File f = new File("testValueOfJavaSymbol");
        final JavaSymbol s = syms.createJavaSymbol(f);
        assertSame(f, Symbols.valueOf(s));
    }
    
    public void testValueOfNullJavaSymbol()
    {
        final JavaSymbol s = syms.createJavaSymbol(null);
        assertNull(Symbols.valueOf(s));
    }

    public void testGetFirstLetterOfEmptyStringReturnsZ()
    {
        assertEquals('Z', Symbols.getFirstLetter(""));
    }
    
    public void testGetFirstLetterOfNonAlphabeticStringReturnsZ()
    {
        assertEquals('Z', Symbols.getFirstLetter("1234"));
    }
    
    public void testGetFirstLetterOfIntegerReturnsZ()
    {
        assertEquals('Z', Symbols.getFirstLetter(5678));
    }
    public void testGetFirstLetterOfDoubleReturnsZ()
    {
        assertEquals('Z', Symbols.getFirstLetter(3.14159));
    }
    public void testGetFirstLetterOfAlphabeticString()
    {
        assertEquals('C', Symbols.getFirstLetter("Cat"));
    }
    

}
