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
 * returns the string length of the output string so that one can get the output
 * to line up nicely. This is useful along with ifeq when the output string
 * varies in length.
 * 
 * <p>rhsfun.cpp:470:strlen_rhs_function_code
 * 
 * @author ray
 */
public class StrLen extends AbstractRhsFunctionHandler
{
    public StrLen()
    {
        super("strlen", 1, 1);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        return context.getSymbols().createInteger(String.format("%#s", arguments.get(0)).length());
    }

}
