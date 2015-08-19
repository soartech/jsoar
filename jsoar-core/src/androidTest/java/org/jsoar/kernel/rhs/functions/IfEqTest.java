/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;

import java.util.List;

/**
 * @author ray
 */
public class IfEqTest extends JSoarTest
{

    public void testReturnThirdArgWhenFirstTwoEqual() throws Exception
    {
        List<Symbol> args = Symbols.asList(syms, "a", "a", "b", "c");
        assertSame(args.get(2), new IfEq().execute(rhsFuncContext, args));
    }
    
    public void testReturnFourthArgWhenFirstTwoNotEqual() throws Exception
    {
        List<Symbol> args = Symbols.asList(syms, "a", "x", "b", "c");
        assertSame(args.get(3), new IfEq().execute(rhsFuncContext, args));
    }

}
