/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.commands;

import java.util.EnumSet;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.collect.Lists;

/**
 * Implementation of the "matches" command.
 * 
 * @author ray
 */
public class MatchesCommand implements SoarCommand
{
    private final Agent agent;
    private final OptionProcessor<Options> options = OptionProcessor.create();
    
    private enum Options
    {
        internal
    }
    
    public MatchesCommand(Agent agent)
    {
        this.agent = agent;
        
        options
        .newOption(Options.internal)
        .done();
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        List<String> nonOpts = options.process(Lists.newArrayList(args));
     
        boolean internal = false;
        if (options.has(Options.internal))
        {
            internal = true;
        }
        
        if(nonOpts.isEmpty())
        {
            agent.printMatchSet(agent.getPrinter(), WmeTraceType.FULL, 
                                EnumSet.of(MatchSetTraceType.MS_ASSERT, MatchSetTraceType.MS_RETRACT));
            agent.getPrinter().flush();
        }
        else if(nonOpts.size() == 1)
        {
            final String prodName = nonOpts.get(0);
            Production p = agent.getProductions().getProduction(prodName);
            if(p == null)
            {
                throw new SoarException("No production '" + prodName + "'");
            }
            if(p.getReteNode() == null)
            {
                throw new SoarException("Production '" + prodName + "' is not in rete");
            }
            p.printPartialMatches(agent.getPrinter(), WmeTraceType.FULL, internal);
        }
        else
        {
            throw new SoarException(String.format("%s [production]", args[0]));
        }
        return "";
    }
}