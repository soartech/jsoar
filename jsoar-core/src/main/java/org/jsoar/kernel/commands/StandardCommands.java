/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 13, 2010
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.wma.WMActivationCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommands;

/**
 * Helper methods for installing standard command handlers in interpreters.
 * 
 * @author ray
 */
public class StandardCommands
{
    /**
     * Install standard Soar command handlers on the given interpreter. This is
     * a convenience method to reduce long, duplicate lists of handlers in 
     * multiple interpreters.
     * 
     * @param agent the agent
     * @param interp the interpreter
     */
    public static void addToInterpreter(Agent agent, SoarCommandInterpreter interp)
    {
        interp.addCommand("sp", new SpCommand(agent));
        interp.addCommand("stats", new StatsCommand(agent));
        interp.addCommand("waitsnc", new WaitSncCommand(agent));
        interp.addCommand("warnings", new WarningsCommand(agent));
        interp.addCommand("verbose", new VerboseCommand(agent));
        interp.addCommand("save-backtraces", new SaveBacktracesCommand(agent));
        interp.addCommand("explain-backtraces", new ExplainBacktracesCommand(agent));
        interp.addCommand("echo", new EchoCommand(agent));
        interp.addCommand("watch", new WatchCommand(agent.getTrace()));
        interp.addCommand("rhs-functions", new RhsFunctionsCommand(agent));
        
        final PrintCommand printCommand = new PrintCommand(agent);
        interp.addCommand("print", printCommand);
        
        interp.addCommand("preferences", new PreferencesCommand(agent));
        interp.addCommand("edit-production", new EditProductionCommand(agent));

        
        interp.addCommand("set-parser", new SetParserCommand(agent));
        interp.addCommand("properties", new PropertiesCommand(agent));
        
        interp.addCommand("decide", new DecideCommand(agent));
        
        interp.addCommand("help", new HelpMainCommand(agent));
        
        interp.addCommand("qmemory", new QMemoryCommand(agent));
        interp.addCommand("timers", new TimersCommand());
        interp.addCommand("version", new VersionCommand(agent));
        interp.addCommand("debugger", new DebuggerCommand(agent));

        interp.addCommand("log", new LogCommand(agent, interp));
        
        interp.addCommand("handler", new HandlerCommand(agent));
        
        interp.addCommand("soar", new SoarSettingsCommand(agent));
        interp.addCommand("output", new OutputCommand(agent, printCommand));
        interp.addCommand("production", new ProductionCommand(agent));
        interp.addCommand("chunk", new ChunkCommand(agent));
        interp.addCommand("wm", new WMActivationCommand(agent));
        interp.addCommand("debug", new DebugCommand(agent));
        interp.addCommand("run", new RunCommand(agent));
        interp.addCommand("trace", new TraceCommand(agent));
        
        SoarCommands.registerCustomCommands(interp, agent);
    }
    
}
