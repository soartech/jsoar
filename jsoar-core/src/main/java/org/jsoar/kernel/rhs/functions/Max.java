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
 * Implementation of (max) RHS function. This is a JSoar-specific function.
 * 
 * <p>This function takes one or more numeric arguments and returns the maximum 
 * value. In the case of mixed int/double arguments, the type of the maximum
 * argument is preserved.
 * 
 * <p>For example:
 * <pre>{@code
 * (max 2 1 4.0 5)   returns integer 5
 * (max 2 1 4.0 5.0) returns double 5.0
 * }</pre>
 * 
 * @author ray
 * @see Max
 */
public class Max extends AbstractRhsFunctionHandler
{
    public Max()
    {
        super("max", 1, Integer.MAX_VALUE);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);
        
        long maxInt = Long.MIN_VALUE;
        double maxDouble = Double.MIN_VALUE;
        boolean useDouble = false;
        
        for(Symbol s : arguments)
        {
            final IntegerSymbol i = s.asInteger();
            if(i != null)
            {
                if(useDouble)
                {
                    if(i.getValue() > maxDouble)
                    {
                        maxInt = i.getValue();
                        useDouble = false;
                    }
                }
                else
                {
                    maxInt = Math.max(maxInt, i.getValue());
                }
            }
            else
            {
                final DoubleSymbol d = s.asDouble();
                if(useDouble)
                {
                    maxDouble = Math.max(maxDouble, d.getValue());
                }
                else
                {
                    if(d.getValue() > maxInt)
                    {
                        maxDouble = d.getValue();
                        useDouble = true;
                    }
                }
            }
        }
        return useDouble ? context.getSymbols().createDouble(maxDouble) : context.getSymbols().createInteger(maxInt);
    }

}
