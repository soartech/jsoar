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
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.jsoar.kernel.commands.SpCommand;
import org.jsoar.kernel.commands.SrandCommand;
import org.jsoar.kernel.commands.StatsCommand;
import org.jsoar.kernel.commands.SymbolsCommand;
import org.jsoar.kernel.commands.VerboseCommand;
import org.jsoar.kernel.commands.WaitSncCommand;
import org.jsoar.kernel.commands.WarningsCommand;
import org.jsoar.kernel.commands.WatchCommand;
import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class DefaultInterpreter implements SoarCommandInterpreter
{
    private final Map<String, SoarCommand> commands = new HashMap<String, SoarCommand>();
    
    private final Agent agent;
    private final SourceCommand sourceCommand;
    
    public DefaultInterpreter(Agent agent)
    {
        this.agent = agent;
        addCommand("source", this.sourceCommand = new SourceCommand(this));
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
        final ByRef<Integer> endIndex = ByRef.create(0);
        final ByRef<Boolean> doneWithCommand = ByRef.create(false);
        
        final List<String> args = new ArrayList<String>();
        int index = 0;
        String lastResult = "";
        while(index < code.length())
        {
            final String word = readWord(code, index, endIndex, doneWithCommand);
            if(word != null)
            {
                args.add(word);
            }
            if(doneWithCommand.value && !args.isEmpty())
            {
                final SoarCommand command = commands.get(args.get(0));
                lastResult = command.execute(args.toArray(new String[0]));
                doneWithCommand.value = false;
                args.clear();
            }
            index = endIndex.value;
        }
        return lastResult;
    }

    private String readWord(String input, int index, ByRef<Integer> endIndex, ByRef<Boolean> done)
    {
        done.value = false;
        while(index < input.length() && Character.isWhitespace(input.charAt(index)))
        {
            if('\n' == input.charAt(index))
            {
                done.value = true;
                endIndex.value = index + 1;
                return null;
            }
            index++;
        }
        if(index >= input.length())
        {
            return null;
        }
        
        final int start = index;
        int braces = 0;
        while(index < input.length())
        {
            final char c = input.charAt(index);
            if(c == '\n' && braces == 0)
            {
                endIndex.value = index + 1;
                done.value = true;
                return input.substring(start, index).trim();
            }
            else if(c == '\r')
            {
                // nothing
            }
            else if(c == '\\')
            {
                index++;
            }
            else if(c == '{')
            {
                braces++;
            }
            else if(c == '}')
            {
                braces--;
                if(braces == 0)
                {
                    endIndex.value = index + 1;
                    return input.substring(start + 1, index).trim();
                }
            }
            else if(braces == 0 && Character.isWhitespace(c))
            {
                endIndex.value = index;
                return input.substring(start, index).trim();
            }
            
            ++index;
        }
        endIndex.value = index;
        done.value = true;
        return input.substring(start);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#source(java.io.File)
     */
    @Override
    public void source(File file) throws SoarException
    {
        try
        {
            final Reader reader = new BufferedReader(new FileReader(file));
            try
            {
                final StringBuilder builder = new StringBuilder();
                final char[] buffer = new char[1024];
                int r = reader.read(buffer);
                while(r >= 0)
                {
                    builder.append(buffer, 0, r);
                    r = reader.read(buffer);
                }
                eval(builder.toString());
            }
            finally
            {
                reader.close();
            }
        }
        catch (FileNotFoundException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
        catch (IOException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandInterpreter#source(java.net.URL)
     */
    @Override
    public void source(URL url) throws SoarException
    {
        // TODO Auto-generated method stub

    }
}
