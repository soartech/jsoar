package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "help" command.
 * @author austin.brehob
 */
public class HelpMainCommand implements SoarCommand
{
    private final Agent agent;
    
    public HelpMainCommand(Agent agent)
    {
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Help(agent), args);
        
        return "";
    }

    
    @Command(name="help", description="Displays help information about the specified command")
    static public class Help implements Runnable
    {
        private Agent agent;
        
        public Help(Agent agent)
        {
            this.agent = agent;
        }
        
        @Parameters(description="The command to display information for")
        String command = null;
        
        @Override
        public void run()
        {
            if (command == null)
            {
                agent.getPrinter().startNewLine().print("Error: command name missing");
            }
            else
            {
                try
                {
                    agent.getInterpreter().eval(command + " help");
                }
                catch (SoarException e)
                {
                    agent.getPrinter().startNewLine().print(e.getMessage());
                }
            }
        }
    }
}
