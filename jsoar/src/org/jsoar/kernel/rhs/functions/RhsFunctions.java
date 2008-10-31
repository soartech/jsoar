/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Various utility methods for working with RHS functions
 * 
 * @author ray
 */
public final class RhsFunctions
{

    private RhsFunctions()
    {
    }

    public static void checkArgumentCount(String name, List<Symbol> arguments, int min, int max) throws RhsFunctionException
    {
        int count = arguments.size();
        
        if(min == max && count != min)
        {
            throw new RhsFunctionException("'" + name + "' function called with " + count + " arguments. Expected " + min + ".");
        }
        if(count < min)
        {
            throw new RhsFunctionException("'" + name + "' function called with " + count + " arguments. Expected at least " + min + ".");
        }
        if(count > max)
        {
            throw new RhsFunctionException("'" + name + "' function called with " + count + " arguments. Expected at most " + max + ".");
        }
    }
    
    /**
     * @param arguments
     * @throws RhsFunctionException
     */
    public static void checkAllArgumentsAreNumeric(String name, List<Symbol> arguments) throws RhsFunctionException
    {
        for (Symbol arg : arguments) 
        {
            if(arg.asInteger() == null && arg.asDouble() == null)
            {
                throw new RhsFunctionException("non-number (" + arg + ") passed to '" + name + "' function");
            }
        }
    }
    
    public static double asDouble(Symbol sym)
    {
        IntegerSymbol ic = sym.asInteger();
        
        return ic != null ? ic.getValue() : sym.asDouble().getValue();
    }
}
