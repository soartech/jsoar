/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import java.util.EnumSet;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.RunType;
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
public class SoarTclInterface
{
    /**
     * @author ray
     */
    private final class MatchesCommand implements Command
    {
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
                throw new TclNumArgsException(interp, 2, args, "[production]");
            }
        }
    }

    final Agent agent;
    private Interp interp = new Interp();
    
    private final SourceCommand sourceCommand;
    private final PushdCommand pushdCommand;
    private final PopdCommand popdCommand;
    
    private Command spCommand = new SpCommand(this);
        
    private Command multiAttrCommand = new MultiAttrCommand(this);
    
    private Command statsCommand = new StatsCommand(this);

    private Command learnCommand = new LearnCommand(this);
    private Command srandCommand = new SrandCommand(this);
        
    private Command maxElaborationsCommand = new MaxElaborationsCommand(this);
        
    private Command matchesCommand = new MatchesCommand();    
        
    private Command waitsncCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 2)
            {
                throw new TclNumArgsException(interp, 2, args, "[--on|--off]");
            }
            
            if("--on".equals(args[1].toString()))
            {
                agent.decider.setWaitsnc(true);
            }
            else if("--off".equals(args[1].toString()))
            {
                agent.decider.setWaitsnc(false);
            }
            else
            {
                throw new TclException(interp, "Option must be --on or --off");
            }
        }};  
            
    private Command initSoarCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 1)
            {
                throw new TclNumArgsException(interp, 1, args, "");
            }
            
            agent.initialize();
        }}; 
        
    /**
     * @param agent
     */
    public SoarTclInterface(Agent agent)
    {
        this.agent = agent;
        
        this.sourceCommand = new SourceCommand();
        interp.createCommand("source", sourceCommand);

        this.pushdCommand = new PushdCommand(sourceCommand);
        interp.createCommand("pushd", pushdCommand);
        this.popdCommand = new PopdCommand(sourceCommand);
        interp.createCommand("popd", this.popdCommand);
        interp.createCommand("pwd", new PwdCommand(sourceCommand));
        
        interp.createCommand("sp", spCommand);
        interp.createCommand("multi-attributes", multiAttrCommand);
        interp.createCommand("stats", statsCommand);
        interp.createCommand("learn", learnCommand);
        interp.createCommand("srand", srandCommand);
        interp.createCommand("max-elaborations", maxElaborationsCommand);
        interp.createCommand("matches", matchesCommand);
        interp.createCommand("waitsnc", waitsncCommand);
        interp.createCommand("init-soar", initSoarCommand);
    }
    
    public Interp getInterpreter()
    {
        return interp;
    }
    
    public void dispose()
    {
        interp.dispose();
    }
    
    public Agent getAgent()
    {
        return agent;
    }
    
    public void sourceFile(String file) throws SoarTclException
    {
        try
        {
            sourceCommand.source(interp, file);
        }
        catch (TclException e)
        {
            throw new SoarTclException(interp);
        }
    }
    
    public void sourceResource(String resource) throws SoarTclException
    {
        try
        {
            interp.evalResource(resource);
        }
        catch (TclException e)
        {
            throw new SoarTclException(interp);
        }
    }
    
    public String eval(String command) throws SoarTclException
    {
        try
        {
            interp.eval(command);
            return interp.getResult().toString();
        }
        catch (TclException e)
        {
            throw new SoarTclException(interp);
        }
    }
    
    public static void main(String[] args) throws SoarTclException
    {
        Agent agent = new Agent();
        SoarTclInterface ifc = new SoarTclInterface(agent);
        agent.initialize();
        
        ifc.sourceFile("single.soar");
        
        agent.trace.setEnabled(false);
        agent.runFor(3000, RunType.DECISIONS);
    }
}
