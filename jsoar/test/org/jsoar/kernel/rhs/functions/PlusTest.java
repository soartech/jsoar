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
public class PlusTest extends JSoarTest
{
    
    @Test
    public void testZeroArgs() throws Exception
    {
        Plus plus = new Plus();
        
        assertEquals(0, plus.execute(syms, syms.makeList()).asIntConstant().value);
    }
    
    @Test
    public void testOneIntArg() throws Exception
    {
        Plus plus = new Plus();
        
        assertEquals(33, plus.execute(syms, syms.makeList(33)).asIntConstant().value);
    }
    
    @Test
    public void testOneFloatArg() throws Exception
    {
        Plus plus = new Plus();
        
        assertEquals(123.4, plus.execute(syms, syms.makeList(123.4)).asFloatConstant().value);
    }
    
    @Test
    public void testMixedArgs() throws Exception
    {
        Plus plus = new Plus();
        
        assertEquals(123.4, plus.execute(syms, syms.makeList(2, 123.4, -2)).asFloatConstant().value);
    }
    
    @Test
    public void testIntArgs() throws Exception
    {
        Plus plus = new Plus();
        
        assertEquals(7, plus.execute(syms, syms.makeList(2, 3, 4, -2)).asIntConstant().value);
    }
    
    @Test
    public void testFloatArgs() throws Exception
    {
        Plus plus = new Plus();
        
        assertEquals(7.0, plus.execute(syms, syms.makeList(2.0, 3.0, 4.0, -2.0)).asFloatConstant().value);
    }

}
