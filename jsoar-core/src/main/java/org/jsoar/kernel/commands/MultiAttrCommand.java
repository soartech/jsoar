/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "multi-attributes" command.
 * 
 * @author ray
 */
public final class MultiAttrCommand implements SoarCommand
{
    private final Agent agent;

    /**
     * @param agent
     */
    public MultiAttrCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length != 3)
        {
            // TODO illegal arguments
            throw new SoarException(String.format("%s attr cost"));
        }
        
        final StringSymbol attr = agent.getSymbols().createString(args[1].toString());
        final int cost = Integer.valueOf(args[2].toString());
        agent.getMultiAttributes().setCost(attr, cost);
        return "";
    }
}