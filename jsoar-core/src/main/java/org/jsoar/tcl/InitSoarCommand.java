/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class InitSoarCommand implements Command
{
    private final Agent agent;

    InitSoarCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length != 1)
        {
            throw new TclNumArgsException(interp, 0, args, "");
        }
        agent.initialize();
        agent.getPrinter().startNewLine().print("Agent reinitialized\n").flush();
    }
}