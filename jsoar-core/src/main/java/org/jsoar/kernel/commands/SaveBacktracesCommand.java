package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/**
 * This is the implementation of the "save-backtraces" command.
 * @author austin.brehob
 */
public class SaveBacktracesCommand extends PicocliSoarCommand
{

    public SaveBacktracesCommand(Agent agent)
    {
        super(agent, new SaveBacktraces(agent));
    }

    @Command(name="save-backtraces", description="Toggles or prints backtrace saving",
            subcommands={HelpCommand.class})
    static public class SaveBacktraces implements Runnable
    {
        private Agent agent;

        public SaveBacktraces(Agent agent)
        {
            this.agent = agent;
        }

        @Option(names={"on", "-e", "--on", "--enable"}, defaultValue="false", description="Enables backtrace saving")
        boolean enable;

        @Option(names={"off", "-d", "--off", "--disable"}, defaultValue="false", description="Disables backtrace saving")
        boolean disable;

        @Override
        public void run()
        {
            if (!enable && !disable)
            {
                agent.getPrinter().startNewLine().print("The current save-backtraces setting is: " +
                        (agent.getProperties().get(SoarProperties.EXPLAIN) ? "enabled" : "disabled"));
            }
            else if (enable)
            {
                agent.getProperties().set(SoarProperties.EXPLAIN, true);
            }
            else
            {
                agent.getProperties().set(SoarProperties.EXPLAIN, false);
            }
        }
    }
}
