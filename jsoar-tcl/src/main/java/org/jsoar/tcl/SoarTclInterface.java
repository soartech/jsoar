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

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.PopdCommand;
import org.jsoar.kernel.commands.PushdCommand;
import org.jsoar.kernel.commands.PwdCommand;
import org.jsoar.kernel.commands.SourceCommand;
import org.jsoar.kernel.commands.SourceCommandAdapter;
import org.jsoar.kernel.commands.StandardCommands;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.TclRuntimeError;

import com.google.common.collect.MapMaker;
import com.google.common.io.ByteStreams;

/**
 * @author ray
 */
public class SoarTclInterface implements SoarCommandInterpreter
{
    private static final String DEFAULT_TCL_CODE = "/org/jsoar/tcl/jsoar.tcl";

    private static final Logger logger = LoggerFactory.getLogger(SoarTclInterface.class);
    
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
        
        // Interpreter-specific handlers
        addCommand("source", this.sourceCommand = new SourceCommand(new MySourceCommandAdapter(), agent.getEvents()));
        addCommand("pushd", new PushdCommand(sourceCommand));
        addCommand("popd", new PopdCommand(sourceCommand));
        addCommand("pwd", new PwdCommand(sourceCommand));

        // Load general handlers
        StandardCommands.addToInterpreter(agent, this);
        
        try
        {
            interp.evalFile(DEFAULT_TCL_CODE);
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
//        for(Map.Entry<String, String> e : System.getenv().entrySet())
//        {
//            try
//            {
//                // Windows env vars are case-insensitive, but Jacl's env implementation,
//                // unlike "real" Tcl doesn't take this into account, so for sanity we'll
//                // make them all upper case.
//                //TODO
//            	interp.setVar("env", e.getKey().toUpperCase(), e.getValue(), TCL.GLOBAL_ONLY);
//            }
//            catch (TclException ex)
//            {
//                final String message = "Failed to set environment variable '" + e + "': " + interp.getResult();
//                logger.error(message);
//                agent.getPrinter().error(message);
//            }
//        }
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
                String errLocation = "In file: " + file.getAbsolutePath() + " line ?" + /*interp.getErrorLine() +*/ ".";
                throw new SoarException(errLocation + System.getProperty("line.separator") + interp.getResult().toString());
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
                    ByteStreams.copy(in, out);
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
