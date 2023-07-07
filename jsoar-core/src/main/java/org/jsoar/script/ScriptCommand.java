package org.jsoar.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.commands.PicocliSoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * This is the implementation of the "script" command.
 * 
 * @author austin.brehob
 */
public class ScriptCommand extends PicocliSoarCommand
{
    private static final Logger LOG = LoggerFactory.getLogger(ScriptCommand.class);
    
    public static class Provider implements SoarCommandProvider
    {
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            try
            {
                interp.addCommand("script", new ScriptCommand((Agent) context));
            }
            catch(NoClassDefFoundError e)
            {
                LOG.warn("Failed to register script command. Probably ScriptEngineManager not supported on this platform (e.g., Android). You can ignore this if you're not using the script command", e);
            }
        }
    }
    
    public ScriptCommand(Agent agent)
    {
        super(new Script(agent));
    }
    
    @Command(name = "script", description = "Runs Javascript, Python, or Ruby code", subcommands = { HelpCommand.class })
    static public class Script implements Callable<String>
    {
        @Spec
        private CommandSpec spec; // injected by picocli
        private final Map<String, ScriptEngineState> engines = new HashMap<>();
        private Agent agent;
        private ScriptEngineManager manager;
        
        public Script(Agent agent)
        {
            this.agent = agent;
        }
        
        @Option(names = { "-d", "--dispose" }, description = "Disposes the given script engine")
        boolean dispose = false;
        
        @Option(names = { "-r", "--reset" }, description = "Re-initializes the given script engine")
        boolean reset = false;
        
        @Parameters(description = "The name of the engine and code to execute")
        String[] engineNameAndCode = null;
        
        @Override
        public String call()
        {
            if(engineNameAndCode == null)
            {
                // script --dispose OR script --reset
                if(dispose || reset)
                {
                    throw new ParameterException(spec.commandLine(), "Error: engine name missing");
                }
                // script
                else
                {
                    return engines.toString();
                }
            }
            
            try
            {
                final String engineName = engineNameAndCode[0];
                
                // script --dispose <engineName>
                if(dispose)
                {
                    return disposeEngine(engineName);
                }
                
                ScriptEngineState state;
                state = getEngineByName(engineName, reset);
                List<String> trailing = new ArrayList<>(Arrays.asList(engineNameAndCode).subList(1, engineNameAndCode.length));
                
                // script (--reset) <engineName> <code>
                if(!trailing.isEmpty())
                {
                    Object result = state.eval(Joiner.on(' ').join(trailing));
                    return result != null ? result.toString() : "";
                }
                // script (--reset) <engineName>
                else
                {
                    return state.toString();
                }
            }
            catch(SoarException e)
            {
                throw new ParameterException(spec.commandLine(), e.getMessage(), e);
            }
        }
        
        private synchronized ScriptEngineManager getEngineManager()
        {
            if(manager == null)
            {
                manager = new ScriptEngineManager();
            }
            return manager;
        }
        
        private synchronized String disposeEngine(String name) throws SoarException
        {
            final ScriptEngineState state = engines.remove(name);
            if(state != null)
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
            if(state != null && reset)
            {
                state.dispose();
                engines.remove(name);
                state = null;
            }
            
            if(state == null)
            {
                ScriptEngine engine = null;
                if("javascript".equals(name))
                {
                    Engine internalGraalEngine = Engine.newBuilder()
                            .option("engine.WarnInterpreterOnly", "false")
                            .build();
                    engine = GraalJSScriptEngine.create(internalGraalEngine,
                            Context.newBuilder("js")
                                    .allowHostAccess(HostAccess.ALL)
                                    .allowHostClassLookup(className -> true));
                }
                else
                {
                    engine = getEngineManager().getEngineByName(name);
                }
                
                if(engine == null)
                {
                    throw new SoarException("Unsupported script engine '" + name + "'");
                }
                
                final ScriptEngineFactory f = engine.getFactory();
                LOG.info(String.format("Loaded '%s' script engine for %s: "
                        + "%s version %s, %s version %s",
                        name, agent, f.getEngineName(), f.getEngineVersion(),
                        f.getLanguageName(), f.getLanguageVersion()));
                
                engines.put(name, state = new ScriptEngineState(agent, name, engine));
            }
            
            return state;
        }
    }
}
