/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 20, 2010
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RhsFunctionsTest
{
    private SymbolFactory syms;
    
    @BeforeEach
    public void setUp()
    {
        syms = new SymbolFactoryImpl();
    }
    
    @Test
    public void testCheckArgumentCountThrowsExceptionWhenMinConstraintIsViolated()
    {
        assertThrows(RhsFunctionException.class, () -> RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 4, Integer.MAX_VALUE));
    }
    
    @Test
    public void testCheckArgumentCountThrowsExceptionWhenMaxConstraintIsViolated()
    {
        assertThrows(RhsFunctionException.class, () -> RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 0, 2));
    }
    
    @Test
    public void testCheckArgumentCountThrowsExceptionWhenExactConstraintIsViolated()
    {
        assertThrows(RhsFunctionException.class, () -> RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 2, 2));
    }
    
    @Test
    public void testCheckArgumentCountPasses() throws Exception
    {
        // No exception should be thrown
        RhsFunctions.checkArgumentCount("test", Symbols.asList(syms, "a", "b", "c"), 1, 5);
    }
    
    @Test
    public void testCheckAllArgumentsAreNumericThrowsExceptionWhenConstraintViolated()
    {
        assertThrows(RhsFunctionException.class, () -> RhsFunctions.checkAllArgumentsAreNumeric("test", Symbols.asList(syms, 1, 2, 3.14, 6, "nan", 99)));
    }
}
