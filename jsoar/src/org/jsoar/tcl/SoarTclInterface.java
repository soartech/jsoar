/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.tracing.Trace.Category;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;

/**
 * @author ray
 */
public class SoarTclInterface
{
    private final static ReferenceMap<Agent, SoarTclInterface> interfaces = 
        new ReferenceMap<Agent, SoarTclInterface>(ReferenceType.WEAK, ReferenceType.STRONG);
    
    public static SoarTclInterface find(Agent agent)
    {
        synchronized (interfaces)
        {
            return interfaces.get(agent);
        }
    }
    
    public static SoarTclInterface findOrCreate(Agent agent)
    {
        synchronized (interfaces)
        {
            SoarTclInterface ifc = interfaces.get(agent);
            if(ifc == null)
            {
                ifc = new SoarTclInterface(agent);
                interfaces.put(agent, ifc);
            }
            return ifc;
        }
    }
    
    public static void dispose(SoarTclInterface ifc)
    {
        synchronized(interfaces)
        {
            interfaces.remove(ifc.agent);
            ifc.dispose();
        }
    }
    
    
    private Agent agent;
    private final Interp interp = new Interp();
    
    private final SourceCommand sourceCommand;
    private final PushdCommand pushdCommand;
    private final PopdCommand popdCommand;
    private final CLogCommand clogCommand = new CLogCommand(this);
    
    private final Command spCommand = new SpCommand(this);
        
    private final Command multiAttrCommand = new MultiAttrCommand(this);
    
    private final Command statsCommand = new StatsCommand(this);

    private final Command learnCommand = new AbstractToggleCommand(this) {
        @Override
        protected void execute(Agent agent, boolean enable) throws TclException
        {
            agent.getProperties().set(SoarProperties.LEARNING_ON, enable);
        }
    }; 
    
    private final Command rlCommand = new ReinforcementLearningCommand(this);
    
    private final Command srandCommand = new SrandCommand(this);
        
    private final Command maxElaborationsCommand = new MaxElaborationsCommand(this);
        
    private final Command matchesCommand = new MatchesCommand(this);    
        
    private Command waitsncCommand = new AbstractToggleCommand(this) {
        @Override
        protected void execute(Agent agent, boolean enable) throws TclException
        {
            agent.getProperties().set(SoarProperties.WAITSNC, enable);
        }
    };
    
    private Command warningsCommand = new AbstractToggleCommand(this) {
        @Override
        protected void execute(Agent agent, boolean enable) throws TclException
        {
            agent.getPrinter().setPrintWarnings(enable);
        }
    }; 
    
    private Command verboseCommand = new AbstractToggleCommand(this) {
        @Override
        protected void execute(Agent agent, boolean enable) throws TclException
        {
            agent.getTrace().setEnabled(Category.VERBOSE, enable);
        }
    }; 
    
    private Command saveBacktracesCommand = new AbstractToggleCommand(this) {
        @Override
        protected void execute(Agent agent, boolean enable) throws TclException
        {
            agent.getProperties().set(SoarProperties.EXPLAIN, enable);
        }
    };  
            
    private Command initSoarCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            if(args.length != 1)
            {
                throw new TclNumArgsException(interp, 0, args, "");
            }
            
            agent.initialize();
        }}; 
        
    private Command echoCommand = new Command() {

        @Override
        public void cmdProc(Interp interp, TclObject[] args) throws TclException
        {
            boolean noNewLine = false;
            for(int i = 1; i < args.length; ++i)
            {
                final String argString = args[i].toString();
                if("--nonewline".equals(argString))
                {
                    noNewLine = true;
                }
                else
                {
                    agent.getPrinter().print(argString);
                }
            }
            if(!noNewLine)
            {
                agent.getPrinter().print("\n");
            }
            agent.getPrinter().flush();
        }}; 
        
    private final WatchCommand watchCommand = new WatchCommand(this);
    
    private final PrintCommand printCommand = new PrintCommand(this);
        
    private final RhsFunctionsCommand rhsFuncsCommand = new RhsFunctionsCommand(this);
    
    private final TclRhsFunction tclRhsFunction = new TclRhsFunction(this);
    
    private SoarTclInterface(Agent agent)
    {
        this.agent = agent;
        
        this.agent.getRhsFunctions().registerHandler(tclRhsFunction);
        
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
        interp.createCommand("rl", rlCommand);
        interp.createCommand("srand", srandCommand);
        interp.createCommand("max-elaborations", maxElaborationsCommand);
        interp.createCommand("matches", matchesCommand);
        interp.createCommand("waitsnc", waitsncCommand);
        interp.createCommand("init-soar", initSoarCommand);
        interp.createCommand("warnings", warningsCommand);
        interp.createCommand("verbose", verboseCommand);
        interp.createCommand("save-backtraces", saveBacktracesCommand);
        interp.createCommand("echo", echoCommand);
        interp.createCommand("clog", clogCommand);
        interp.createCommand("watch", watchCommand);
        interp.createCommand("rhs-functions", rhsFuncsCommand);
        interp.createCommand("print", printCommand);
        interp.createCommand("p", printCommand); // TODO do aliases
    }
    
    public Interp getInterpreter()
    {
        return interp;
    }
    
    private void dispose()
    {
        interp.dispose();
        agent.getRhsFunctions().unregisterHandler(tclRhsFunction.getName());
        agent = null;
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
}
