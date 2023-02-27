/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 29, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class ExecRhsFunctionTest extends JSoarTest
{
    @Test
    void testExecRequiresAtLeastOneArgument()
    {
        final RhsFunctionManager rhsFuncs = new RhsFunctionManager(rhsFuncContext);
        final ExecRhsFunction exec = new ExecRhsFunction(rhsFuncs);
        assertThrows(RhsFunctionException.class, () -> exec.execute(rhsFuncContext, new ArrayList<Symbol>()));
    }
    
    @Test
    void testExecCantCallItself()
    {
        final RhsFunctionManager rhsFuncs = new RhsFunctionManager(rhsFuncContext);
        final ExecRhsFunction exec = new ExecRhsFunction(rhsFuncs);
        assertThrows(RhsFunctionException.class, () -> exec.execute(rhsFuncContext, Symbols.asList(syms, exec.getName())));
    }
    
    @Test
    void testExecCallsNamedFunctionWithRestOfArguments() throws Exception
    {
        final RhsFunctionManager rhsFuncs = new RhsFunctionManager(rhsFuncContext);
        rhsFuncs.registerHandler(new Plus());
        final ExecRhsFunction exec = new ExecRhsFunction(rhsFuncs);
        final Symbol result = exec.execute(rhsFuncContext, Symbols.asList(syms, "+", 1, 2, 3, 4));
        assertEquals(1 + 2 + 3 + 4, result.asInteger().getValue());
    }
}
