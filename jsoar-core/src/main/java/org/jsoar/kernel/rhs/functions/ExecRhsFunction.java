/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 29, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.symbols.Symbol;

/**
 * Implementation of 'exec' RHS function from SML. This function simply 
 * assumes that its first argument is the name of a RHS function. It
 * routes the function call back to that function and returns the result.
 * This function is intended mostly for backward compatibility with
 * existing SML-based Soar code, but could be used for some clever
 * indirection I suppose, i.e. choose RHS function name from working
 * memory.
 * 
 * @author ray
 */
public class ExecRhsFunction extends AbstractRhsFunctionHandler
{
    private final RhsFunctionManager rhsFunctions;
    
    public ExecRhsFunction(RhsFunctionManager rhsFunctions)
    {
        super("exec", 1, Integer.MAX_VALUE);
        
        this.rhsFunctions = rhsFunctions;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        final String functionName = arguments.get(0).toString();
        if(functionName.equals(getName()))
        {
            throw new RhsFunctionException("'exec' RHS function calling itself");
        }
        return rhsFunctions.execute(functionName, arguments.subList(1, arguments.size()));
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeStandalone()
     */
    @Override
    public boolean mayBeStandalone()
    {
        return true;
    }
}
