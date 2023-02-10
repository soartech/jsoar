/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 28, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class MinTest extends JSoarTest
{
    
    @Test
    public void testMinWithAllIntegers() throws Exception
    {
        final Min min = new Min();
        final Symbol result = min.execute(rhsFuncContext, Symbols.asList(syms, 2, 1, -40, 99));
        assertEquals(-40, result.asInteger().getValue());
    }
    
    @Test
    public void testMinWithLargeIntegers() throws Exception
    {
        final Min min = new Min();
        final Symbol result = min.execute(rhsFuncContext, Symbols.asList(syms, 2, 1, -40000000000L, 99));
        assertEquals(-40000000000L, result.asInteger().getValue());
    }
    
    @Test
    public void testMinWithAllDoubles() throws Exception
    {
        final Min min = new Min();
        final Symbol result = min.execute(rhsFuncContext, Symbols.asList(syms, 2., 1., -40., 99.));
        assertEquals(-40., result.asDouble().getValue(), 0.00001);
    }
    
    @Test
    public void testMinWithMixedTypesAndIntResult() throws Exception
    {
        final Min min = new Min();
        final Symbol result = min.execute(rhsFuncContext, Symbols.asList(syms, 2., 1., -40, 99));
        assertEquals(-40, result.asInteger().getValue());
    }
    
    @Test
    public void testMinWithMixedTypesAndDoubleResult() throws Exception
    {
        final Min min = new Min();
        final Symbol result = min.execute(rhsFuncContext, Symbols.asList(syms, 2, 1., -40., 99));
        assertEquals(-40., result.asDouble().getValue(), 0.0001);
    }
}
