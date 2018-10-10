/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 12, 2010
 */
package org.jsoar.util.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.PopdCommand;
import org.jsoar.kernel.commands.PushdCommand;
import org.jsoar.kernel.commands.PwdCommand;
import org.jsoar.kernel.commands.ReteNetCommand;
import org.jsoar.kernel.commands.SourceCommand;
import org.jsoar.kernel.commands.SourceCommandAdapter;
import org.jsoar.kernel.commands.StandardCommands;
import org.jsoar.util.ByRef;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.StringTools;
import org.jsoar.util.UrlTools;

import com.google.common.base.Joiner;

/**
 * Default implementation of {@link SoarCommandInterpreter}.
 * 
 * @author ray
 */
public class DefaultInterpreter implements SoarCommandInterpreter
{
    private final Map<String, SoarCommand> commands = new HashMap<String, SoarCommand>();
    private final Map<String, List<String>> aliases = new LinkedHashMap<String, List<String>>();
    
    private final SourceCommand sourceCommand;
    private final ReteNetCommand reteNetCommand;
    
    public DefaultInterpreter(Agent agent)
    {
        // Interpreter-specific handlers
        addCommand("alias", new AliasCommand());
        addCommand("source", this.sourceCommand = new SourceCommand(new MySourceCommandAdapter(), agent.getEvents()));
        addCommand("pushd", new PushdCommand(sourceCommand));
        addCommand("popd", new PopdCommand(sourceCommand));
        addCommand("pwd", new PwdCommand(sourceCommand, agent));
        addCommand("rete-net", this.reteNetCommand = new ReteNetCommand(sourceCommand, agent));
        
        // Load general handlers
        StandardCommands.addToInterpreter(agent, this);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#addCommand(java.lang.String, org.jsoar.util.commands.SoarCommand)
     */
    @Override
    public void addCommand(String name, SoarCommand handler)
    {
        commands.put(name, handler);
    }
    
    /*
     * (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#getCommand(java.lang.String, org.jsoar.util.SourceLocation)
     */
    public SoarCommand getCommand(String name, SourceLocation srcLoc) throws SoarException
    {
        final List<String> command = new ArrayList<String>();
        command.add(name);
        return getSoarCommand(ByRef.create(new ParsedCommand(srcLoc, command)));
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#getName()
     */
    @Override
    public String getName()
    {
        return "default";
    }
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#dispose()
     */
    @Override
    public void dispose()
    {

    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#eval(java.lang.String)
     */
    @Override
    public String eval(String code) throws SoarException
    {
        return evalAndClose(new StringReader(code), code.length() > 100 ? code.substring(0, 100) : code);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#source(java.io.File)
     */
    @Override
    public void source(File file) throws SoarException
    {
        sourceCommand.source(file.getAbsolutePath());
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#source(java.net.URL)
     */
    @Override
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
        reteNetCommand.load(file.getAbsolutePath());
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
    
    @Override
    public Collection<String> getSourcedFiles() 
    {
        return sourceCommand.getSourcedFiles();
    }

    private String evalAndClose(Reader reader, String context) throws SoarException
    {
        try
        {
            return eval(reader);
        }
        catch (IOException e)
        {
            throw new SoarException("Error while evaluating '" + context + "': " + e.getMessage(), e);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                throw new SoarException("Error while closing '" + context + "': " + e.getMessage(), e);
            }
        }
    }
    
    private String eval(Reader reader) throws IOException, SoarException
    {
        final DefaultInterpreterParser parser = new DefaultInterpreterParser();
        final ParserBuffer pbReader = new ParserBuffer(new PushbackReader(reader));
        pbReader.setFile(sourceCommand.getCurrentFile());
        
        ParsedCommand parsedCommand = parser.parseCommand(pbReader);
        String lastResult = "";
        while(!parsedCommand.isEof())
        {
            lastResult = executeParsedCommand(parsedCommand);
            
            parsedCommand = parser.parseCommand(pbReader);
        }
        return lastResult;
    }
    
    private String executeParsedCommand(ParsedCommand parsedCommand) throws SoarException
    {
        final ByRef<ParsedCommand> parsedCommandRef = new ByRef<ParsedCommand>(parsedCommand); 
        final SoarCommand command = getSoarCommand(parsedCommandRef);
        final SoarCommandContext commandContext = new DefaultSoarCommandContext(parsedCommandRef.value.getLocation());
        return command.execute(commandContext, parsedCommandRef.value.getArgs().toArray(new String[]{}));
    }
    
    private SoarCommand getSoarCommand(ByRef<ParsedCommand> parsedCommand) throws SoarException
    {
        parsedCommand.value = resolveAliases(parsedCommand.value);
        final SoarCommand command = resolveCommand(parsedCommand.value.getArgs().get(0));
        if(command != null)
        {
            return command;
        }
        else
        {
            throw new SoarException(parsedCommand.value.getLocation() + ": Unknown command '" + parsedCommand.value.getArgs().get(0) + "'");
        }
    }
    
    private List<Map.Entry<String, SoarCommand>> resolvePossibleCommands(String prefix)
    {
        final List<Map.Entry<String, SoarCommand>> result = new ArrayList<Map.Entry<String,SoarCommand>>();
        for(Map.Entry<String, SoarCommand> entry : commands.entrySet())
        {
            if(entry.getKey().startsWith(prefix))
            {
                result.add(entry);
            }
        }
        return result;
    }
    
    private List<String> getNames(List<Map.Entry<String, SoarCommand>> possible)
    {
        final List<String> result = new ArrayList<String>(possible.size());
        for(Map.Entry<String, SoarCommand> e : possible)
        {
            result.add(e.getKey());
        }
        return result;
    }
    
    private SoarCommand resolveCommand(String name) throws SoarException
    {
        // First a quick check for an exact match
        final SoarCommand quick = commands.get(name);
        if(quick != null)
        {
            return quick;
        }
        
        // Otherwise check for partial matches
        final List<Map.Entry<String, SoarCommand>> possible = resolvePossibleCommands(name);
        if(possible.isEmpty())
        {
            return null;
        }
        else if(possible.size() == 1)
        {
            return possible.get(0).getValue();
        }
        else
        {
            throw new SoarException("Ambiguous command '" + name + "'. Could be one of '" + Joiner.on(", ").join(getNames(possible)));
        }
    }
    
    private ParsedCommand resolveAliases(ParsedCommand parsedCommand)
    {
        final String first = parsedCommand.getArgs().get(0);
        final List<String> alias = aliases.get(first);
        if(alias == null)
        {
            return parsedCommand;
        }
        else
        {
            final List<String> result = new ArrayList<String>(alias);
            result.addAll(parsedCommand.getArgs().subList(1, parsedCommand.getArgs().size()));
            return new ParsedCommand(parsedCommand.getLocation(), result);
        }
    }
    
    private class AliasCommand implements SoarCommand
    {
        private String aliasToString(String name, List<String> args)
        {
            return name + "=" + StringTools.join(args, " ");
        }
        
        @Override
        public String execute(SoarCommandContext context, String[] args) throws SoarException
        {
            if(args.length == 1)
            {
                final StringBuilder b = new StringBuilder();
                for(Map.Entry<String, List<String>> e : aliases.entrySet())
                {
                    b.append(aliasToString(e.getKey(), e.getValue()));
                    b.append('\n');
                }
                return b.toString();
            }
            else if(args.length == 2)
            {
                final List<String> aliasArgs = aliases.get(args[1]);
                if(aliasArgs == null)
                {
                    throw new SoarException("Unknown alias '" + args[1] + "'");
                }
                return aliasToString(args[1], aliasArgs);
            }
            else
            {
                final List<String> aliasArgs = new ArrayList<String>(args.length - 2);
                for(int i = 2; i < args.length; ++i)
                {
                    aliasArgs.add(args[i]);
                }
                aliases.put(args[1], aliasArgs);
                return aliasToString(args[1], aliasArgs);
            }
        }
    }
    private class MySourceCommandAdapter implements SourceCommandAdapter
    {
        @Override
        public void eval(File file) throws SoarException
        {
            try
            {
                evalAndClose(new BufferedReader(new FileReader(file)), file.getAbsolutePath());
            }
            catch (FileNotFoundException e)
            {
                throw new SoarException(e.getMessage(), e);
            }
        }

        @Override
        public void eval(URL url) throws SoarException
        {
            try
            {   
                url = UrlTools.normalize(url);
                
                evalAndClose(new BufferedReader(new InputStreamReader(url.openStream())), url.toExternalForm());
            }
            catch (IOException | URISyntaxException e)
            {
                throw new SoarException("Failed to open '" + url + "': " + e.getStackTrace(), e);
            }
        }

        @Override
        public String eval(String code) throws SoarException
        {
            return DefaultInterpreter.this.eval(code);
        }
    }

}