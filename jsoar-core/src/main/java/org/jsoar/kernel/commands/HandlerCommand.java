package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/**
 * This is the implementation of the "handler" command.
 * @author austin.brehob
 */
public class HandlerCommand implements SoarCommand
{
    private final Agent agent;
    
    public HandlerCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Handler(agent), args);

        return "";
    }
    @Override
    public Object getCommand() {
        return new Handler(agent);
    }

    @Command(name="handler", description="Prints, enables, or disables RHS functions",
            subcommands={HelpCommand.class})
    static public class Handler implements Runnable
    {
        private Agent agent;

        public Handler(Agent agent)
        {
            this.agent = agent;
        }

        @Option(names={"on", "-e", "--on", "--enable"},
                description="Enables RHS function")
        String functionToEnable = null;

        @Option(names={"off", "-d", "--off", "--disable"},
                description="Disables timers")
        String functionToDisable = null;

        @Override
        public void run()
        {
            RhsFunctionManager rhsFunctionManager = agent.getRhsFunctions();

            if (functionToEnable != null)
            {
                rhsFunctionManager.enableHandler(functionToEnable);
                agent.getPrinter().startNewLine().print("RHS function enabled: " + functionToEnable);
            }
            else if (functionToDisable != null)
            {
                rhsFunctionManager.disableHandler(functionToDisable);
                agent.getPrinter().startNewLine().print("RHS function disabled: " + functionToDisable);
            }
            else
            {
                String result = "===== Disabled RHS Functions =====\n";
                for (RhsFunctionHandler handler : rhsFunctionManager.getDisabledHandlers())
                {
                    result += handler.getName() + "\n";
                }
                agent.getPrinter().startNewLine().print(result);
            }
        }
    }
}
