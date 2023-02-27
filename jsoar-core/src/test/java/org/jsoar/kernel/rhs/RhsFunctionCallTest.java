/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 28, 2009
 */
package org.jsoar.kernel.rhs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class RhsFunctionCallTest
{
    @Test
    void testPlusFunctionNameIsPrintedWithNoPipes()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final RhsFunctionCall f = new RhsFunctionCall(syms.createString("+"), false);
        assertEquals("(+)", String.format("%s", f));
    }
    
    @Test
    void testMinusFunctionNameIsPrintedWithNoPipes()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final RhsFunctionCall f = new RhsFunctionCall(syms.createString("-"), false);
        assertEquals("(-)", String.format("%s", f));
    }
    
    @Test
    void testDivFunctionNameIsPrintedWithNoPipes()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final RhsFunctionCall f = new RhsFunctionCall(syms.createString("/"), false);
        assertEquals("(/)", String.format("%s", f));
    }
}
