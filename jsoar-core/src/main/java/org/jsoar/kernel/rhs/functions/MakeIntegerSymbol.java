/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import java.util.UUID;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * Create a unique IntegerSymbol. This is done using {@link UUID#getMostSignificantBits()}.
 * 
 * @author chris.kawatsu
 *
 */
public class MakeIntegerSymbol extends AbstractRhsFunctionHandler
{
    public MakeIntegerSymbol()
    {
        super("make-integer-symbol");
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        final SymbolFactory syms = context.getSymbols();
        final long value = UUID.randomUUID().getMostSignificantBits();
        return syms.createInteger(value);
    }

}
