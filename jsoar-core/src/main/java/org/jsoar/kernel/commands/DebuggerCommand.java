/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on June 9, 2010
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * A command that opens the agent's debugger
 * 
 * @author ray
 */
public class DebuggerCommand implements SoarCommand
{
    private final Agent agent;
    
    public DebuggerCommand(Agent agent)
    {
        this.agent = agent;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        this.agent.openDebugger();
        return "";
    }

}
