package org.jsoar.kernel.commands;

import java.io.IOException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.PicocliSoarCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "preferences" command.
 * @author austin.brehob
 */
public class PreferencesCommand extends PicocliSoarCommand
{

    public PreferencesCommand(Agent agent)
    {
        super(agent, new PreferencesC(agent));
    }


    @Command(name="preferences", description="Examines details about the preferences "
            + "that support the specified identifier and attribute",
            subcommands={HelpCommand.class})
    static public class PreferencesC implements Runnable
    {
        private Agent agent;

        public PreferencesC(Agent agent)
        {
            this.agent = agent;
        }

        @Option(names={"-n", "-0", "--none"}, defaultValue="false", description="Prints just the preferences themselves")
        boolean level0;

        @Option(names={"-N", "-1", "--names"}, defaultValue="false", description="Prints the preferences "
                + "and the names of the productions that generated them")
        boolean level1;

        @Option(names={"-t", "-2", "--timetags"}, defaultValue="false", description="Prints the information for the --names option "
                + "above plus the timetags of the wmes matched by the LHS of the indicated productions")
        boolean level2;

        @Option(names={"-w", "-3", "--wmes"}, defaultValue="false", description="Prints the information for the "
                + "--timetags option above plus the entire WME matched on the LHS")
        boolean level3;

        @Option(names={"-o", "--object"}, defaultValue="false", description="Prints the support for all "
                + "the WMEs that comprise the object (the specified identifier)")
        boolean object;

        @Parameters(index="0", arity="0..1", description="Must be an existing Soar object identifier")
        String identifier;

        @Parameters(index="1", arity="0..1", description="Must be an "
                + "existing attribute of the specified identifier")
        String attribute;

        @Override
        public void run()
        {
            final PrintPreferencesCommand ppc = new PrintPreferencesCommand();

            ppc.setWmeTraceType(WmeTraceType.NONE);
            ppc.setPrintProduction(false);
            ppc.setObject(false);

            // Set default identifier and attribute
            final Decider decider = Adaptables.adapt(agent, Decider.class);
            ppc.setId(decider.bottom_goal);
            final PredefinedSymbols preSyms = Adaptables.adapt(agent, PredefinedSymbols.class);
            ppc.setAttr(preSyms.operator_symbol);
            
            // Determine level of output
            if (level0)
            {
                ppc.setWmeTraceType(WmeTraceType.NONE);
            }
            else if (level1)
            {
                ppc.setWmeTraceType(WmeTraceType.NONE);
                ppc.setPrintProduction(true);
            }
            else if (level2)
            {
                ppc.setWmeTraceType(WmeTraceType.TIMETAG);
                ppc.setPrintProduction(true);
            }
            else if (level3)
            {
                ppc.setWmeTraceType(WmeTraceType.FULL);
                ppc.setPrintProduction(true);
            }
            else if (object)
            {
                ppc.setObject(true);
            }

            if (identifier != null)
            {
                // Obtain ID from user input if possible
                Symbol idSym = agent.readIdentifierOrContextVariable(identifier);
                if (idSym == null)
                {
                    agent.getPrinter().startNewLine().print("Could not find identifier '" + identifier + "'");
                    return;
                }
                final Identifier id = idSym.asIdentifier();
                if (id == null)
                {
                    agent.getPrinter().startNewLine().print("'" + identifier + "' is not an identifier");
                    return;
                }
                ppc.setId(id);

                // Obtain attribute from user input if possible
                if (attribute != null)
                {
                    final Symbol attr = Symbols.readAttributeFromString(agent, attribute);
                    if (attr == null)
                    {
                        agent.getPrinter().startNewLine().print("'" + attribute
                                + "' is not a known attribute");
                        return;
                    }
                    ppc.setAttr(attr);
                }
                else if (!id.isGoal())
                {
                    ppc.setAttr(null);
                }
            }

            // Print the preferences at the specified level of output
            try
            {
                ppc.print(agent, agent.getPrinter());
            }
            catch (IOException e)
            {
                agent.getPrinter().startNewLine().print(e.getMessage());
            }
            agent.getPrinter().flush();
        }
    }
}
