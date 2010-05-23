/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.CLogCommand;
import org.jsoar.kernel.commands.DefaultWmeDepthCommand;
import org.jsoar.kernel.commands.EchoCommand;
import org.jsoar.kernel.commands.EditProductionCommand;
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
import org.jsoar.kernel.commands.PopdCommand;
import org.jsoar.kernel.commands.PreferencesCommand;
import org.jsoar.kernel.commands.PrintCommand;
import org.jsoar.kernel.commands.ProductionFindCommand;
import org.jsoar.kernel.commands.PropertiesCommand;
import org.jsoar.kernel.commands.PushdCommand;
import org.jsoar.kernel.commands.PwdCommand;
import org.jsoar.kernel.commands.QMemoryCommand;
import org.jsoar.kernel.commands.ReinforcementLearningCommand;
import org.jsoar.kernel.commands.RhsFunctionsCommand;
import org.jsoar.kernel.commands.SaveBacktracesCommand;
import org.jsoar.kernel.commands.SetParserCommand;
import org.jsoar.kernel.commands.Soar8Command;
import org.jsoar.kernel.commands.SourceCommand;
import org.jsoar.kernel.commands.SourceCommandAdapter;
import org.jsoar.kernel.commands.SpCommand;
import org.jsoar.kernel.commands.SrandCommand;
import org.jsoar.kernel.commands.StatsCommand;
import org.jsoar.kernel.commands.SymbolsCommand;
import org.jsoar.kernel.commands.TimersCommand;
import org.jsoar.kernel.commands.VerboseCommand;
import org.jsoar.kernel.commands.WaitSncCommand;
import org.jsoar.kernel.commands.WarningsCommand;
import org.jsoar.kernel.commands.WatchCommand;
import org.jsoar.util.FileTools;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclRuntimeError;

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
        
        initializeEnv();
        this.agent.getRhsFunctions().registerHandler(tclRhsFunction);
        
        addCommand("source", this.sourceCommand = new SourceCommand(new MySourceCommandAdapter()));
        addCommand("pushd", new PushdCommand(sourceCommand));
        addCommand("popd", new PopdCommand(sourceCommand));
        addCommand("pwd", new PwdCommand(sourceCommand));
        
        addCommand("sp", new SpCommand(this.agent, this.sourceCommand));
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
        
        final PrintCommand printCommand = new PrintCommand(this.agent);
        addCommand("print", printCommand);
        addCommand("default-wme-depth", new DefaultWmeDepthCommand(printCommand));
        
        addCommand("o-support-mode", new OSupportModeCommand());
        addCommand("soar8", new Soar8Command());
        addCommand("firing-counts", new FiringCountsCommand(this.agent));
        addCommand("excise", new ExciseCommand(this.agent));
        addCommand("init-soar", new InitSoarCommand(this.agent));
        addCommand("preferences", new PreferencesCommand(this.agent));
        addCommand("memories", new MemoriesCommand(this.agent));
        addCommand("edit-production", new EditProductionCommand(this.agent));
        addCommand("production-find", new ProductionFindCommand(this.agent));
        
        addCommand("set-parser", new SetParserCommand(this.agent));
        addCommand("properties", new PropertiesCommand(this.agent));
        addCommand("symbols", new SymbolsCommand(this.agent));
        
        addCommand("help", new HelpCommand(this));
        
        addCommand("qmemory", new QMemoryCommand(this.agent));
        addCommand("timers", new TimersCommand());
        
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
    
    /**
     * Jacl only puts system properties in the <code>env</code> array. Let's add
     * environment variables as well..
     */
    private void initializeEnv()
    {
        for(Map.Entry<String, String> e : System.getenv().entrySet())
        {
            try
            {
                // Windows env vars are case-insensitive, but Jacl's env implementation,
                // unlike "real" Tcl doesn't take this into account, so for sanity we'll
                // make them all upper case.
                interp.setVar("env", e.getKey().toUpperCase(), e.getValue(), TCL.GLOBAL_ONLY);
            }
            catch (TclException ex)
            {
                final String message = "Failed to set environment variable '" + e + "': " + interp.getResult();
                logger.error(message);
                agent.getPrinter().error(message);
            }
        }
    }

    private Command adapt(SoarCommand c)
    {
        return new SoarTclCommandAdapter(c);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#getName()
     */
    @Override
    public String getName()
    {
        return "tcl";
    }

    public void dispose()
    {
        synchronized(interfaces)
        {
            interfaces.remove(agent);
            try
            {
                interp.dispose();
            }
            catch (TclRuntimeError e)
            {
                logger.warn("In dispose(): " + e.getMessage());
            }
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
        sourceCommand.source(file.getPath());
    }
    
    public void source(URL url) throws SoarException
    {
        sourceCommand.source(url.toExternalForm());
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
    
    private class MySourceCommandAdapter implements SourceCommandAdapter
    {
        @Override
        public void eval(File file) throws SoarException
        {
            try
            {
                interp.evalFile(file.getAbsolutePath());
            }
            catch (TclException e)
            {
                throw new SoarException(interp.getResult().toString());
            }
        }

        @Override
        public void eval(URL url) throws SoarException
        {
            try
            {
                final InputStream in = new BufferedInputStream(url.openStream());
                try
                {
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    FileTools.copy(in, out);
                    eval(out.toString());
                }
                finally
                {
                    in.close();
                }
            }
            catch(IOException e)
            {
                throw new SoarException(e.getMessage(), e);
            }
        }

        @Override
        public String eval(String code) throws SoarException
        {
            return SoarTclInterface.this.eval(code);
        }
    }
}
