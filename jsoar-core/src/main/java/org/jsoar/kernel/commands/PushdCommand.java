package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "pushd" command.
 * 
 * @author austin.brehob
 */
public class PushdCommand extends PicocliSoarCommand
{
    
    public PushdCommand(SourceCommand sourceCommand, Agent agent)
    {
        super(agent, new Pushd(sourceCommand, agent));
    }
    
    @Override
    public Object getCommand()
    {
        return super.getCommand();
    }
    
    @Command(name = "pushd", description = "Saves the current working directory on a stack", subcommands = { HelpCommand.class })
    static public class Pushd implements Runnable
    {
        private final SourceCommand sourceCommand;
        private Agent agent;
        
        public Pushd(SourceCommand sourceCommand, Agent agent)
        {
            this.sourceCommand = sourceCommand;
            this.agent = agent;
        }
        
        @Parameters(index = "0", description = "The directory to push")
        private String dir;
        
        @Override
        public void run()
        {
            try
            {
                sourceCommand.pushd(dir);
            }
            catch(SoarException e)
            {
                this.agent.getPrinter().print(e.getMessage());
            }
        }
    }
}
