/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 20, 2010
 */
package org.jsoar.kernel.rhs.functions;

import android.test.AndroidTestCase;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.Symbols;

public class RhsFunctionsTest extends AndroidTestCase
{
    private SymbolFactory syms;
    
    @Override
    public void setUp()
    {
        syms = new SymbolFactoryImpl();
    }
    
    public void testCheckArgumentCountThrowsExceptionWhenMinConstraintIsViolated() throws Exception
    {
        RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 4, Integer.MAX_VALUE);
    }
    
    public void testCheckArgumentCountThrowsExceptionWhenMaxConstraintIsViolated() throws Exception
    {
        RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 0, 2);
    }
    
    public void testCheckArgumentCountThrowsExceptionWhenExactConstraintIsViolated() throws Exception
    {
        RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 2, 2);
    }
    
    public void testCheckArgumentCountPasses() throws Exception
    {
        // No exception should be thrown
        RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 1, 5);
    }

    public void testCheckAllArgumentsAreNumericThrowsExceptionWhenConstraintViolated() throws Exception
    {
        RhsFunctions.checkAllArgumentsAreNumeric("test", Symbols.asList(syms, 1, 2, 3.14, 6, "nan", 99));
    }
}
