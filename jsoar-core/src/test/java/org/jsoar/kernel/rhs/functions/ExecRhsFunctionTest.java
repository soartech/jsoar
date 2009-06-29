/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 29, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;


/**
 * @author ray
 */
public class ExecRhsFunctionTest extends JSoarTest
{
    @Test(expected=RhsFunctionException.class)
    public void testExecRequiresAtLeastOneArgument() throws Exception
    {
        final RhsFunctionManager rhsFuncs = new RhsFunctionManager(rhsFuncContext);
        final ExecRhsFunction exec = new ExecRhsFunction(rhsFuncs);
        exec.execute(rhsFuncContext, new ArrayList<Symbol>());
    }
    
    @Test(expected=RhsFunctionException.class)
    public void testExecCantCallItself() throws Exception
    {
        final RhsFunctionManager rhsFuncs = new RhsFunctionManager(rhsFuncContext);
        final ExecRhsFunction exec = new ExecRhsFunction(rhsFuncs);
        exec.execute(rhsFuncContext, Symbols.asList(syms, exec.getName()));
    }
    
    @Test
    public void testExecCallsNamedFunctionWithRestOfArguments() throws Exception
    {
        final RhsFunctionManager rhsFuncs = new RhsFunctionManager(rhsFuncContext);
        rhsFuncs.registerHandler(new Plus());
        final ExecRhsFunction exec = new ExecRhsFunction(rhsFuncs);
        final Symbol result = exec.execute(rhsFuncContext, Symbols.asList(syms, "+", 1, 2, 3, 4));
        assertEquals(1 + 2 + 3 + 4, result.asInteger().getValue());
    }
}
