package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.learning.Explain;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "explain-backtraces" command.
 * @author austin.brehob
 */
public class ExplainBacktracesCommand extends PicocliSoarCommand
{
    public ExplainBacktracesCommand(Agent agent)
    {
        super(agent, new ExplainBacktraces(agent));
    }

    @Command(name="explain-backtraces", description="Allows you to explore how rules were learned",
            subcommands={HelpCommand.class})
    static public class ExplainBacktraces implements Runnable
    {
        private Agent agent;

        public ExplainBacktraces(Agent agent)
        {
            this.agent = agent;
        }

        @Option(names={"-c", "--condition"}, description="Explain why condition "
                + "number n is in the chunk or justification")
        Integer chunkNum;

        @Option(names={"-f", "--full"}, defaultValue="false", description="Print the full backtrace for the named production")
        boolean printFull;

        @Parameters(arity="0..1", description="List all conditions "
                + "and grounds for the chunk or justification")
        String prodName;

        @Override
        public void run()
        {
            int condition = -1;

            // Obtain value of chunk/justification number if possible
            if (chunkNum != null)
            {
                condition = chunkNum;
            }

            // Obtain agent's Explain object if possible
            final Explain explain = Adaptables.adapt(agent, Explain.class);
            if (explain == null)
            {
                agent.getPrinter().startNewLine().print("Internal error: "
                        + "Could not find Explain object in agent!");
                return;
            }

            // Print explanation
            if (prodName == null)
            {
                explain.explain_list_chunks();
            }
            else if (printFull)
            {
                explain.explain_trace_named_chunk(prodName);
            }
            else if (condition == -1)
            {
                explain.explain_cond_list(prodName);
            }
            else
            {
                explain.explain_chunk(prodName, condition);
            }
        }
    }
}
