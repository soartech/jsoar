package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "popd" command.
 * 
 * @author austin.brehob
 */
public class PopdCommand extends PicocliSoarCommand
{
    
    public PopdCommand(SourceCommand sourceCommand, Agent agent)
    {
        super(agent, new Popd(sourceCommand, agent));
    }
    
    @Command(name = "popd", description = "Pops the top working directory off the stack and sets "
            + "the current working directory to it", subcommands = { HelpCommand.class })
    static public class Popd implements Runnable
    {
        private final SourceCommand sourceCommand;
        private Agent agent;
        
        public Popd(SourceCommand sourceCommand, Agent agent)
        {
            this.sourceCommand = sourceCommand;
            this.agent = agent;
        }
        
        @Override
        public void run()
        {
            try
            {
                sourceCommand.popd();
            }
            catch(SoarException e)
            {
                this.agent.getPrinter().print(e.getMessage());
            }
        }
    }
}
