/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class EchoCommand implements Command
{
    private final Agent agent;

    EchoCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        boolean noNewLine = false;
        for(int i = 1; i < args.length; ++i)
        {
            final String argString = args[i].toString();
            if("--nonewline".equals(argString))
            {
                noNewLine = true;
            }
            else
            {
                agent.getPrinter().print(argString);
            }
        }
        if(!noNewLine)
        {
            agent.getPrinter().print("\n");
        }
        agent.getPrinter().flush();
    }
}