/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.tcl;

import android.content.res.AssetManager;

import com.google.common.collect.MapMaker;
import com.google.common.io.ByteStreams;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.PopdCommand;
import org.jsoar.kernel.commands.PushdCommand;
import org.jsoar.kernel.commands.PwdCommand;
import org.jsoar.kernel.commands.ReteNetCommand;
import org.jsoar.kernel.commands.SourceCommand;
import org.jsoar.kernel.commands.SourceCommandAdapter;
import org.jsoar.kernel.commands.StandardCommands;
import org.jsoar.kernel.rhs.functions.CmdRhsFunction;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.UrlTools;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclRuntimeError;

/**
 * @author ray
 */
public class SoarTclInterface implements SoarCommandInterpreter
{
    private static final String DEFAULT_TCL_CODE = "/org/jsoar/tcl/jsoar.tcl";

    private static final Logger logger = LoggerFactory.getLogger(SoarTclInterface.class);

    private final static ConcurrentMap<Agent, SoarTclInterface> interfaces = new MapMaker().weakKeys().makeMap();
    
    private SoarCommandContext context = DefaultSoarCommandContext.empty();
    
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
    public static SoarTclInterface findOrCreate(Agent agent, AssetManager assetManager)
    {
        synchronized (interfaces)
        {
            SoarTclInterface ifc = interfaces.get(agent);
            if(ifc == null)
            {
                ifc = new SoarTclInterface(agent, assetManager);
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
    private ReteNetCommand reteNetCommand;
    
    private final TclRhsFunction tclRhsFunction = new TclRhsFunction(this);
    private final CmdRhsFunction cmdRhsFunction;
    
    private SoarTclInterface(Agent agent, AssetManager assetManager)
    {
        this.agent = agent;
        this.cmdRhsFunction = new CmdRhsFunction(this, agent);
        
        initializeEnv();
        this.agent.getRhsFunctions().registerHandler(tclRhsFunction);
        this.agent.getRhsFunctions().registerHandler(cmdRhsFunction);
        
        // Interpreter-specific handlers
        addCommand("source", this.sourceCommand = new SourceCommand(new MySourceCommandAdapter(), agent.getEvents(), assetManager));
        addCommand("pushd", new PushdCommand(sourceCommand));
        addCommand("popd", new PopdCommand(sourceCommand));
        addCommand("pwd", new PwdCommand(sourceCommand));
        addCommand("rete-net", this.reteNetCommand = new ReteNetCommand(sourceCommand, agent));
        
        // Load general handlers
        StandardCommands.addToInterpreter(agent, this);
        
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
        return new SoarTclCommandAdapter(c, this);
    }
    
    public SoarCommandContext getContext()
    {
        return context;
    }
    
    private void updateLastKnownSourceLocation(String location)
    {
        if (location != null)
        {
            try
            {
                SourceLocation sourceLocation = DefaultSourceLocation.newBuilder()
                        .file(new File(location).getCanonicalPath())
                        .build();
                context = new DefaultSoarCommandContext(sourceLocation);
            }
            catch (IOException e)
            {
                // Do nothing.
            }
        }
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
    
    @Override
    public Collection<String> getSourcedFiles() 
    {
        return sourceCommand.getSourcedFiles();
    }

    public Agent getAgent()
    {
        return agent;
    }
    
    public void source(File file) throws SoarException
    {
        String directory = file.getParent();
        sourceCommand.initDirectoryStack(directory);
        sourceCommand.source(file.getName());
    }
    
    public void source(URL url) throws SoarException
    {
        sourceCommand.source(url.toExternalForm());
    }
    
    /*
     * (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#loadRete(java.io.File)
     */
    @Override
    public void loadRete(File file) throws SoarException
    {
        reteNetCommand.load(file.getPath());
    }
    
    /*
     * (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#loadRete(java.net.URL)
     */
    @Override
    public void loadRete(URL url) throws SoarException
    {
        reteNetCommand.load(url.toExternalForm());
    }
    
    /*
     * (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#saveRete(java.io.File)
     */
    @Override
    public void saveRete(File file) throws SoarException
    {
        reteNetCommand.save(file.getPath());
    }
    
    @Override
    public String getWorkingDirectory()
    {
        return sourceCommand.getWorkingDirectory();
    }
    
    public String eval(String command) throws SoarException
    {        
    	// Convert CRLFs (Windows line delimiters) to LFs.
        // (jTcl has an issue with parsing CRLFs: http://kenai.com/bugzilla/show_bug.cgi?id=5817 )
        // See {@link TclLineContinuationTest}
        command = command.replaceAll("\r\n", "\n");
        command = command.replaceAll("\r", "\n");

        try
        {
            interp.eval(command);
            return interp.getResult().toString();
        }
        catch (TclException e)
        {
            throw new SoarException(interp.getResult().toString(), e);
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
    
    /*
     * (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#getCommand(java.lang.String, org.jsoar.util.SourceLocation)
     */
    @Override
    public SoarCommand getCommand(String name, SourceLocation srcLoc) throws SoarException
    {
        SoarTclCommandAdapter commandAdapter = (SoarTclCommandAdapter)interp.getCommand(name);
        if (commandAdapter == null)
        {
            throw new SoarException(srcLoc + ": Unknown command '" + name + "'");
        }
        return commandAdapter.getSoarCommand();
    }
    
    private class MySourceCommandAdapter implements SourceCommandAdapter
    {
        @Override
        public void eval(File file) throws SoarException
        {
            SoarTclInterface.this.updateLastKnownSourceLocation(file.getPath());
            
            try
            {
                interp.evalFile(file.getAbsolutePath());
            }
            catch (TclException e)
            {
                String errLocation = "In file: " + file.getAbsolutePath() + " line " + interp.getErrorLine() + ".";
                throw new SoarException(errLocation + System.getProperty("line.separator") + interp.getResult().toString());
            }
        }

        @Override
        public void eval(URL url) throws SoarException
        {
            try
            {
                url = UrlTools.normalize(url);
                
                SoarTclInterface.this.updateLastKnownSourceLocation(url.getPath());
                
                final InputStream in = new BufferedInputStream(url.openStream());
                try
                {
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ByteStreams.copy(in, out);
                    eval(out.toString());
                }
                finally
                {
                    in.close();
                }
            }
            catch(IOException | URISyntaxException e)
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