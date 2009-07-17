/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.tcl.SourceCommand;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
public final class SpCommand implements SoarCommand
{
    private final Agent agent;
    private final SourceCommand sourceCommand;

    public SpCommand(Agent agent, SourceCommand sourceCommand)
    {
        this.agent = agent;
        this.sourceCommand = sourceCommand;
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
            final SourceLocation location = new DefaultSourceLocation(sourceCommand.getCurrentFile(), -1, -1);
            agent.getProductions().loadProduction(args[1], location);
            agent.getPrinter().print("*");
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