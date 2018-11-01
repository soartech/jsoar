package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/**
 * This is the implementation of the "save-backtraces" command.
 * @author austin.brehob
 */
public class SaveBacktracesCommand implements SoarCommand
{
    private final Agent agent;

    public SaveBacktracesCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new SaveBacktraces(agent), args);

        return "";
    }
    @Override
    public Object getCommand() {
        return new SaveBackgraces(agent);
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

        @Option(names={"on", "-e", "--on", "--enable"}, description="Enables backtrace saving")
        boolean enable = false;

        @Option(names={"off", "-d", "--off", "--disable"}, description="Disables backtrace saving")
        boolean disable = false;

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
