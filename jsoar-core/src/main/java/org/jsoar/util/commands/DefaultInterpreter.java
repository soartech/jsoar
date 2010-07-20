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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.PopdCommand;
import org.jsoar.kernel.commands.PushdCommand;
import org.jsoar.kernel.commands.PwdCommand;
import org.jsoar.kernel.commands.SourceCommand;
import org.jsoar.kernel.commands.SourceCommandAdapter;
import org.jsoar.kernel.commands.SpCommand;
import org.jsoar.kernel.commands.StandardCommands;
import org.jsoar.util.StringTools;

import com.google.common.base.Joiner;

/**
 * @author ray
 */
public class DefaultInterpreter implements SoarCommandInterpreter
{
    private final Map<String, SoarCommand> commands = new HashMap<String, SoarCommand>();
    private final Map<String, List<String>> aliases = new LinkedHashMap<String, List<String>>();
    
    private final Agent agent;
    private final SourceCommand sourceCommand;
    
    public DefaultInterpreter(Agent agent)
    {
        this.agent = agent;
        
        // Interpreter-specific handlers
        addCommand("alias", new AliasCommand());
        addCommand("source", this.sourceCommand = new SourceCommand(new MySourceCommandAdapter(), agent.getEvents()));
        addCommand("pushd", new PushdCommand(sourceCommand));
        addCommand("popd", new PopdCommand(sourceCommand));
        addCommand("pwd", new PwdCommand(sourceCommand));
        addCommand("sp", new SpCommand(this.agent, this.sourceCommand));
        
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
        final PushbackReader pbReader = new PushbackReader(reader);
        List<String> parsedCommand = parser.parseCommand(pbReader);
        String lastResult = "";
        while(!parsedCommand.isEmpty())
        {
            lastResult = executeParsedCommand(parsedCommand);
            
            parsedCommand = parser.parseCommand(pbReader);
        }
        return lastResult;
    }
    
    private String executeParsedCommand(List<String> parsedCommand) throws SoarException
    {
        parsedCommand = resolveAliases(parsedCommand);
        final SoarCommand command = resolveCommand(parsedCommand.get(0));
        if(command != null)
        {
            return command.execute(parsedCommand.toArray(new String[]{}));
        }
        else
        {
            throw new SoarException("Unknown command '" + parsedCommand.get(0) + "' in " + parsedCommand);
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
    
    private List<String> resolveAliases(List<String> parsedCommand)
    {
        final String first = parsedCommand.get(0);
        final List<String> alias = aliases.get(first);
        if(alias == null)
        {
            return parsedCommand;
        }
        else
        {
            final List<String> result = new ArrayList<String>(alias);
            result.addAll(parsedCommand.subList(1, parsedCommand.size()));
            return result;
        }
    }
    
    private class AliasCommand implements SoarCommand
    {
        private String aliasToString(String name, List<String> args)
        {
            return name + "=" + StringTools.join(args, " ");
        }
        
        @Override
        public String execute(String[] args) throws SoarException
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
                evalAndClose(new BufferedReader(new InputStreamReader(url.openStream())), url.toExternalForm());
            }
            catch (IOException e)
            {
                throw new SoarException("Failed to open '" + url + "': " + e.getMessage(), e);
            }
        }

        @Override
        public String eval(String code) throws SoarException
        {
            return DefaultInterpreter.this.eval(code);
        }
    }
}