/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import java.util.concurrent.ConcurrentMap;

import org.jsoar.kernel.Agent;

import tcl.lang.Interp;
import tcl.lang.TclException;

import com.google.common.collect.MapMaker;

/**
 * @author ray
 */
public class SoarTclInterface
{
    private final static ConcurrentMap<Agent, SoarTclInterface> interfaces = new MapMaker().weakKeys().makeMap();
    
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
        if(ifc != null)
        {
            ifc.dispose();
        }
    }
    
    
    private Agent agent;
    private final Interp interp = new Interp();
    
    private final SourceCommand sourceCommand;
    private final PushdCommand pushdCommand;
    private final PopdCommand popdCommand;
    
    final TclRhsFunction tclRhsFunction = new TclRhsFunction(this);
    
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
        
        interp.createCommand("sp", new SpCommand(this.agent));
        interp.createCommand("multi-attributes", new MultiAttrCommand(this.agent));
        interp.createCommand("stats", new StatsCommand(this.agent));
        interp.createCommand("learn", new LearnCommand(this.agent));
        interp.createCommand("rl", new ReinforcementLearningCommand(this.agent));
        interp.createCommand("srand", new SrandCommand(this.agent));
        interp.createCommand("max-elaborations", new MaxElaborationsCommand(this.agent));
        interp.createCommand("matches", new MatchesCommand(this.agent));
        interp.createCommand("waitsnc", new WaitSncCommand(this.agent));
        interp.createCommand("init-soar", new InitSoarCommand(this.agent));
        interp.createCommand("warnings", new WarningsCommand(this.agent));
        interp.createCommand("verbose", new VerboseCommand(this.agent));
        interp.createCommand("save-backtraces", new SaveBacktracesCommand(this.agent));
        interp.createCommand("echo", new EchoCommand(this.agent));
        interp.createCommand("clog", new CLogCommand(this.agent));
        interp.createCommand("watch", new WatchCommand(this.agent));
        interp.createCommand("rhs-functions", new RhsFunctionsCommand(this.agent));
        interp.createCommand("print", new PrintCommand(this.agent));
        interp.createCommand("p", new PrintCommand(this.agent)); // TODO do aliases
        interp.createCommand("o-support-mode", new OSupportModeCommand());
        interp.createCommand("soar8", new Soar8Command());
        interp.createCommand("firing-counts", new FiringCountsCommand(this.agent));
        interp.createCommand("fc", interp.getCommand("firing-counts")); // TODO do aliases
        interp.createCommand("excise", new ExciseCommand(this.agent));
        interp.createCommand("ex", interp.getCommand("excise")); // TODO do aliases
        interp.createCommand("init-soar", new InitSoarCommand(this.agent));
        
        interp.createCommand("set-parser", new SetParserCommand(this.agent));
        interp.createCommand("properties", new PropertiesCommand(this.agent));
    }
    
    public Interp getInterpreter()
    {
        return interp;
    }
    
    public void dispose()
    {
        synchronized(interfaces)
        {
            interfaces.remove(agent);
            interp.dispose();
            agent.getRhsFunctions().unregisterHandler(tclRhsFunction.getName());
            agent = null;
        }
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
