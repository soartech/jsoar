/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbols;


/**
 * @author ray
 */
public class MinusTest extends JSoarTest
{
    
    public void testZeroArgs() throws Exception
    {
        Minus minus = new Minus();
        
        minus.execute(rhsFuncContext, Symbols.asList(syms));
    }
    
    public void testOneIntArg() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(-33, minus.execute(rhsFuncContext, Symbols.asList(syms, 33)).asInteger().getValue());
    }
    
    public void testOneLargeIntArg() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(-33000000000L, minus.execute(rhsFuncContext, Symbols.asList(syms, 33000000000L)).asInteger().getValue());
    }
    
    public void testOneFloatArg() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(-123.4, minus.execute(rhsFuncContext, Symbols.asList(syms, 123.4)).asDouble().getValue(), 0.0001);
    }
    
    public void testMixedArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2 - 123.4 - -2, minus.execute(rhsFuncContext, Symbols.asList(syms, 2, 123.4, -2)).asDouble().getValue(), 0.0001);
    }
    
    public void testIntArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2 - 3 - 4 - -2, minus.execute(rhsFuncContext, Symbols.asList(syms, 2, 3, 4, -2)).asInteger().getValue());
    }
    
    public void testLargeIntArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2 - 3000000000L - 4 - -2, minus.execute(rhsFuncContext, Symbols.asList(syms, 2, 3000000000L, 4, -2)).asInteger().getValue());
    }
    
    public void testFloatArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2.0 - 3.0 - 4.0 - -2.0, minus.execute(rhsFuncContext, Symbols.asList(syms, 2.0, 3.0, 4.0, -2.0)).asDouble().getValue(), 0.0001);
    }

}
