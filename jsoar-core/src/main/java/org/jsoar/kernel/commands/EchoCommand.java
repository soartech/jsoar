/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "echo" command.
 * 
 * @author ray
 */
public final class EchoCommand implements SoarCommand
{
    private final Agent agent;

    public EchoCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        boolean noNewLine = false;
        agent.getPrinter().startNewLine();
        for(int i = 1; i < args.length; ++i)
        {
            final String argString = args[i].toString();
            if("--nonewline".equals(argString))
            {
                noNewLine = true;
            }
            else
            {
            	if (i != 1)
            		agent.getPrinter().print(" ");
                agent.getPrinter().print(argString);
            }
        }
        if(!noNewLine)
        {
            agent.getPrinter().print("\n");
        }
        agent.getPrinter().flush();
        return "";
    }
}