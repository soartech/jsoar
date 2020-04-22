package org.jsoar.kernel.commands;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * This is the implementation of the "rhs-functions" command.
 * @author austin.brehob
 */
public class RhsFunctionsCommand extends PicocliSoarCommand
{
    public RhsFunctionsCommand(Agent agent)
    {
        super(agent, new RhsFunctionsC(agent));
    }

    @Command(name="rhs-functions", description="Prints a list of all RHS functions",
            subcommands={HelpCommand.class})
    static public class RhsFunctionsC implements Runnable
    {
        private Agent agent;
        
        public RhsFunctionsC(Agent agent)
        {
            this.agent = agent;
        }

        @Override
        public void run()
        {
            final Printer p = agent.getPrinter();
            p.startNewLine();

            // Obtain all RHS functions and sort them
            final List<RhsFunctionHandler> handlers = agent.getRhsFunctions().getHandlers();
            Collections.sort(handlers, new Comparator<RhsFunctionHandler>()
            {
                @Override
                public int compare(RhsFunctionHandler a, RhsFunctionHandler b)
                {
                    return a.getName().compareTo(b.getName());
                }
            });

            for (RhsFunctionHandler f : handlers)
            {
                int max = f.getMaxArguments();
                p.print("%20s (%d, %s)%n", f.getName(), f.getMinArguments(),
                        max == Integer.MAX_VALUE ? "*" : Integer.toString(max));
            }
        }
    }
}
