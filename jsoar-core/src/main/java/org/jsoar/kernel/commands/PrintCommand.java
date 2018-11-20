package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Goal;
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WorkingMemoryPrinter;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.base.Joiner;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "print" command.
 * @author austin.brehob
 */
public class PrintCommand implements SoarCommand
{
    private Agent agent;
    protected int defaultDepth = 1;
    protected int depth = 1;
    protected WorkingMemoryPrinter wmp = new WorkingMemoryPrinter();
    
    public PrintCommand(Agent agent)
    {
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        // 'this' is passed to the Print class to provide access to
        // PrintCommand's variables and functions
        Utils.parseAndRun(agent, new Print(agent, this), args);
        
        return "";
    }
    
    @Command(name="print", description="Prints data in the console window",
            subcommands={HelpCommand.class})
    static public class Print implements Runnable
    {
        private Agent agent;
        private PrintCommand pc;
        
        public Print(Agent agent, PrintCommand pc)
        {
            this.agent = agent;
            this.pc = pc;
        }
        
        @Option(names={"-a", "--all"}, description="Print the names of all productions currently loaded")
        boolean printAll = false;
        
        @Option(names={"-c", "--chunks"}, description="Print the names of all chunks currently loaded")
        boolean printChunks = false;
        
        @Option(names={"-D", "--defaults"}, description="Print the names of "
                + "all default productions currently loaded")
        boolean printDefaults = false;
        
        @Option(names={"-d", "--depth"}, description="This option overrides the default printing depth")
        Integer overrideDepth = null;
        
        @Option(names={"-e", "--exact"}, description="Print only the wmes that match the pattern")
        boolean printExactMatch = false;
        
        @Option(names={"-F", "--filename"}, description="Also prints the name "
                + "of the file that contains the production")
        boolean printFilename = false;
        
        @Option(names={"-f", "--full"}, description="When printing productions, print "
                + "the whole production (default behavior for individual productions)")
        boolean printFullProd = false;
        
        @Option(names={"-g", "--gds"}, description="Print the Goal Dependency Set")
        boolean printGDS = false;
        
        @Option(names={"-i", "--internal"}, description="Items will be printed in their internal form")
        boolean printInternalForm = false;
        
        @Option(names={"-j", "--justifications"}, description="Print the names of "
                + "all justifications currently loaded")
        boolean printJustify = false;
        
        @Option(names={"-n", "--name"}, description="When printing productions, print only "
                + "the name and not the whole production (default behavior for multiple productions")
        boolean printOnlyName = false;
        
        @Option(names={"-o", "--operators"}, description="When printing the stack, print only operators")
        boolean printOnlyOperators = false;
        
        @Option(names={"-r", "--rl"}, description="Print Soar-RL rules")
        boolean printRLRules = false;
        
        @Option(names={"-s", "--stack"}, description="Specifies that the Soar goal stack should be printed")
        boolean printStack = false;
        
        @Option(names={"-S", "--states"}, description="When printing the stack, print only states")
        boolean printOnlyStates = false;
        
        @Option(names={"-T", "--template"}, description="Print Soar-RL templates")
        boolean printTemplates = false;
        
        @Option(names={"-t", "--tree"}, description="wmes will be printed in a tree form (one wme per line)")
        boolean printInTreeForm = false;
        
        @Option(names={"-u", "--user"}, description="Print the names of all user productions currently loaded")
        boolean printUserProds = false;
        
        @Option(names={"-v", "--varprint"}, description="Print identifiers enclosed in angle brackets")
        boolean printVars = false;
        
        @Parameters(description="A symbol, pattern, timetag, or production name")
        private String[] params = null;
        
        
        @Override
        public void run()
        {
            if (printGDS)
            {
                String result = "********************* Current GDS **************************\n"
                        + "stepping thru all wmes in rete, looking for any that are in a gds...\n";

                // the command outputs two lists of wmes:
                //  the wme-only list, which is in bottom-to-top order by goal
                //  the goal list, which is in top-to-bottom order by goal

                final List<Goal> goalsTopToBottom = agent.getGoalStack();
                final List<Goal> goalsBottomtoTop = new ArrayList<Goal>(goalsTopToBottom);
                Collections.reverse(goalsBottomtoTop);

                // list wmes from goals in bottom-to-top order
                for (final Goal goal : goalsBottomtoTop)
                {
                    final GoalDependencySet gds = Adaptables.adapt(goal, GoalDependencySet.class);
                    if (gds == null)
                    {
                        continue;
                    }

                    final Iterator<Wme> itr = gds.getWmes();
                    while (itr.hasNext())
                    {
                        final Wme w = itr.next();
                        result += "  For Goal  " + goal.toString() + "  " + w.toString() + "\n";
                    }
                }

                result += "************************************************************\n";

                // list goals with wmes in top-to-bottom order
                for (final Goal goal : goalsTopToBottom)
                {
                    result += "  For Goal  " + goal.toString();
                    final GoalDependencySet gds = Adaptables.adapt(goal, GoalDependencySet.class);
                    if (gds == null)
                    {
                        result += "  : No GDS for this goal.\n";
                        continue;
                    }

                    result += "\n";

                    final Iterator<Wme> itr = gds.getWmes();
                    while (itr.hasNext())
                    {
                        final Wme w = itr.next();
                        result += "                " + w.toString() + "\n";
                    }
                }

                result += "************************************************************\n";

                agent.getPrinter().startNewLine().print(result);
            }
            
            agent.getPrinter().startNewLine();
            pc.depth = pc.defaultDepth;

            if (overrideDepth != null)
            {
                int newDepth = overrideDepth;
                if (pc.checkDepth(newDepth))
                {
                    pc.depth = newDepth;
                }
            }

            if (printVars)
            {
                agent.getPrinter().print("--varprint not implemented yet");
                return;
            }

            // New in Soar 8.6.3: if no args or options given, print all prods
            if (!printChunks && !printDefaults && !printJustify &&
                    !printRLRules && !printTemplates && !printUserProds)
                printAll = true;

            if (printStack)
            {
                agent.printStackTrace(printOnlyStates, printOnlyOperators);
                agent.getPrinter().print("\n").flush();
                return;
            }
            else if (printOnlyStates || printOnlyOperators)
            {
                agent.getPrinter().print("Options --operators (-o) and --states (-S) "
                        + "are only valid when printing the stack.");
                return;
            }

            if (params != null)
            {
                String argString = Joiner.on(' ').join(params);

                // Test if the parameter(s) passed is a symbol or pattern
                Symbol arg = agent.readIdentifierOrContextVariable(argString);
                if (arg != null || argString.charAt(0) == '(')
                {
                    agent.getPrinter().startNewLine();
                    pc.wmp.setInternal(printInternalForm);
                    pc.wmp.setExact(printExactMatch);

                    // These are ignored if the parameters form a pattern
                    pc.wmp.setDepth(pc.depth);
                    pc.wmp.setTree(printInTreeForm);

                    try
                    {
                        pc.wmp.print(agent, agent.getPrinter(), arg, argString);
                    }
                    catch (Exception e)
                    {
                        agent.getPrinter().print(e.toString());
                    }

                    agent.getPrinter().flush();
                    return;
                }

                // Test if the parameter passed is a timetag
                try
                {
                    int tt = Integer.parseInt(argString);
                    for (Wme wme : agent.getAllWmesInRete())
                    {
                        // TODO: Print full object, not just wme
                        if (wme.getTimetag() == tt)
                        {
                            agent.getPrinter().startNewLine();
                            agent.getPrinter().print(wme.toString());
                            agent.getPrinter().flush();
                            return;
                        }
                    }
                    agent.getPrinter().startNewLine().print("No wme " + tt + " in working memory.");
                    return;
                }
                catch (NumberFormatException ignored)
                {
                }

                // Test if the parameter passed is a production name
                // Default is to print full production
                if (!printOnlyName)
                {
                    printFullProd = true;
                }

                agent.getPrinter().startNewLine();
                Production p = agent.getProductions().getProduction(argString);
                if (p != null)
                {
                    do_print_for_production(p);
                }
                else
                {
                    agent.getPrinter().print("No production named " + argString);
                }
                agent.getPrinter().flush();
                return;
            }

            if (printAll)
            {
                printChunks = true;
                printDefaults = true;
                printJustify = true;
                printUserProds = true;
                printTemplates = true;
            }

            agent.getPrinter().startNewLine();
            for (Production p : collectProductions())
            {
                do_print_for_production(p);
            }

            if (printRLRules)
            {
                for (Production p : agent.getProductions().getProductions(null))
                {
                    if (p.rlRuleInfo != null)
                    {
                        do_print_for_production(p);
                    }
                }
            }
            agent.getPrinter().flush();
        }

        private void do_print_for_production(Production prod)
        {
            final Printer p = agent.getPrinter();

            if (printFilename)
            {
                if (printFullProd)
                {
                    p.print("# source file: ", prod.getLocation());
                }

                p.print("%s", prod.getLocation());

                if (printFullProd)
                    p.print("\n");
                else
                    p.print(": ");
            }

            if (printFullProd)
            {
                prod.print(p, printInternalForm);
            }
            else
            {
                p.print("%s ", prod.getName());

                if (prod.rlRuleInfo != null)
                {
                    // Do extra logging if this agent is in delta bar delta mode.
                    if (agent.getProperties().get(ReinforcementLearningParams.DECAY_MODE)
                            == ReinforcementLearningParams.DecayMode.delta_bar_delta_decay)
                    {
                      p.print(" %y", agent.getSymbols().createDouble(prod.rlRuleInfo.rl_delta_bar_delta_beta ) );
                      p.print(" %y", agent.getSymbols().createDouble(prod.rlRuleInfo.rl_delta_bar_delta_h ) );
                    }
                    p.print("%f  ", prod.rlRuleInfo.rl_update_count);
                    p.print("%s", prod.getFirstAction().asMakeAction().referent);
                }
            }
            p.print("\n");
        }

        private List<Production> collectProductions()
        {
            final ProductionManager pm = agent.getProductions();
            final List<Production> result = new ArrayList<Production>();

            if (printChunks)
                result.addAll(pm.getProductions(ProductionType.CHUNK));
            if (printUserProds)
                result.addAll(pm.getProductions(ProductionType.USER));
            if (printDefaults)
                result.addAll(pm.getProductions(ProductionType.DEFAULT));
            if (printTemplates)
                result.addAll(pm.getProductions(ProductionType.TEMPLATE));
            if (printJustify)
                result.addAll(pm.getProductions(ProductionType.JUSTIFICATION));

            return result;
        }
    }
    
    protected boolean checkDepth(int depth)
    {
        if (depth <= 0)
        {
            agent.getPrinter().startNewLine().print("depth must be greater than 0");
            return false;
        }
        return true;
    }
    
    public void setDefaultDepth(int depth)
    {
        if (checkDepth(depth))
        {
            defaultDepth = depth;
        }
    }
    
    public int getDefaultDepth()
    {
        return defaultDepth;
    }
}
