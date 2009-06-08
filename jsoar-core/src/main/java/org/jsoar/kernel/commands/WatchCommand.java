/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
public final class WatchCommand implements SoarCommand
{
    private final Agent agent;

    public WatchCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length != 2)
        {
            // TODO illegal args
            throw new SoarException(String.format("%s <level>", args[0]));
        }
        
        try
        {
            int level = Integer.valueOf(args[1]);
            agent.getTrace().setWatchLevel(level);
            return "";
        }
        catch(NumberFormatException e)
        {
            throw new SoarException(args[1] + " is not a valid number");
        }
        catch(IllegalArgumentException e)
        {
            throw new SoarException(e.getMessage());
        }
    }
}