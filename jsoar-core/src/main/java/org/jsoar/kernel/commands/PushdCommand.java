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
@Command(name="pushd", description="Saves the current working directory on a stack",
         subcommands={HelpCommand.class})
public class PushdCommand implements SoarCommand, Runnable
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
        Utils.parseAndRun(agent, this, args);
        
        return "";
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
