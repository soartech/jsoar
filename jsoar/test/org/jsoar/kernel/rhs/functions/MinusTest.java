/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;


/**
 * @author ray
 */
public class MinusTest extends JSoarTest
{
    
    @Test(expected=RhsFunctionException.class)
    public void testZeroArgs() throws Exception
    {
        Minus minus = new Minus();
        
        minus.execute(syms, Symbols.asList(syms));
    }
    
    @Test
    public void testOneIntArg() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(-33, minus.execute(syms, Symbols.asList(syms, 33)).asInteger().getValue());
    }
    
    @Test
    public void testOneFloatArg() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(-123.4, minus.execute(syms, Symbols.asList(syms, 123.4)).asDouble().getValue(), 0.0001);
    }
    
    @Test
    public void testMixedArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2 - 123.4 - -2, minus.execute(syms, Symbols.asList(syms, 2, 123.4, -2)).asDouble().getValue(), 0.0001);
    }
    
    @Test
    public void testIntArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2 - 3 - 4 - -2, minus.execute(syms, Symbols.asList(syms, 2, 3, 4, -2)).asInteger().getValue());
    }
    
    @Test
    public void testFloatArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2.0 - 3.0 - 4.0 - -2.0, minus.execute(syms, Symbols.asList(syms, 2.0, 3.0, 4.0, -2.0)).asDouble().getValue(), 0.0001);
    }

}
