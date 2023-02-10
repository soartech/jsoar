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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.LoadCommand;
import org.jsoar.kernel.commands.PopdCommand;
import org.jsoar.kernel.commands.PushdCommand;
import org.jsoar.kernel.commands.PwdCommand;
import org.jsoar.kernel.commands.SaveCommand;
import org.jsoar.kernel.commands.SourceCommand;
import org.jsoar.kernel.commands.SourceCommandAdapter;
import org.jsoar.kernel.commands.SpCommand;
import org.jsoar.kernel.commands.StandardCommands;
import org.jsoar.kernel.exceptions.TclInterpreterException;
import org.jsoar.kernel.rhs.functions.CmdRhsFunction;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.UrlTools;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.commands.ParsedCommand;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarExceptionsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;
import com.google.common.io.ByteStreams;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclException;
import tcl.lang.cmd.InterpAliasCmd;

/**
 * @author ray
 */
public class SoarTclInterface implements SoarCommandInterpreter
{
    private static final String DEFAULT_TCL_CODE = "/org/jsoar/tcl/jsoar.tcl";
    
    private static final Logger LOG = LoggerFactory.getLogger(SoarTclInterface.class);
    
    private final static ConcurrentMap<Agent, SoarTclInterface> INTERFACES = new MapMaker().weakKeys().makeMap();
    
    private SoarCommandContext context = DefaultSoarCommandContext.empty();
    
    private SoarExceptionsManager exceptionsManager;
    
    // Making these volatile since we're always just swapping in entirely new copies
    private volatile HashMap<String, String> aliasMap = new HashMap<>();
    private volatile ArrayList<String> commandList = new ArrayList<>();
    
    /**
     * Find the SoarTclInterface for the given agent.
     * 
     * <p>Note: you almost never want to use this. You'd be much happier
     * with {@link Agent#getInterpreter()}.
     * 
     * @param agent the agent
     * @return the Tcl interface, or {@code null} if none is associated with the agent.
     */
    public static SoarTclInterface find(Agent agent)
    {
        synchronized (INTERFACES)
        {
            return INTERFACES.get(agent);
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
        synchronized (INTERFACES)
        {
            SoarTclInterface ifc = INTERFACES.get(agent);
            if(ifc == null)
            {
                ifc = new SoarTclInterface(agent);
                INTERFACES.put(agent, ifc);
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
    private LoadCommand loadCommand;
    private SaveCommand saveCommand;
    
    private final TclRhsFunction tclRhsFunction = new TclRhsFunction(this);
    private final CmdRhsFunction cmdRhsFunction;
    
    private SoarTclInterface(Agent agent)
    {
        this.agent = agent;
        this.cmdRhsFunction = new CmdRhsFunction(this, agent);
        this.exceptionsManager = new SoarExceptionsManager();
        
        initializeEnv();
        this.agent.getRhsFunctions().registerHandler(tclRhsFunction);
        this.agent.getRhsFunctions().registerHandler(cmdRhsFunction);
        
        // Interpreter-specific handlers
        this.sourceCommand = new SourceCommand(new MySourceCommandAdapter(), agent.getEvents());
        addCommand("pushd", new PushdCommand(sourceCommand, agent));
        addCommand("popd", new PopdCommand(sourceCommand, agent));
        addCommand("pwd", new PwdCommand(sourceCommand));
        
        addCommand("save", this.saveCommand = new SaveCommand(sourceCommand, agent));
        
        // rename the tcl built-in trace command to tcl-trace, to avoid a conflict with Soar's trace command
        try
        {
            interp.renameCommand("trace", "tcl-trace");
        }
        catch(TclException e)
        {
            final String message = "Failed to rename tcl built-in trace command to tcl-trace: " + interp.getResult();
            LOG.error(message, e);
            agent.getPrinter().error(message).flush();
        }
        
        // Load general handlers
        StandardCommands.addToInterpreter(agent, this);
        
        // this interpreter-specific handler depends on SpCommand, which is created as part of the standard commands
        try
        {
            addCommand("load", this.loadCommand = new LoadCommand(sourceCommand, (SpCommand) this.getCommand("sp", null), agent));
        }
        catch(SoarException e)
        {
            final String message = "Failed to get 'sp' command";
            LOG.error(message, e);
            agent.getPrinter().error(message).flush();
        }
        
        addAliasedCommand("source", new String[] { "load", "file" }, this.loadCommand);
        
        try
        {
            URL url = SoarTclInterface.class.getResource(DEFAULT_TCL_CODE);
            interp.evalURL(null, url.toString());
        }
        catch(TclException e)
        {
            final String message = "Failed to load resource " + DEFAULT_TCL_CODE +
                    ". Some commands may not work as expected: " + interp.getResult();
            LOG.error(message, e);
            agent.getPrinter().error(message);
        }
    }
    
    private void updateTCLInfo()
    {
        try
        {
            updateTCLAliases();
            updateTCLProcsAndCommands();
        }
        catch(SoarException e)
        {
            LOG.error("Unable to retreive alias information", e);
            return;
        }
    }
    
    private void updateTCLProcsAndCommands() throws SoarException
    {
        ArrayList<String> curCommands = new ArrayList<>();
        String result = this.eval("info commands");
        LOG.info("Commands\n\n{}", result);
        String[] cmds = result.split("\\s+");
        Collections.addAll(curCommands, cmds);
        
        result = this.eval("info procs");
        LOG.info("Procs\n{}", result);
        String[] procs = result.split("\\s+");
        Collections.addAll(curCommands, procs);
        this.commandList = curCommands;
    }
    
    private void updateTCLAliases() throws SoarException
    {
        String result = this.eval("alias");
        String[] lines = result.split("\\R+");
        HashMap<String, String> curAliases = new HashMap<>();
        for(String line : lines)
        {
            String[] args = line.split("->");
            if(args.length == 2)
            {
                String alias = args[0].trim();
                String cmd = args[1].trim().replace("{", "").replace("}", "");
                LOG.debug("args = {} -> {}", alias, cmd);
                curAliases.put(alias, cmd);
            }
            else
            {
                LOG.error("Unable to parse alias = {}", line);
            }
        }
        this.aliasMap = curAliases;
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
            catch(TclException ex)
            {
                final String message = "Failed to set environment variable '" + e + "': " + interp.getResult();
                LOG.error(message);
                agent.getPrinter().error(message);
            }
        }
    }
    
    private Command adapt(SoarCommand c)
    {
        return new SoarTclCommandAdapter(c, this);
    }
    
    private Command adapt(SoarCommand c, String[] prefix)
    {
        return new SoarTclCommandAdapter(c, prefix, this);
    }
    
    public SoarCommandContext getContext()
    {
        return context;
    }
    
    private void updateLastKnownSourceLocation(String location)
    {
        if(location != null)
        {
            try
            {
                SourceLocation sourceLocation = DefaultSourceLocation.newBuilder()
                        .file(new File(location).getCanonicalPath())
                        .build();
                context = new DefaultSoarCommandContext(sourceLocation);
            }
            catch(IOException e)
            {
                // Do nothing.
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.commands.SoarCommandInterpreter#getName()
     */
    @Override
    public String getName()
    {
        return "tcl";
    }
    
    public void dispose()
    {
        synchronized (INTERFACES)
        {
            INTERFACES.remove(agent);
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
    
    public SoarExceptionsManager getTclContext()
    {
        return this.exceptionsManager;
    }
    
    public void source(File file) throws SoarException
    {
        sourceCommand.source(file.getPath());
        updateTCLInfo();
    }
    
    public void source(URL url) throws SoarException
    {
        sourceCommand.source(url.toExternalForm());
        updateTCLInfo();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.commands.SoarCommandInterpreter#loadRete(java.io.File)
     */
    @Override
    public void loadRete(File file) throws SoarException
    {
        this.loadCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "load", "rete-net", "--load", file.getAbsolutePath().replace('\\', '/') });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.commands.SoarCommandInterpreter#loadRete(java.net.URL)
     */
    @Override
    public void loadRete(URL url) throws SoarException
    {
        this.loadCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "load", "rete-net", "--load", url.toExternalForm() });
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.commands.SoarCommandInterpreter#saveRete(java.io.File)
     */
    @Override
    public void saveRete(File file) throws SoarException
    {
        this.saveCommand.execute(DefaultSoarCommandContext.empty(), new String[] { "save", "rete-net", "--save", file.getPath().replace('\\', '/') });
    }
    
    @Override
    public String getWorkingDirectory()
    {
        return sourceCommand.getWorkingDirectory();
    }
    
    public synchronized String eval(String command) throws SoarException
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
        catch(TclException e)
        {
            throw new TclInterpreterException(interp.getResult().toString());
            // throw new SoarException(interp.getResult().toString());
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.commands.SoarCommandInterpreter#addCommand(java.lang.String, org.jsoar.util.commands.SoarCommand)
     */
    @Override
    public void addCommand(String name, SoarCommand handler)
    {
        interp.createCommand(name, adapt(handler));
    }
    
    public void addAliasedCommand(String alias, String[] actualCommand, SoarCommand handler)
    {
        interp.createCommand(alias, adapt(handler, actualCommand));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.commands.SoarCommandInterpreter#findCommand(java.lang.String, org.jsoar.util.SourceLocation)
     */
    @Override
    public SoarCommand getCommand(String name, SourceLocation srcLoc) throws SoarException
    {
        Command command = interp.getCommand(name);
        while(command instanceof InterpAliasCmd)
        {
            try
            {
                command = ((InterpAliasCmd) command).getTargetCmd(interp).cmd;
            }
            catch(TclException e)
            {
                e.printStackTrace();
            }
        }
        if(command instanceof SoarTclCommandAdapter)
        {
            SoarTclCommandAdapter commandAdapter = (SoarTclCommandAdapter) command;
            return commandAdapter.getSoarCommand();
        }
        else if(command instanceof SoarCommand)
        {
            return (SoarCommand) command;
        }
        throw new SoarException(srcLoc + ": Unknown command '" + name + "'");
    }
    
    @Override
    public String[] getCompletionList(String command, int cursorPosition)
    {
        List<String> commandsList = new ArrayList<>();
        for(String s : this.commandList)
        {
            if(s.startsWith(command))
            {
                commandsList.add(s);
            }
        }
        return commandsList.toArray(new String[0]);
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
            catch(TclException e)
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
                
                try(final InputStream in = new BufferedInputStream(url.openStream()))
                {
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ByteStreams.copy(in, out);
                    eval(out.toString());
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
    
    @Override
    public ParsedCommand getParsedCommand(String name, SourceLocation srcLoc)
    {
        List<String> args = new ArrayList<>(Arrays.asList(name.split("\\s+")));
        String aliasCmd = this.aliasMap.get(args.get(0));
        
        // there is no alias, so just return the original args
        if(aliasCmd == null)
        {
            return new ParsedCommand(srcLoc, args);
        }
        
        // there is an alias, so split it and add the original args to it
        List<String> aliasArgs = new ArrayList<>(Arrays.asList(aliasCmd.split("\\s")));
        aliasArgs.addAll(args.subList(1, args.size()));
        
        return new ParsedCommand(srcLoc, aliasArgs);
    }
    
    @Override
    public List<String> getCommandStrings() throws SoarException
    {
        String[] commandNames = this.eval("info commands").split("\\s");
        List<String> soarCommandNames = new ArrayList<>();
        
        for(String commandName : commandNames)
        {
            Command command = interp.getCommand(commandName);
            if(command instanceof SoarTclCommandAdapter || command instanceof SoarCommand)
            {
                soarCommandNames.add(commandName);
            } // else ignore (e.g., aliases, tcl commands)
        }
        Collections.sort(soarCommandNames);
        return soarCommandNames;
    }
    
    @Override
    public SoarExceptionsManager getExceptionsManager()
    {
        return exceptionsManager;
    }
}
