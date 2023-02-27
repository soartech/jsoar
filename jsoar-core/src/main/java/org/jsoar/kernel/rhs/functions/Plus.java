package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * Takes any number of int_constant or float_constant arguments, and
 * returns their sum.
 * 
 * <p>rhsfun_math.cpp:41:plus_rhs_function_code
 */
public final class Plus extends AbstractRhsFunctionHandler
{
    public Plus()
    {
        super("+");
    }
    
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);
        
        long i = 0;
        double f = 0;
        boolean float_found = false;
        for(Symbol arg : arguments)
        {
            IntegerSymbol ic = arg.asInteger();
            if(ic != null)
            {
                if(float_found)
                {
                    f += ic.getValue();
                }
                else
                {
                    i += ic.getValue();
                }
            }
            else
            {
                if(float_found)
                {
                    f += arg.asDouble().getValue();
                }
                else
                {
                    float_found = true;
                    f = i + arg.asDouble().getValue();
                }
            }
        }
        
        final SymbolFactory syms = context.getSymbols();
        return float_found ? syms.createDouble(f) : syms.createInteger(i);
    }
}
