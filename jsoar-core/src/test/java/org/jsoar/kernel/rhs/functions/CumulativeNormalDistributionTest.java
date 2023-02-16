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
 * 
 * @author chris.kawatsu
 *
 */
class CumulativeNormalDistributionTest extends JSoarTest
{
    @Test
    void testZero() throws Exception
    {
        final CumulativeNormalDistribution cndf = new CumulativeNormalDistribution();
        final Symbol result = cndf.execute(rhsFuncContext, Symbols.asList(syms, 0.0d));
        assertEquals(0.5d, result.asDouble().getValue(), .0001d);
    }
    
    @Test
    void testOne() throws Exception
    {
        final CumulativeNormalDistribution cndf = new CumulativeNormalDistribution();
        final Symbol result = cndf.execute(rhsFuncContext, Symbols.asList(syms, 1.0d));
        assertEquals(0.8413d, result.asDouble().getValue(), .0001d);
    }
}
