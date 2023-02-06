/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class CapitalizeSymbolTest extends JSoarTest
{

    @Test
    public void testExecute() throws Exception
    {
        CapitalizeSymbol capitalizeSymbol = new CapitalizeSymbol();
        
        assertEquals("Foo bar", capitalizeSymbol.execute(rhsFuncContext, Symbols.asList(syms, "foo bar")).asString().getValue());
        
        assertEquals("Foo bar", capitalizeSymbol.execute(rhsFuncContext, Symbols.asList(syms, "Foo bar")).asString().getValue());
    }

}
