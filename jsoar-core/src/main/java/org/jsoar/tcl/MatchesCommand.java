/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.tcl;

import java.util.EnumSet;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
public class MatchesCommand implements Command
{
    private final Agent agent;
    
    /**
     * @param agent
     */
    public MatchesCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
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
                throw new TclException(interp, "No production '" + args[1] + "'");
            }
            if(p.getReteNode() == null)
            {
                throw new TclException(interp, "Production '" + args[1] + "' is not in rete");
            }
            p.printPartialMatches(agent.getPrinter(), WmeTraceType.FULL);
        }
        else
        {
            throw new TclNumArgsException(interp, 0, args, "[production]");
        }
    }
}