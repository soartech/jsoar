/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.commands;

import java.util.EnumSet;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "matches" command.
 * 
 * @author ray
 */
public class MatchesCommand implements SoarCommand
{
    private final Agent agent;
    
    public MatchesCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length == 1)
        {
            agent.printMatchSet(agent.getPrinter(), WmeTraceType.FULL, 
                                EnumSet.of(MatchSetTraceType.MS_ASSERT, MatchSetTraceType.MS_RETRACT));
            agent.getPrinter().flush();
        }
        else if(args.length == 2)
        {
            Production p = agent.getProductions().getProduction(args[1].toString());
            if(p == null)
            {
                throw new SoarException("No production '" + args[1] + "'");
            }
            if(p.getReteNode() == null)
            {
                throw new SoarException("Production '" + args[1] + "' is not in rete");
            }
            p.printPartialMatches(agent.getPrinter(), WmeTraceType.FULL);
        }
        else
        {
            throw new SoarException(String.format("%s [production]", args[0]));
        }
        return "";
    }
}