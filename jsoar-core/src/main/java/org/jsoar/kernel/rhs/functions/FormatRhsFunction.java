/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 28, 2010
 */
package org.jsoar.kernel.rhs.functions;

import java.util.IllegalFormatConversionException;
import java.util.List;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;

/**
 * RHS function that formats text like {@code sprintf} or {@link String#format(String, Object...)}.
 * 
 * @author ray
 */
public class FormatRhsFunction extends AbstractRhsFunctionHandler
{
    /**
     * @param name
     */
    public FormatRhsFunction()
    {
        super("format", 1, Integer.MAX_VALUE);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        final String format = arguments.get(0).toString();
        final Object[] derefed = new Object[arguments.size() - 1];
        for(int i = 0; i < derefed.length; i++)
        {
            derefed[i] = Symbols.valueOf(arguments.get(i + 1));
        }
        try
        {
            return context.getSymbols().createString(String.format(format, derefed));
        }
        catch(IllegalFormatConversionException e)
        {
            final Production p = context.getProductionBeingFired();
            throw new RhsFunctionException("Invalid format '" + format + "' in rule '" + (p != null ? p.getName() : "unknown") + "': " + e.getMessage(), e);
        }
    }

}
