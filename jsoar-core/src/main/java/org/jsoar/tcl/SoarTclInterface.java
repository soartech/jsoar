/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.CLogCommand;
import org.jsoar.kernel.commands.EchoCommand;
import org.jsoar.kernel.commands.ExciseCommand;
import org.jsoar.kernel.commands.FiringCountsCommand;
import org.jsoar.kernel.commands.HelpCommand;
import org.jsoar.kernel.commands.InitSoarCommand;
import org.jsoar.kernel.commands.LearnCommand;
import org.jsoar.kernel.commands.MatchesCommand;
import org.jsoar.kernel.commands.MaxElaborationsCommand;
import org.jsoar.kernel.commands.MemoriesCommand;
import org.jsoar.kernel.commands.MultiAttrCommand;
import org.jsoar.kernel.commands.OSupportModeCommand;
import org.jsoar.kernel.commands.PreferencesCommand;
import org.jsoar.kernel.commands.PrintCommand;
import org.jsoar.kernel.commands.PropertiesCommand;
import org.jsoar.kernel.commands.QMemoryCommand;
import org.jsoar.kernel.commands.ReinforcementLearningCommand;
import org.jsoar.kernel.commands.RhsFunctionsCommand;
import org.jsoar.kernel.commands.SaveBacktracesCommand;
import org.jsoar.kernel.commands.SetParserCommand;
import org.jsoar.kernel.commands.Soar8Command;
import org.jsoar.kernel.commands.SpCommand;
import org.jsoar.kernel.commands.SrandCommand;
import org.jsoar.kernel.commands.StatsCommand;
import org.jsoar.kernel.commands.SymbolsCommand;
import org.jsoar.kernel.commands.VerboseCommand;
import org.jsoar.kernel.commands.WaitSncCommand;
import org.jsoar.kernel.commands.WarningsCommand;
import org.jsoar.kernel.commands.WatchCommand;
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
    private static final String DEFAULT_TCL_CODE = "/org/jsoar/tcl/jsoar.tcl";

    private static final Log logger = LogFactory.getLog(SoarTclInterface.class);
    
    private final static ConcurrentMap<Agent, SoarTclInterface> interfaces = new MapMaker().weakKeys().makeMap();
    
    /**
     * Find the SoarTclInterface for the given agent.
     * 
     * <p>Note: you almost never want to use this. You'd be much happier
     * with {@link Agent#getInterpreter()}.
     * 
     * @param agent the agent
     * @return the Tcl interface, or {@code} if none is associated with the agent.
     */
    public static SoarTclInterface find(Agent agent)
    {
        synchronized (interfaces)
        {
            return interfaces.get(agent);
        }
    }
    
    /**
     * Find the Tcl interface associated with the given agent, or create
     * a new one if there is none.
     * 
     * <p>Note: you almost never want to use this. You'd be much happier
     * with {@link Agent#getInterpreter()}.
     * 
     * @param agent the agent
     * @return the Tcl interface
     */
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
    
    /**
     * Dispose of a Tcl interface, removing it from its agent.
     * 
     * <p>Note: you almost never want to use this. You'd be much happier
     * with {@link Agent#getInterpreter()}.
     * 
     * @param ifc the interface. If it's {@code null} this method is a no-op.
     */
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
    
    final TclRhsFunction tclRhsFunction = new TclRhsFunction(this);
    
    private SoarTclInterface(Agent agent)
    {
        this.agent = agent;
        
        this.agent.getRhsFunctions().registerHandler(tclRhsFunction);
        
        interp.createCommand("source", this.sourceCommand = new SourceCommand());
        interp.createCommand("pushd", new PushdCommand(sourceCommand));
        interp.createCommand("popd", new PopdCommand(sourceCommand));
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
        addCommand("print", new PrintCommand(this.agent));
        addCommand("o-support-mode", new OSupportModeCommand());
        addCommand("soar8", new Soar8Command());
        addCommand("firing-counts", new FiringCountsCommand(this.agent));
        addCommand("excise", new ExciseCommand(this.agent));
        addCommand("init-soar", new InitSoarCommand(this.agent));
        addCommand("preferences", new PreferencesCommand(this.agent));
        addCommand("memories", new MemoriesCommand(this.agent));
        
        addCommand("set-parser", new SetParserCommand(this.agent));
        addCommand("properties", new PropertiesCommand(this.agent));
        addCommand("symbols", new SymbolsCommand(this.agent));
        
        addCommand("help", new HelpCommand(this));
        
        addCommand("qmemory", new QMemoryCommand(this.agent));
        
        try
        {
            interp.evalResource(DEFAULT_TCL_CODE);
        }
        catch (TclException e)
        {
            final String message = "Failed to load resource " + DEFAULT_TCL_CODE + 
                ". Some commands may not work as expected: " + interp.getResult();
            logger.error(message);
            agent.getPrinter().error(message);
        }
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
    
    public void source(File file) throws SoarException
    {
        try
        {
            sourceCommand.source(interp, file.getPath());
        }
        catch (TclException e)
        {
            throw new SoarTclException(interp);
        }
    }
    
    public void source(URL url) throws SoarException
    {
        try
        {
            sourceCommand.source(interp, url.toExternalForm());
        }
        catch (TclException e)
        {
            throw new SoarTclException(interp);
        }
    }
    
    public String eval(String command) throws SoarException
    {
        try
        {
            interp.eval(command);
            return interp.getResult().toString();
        }
        catch (TclException e)
        {
            throw new SoarException(interp.getResult().toString());
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
