package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.JSoarVersion;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.commands.ParsedCommand;
import org.jsoar.util.commands.PicocliSoarCommand;
import org.jsoar.util.commands.SoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Unmatched;

/**
 * This is the implementation of the "help" command.
 * The class name contains the word "main" so it doesn't interfere with picocli's HelpCommand class.
 * @author austin.brehob
 */
public class HelpMainCommand extends PicocliSoarCommand
{
    
    public HelpMainCommand(Agent agent)
    {
        super(agent, new Help(agent));
    }
    

    @Command(name="help", description="Displays help information about the specified command", subcommands={HelpCommand.class})
    static public class Help implements Runnable
    {
        private final Agent agent;
        
        @Unmatched
        private String[] remainder;
        
        public Help(Agent agent)
        {
            this.agent = agent;
        }
        
        @Parameters(description="The command to display information for", arity="0..1")
        private String command = null;
        
        @Override
        public void run()
        {
            if (command == null)
            {
                agent.getPrinter().print("JSoar " + JSoarVersion.getInstance().getVersion() + " Command List:\n\r");
                
                try
                {
                    for(String command : agent.getInterpreter().getCommandStrings()) {
                        agent.getPrinter().startNewLine().print(command);
                    }
                }
                catch (SoarException e)
                {
                    agent.getPrinter().error(e.getMessage());
                }
                
                agent.getPrinter().startNewLine().print("\n\rNote: Many previous Soar commands are now sub-commands. To locate a help entry, try 'help <old command name>'.");
            }
            else
            {
                try
                {
                    // we're using a ParsedCommand in case they passed in arguments -- this allows us to get the base command so we can ignore args, like csoar
                    ParsedCommand parsedCommand = agent.getInterpreter().getParsedCommand(command, DefaultSourceLocation.newBuilder().build());
                    // we're using getCommand, because it resolves aliases
                    SoarCommand soarCommand = agent.getInterpreter().getCommand(parsedCommand.getArgs().get(0), parsedCommand.getLocation());
                    String result = soarCommand.execute(new DefaultSoarCommandContext(null), new String[] {parsedCommand.getArgs().get(0), "help"});
                    agent.getPrinter().startNewLine().print(result);
                }
                catch (SoarException e)
                {
                    agent.getPrinter().error(e.getMessage());
                }
            }
        }
    }
}
