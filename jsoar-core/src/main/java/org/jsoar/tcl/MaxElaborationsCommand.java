/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class MaxElaborationsCommand implements Command
{
    /**
     * 
     */
    private final Agent agent;

    MaxElaborationsCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length > 2)
        {
            throw new TclNumArgsException(interp, 0, args, "[value]");
        }

        if(args.length == 1)
        {
            agent.getPrinter().print("\n%d", agent.getProperties().get(SoarProperties.MAX_ELABORATIONS));
        }
        else
        {
            try
            {
                final int value = Integer.parseInt(args[1].toString());
                agent.getProperties().set(SoarProperties.MAX_ELABORATIONS, value);
            }
            catch(NumberFormatException e)
            {
                throw new TclException(interp, "'" + args[1] + "' is not a valid integer");
            }
            catch(IllegalArgumentException e)
            {
                throw new TclException(interp, e.getMessage());
            }
        }
    }
}