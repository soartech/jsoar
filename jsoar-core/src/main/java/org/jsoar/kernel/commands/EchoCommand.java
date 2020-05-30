package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "echo" command.
 * @author austin.brehob
 */
public class EchoCommand extends PicocliSoarCommand
{

    public EchoCommand(Agent agent)
    {
        super(agent, new Echo(agent));
    }

    @Command(name="echo", description="Outputs the given string",
            subcommands={HelpCommand.class})
    static public class Echo implements Runnable
    {
        private Agent agent;
        
        public Echo(Agent agent)
        {
            this.agent = agent;
        }
        
        @Option(names={"-n", "--no-newline"}, defaultValue="false", description="Suppress printing of the newline character")
        boolean noNewline;
        
        @Parameters(description="The string to output")
        String[] outputString = null;

        @Override
        public void run()
        {
            if (outputString != null)
            {
                for (int i = 0; i < outputString.length; i++)
                {
                    if (i != 0)
                    {
                        agent.getPrinter().print(" ");
                    }
                    agent.getPrinter().print(outputString[i]);
                }
            }
            
            if (!noNewline)
            {
                agent.getPrinter().print("\n");
            }
            agent.getPrinter().flush();
        }
    }
}
