/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.springframework.util.StringUtils;

/**
 * Concatenates arguments into a single string
 * 
 * <p>sml_RhsFunction.cpp:97:sml::ConcatRhsFunction::Execute
 * 
 * @author ray
 */
public class CapitalizeSymbol extends AbstractRhsFunctionHandler
{
    public CapitalizeSymbol()
    {
        super("capitalize-symbol", 1, 1);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        Symbol sym = arguments.get(0);
        StringSymbol strsym = sym.asString();
        if(strsym == null) {
            throw new RhsFunctionException("capitalize-symbol RHS function expects a string argument, got " + sym.toString());
        }
        
        return context.getSymbols().createString(StringUtils.capitalize(strsym.getValue()));
    }
}
