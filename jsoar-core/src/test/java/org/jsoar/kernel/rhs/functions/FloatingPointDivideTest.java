/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class FloatingPointDivideTest extends JSoarTest
{
    
    @Test
    void testZeroArgs()
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertThrows(RhsFunctionException.class, () -> divide.execute(rhsFuncContext, Symbols.asList(syms)));
    }
    
    @Test
    void testOneIntArg() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(1.0 / 33.0, divide.execute(rhsFuncContext, Symbols.asList(syms, 33)).asDouble().getValue(), 0.0001);
    }
    
    @Test
    void testOneFloatArg() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(1 / 123.4, divide.execute(rhsFuncContext, Symbols.asList(syms, 123.4)).asDouble().getValue(), 0.0001);
    }
    
    @Test
    void testMixedArgs() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(2 / 123.4 / -2, divide.execute(rhsFuncContext, Symbols.asList(syms, 2, 123.4, -2)).asDouble().getValue(), 0.0001);
    }
    
    @Test
    void testIntArgs() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(2.0 / 3 / 4 / -2, divide.execute(rhsFuncContext, Symbols.asList(syms, 2, 3, 4, -2)).asDouble().getValue(), 0.0001);
    }
    
    @Test
    void testFloatArgs() throws Exception
    {
        FloatingPointDivide divide = new FloatingPointDivide();
        
        assertEquals(2.0 / 3.0 / 4.0 / -2.0, divide.execute(rhsFuncContext, Symbols.asList(syms, 2.0, 3.0, 4.0, -2.0)).asDouble().getValue(), 0.0001);
    }
    
}
