package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Takes any number of int_constant or float_constant arguments, and
 * returns their sum.
 *  
 * rhsfun_math.cpp:41:plus_rhs_function_code
 */
public final class Plus extends AbstractRhsFunctionHandler
{
    /**
     * @param name
     */
    public Plus()
    {
        super("+");
    }

    @Override
    public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctionTools.checkAllArgumentsAreNumeric(getName(), arguments);

        int i = 0;
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
        
        return float_found ? syms.createDouble(f) : syms.createInteger(i);
    }
}