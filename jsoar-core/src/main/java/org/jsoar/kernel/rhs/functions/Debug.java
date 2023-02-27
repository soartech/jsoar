/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Implements the "debug" RHS function. When called, the currently registered
 * ({@link Agent#setDebuggerProvider(org.jsoar.kernel.DebuggerProvider)}
 * debugger will be opened.
 * 
 * @author ray
 */
public class Debug extends StandaloneRhsFunctionHandler
{
    private final Agent agent;
    
    /**
     * @param agent the agent
     */
    public Debug(Agent agent)
    {
        super("debug", 0, 0);
        
        this.agent = agent;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        try
        {
            agent.getDebuggerProvider().openDebugger(agent);
        }
        catch(SoarException e)
        {
            throw new RhsFunctionException("debug RHS function failed to open debugger: " + e.getMessage(), e);
        }
        return null;
    }
    
}
