/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 28, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Implementation of (min) RHS function. This is a JSoar-specific function.
 * 
 * <p>This function takes one or more numeric arguments and returns the minimum 
 * value. In the case of mixed int/double arguments, the type of the minimum
 * argument is preserved.
 * 
 * <p>For example:
 * <pre>{@code
 * (min 2 1 4.0 5)   returns integer 1
 * (min 2 1 -4.0 5.0) returns double -4.0
 * }</pre>
 * 
 * @author ray
 * @see Max
 */
public class Min extends AbstractRhsFunctionHandler
{
    public Min()
    {
        super("min", 1, Integer.MAX_VALUE);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);
        
        int minInt = Integer.MAX_VALUE;
        double minDouble = Double.MAX_VALUE;
        boolean useDouble = false;
        
        for(Symbol s : arguments)
        {
            final IntegerSymbol i = s.asInteger();
            if(i != null)
            {
                if(useDouble)
                {
                    if(i.getValue() < minDouble)
                    {
                        minInt = i.getValue();
                        useDouble = false;
                    }
                }
                else
                {
                    minInt = Math.min(minInt, i.getValue());
                }
            }
            else
            {
                final DoubleSymbol d = s.asDouble();
                if(useDouble)
                {
                    minDouble = Math.min(minDouble, d.getValue());
                }
                else
                {
                    if(d.getValue() < minInt)
                    {
                        minDouble = d.getValue();
                        useDouble = true;
                    }
                }
            }
        }
        return useDouble ? context.getSymbols().createDouble(minDouble) : context.getSymbols().createInteger(minInt);
    }

}
