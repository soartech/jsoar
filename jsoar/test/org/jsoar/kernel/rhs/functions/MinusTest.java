/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
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
        
        minus.execute(syms, syms.makeList());
    }
    
    @Test
    public void testOneIntArg() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(-33, minus.execute(syms, syms.makeList(33)).asIntConstant().value);
    }
    
    @Test
    public void testOneFloatArg() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(-123.4, minus.execute(syms, syms.makeList(123.4)).asFloatConstant().value);
    }
    
    @Test
    public void testMixedArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2 - 123.4 - -2, minus.execute(syms, syms.makeList(2, 123.4, -2)).asFloatConstant().value);
    }
    
    @Test
    public void testIntArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2 - 3 - 4 - -2, minus.execute(syms, syms.makeList(2, 3, 4, -2)).asIntConstant().value);
    }
    
    @Test
    public void testFloatArgs() throws Exception
    {
        Minus minus = new Minus();
        
        assertEquals(2.0 - 3.0 - 4.0 - -2.0, minus.execute(syms, syms.makeList(2.0, 3.0, 4.0, -2.0)).asFloatConstant().value);
    }

}
