/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class IfEqTest extends JSoarTest
{
    
    @Test
    void testReturnThirdArgWhenFirstTwoEqual() throws Exception
    {
        List<Symbol> args = Symbols.asList(syms, "a", "a", "b", "c");
        assertSame(args.get(2), new IfEq().execute(rhsFuncContext, args));
    }
    
    @Test
    void testReturnFourthArgWhenFirstTwoNotEqual() throws Exception
    {
        List<Symbol> args = Symbols.asList(syms, "a", "x", "b", "c");
        assertSame(args.get(3), new IfEq().execute(rhsFuncContext, args));
    }
    
}
