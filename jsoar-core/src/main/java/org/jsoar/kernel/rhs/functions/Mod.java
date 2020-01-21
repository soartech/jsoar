/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * @author ray
 */
public class Mod extends AbstractRhsFunctionHandler
{
    public Mod()
    {
        super("mod", 2, 2);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        final IntegerSymbol a = arguments.get(0).asInteger();
        if(a == null)
        {
            throw new RhsFunctionException(String.format("Non-integer (%d) passed to '%s' function", arguments.get(0), getName()));
        }
        final IntegerSymbol b = arguments.get(1).asInteger(); 
        if(b == null)
        {
            throw new RhsFunctionException(String.format("Non-integer (%d) passed to '%s' function", arguments.get(1), getName()));
        }
        
        if(b.getValue() == 0)
        {
            throw new RhsFunctionException("Attempt to divide (mod) by zero");
        }
        
        return context.getSymbols().createInteger(a.getValue() % b.getValue());
    }

}
