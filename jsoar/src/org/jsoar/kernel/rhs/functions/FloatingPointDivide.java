package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.IntConstant;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * Takes one or more int_constant or float_constant arguments.
 * If 0 arguments, returns NIL (error).
 * If 1 argument (x), returns 1/x.
 * If >=2 arguments (x, y1, ..., yk), returns x / y1 / ... / yk.
 *  
 * rhsfun_math.cpp:125:minus_rhs_function_code
 */
public final class FloatingPointDivide extends AbstractRhsFunctionHandler
{
    /**
     * @param name
     */
    public FloatingPointDivide()
    {
        super("/");
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
            
            double f =  i != null ? i.value : arg.asFloatConstant().value;
            if(f == 0.0)
            {
                throw new RhsFunctionException("Attempt to divide ('/') by zero");
            }
            return syms.make_float_constant(1.0 / f);
        }
        
        double f = RhsFunctionTools.asDouble(arg);
        for(int index = 1; index < arguments.size(); ++index)
        {
            arg = arguments.get(index);
            
            double nextf = RhsFunctionTools.asDouble(arg);
            if(nextf == 0.0)
            {
                throw new RhsFunctionException("Attempt to divide ('/') by zero");
            }
            
            f /= nextf;
        }
        return syms.make_float_constant(f);
    }
}