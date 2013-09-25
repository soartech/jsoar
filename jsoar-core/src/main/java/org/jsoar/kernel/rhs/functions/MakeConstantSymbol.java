/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.ByRef;

/**
 * Returns a newly generated sym_constant. If no arguments are given, the
 * constant will start with "constant". If one or more arguments are given, the
 * constant will start with a string equal to the concatenation of those
 * arguments.
 * 
 * <p>
 * rhsfun.cpp:255:make_constant_symbol_rhs_function_code
 * 
 * @author ray
 */
public class MakeConstantSymbol extends AbstractRhsFunctionHandler
{
    /**
     * agent.h:654:mcs_counter
     */
    private final ByRef<Integer> counter = new ByRef<Integer>(Integer.valueOf(1));
    
    public MakeConstantSymbol()
    {
        super("make-constant-symbol");
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        final SymbolFactory syms = context.getSymbols();
        final String prefix = arguments.isEmpty() ? "constant" : Concat.concat(arguments);
        
        final StringSymbol result = syms.findString(prefix);
        if(result == null)
        {
            return syms.createString(prefix);
        }
        return syms.generateUniqueString(prefix, counter);
    }

}
