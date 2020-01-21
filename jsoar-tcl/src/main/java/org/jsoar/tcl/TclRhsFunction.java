/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.tcl;

import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.Concat;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.symbols.Symbol;

/**
 * RHS function that takes a list of arguments, concatenates them together and
 * executes them as Tcl in the Tcl interpreter. 
 * 
 * <p>This RHS function is only available when a {@link SoarTclInterface} has
 * been attached to an agent. The command is evaluated in the global scope of
 * the Tcl interpreter.
 * 
 * @author ray
 */
public class TclRhsFunction extends AbstractRhsFunctionHandler
{
    private final SoarTclInterface ifc;
    
    /**
     * @param ifc The Tcl interface that includes the Tcl interpreter we'll be
     *      using
     */
    public TclRhsFunction(SoarTclInterface ifc)
    {
        super("tcl", 1, Integer.MAX_VALUE);
        
        this.ifc = ifc;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        try
        {
            final String exp = Concat.concat(arguments);
            final String result = ifc.eval(exp);
            return context.getSymbols().createString(result);
        }
        catch (SoarException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
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
