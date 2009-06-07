/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
final class SrandCommand implements SoarCommand
{
    private final Agent agent;

    /**
     * @param soarTclInterface
     */
    SrandCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length > 2)
        {
            // TODO illegal args
            throw new SoarException(String.format("%s [seed]", args[0]));
        }

        long seed = 0;
        if(args.length == 1)
        {
            seed = System.nanoTime();
        }
        else
        {
            try
            {
                seed = Long.parseLong(args[1]);
            }
            catch(NumberFormatException e)
            {
                throw new SoarException(String.format("%s is not a valid integer: %s", args[1], e.getMessage()));
            }
        }
        agent.getRandom().setSeed(seed);
        return "";
    }
}