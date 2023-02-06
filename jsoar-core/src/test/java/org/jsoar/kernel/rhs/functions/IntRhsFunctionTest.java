/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class IntRhsFunctionTest extends JSoarTest
{

    @Test
    public void testConvertString() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, "99"));
        assertEquals(99, result.asInteger().getValue());
    }
    
    @Test
    public void testConvertStringNegative() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, "-99"));
        assertEquals(-99, result.asInteger().getValue());
    }
        
    @Test
    public void testConvertStringWithLargeInteger() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, "999999999999999"));
        assertEquals(999999999999999L, result.asInteger().getValue());
    }
    
    @Test
    public void testConvertStringWithDouble() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, "3.14159"));
        assertEquals(3, result.asInteger().getValue());
    }
    
    @Test
    public void testConvertStringWithRoundedDouble() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, "3.0"));
        assertEquals(3, result.asInteger().getValue());
    }
    
    @Test
    public void testConvertStringWithNaN() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        try
        {
            f.execute(rhsFuncContext, Symbols.asList(syms, Double.toString(Double.NaN)));
            fail("NaN can't be cast to an integer.");
        }
        catch (RhsFunctionException e)
        {
            
        }
    }
    
    @Test
    public void testConvertEmptyString() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        try
        {
            f.execute(rhsFuncContext, Symbols.asList(syms, ""));
            fail("Empty string can't be cast to an integer.");
        }
        catch (RhsFunctionException e)
        {
            
        }
    }
    
    @Test
    public void testConvertNonSenseString() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        try
        {
            f.execute(rhsFuncContext, Symbols.asList(syms, "abc123abc"));
            fail("String 'abc123abc' can't be cast to an integer.");
        }
        catch (RhsFunctionException e)
        {
            
        }
    }
        
    @Test
    public void testConvertMinus() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        try
        {
            f.execute(rhsFuncContext, Symbols.asList(syms, "-"));
            fail("String '-' can't be cast to an integer.");
        }
        catch (RhsFunctionException e)
        {
            
        }
    }
    
    @Test
    public void testConvertDoubleMinus() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        try
        {
            f.execute(rhsFuncContext, Symbols.asList(syms, "--"));
            fail("String '--' can't be cast to an integer.");
        }
        catch (RhsFunctionException e)
        {
            
        }
    }
    
    @Test
    public void testConvertStringWithInfinity() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        try
        {
            f.execute(rhsFuncContext, Symbols.asList(syms, Double.toString(Double.POSITIVE_INFINITY)));
            fail("Infinity can't be cast to an integer.");
        }
        catch (RhsFunctionException e)
        {
            
        }
    }   
    
    @Test
    public void testConvertDouble() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, 3.14159));
        assertEquals(3, result.asInteger().getValue());
    }
    
    @Test
    public void testExponential() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, "3.14159e5"));
        assertEquals(3, result.asInteger().getValue());
        result = f.execute(rhsFuncContext, Symbols.asList(syms, "3.9e-10"));
        assertEquals(3, result.asInteger().getValue());
        result = f.execute(rhsFuncContext, Symbols.asList(syms, "3.9E-10"));
        assertEquals(3, result.asInteger().getValue());
        result = f.execute(rhsFuncContext, Symbols.asList(syms, "-100E-10"));
        assertEquals(-100, result.asInteger().getValue());
    }
    
    @Test
    public void testStringBehavior() throws Exception
    {
        IntRhsFunction f = new IntRhsFunction();
        Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, "123abc"));
        assertEquals(123, result.asInteger().getValue());
        result = f.execute(rhsFuncContext, Symbols.asList(syms, "-123abc"));
        assertEquals(-123, result.asInteger().getValue());
        result = f.execute(rhsFuncContext, Symbols.asList(syms, "123.45abc"));
        assertEquals(123, result.asInteger().getValue());
        result = f.execute(rhsFuncContext, Symbols.asList(syms, "-123.45abc"));
        assertEquals(-123, result.asInteger().getValue());
        result = f.execute(rhsFuncContext, Symbols.asList(syms, "-1abldk\\fjasd\0f098234523.45asdlkfjxcv......abc"));
        assertEquals(-1, result.asInteger().getValue());
        result = f.execute(rhsFuncContext, Symbols.asList(syms, "-0"));
        assertEquals(0, result.asInteger().getValue());
    }
    
}
