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
import org.jsoar.kernel.commands.VersionCommand;
import org.jsoar.kernel.commands.WaitSncCommand;
import org.jsoar.kernel.commands.WarningsCommand;
import org.jsoar.kernel.commands.WatchCommand;
import org.jsoar.util.StringTools;

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
        addCommand("alias", new AliasCommand());
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
        addCommand("version", new VersionCommand());
        
        addCommand("qmemory", new QMemoryCommand(this.agent));
        addCommand("timers", new TimersCommand());
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
        final SoarCommand command = commands.get(parsedCommand.get(0));
        if(command != null)
        {
            return command.execute(parsedCommand.toArray(new String[]{}));
        }
        else
        {
            throw new SoarException("Unknown command '" + parsedCommand.get(0) + "' in " + parsedCommand);
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