/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import java.util.Arrays;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * @author ray
 */
public final class SpCommand implements SoarCommand
{
    private final Agent agent;

    public SpCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length != 2)
        {
            // TODO illegal argument
            throw new SoarException(String.format("%s: Expected %s body, got %s", 
                                        commandContext.getSourceLocation(), 
                                        args[0], 
                                        Arrays.asList(args)));
        }
        
        try
        {
            agent.getProductions().loadProduction(args[1], commandContext.getSourceLocation());
            agent.getPrinter().print("*");
            return "";
        }
        catch (ReordererException e)
        {
            throw new SoarException(commandContext.getSourceLocation() + ":" + e.getMessage());
        }
        catch (ParserException e)
        {
            throw new SoarException(commandContext.getSourceLocation() + ":" + e.getMessage());
        }
    }
}