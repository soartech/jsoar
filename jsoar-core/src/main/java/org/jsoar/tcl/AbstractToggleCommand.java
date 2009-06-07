/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import java.util.Arrays;
import java.util.List;

import org.jsoar.kernel.Agent;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
abstract class AbstractToggleCommand implements Command
{
    private static final List<String> enableOpts = Arrays.asList("--on", "-e", "--enable");
    private static final List<String> disableOpts = Arrays.asList("--off", "-d", "--disable");
    
    private final Agent agent;

    AbstractToggleCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length != 2)
        {
            throw new TclNumArgsException(interp, 0, args, "[--on|--off|-e|-d|--enable|--disable]");
        }
        
        final String a = args[1].toString();
        if(enableOpts.contains(a))
        {
            execute(agent, true);
        }
        else if(disableOpts.contains(a))
        {
            execute(agent, false);
        }
        else
        {
            throw new TclException(interp, "Option must be --on, --off, -e, -d, --enable, or --disable");
        }
    }
    
    protected abstract void execute(Agent agent, boolean enable) throws TclException;
}