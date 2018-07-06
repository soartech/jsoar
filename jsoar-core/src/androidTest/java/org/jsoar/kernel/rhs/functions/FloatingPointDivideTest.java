/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import junit.framework.Assert;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbols;


/**
 * @author ray
 */
public class FloatingPointDivideTest extends JSoarTest
{
    
    public void testZeroArgs() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        try {
            divide.execute(rhsFuncContext, Symbols.asList(syms));
            Assert.fail("Should have thrown");
        }catch (RhsFunctionException e){
            Assert.assertEquals("'/' function called with 0 arguments. Expected at least 1.", e.getMessage());
        }
    }
    
    public void testOneIntArg() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(1.0 / 33.0, divide.execute(rhsFuncContext, Symbols.asList(syms, 33)).asDouble().getValue(), 0.0001);
    }
    
    public void testOneFloatArg() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(1 / 123.4, divide.execute(rhsFuncContext, Symbols.asList(syms, 123.4)).asDouble().getValue(), 0.0001);
    }
    
    public void testMixedArgs() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(2 / 123.4 / -2, divide.execute(rhsFuncContext, Symbols.asList(syms, 2, 123.4, -2)).asDouble().getValue(), 0.0001);
    }
    
    public void testIntArgs() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(2.0 / 3 / 4 / -2, divide.execute(rhsFuncContext, Symbols.asList(syms, 2, 3, 4, -2)).asDouble().getValue(), 0.0001);
    }
    
    public void testFloatArgs() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(2.0 / 3.0 / 4.0 / -2.0, divide.execute(rhsFuncContext, Symbols.asList(syms, 2.0, 3.0, 4.0, -2.0)).asDouble().getValue(), 0.0001);
    }

}
