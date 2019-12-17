package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import org.jsoar.util.commands.SoarExceptionsManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "sp" command.
 * @author austin.brehob
 */
public class SpCommand implements SoarCommand
{
    private Agent agent;

    public SpCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Sp(agent, context), args);
        
        return "";
    }


    @Command(name="sp", description="Define a Soar production",
            subcommands={HelpCommand.class})
    static public class Sp implements Runnable
    {
        private Agent agent;
        private SoarCommandContext context;

        public Sp(Agent agent, SoarCommandContext context)
        {
            this.agent = agent;
            this.context = context;
        }

        @Parameters(description="A Soar production")
        String production = null;

        @Override
        public void run()
        {
            if (production == null)
            {
                agent.getPrinter().startNewLine().print("Use this command to define a Soar production");
            }
            else
            {
                try
                {
                    agent.getProductions().loadProduction(production, context.getSourceLocation());
                    agent.getPrinter().print("*");
                    SoarExceptionsManager exceptionsManager = agent.getInterpreter().getExceptionsManager();
                    agent.getPrinter().getWarningsAndClear().forEach(warning -> exceptionsManager.addException(warning, context, production));
                }
                catch (ReordererException | ParserException e)
                {
                    agent.getPrinter().startNewLine().print(
                            context.getSourceLocation() + ":" + e.getMessage());
                    agent.getInterpreter().getExceptionsManager().addException(e, context, production);
                }
            }
        }
    }
    @Override
    public Object getCommand() {
        return new Sp(agent,null);
    }
}
