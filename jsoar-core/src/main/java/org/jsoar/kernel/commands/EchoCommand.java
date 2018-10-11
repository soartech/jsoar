package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "echo" command.
 * @author austin.brehob
 */
@Command(name="echo", description="Outputs the given string",
         subcommands={HelpCommand.class})
public class EchoCommand implements SoarCommand, Runnable
{
    private Agent agent;
    
    public EchoCommand(Agent agent)
    {
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, this, args);
        
        return "";
    }
    
    @Option(names={"-n", "--no-newline"}, description="Suppress printing of the newline character")
    boolean noNewline = false;
    
    @Parameters(description="The string to output")
    String[] outputString = null;

    @Override
    public void run()
    {
        for (int i = 0; i < outputString.length; i++)
        {
            if (i != 0)
            {
                agent.getPrinter().print(" ");
            }
            agent.getPrinter().print(outputString[i]);
        }
        
        if (!noNewline)
        {
            agent.getPrinter().print("\n");
        }
        agent.getPrinter().flush();
    }
}
