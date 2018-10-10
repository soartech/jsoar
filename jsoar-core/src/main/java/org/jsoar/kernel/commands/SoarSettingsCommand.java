package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.ParentCommand;

/**
 * This command is the implementation of the "soar" command.
 * It should be called "SoarCommand" to follow the naming convention, but that is already the name of an interface.
 * @author bob.marinier
 *
 */
@Command(name="soar", description="Commands and settings related to running Soar",
         subcommands={HelpCommand.class,
                      SoarSettingsCommand.Init.class})
public class SoarSettingsCommand implements SoarCommand, Runnable
{
    private Agent agent;
    
    public SoarSettingsCommand(Agent agent)
    {
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
                "=======================================================\n" +
                "-                   Soar 9.6.0 Summary                -\n" +
                "=======================================================\n"
                );
        
    }
    
    @Command(name="init", description="Re-initializes Soar", subcommands={HelpCommand.class} )
    static public class Init implements Runnable
    {
        @ParentCommand
        SoarSettingsCommand parent; // injected by picocli

        @Override
        public void run()
        {
            parent.agent.initialize();
            parent.agent.getPrinter().startNewLine().print("Agent reinitialized\n").flush();
            
        }
    }

}
