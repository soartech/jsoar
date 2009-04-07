package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

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
        super("/", 1, Integer.MAX_VALUE);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);
        RhsFunctions.checkArgumentCount(this, arguments);

        final SymbolFactory syms = context.getSymbols();
        Symbol arg = arguments.get(0);
        if(arguments.size() == 1)
        {
            IntegerSymbol i = arg.asInteger();
            
            double f =  i != null ? i.getValue() : arg.asDouble().getValue();
            if(f == 0.0)
            {
                throw new RhsFunctionException("Attempt to divide ('/') by zero");
            }
            return syms.createDouble(1.0 / f);
        }
        
        double f = RhsFunctions.asDouble(arg);
        for(int index = 1; index < arguments.size(); ++index)
        {
            arg = arguments.get(index);
            
            double nextf = RhsFunctions.asDouble(arg);
            if(nextf == 0.0)
            {
                throw new RhsFunctionException("Attempt to divide ('/') by zero");
            }
            
            f /= nextf;
        }
        return syms.createDouble(f);
    }
}