package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "debugger" command.
 * 
 * @author austin.brehob
 */
public class DebuggerCommand extends PicocliSoarCommand
{
    
    public DebuggerCommand(Agent agent)
    {
        super(agent, new Debugger(agent));
    }
    
    @Command(name = "debugger", description = "Opens the agent's debugger", subcommands = { HelpCommand.class })
    static public class Debugger implements Runnable
    {
        private Agent agent;
        
        public Debugger(Agent agent)
        {
            this.agent = agent;
        }
        
        @Override
        public void run()
        {
            try
            {
                agent.openDebugger();
            }
            catch(SoarException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
            }
        }
    }
    
}
