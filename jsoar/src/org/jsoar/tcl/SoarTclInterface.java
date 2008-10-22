/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import java.io.IOException;
import java.util.Calendar;
import java.util.EnumSet;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.tracing.Printer;
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
    private final Agent agent;
    private Interp interp = new Interp();
    
    private Command spCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 2)
            {
                throw new TclNumArgsException(interp, 1, args, "body");
            }
            
            try
            {
                agent.loadProduction(args[1].toString());
            }
            catch (IOException e)
            {
                throw new TclException(interp, e.getMessage());
            }
            catch (ReordererException e)
            {
                throw new TclException(interp, e.getMessage());
            }
            catch (ParserException e)
            {
                throw new TclException(interp, e.getMessage());
            }
        }};
        
    private Command multiAttrCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 3)
            {
                throw new TclNumArgsException(interp, 2, args, "attr cost");
            }
            
            StringSymbolImpl attr = agent.syms.createString(args[1].toString());
            int cost = Integer.valueOf(args[2].toString());
            agent.getMultiAttributes().setCost(attr, cost);
        }
        
    };
    
    private Command statsCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            final Printer p = agent.getPrinter();
            
            p.startNewLine();
            
            p.print("jsoar 0.0.0 on %s at %s%n%n", System.getenv("HOSTNAME"), Calendar.getInstance().getTime());
            p.print("%d productions (%d default, %d user, %d chunks)%n   + %d justifications%n",
                    agent.getProductions(null).size(),
                    agent.getProductions(ProductionType.DEFAULT_PRODUCTION_TYPE).size(),
                    agent.getProductions(ProductionType.USER_PRODUCTION_TYPE).size(),
                    agent.getProductions(ProductionType.CHUNK_PRODUCTION_TYPE).size(),
                    agent.getProductions(ProductionType.CHUNK_PRODUCTION_TYPE).size());
            p.print("\n");
            p.print("Values from single timers:%n" +
            		" Kernel CPU Time: %f sec. %n" +
            		" Total  CPU Time: %f sec. %n%n",
            		agent.getTotalKernelTimer().getTotalSeconds(),
            		agent.getTotalCpuTimer().getTotalSeconds());
            
            p.print("%d decisions%n" +
            		"%d elaboration cycles%n" +
            		"%d p-elaboration cycles",
            		agent.decisionCycle.decision_phases_count,
            		agent.decisionCycle.e_cycle_count,
            		agent.decisionCycle.pe_cycle_count);
        }};

    private Command learnCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 2)
            {
                throw new TclNumArgsException(interp, 2, args, "[--on|--off]");
            }
            
            if("--on".equals(args[1].toString()))
            {
                agent.chunker.setLearningOn(true);
            }
            else if("--off".equals(args[1].toString()))
            {
                agent.chunker.setLearningOn(false);
            }
            else
            {
                throw new TclException(interp, "Option must be --on or --off");
            }
        }};
    private Command srandCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length > 2)
            {
                throw new TclNumArgsException(interp, 2, args, "[seed]");
            }

            long seed = 0;
            if(args.length == 1)
            {
                seed = System.nanoTime();
            }
            else
            {
                seed = Long.parseLong(args[1].toString());
            }
            agent.getRandom().setSeed(seed);
        }};
        
    private Command maxElaborationsCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length > 2)
            {
                throw new TclNumArgsException(interp, 2, args, "[value]");
            }

            if(args.length == 1)
            {
                agent.getPrinter().print("%d", agent.consistency.getMaxElaborations());
            }
            else
            {
                agent.consistency.setMaxElaborations(Integer.parseInt(args[1].toString()));
            }
        }};
        
    private Command matchesCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length == 1)
            {
                agent.soarReteListener.print_match_set(agent.getPrinter(), 
                                                       WmeTraceType.FULL_WME_TRACE, 
                                                       EnumSet.of(MatchSetTraceType.MS_ASSERT, MatchSetTraceType.MS_RETRACT));
                agent.getPrinter().flush();
            }
            else if(args.length == 2)
            {
                Production p = agent.getProduction(args[1].toString());
                if(p == null)
                {
                    throw new TclException(interp, "No production '" + args[1] + "'");
                }
                if(p.p_node == null)
                {
                    throw new TclException(interp, "Production '" + args[1] + "' is not in rete");
                }
                agent.rete.print_partial_match_information(agent.getPrinter(), p.p_node, WmeTraceType.FULL_WME_TRACE);
            }
            else
            {
                throw new TclNumArgsException(interp, 2, args, "[production]");
            }
        }};    
        
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
            interp.evalFile(file);
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
