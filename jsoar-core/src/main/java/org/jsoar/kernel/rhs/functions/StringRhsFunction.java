/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * String RHS function implementation. Converts its single argument to a string.
 * 
 * @author ray
 */
public class StringRhsFunction extends AbstractRhsFunctionHandler
{
    
    public StringRhsFunction()
    {
        super("string", 0, 1);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        final SymbolFactory syms = context.getSymbols();
        if(arguments.size() == 0)
        {
            return syms.createString("");
        }
        else
        {
            final Symbol arg = arguments.get(0);
            
            return arg.asString() != null ? arg : syms.createString(arg.toString());
        }
    }
    
}
