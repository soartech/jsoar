/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 6, 2008
 */
package org.jsoar.kernel.symbols;


import static org.junit.Assert.*;

import org.jsoar.JSoarTest;
import org.junit.Test;

/**
 * @author ray
 */
public class SymbolTest extends JSoarTest
{

    @Test
    public void testNumericComparisons()
    {
        SymbolImpl i5 = syms.make_int_constant(5);
        SymbolImpl im99 = syms.make_int_constant(-99);
        SymbolImpl f33_3 = syms.make_float_constant(33.3);
        SymbolImpl string = syms.make_sym_constant("S");
        
        assertFalse(i5.numericLess(im99));
        assertFalse(i5.numericLessOrEqual(im99));
        assertTrue(i5.numericGreater(im99));
        assertTrue(i5.numericGreaterOrEqual(im99));
        assertFalse(i5.numericGreater(i5));
        assertFalse(i5.numericLess(i5));
        assertTrue(i5.numericGreaterOrEqual(i5));
        assertTrue(i5.numericLessOrEqual(i5));
        
        assertTrue(i5.numericLess(f33_3));
        assertTrue(i5.numericLessOrEqual(f33_3));
        assertTrue(f33_3.numericGreater(i5));
        assertTrue(f33_3.numericGreaterOrEqual(i5));
        
        assertFalse(f33_3.numericGreater(f33_3));
        assertFalse(f33_3.numericLess(f33_3));
        assertTrue(f33_3.numericGreaterOrEqual(f33_3));
        assertTrue(f33_3.numericLessOrEqual(f33_3));
        
        assertFalse(i5.numericGreater(string));
        assertFalse(i5.numericGreaterOrEqual(string));
        assertFalse(i5.numericLess(string));
        assertFalse(i5.numericLessOrEqual(string));
        
        assertFalse(f33_3.numericGreater(string));
        assertFalse(f33_3.numericGreaterOrEqual(string));
        assertFalse(f33_3.numericLess(string));
        assertFalse(f33_3.numericLessOrEqual(string));
    }
}
