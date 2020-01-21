package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "pushd" command.
 * @author austin.brehob
 */
public class PushdCommand implements SoarCommand
{
    private final SourceCommand sourceCommand;
    private Agent agent;
    
    public PushdCommand(SourceCommand sourceCommand, Agent agent)
    {
        this.sourceCommand = sourceCommand;
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Pushd(sourceCommand, agent), args);
        
        return "";
    }
    @Override
    public Object getCommand() {
        return new Pushd(sourceCommand,agent);
    }
    
    @Command(name="pushd", description="Saves the current working directory on a stack",
            subcommands={HelpCommand.class})
    static public class Pushd implements Runnable
    {
        private final SourceCommand sourceCommand;
        private Agent agent;
        
        public Pushd(SourceCommand sourceCommand, Agent agent)
        {
            this.sourceCommand = sourceCommand;
            this.agent = agent;
        }
        
        @Parameters(index="0", description="The directory to push")
        private String dir;
        
        @Override
        public void run()
        {
            try
            {
                sourceCommand.pushd(dir);
            }
            catch (SoarException e)
            {
                this.agent.getPrinter().print(e.getMessage());
            }
        }
    }
}
