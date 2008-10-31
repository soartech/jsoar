package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Takes any number of int_constant or float_constant arguments, and
   returns their product.
 *  
 * rhsfun_math.cpp:82:times_rhs_function_code
 */
public final class Multiply extends AbstractRhsFunctionHandler
{
    /**
     * @param name
     */
    public Multiply()
    {
        super("*");
    }

    @Override
    public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);

        int i = 1;
        double f = 1.0;
        boolean float_found = false;
        for(Symbol arg : arguments)
        {
            IntegerSymbol ic = arg.asInteger();
            if(ic != null)
            {
                if(float_found)
                {
                    f *= ic.getValue();
                }
                else
                {
                    i *= ic.getValue();
                }
            }
            else
            {
                if(float_found)
                {
                    f *= arg.asDouble().getValue();
                }
                else
                {
                    float_found = true;
                    f = i * arg.asDouble().getValue();
                }
            }
        }
        
        return float_found ? syms.createDouble(f) : syms.createInteger(i);
    }
}