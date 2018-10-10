/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 13, 2010
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
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
        interp.addCommand("multi-attributes", new MultiAttrCommand(agent));
        interp.addCommand("stats", new StatsCommand(agent));
        interp.addCommand("learn", new LearnCommand(agent));
        interp.addCommand("srand", new SrandCommand(agent));
        interp.addCommand("max-elaborations", new MaxElaborationsCommand(agent));
        interp.addCommand("matches", new MatchesCommand(agent));
        interp.addCommand("waitsnc", new WaitSncCommand(agent));
        interp.addCommand("init-soar", new InitSoarCommand(agent));
        interp.addCommand("warnings", new WarningsCommand(agent));
        interp.addCommand("verbose", new VerboseCommand(agent));
        interp.addCommand("save-backtraces", new SaveBacktracesCommand(agent));
        interp.addCommand("explain-backtraces", new ExplainBacktracesCommand(agent));
        interp.addCommand("echo", new EchoCommand(agent));
        interp.addCommand("clog", new CLogCommand(agent));
        interp.addCommand("watch", new WatchCommand(agent.getTrace()));
        interp.addCommand("pwatch", new ProductionWatchCommand(agent.getProductions()));
        interp.addCommand("pbreak", new ProductionBreakCommand(agent.getProductions()));
        interp.addCommand("rhs-functions", new RhsFunctionsCommand(agent));
        
        final PrintCommand printCommand = new PrintCommand(agent);
        interp.addCommand("print", printCommand);
        interp.addCommand("default-wme-depth", new DefaultWmeDepthCommand(printCommand));
        
        interp.addCommand("o-support-mode", new OSupportModeCommand());
        interp.addCommand("soar8", new Soar8Command());
        interp.addCommand("firing-counts", new FiringCountsCommand(agent));
        interp.addCommand("excise", new ExciseCommand(agent));
        interp.addCommand("init-soar", new InitSoarCommand(agent));
        interp.addCommand("preferences", new PreferencesCommand(agent));
        interp.addCommand("memories", new MemoriesCommand(agent));
        interp.addCommand("edit-production", new EditProductionCommand(agent));
        interp.addCommand("production-find", new ProductionFindCommand(agent));
        
        interp.addCommand("set-parser", new SetParserCommand(agent));
        interp.addCommand("properties", new PropertiesCommand(agent));
        interp.addCommand("symbols", new SymbolsCommand(agent));
        
        interp.addCommand("decide", new DecideCommand(agent));
        
        interp.addCommand("help", new HelpCommand());
        
        interp.addCommand("qmemory", new QMemoryCommand(agent));
        interp.addCommand("timers", new TimersCommand());
        interp.addCommand("version", new VersionCommand(agent));
        interp.addCommand("set-stop-phase", new SetStopPhaseCommand(agent.getProperties()));
        interp.addCommand("debugger", new DebuggerCommand(agent));

        interp.addCommand("gds-print", new GdsPrintCommand(agent));
        
        interp.addCommand("time", new TimeCommand(agent));
        
        interp.addCommand("log", new LogCommand(agent, interp));
        
        interp.addCommand("handler", new HandlerCommand(agent));
        
        interp.addCommand("soar", new SoarSettingsCommand(agent));
        
        SoarCommands.registerCustomCommands(interp, agent);
    }
    
}
