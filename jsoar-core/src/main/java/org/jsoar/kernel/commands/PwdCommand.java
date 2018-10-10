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
@Command(name="pwd", description="Prints the working directory to the screen",
         subcommands={HelpCommand.class})
public class PwdCommand implements SoarCommand, Runnable
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
        Utils.parseAndRun(agent, this, args);
        
        return "";
    }

    @Override
    public void run()
    {
        agent.getPrinter().startNewLine().print(
                sourceCommand.getWorkingDirectory().replace('\\', '/'));
    }
}
