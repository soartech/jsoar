/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;


/**
 * @author ray
 */
public class MakeConstantSymbolTest extends JSoarTest
{
    @Test
    public void testNoArgsCall() throws Exception
    {
        MakeConstantSymbol mcs = new MakeConstantSymbol();
        StringSymbol result = (StringSymbol) mcs.execute(rhsFuncContext, new ArrayList<Symbol>());
        assertNotNull(result);
        assertEquals("constant", result.getValue());
        result = (StringSymbol) mcs.execute(rhsFuncContext, new ArrayList<Symbol>());
        assertNotNull(result);
        assertEquals("constant1", result.getValue());
        result = (StringSymbol) mcs.execute(rhsFuncContext, new ArrayList<Symbol>());
        assertNotNull(result);
        assertEquals("constant2", result.getValue());
    }
    
    @Test
    public void testWithArgsCall() throws Exception
    {
        List<Symbol> args = Symbols.asList(syms, "s", "1", "hello-", "goodbye-");
        MakeConstantSymbol mcs = new MakeConstantSymbol();
        final StringSymbol result1 = (StringSymbol) mcs.execute(rhsFuncContext, args);
        assertEquals("s1hello-goodbye-", result1.getValue());
        final StringSymbol result2 = (StringSymbol) mcs.execute(rhsFuncContext, args);
        assertEquals("s1hello-goodbye-1", result2.getValue());
        final StringSymbol result3 = (StringSymbol) mcs.execute(rhsFuncContext, args);
        assertEquals("s1hello-goodbye-2", result3.getValue());
        
    }
}
