/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import java.util.Arrays;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
abstract class AbstractToggleCommand implements SoarCommand
{
    private static final List<String> enableOpts = Arrays.asList("--on", "-e", "--enable");
    private static final List<String> disableOpts = Arrays.asList("--off", "-d", "--disable");
    
    private final Agent agent;

    AbstractToggleCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length != 2)
        {
            throw new SoarException("Option must be one of [--on|--off|-e|-d|--enable|--disable]");
        }
        
        final String a = args[1];
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
            throw new SoarException("Option must be --on, --off, -e, -d, --enable, or --disable");
        }
        return "";
    }
    
    protected abstract void execute(Agent agent, boolean enable) throws SoarException;
}