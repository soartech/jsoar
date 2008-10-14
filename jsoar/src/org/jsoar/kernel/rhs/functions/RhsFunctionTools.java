/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.IntConstant;
import org.jsoar.kernel.symbols.Symbol;

/**
 * @author ray
 */
public final class RhsFunctionTools
{

    private RhsFunctionTools()
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
            if(arg.asIntConstant() == null && arg.asFloatConstant() == null)
            {
                throw new RhsFunctionException("non-number (" + arg + ") passed to '" + name + "' function");
            }
        }
    }
    
    public static double asDouble(Symbol sym)
    {
        IntConstant ic = sym.asIntConstant();
        
        return ic != null ? ic.getValue() : sym.asFloatConstant().getValue();
    }
}
