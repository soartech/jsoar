/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 28, 2009
 */
package org.jsoar.kernel.rhs;


import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author ray
 */
public class RhsFunctionCallTest
{
    @Test public void testPlusFunctionNameIsPrintedWithNoPipes()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final RhsFunctionCall f = new RhsFunctionCall(syms.createString("+"), false);
        assertEquals("(+)", String.format("%s", f));
    }
    
    @Test public void testMinusFunctionNameIsPrintedWithNoPipes()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final RhsFunctionCall f = new RhsFunctionCall(syms.createString("-"), false);
        assertEquals("(-)", String.format("%s", f));
    }
}
