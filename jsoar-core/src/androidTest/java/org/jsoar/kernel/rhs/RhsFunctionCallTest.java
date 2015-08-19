/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 28, 2009
 */
package org.jsoar.kernel.rhs;


import android.test.AndroidTestCase;

import org.jsoar.kernel.symbols.SymbolFactoryImpl;

/**
 * @author ray
 */
public class RhsFunctionCallTest extends AndroidTestCase
{
    public void testPlusFunctionNameIsPrintedWithNoPipes()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final RhsFunctionCall f = new RhsFunctionCall(syms.createString("+"), false);
        assertEquals("(+)", String.format("%s", f));
    }
    
    public void testMinusFunctionNameIsPrintedWithNoPipes()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final RhsFunctionCall f = new RhsFunctionCall(syms.createString("-"), false);
        assertEquals("(-)", String.format("%s", f));
    }
    
    public void testDivFunctionNameIsPrintedWithNoPipes()
    {
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final RhsFunctionCall f = new RhsFunctionCall(syms.createString("/"), false);
        assertEquals("(/)", String.format("%s", f));
    }
}
