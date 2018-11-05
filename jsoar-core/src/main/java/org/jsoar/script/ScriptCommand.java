package org.jsoar.script;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.Utils;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "script" command.
 * @author austin.brehob
 */
public class ScriptCommand implements SoarCommand
{
    private static final Logger logger = LoggerFactory.getLogger(ScriptCommand.class);
    private static final Map<String, ScriptEngineState> engines =
            new HashMap<String, ScriptEngineState>();
    private static ScriptEngineManager manager;
    private final Agent agent;
    
    public static class Provider implements SoarCommandProvider
    {
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            interp.addCommand("script", new ScriptCommand(context));
        }
    }

    public ScriptCommand(Adaptable context)
    {
        this.agent = (Agent) context;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Script(agent, logger, engines), args);
        
        return "";
    }

    
    @Command(name="script", description="Runs Javascript, Python, or Ruby code",
            subcommands={HelpCommand.class})
    static public class Script implements Runnable
    {
        private final Logger logger;
        private final Map<String, ScriptEngineState> engines;
        private Agent agent;
        
        public Script(Agent agent, Logger logger, Map<String, ScriptEngineState> engines)
        {
            this.agent = agent;
            this.logger = logger;
            this.engines = engines;
        }
        
        @Option(names={"-d", "--dispose"}, description="Disposes the given script engine")
        boolean dispose = false;
        
        @Option(names={"-r", "--reset"}, description="Re-initializes the given script engine")
        boolean reset = false;
        
        @Parameters(description="The name of the engine and code to execute")
        String[] engineNameAndCode = null;
        
        @Override
        public void run()
        {
            if (engineNameAndCode == null)
            {
                // script --dispose OR script --reset
                if (dispose || reset)
                {
                    agent.getPrinter().startNewLine().print("Error: engine name missing");
                }
                // script
                else
                {
                    agent.getPrinter().startNewLine().print(engines.toString());
                }
                return;
            }

            try
            {
                final String engineName = engineNameAndCode[0];
                
                // script --dispose <engineName>
                if (dispose)
                {
                    agent.getPrinter().startNewLine().print(disposeEngine(engineName));
                    return;
                }

                ScriptEngineState state;
                state = getEngineByName(engineName, reset);
                String[] code = Arrays.copyOfRange(engineNameAndCode, 1, engineNameAndCode.length);
                
                // script (--reset) <engineName> <code>
                if (code.length > 0)
                {
                    Object result = state.eval(Joiner.on(' ').join(code));
                    if (result != null)
                    {
                        agent.getPrinter().startNewLine().print(result.toString());
                    }
                }
                // script (--reset) <engineName>
                else
                {
                    agent.getPrinter().startNewLine().print(state.toString());
                }
            }
            catch (SoarException e)
            {
                agent.getPrinter().startNewLine().print("Error: " + e.getMessage());
            }
        }
        
        private synchronized ScriptEngineManager getEngineManager()
        {
            if (manager == null)
            {
                manager = new ScriptEngineManager();
            }
            return manager;
        }
        
        private synchronized String disposeEngine(String name) throws SoarException
        {
            final ScriptEngineState state = engines.remove(name);
            if (state != null)
            {
                state.dispose();
                return "Disposed '" + name + "'";
            }
            else
            {
                return "No engine '" + name + "'";
            }
        }


        private synchronized ScriptEngineState getEngineByName(String name, boolean reset)
                throws SoarException
        {
            ScriptEngineState state = engines.get(name);
            if (state != null && reset)
            {
                state.dispose();
                engines.remove(name);
                state = null;
            }
            
            if (state == null)
            {
                final ScriptEngine engine = getEngineManager().getEngineByName(name);
                if (engine == null)
                {
                    throw new SoarException("Unsupported script engine '" + name + "'");
                }
                
                final ScriptEngineFactory f = engine.getFactory();
                logger.info(String.format("Loaded '%s' script engine for %s: "
                        + "%s version %s, %s version %s", 
                        name, agent, f.getEngineName(), f.getEngineVersion(), 
                        f.getLanguageName(), f.getLanguageVersion()));
                
                engines.put(name, state = new ScriptEngineState(agent, name, engine));
            }
            
            return state;
        }
    }
}
