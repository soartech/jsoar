/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
final class SpCommand implements SoarCommand
{
    private final Agent agent;

    SpCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length != 2)
        {
            // TODO illegal argument
            throw new SoarException(String.format("%s body", args[0]));
        }
        
        try
        {
            agent.getProductions().loadProduction(args[1]);
            return "";
        }
        catch (ReordererException e)
        {
            throw new SoarException(e.getMessage());
        }
        catch (ParserException e)
        {
            throw new SoarException(e.getMessage());
        }
    }
}