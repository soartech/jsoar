/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 28, 2009
 */
package org.jsoar.kernel.rhs.functions;


import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;

/**
 * 
 * @author chris.kawatsu
 *
 */
public class CumulativeNormalDistributionTest extends JSoarTest
{
    public void testZero() throws Exception
    {
        final CumulativeNormalDistribution cndf = new CumulativeNormalDistribution();
        final Symbol result = cndf.execute(rhsFuncContext, Symbols.asList(syms, 0.0d));
        assertEquals(0.5d, result.asDouble().getValue(), .0001d);
    }
    
    public void testOne() throws Exception
    {
        final CumulativeNormalDistribution cndf = new CumulativeNormalDistribution();
        final Symbol result = cndf.execute(rhsFuncContext, Symbols.asList(syms, 1.0d));
        assertEquals(0.8413d, result.asDouble().getValue(), .0001d);
    }
}
