/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * Concatenates arguments into a single string
 * 
 * <p>sml_RhsFunction.cpp:97:sml::ConcatRhsFunction::Execute
 * 
 * @author ray
 */
public class Concat extends AbstractRhsFunctionHandler
{
    /**
     * Concatenate the given list of symbols into a string using default, unescaped
     * string representation of each symbol.
     * 
     * @param arguments List of symbols
     * @return Concatenated symbols as a string
     */
    public static String concat(List<Symbol> arguments)
    {
        StringBuilder result = new StringBuilder();
        for(Symbol s : arguments)
        {
            result.append(String.format("%#s", s));
        }
        return result.toString();
    }
    
    /**
     * @param name
     */
    public Concat()
    {
        super("concat");
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
     */
    @Override
    public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
    {
        return syms.createString(concat(arguments));
    }
}
