/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 20, 2010
 */
package org.jsoar.kernel.rhs.functions;

import android.test.AndroidTestCase;

import junit.framework.Assert;

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
        try {
            RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 4, Integer.MAX_VALUE);
            Assert.fail("Should have thrown");
        }catch(RhsFunctionException e){
            Assert.assertEquals("'test' function called with 3 arguments. Expected at least 4.", e.getMessage());
        }
    }
    
    public void testCheckArgumentCountThrowsExceptionWhenMaxConstraintIsViolated() throws Exception
    {
        try {
            RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 0, 2);
            Assert.fail("Should have thrown");
        } catch(RhsFunctionException e){
            Assert.assertEquals("'test' function called with 3 arguments. Expected at most 2.", e.getMessage());
        }
    }
    
    public void testCheckArgumentCountThrowsExceptionWhenExactConstraintIsViolated() throws Exception
    {
        try {
            RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 2, 2);
            Assert.fail("Should have thrown");
        }catch(RhsFunctionException e){
            Assert.assertEquals("'test' function called with 3 arguments. Expected 2.", e.getMessage());
        }
    }
    
    public void testCheckArgumentCountPasses() throws Exception
    {
        // No exception should be thrown
        RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 1, 5);
    }

    public void testCheckAllArgumentsAreNumericThrowsExceptionWhenConstraintViolated() throws Exception
    {
        try {
            RhsFunctions.checkAllArgumentsAreNumeric("test", Symbols.asList(syms, 1, 2, 3.14, 6, "nan", 99));
            Assert.fail("Should have thrown");
        }catch(RhsFunctionException e){
            Assert.assertEquals("non-number (nan) passed to 'test' function", e.getMessage());
        }
    }
}
