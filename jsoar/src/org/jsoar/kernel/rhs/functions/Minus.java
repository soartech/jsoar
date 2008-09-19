package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.IntConstant;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * Takes one or more int_constant or float_constant arguments.
 * If 0 arguments, returns NIL (error).
 * If 1 argument (x), returns -x.
 * If >=2 arguments (x, y1, ..., yk), returns x - y1 - ... - yk.
 *  
 * rhsfun_math.cpp:125:minus_rhs_function_code
 */
public final class Minus extends AbstractRhsFunctionHandler
{
    /**
     * @param name
     */
    public Minus()
    {
        super("-");
    }

    @Override
    public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctionTools.checkAllArgumentsAreNumeric(getName(), arguments);
        RhsFunctionTools.checkArgumentCount(getName(), arguments, 1, Integer.MAX_VALUE);
        
        Symbol arg = arguments.get(0);
        if(arguments.size() == 1)
        {
            IntConstant i = arg.asIntConstant();
            
            return i != null ? syms.make_int_constant(-i.value) : 
                               syms.make_float_constant(-arg.asFloatConstant().value);
        }
        
        int i = 0;
        double f = 0;
        boolean float_found = false;
        IntConstant ic = arg.asIntConstant();
        if(ic != null)
        {
            i = ic.value;
        }
        else
        {
            float_found = true;
            f = arg.asFloatConstant().value;
        }
        for(int index = 1; index < arguments.size(); ++index)
        {
            arg = arguments.get(index);
            
            ic = arg.asIntConstant();
            if(ic != null)
            {
                if(float_found)
                {
                    f -= ic.value;
                }
                else
                {
                    i -= ic.value;
                }
            }
            else
            {
                if(float_found)
                {
                    f -= arg.asFloatConstant().value;
                }
                else
                {
                    float_found = true;
                    f = i - arg.asFloatConstant().value;
                }
            }
        }
        
        return float_found ? syms.make_float_constant(f) : syms.make_int_constant(i);
    }
}