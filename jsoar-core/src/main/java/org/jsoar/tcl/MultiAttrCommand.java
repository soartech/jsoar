/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
final class MultiAttrCommand implements SoarCommand
{
    private final Agent agent;

    /**
     * @param agent
     */
    MultiAttrCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(String[] args) throws SoarException
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