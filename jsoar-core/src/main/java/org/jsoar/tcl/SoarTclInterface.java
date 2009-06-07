/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import java.util.concurrent.ConcurrentMap;

import org.jsoar.kernel.Agent;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;

import com.google.common.collect.MapMaker;

/**
 * @author ray
 */
public class SoarTclInterface implements SoarCommandInterpreter
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
        
        addCommand("sp", new SpCommand(this.agent));
        addCommand("multi-attributes", new MultiAttrCommand(this.agent));
        addCommand("stats", new StatsCommand(this.agent));
        addCommand("learn", new LearnCommand(this.agent));
        addCommand("rl", new ReinforcementLearningCommand(this.agent));
        addCommand("srand", new SrandCommand(this.agent));
        addCommand("max-elaborations", new MaxElaborationsCommand(this.agent));
        addCommand("matches", new MatchesCommand(this.agent));
        addCommand("waitsnc", new WaitSncCommand(this.agent));
        addCommand("init-soar", new InitSoarCommand(this.agent));
        addCommand("warnings", new WarningsCommand(this.agent));
        addCommand("verbose", new VerboseCommand(this.agent));
        addCommand("save-backtraces", new SaveBacktracesCommand(this.agent));
        addCommand("echo", new EchoCommand(this.agent));
        addCommand("clog", new CLogCommand(this.agent));
        addCommand("watch", new WatchCommand(this.agent));
        addCommand("rhs-functions", new RhsFunctionsCommand(this.agent));
        final PrintCommand print = new PrintCommand(this.agent);
        addCommand("print", print);
        addCommand("p", print); // TODO do aliases
        addCommand("o-support-mode", new OSupportModeCommand());
        addCommand("soar8", new Soar8Command());
        final FiringCountsCommand fc = new FiringCountsCommand(this.agent);
        addCommand("firing-counts", fc);
        addCommand("fc", fc); // TODO do aliases
        final ExciseCommand excise = new ExciseCommand(this.agent);
        addCommand("excise", excise);
        addCommand("ex", excise); // TODO do aliases
        addCommand("init-soar", new InitSoarCommand(this.agent));
        
        addCommand("set-parser", new SetParserCommand(this.agent));
        addCommand("properties", new PropertiesCommand(this.agent));
    }
    
    private Command adapt(SoarCommand c)
    {
        return new SoarTclCommandAdapter(c);
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

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#addCommand(java.lang.String, org.jsoar.util.commands.SoarCommand)
     */
    @Override
    public void addCommand(String name, SoarCommand handler)
    {
        interp.createCommand(name, adapt(handler));
    }
    
    
}
