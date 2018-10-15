package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "pwd" command.
 * @author austin.brehob
 */
public class PwdCommand implements SoarCommand
{
    private final SourceCommand sourceCommand;
    private Agent agent;
    
    public PwdCommand(SourceCommand sourceCommand, Agent agent)
    {
        this.sourceCommand = sourceCommand;
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Pwd(sourceCommand, agent), args);
        
        return "";
    }

    @Command(name="pwd", description="Prints the working directory to the screen",
            subcommands={HelpCommand.class})
    static public class Pwd implements Runnable
    {
        private final SourceCommand sourceCommand;
        private Agent agent;
        
        public Pwd(SourceCommand sourceCommand, Agent agent)
        {
            this.sourceCommand = sourceCommand;
            this.agent = agent;
        }
        
        @Override
        public void run()
        {
            agent.getPrinter().startNewLine().print(
                    sourceCommand.getWorkingDirectory().replace('\\', '/'));
        }
    }
}
