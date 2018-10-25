package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "debugger" command.
 * @author austin.brehob
 */
public class DebuggerCommand implements SoarCommand
{
    private final Agent agent;
    
    public DebuggerCommand(Agent agent)
    {
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Debugger(agent), args);
        
        return "";
    }

    
    @Command(name="debugger", description="Opens the agent's debugger",
            subcommands={HelpCommand.class})
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
            catch (SoarException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
            }
        }
    }
}
