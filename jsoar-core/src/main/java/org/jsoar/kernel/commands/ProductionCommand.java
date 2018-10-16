package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * This is the implementation of the "production" command.
 * @author austin.brehob
 */
public class ProductionCommand implements SoarCommand
{
    private Agent agent;
    
    public ProductionCommand(Agent agent)
    {
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new ProductionC(agent), args);
        
        return "";
    }
    
    @Command(name="production", description="Commands related to altering and printing production info",
            subcommands={HelpCommand.class,
                         Excise.class,
                         FiringCounts.class})
    static public class ProductionC implements Runnable
    {
        private Agent agent;
        
        public ProductionC(Agent agent)
        {
            this.agent = agent;
        }
        
        // TODO Provide summary
        @Override
        public void run()
        {
            agent.getPrinter().startNewLine().print(
                    "=======================================================\n" +
                    "-                     Productions                     -\n" +
                    "=======================================================\n"
            );
        }
    }
    
    @Command(name="excise", description="Excises specified productions", subcommands={HelpCommand.class} )
    static public class Excise implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        @Option(names={"-a", "--all"}, description="Remove all productions from "
                + "memory and perform an init-soar command")
        boolean exciseAll = false;
        
        @Option(names={"-c", "--chunks"}, description="Remove all chunks (learned productions) from memory")
        boolean exciseChunks = false;
        
        @Option(names={"-d", "--default"}, description="Remove all default productions from memory")
        boolean exciseDefault = false;
        
        @Option(names={"-n", "--never-fired"}, description="Excise rules that have a firing count of 0")
        boolean exciseNeverFired = false;
        
        @Option(names={"-r", "--rl"}, description="Excise Soar-RL rules")
        boolean exciseRL = false;
        
        @Option(names={"-t", "--task"}, description="Remove chunks, "
                + "justifications, and user productions from memory")
        boolean exciseTasks = false;
        
        @Option(names={"-T", "--templates"}, description="Excise Soar-RL templates")
        boolean exciseTemplates = false;
        
        @Option(names={"-u", "--user"}, description="Remove all user productions "
                + "(but not chunks or default rules) from memory")
        boolean exciseUser = false;
        
        @Parameters(index="0", arity="0..1", description="Remove the specific production with this name")
        private String prodName = null;

        @Override
        public void run()
        {
            ProductionManager pm = parent.agent.getProductions();
            
            if (prodName != null)
            {
                // If the production of the given name exists, excise it
                Production p = pm.getProduction(prodName);
                if (p == null)
                {
                    parent.agent.getPrinter().startNewLine().print("No production named '" + prodName + "'");
                }
                else
                {
                    pm.exciseProduction(p, false);
                    parent.agent.getPrinter().startNewLine().print("1 production excised");
                }
            }
            else
            {
                final Set<Production> toExcise = new LinkedHashSet<Production>();
                boolean doInit = false;
                
                // Determine which productions to excise based on the options provided
                if (exciseAll)
                {
                    toExcise.addAll(pm.getProductions(null));
                    doInit = true;
                }
                if (exciseChunks)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.CHUNK));
                }
                if (exciseDefault)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.DEFAULT));
                }
                if (exciseNeverFired)
                {
                    for (Production p : pm.getProductions(null))
                    {
                        if (p.getFiringCount() == 0)
                        {
                            toExcise.add(p);
                        }
                    }
                }
                if (exciseRL)
                {
                    final Set<Production> maybeExcise = new LinkedHashSet<Production>();
                    maybeExcise.addAll(pm.getProductions(ProductionType.DEFAULT));
                    maybeExcise.addAll(pm.getProductions(ProductionType.USER));
                    maybeExcise.addAll(pm.getProductions(ProductionType.CHUNK));
                    
                    for (Production p : toExcise)
                    {
                        if (p.rlRuleInfo != null)
                        {
                            toExcise.add(p);
                        }
                    }
                }
                if (exciseTasks)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.CHUNK));
                    toExcise.addAll(pm.getProductions(ProductionType.JUSTIFICATION));
                    toExcise.addAll(pm.getProductions(ProductionType.USER));
                }
                if (exciseTemplates)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.TEMPLATE));
                }
                if (exciseUser)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.USER));
                }
                
                for (Production p : toExcise)
                {
                    pm.exciseProduction(p, false);
                }
                
                if (exciseRL)
                {
                    // cli_excise.cpp:DoExcise
                    Adaptables.adapt(parent.agent, ReinforcementLearning.class).rl_initialize_template_tracking();
                }
                
                // Initialize the agent if the "-a" option is provided
                if (doInit)
                {
                    parent.agent.initialize();
                }
                
                parent.agent.getPrinter().startNewLine().print(String.format(
                        "\n%d production%s excised.", toExcise.size(), toExcise.size() == 1 ? "" : "s"));
            }
        }
    }
    
    @Command(name="firing-counts", description="Print the number of times productions have fired",
            subcommands={HelpCommand.class} )
    static public class FiringCounts implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        // TODO: Implement these options
        // In CSoar, when multiple options are provided, only productions that fit into all categories
        // are displayed along with their firing counts
        @Option(names={"-a", "--all"}, description="Print how many times all productions fired")
        boolean countAll = false;
        
        @Option(names={"-c", "--chunks"}, description="Print how many times chunks (learned rules) fired")
        boolean countChunks = false;
        
        @Option(names={"-d", "--default"}, description="Print how many times default productions fired")
        boolean countDefault = false;
        
        @Option(names={"-f", "--fired"}, description="Prints only rules that have fired")
        boolean countFired = false;
        
        @Option(names={"-r", "--rl"}, description="Print how many times Soar-RL rules fired")
        boolean countRL = false;
        
        @Option(names={"-T", "--templates"}, description="Print how many times Soar-RL templates fired")
        boolean countTemplates = false;
        
        @Option(names={"-u", "--user"}, description="Print how many times user productions fired")
        boolean countUser = false;
        
        @Parameters(index="0", arity="0..1", description="If an integer, list the top n productions; "
                + "if n is 0, only the productions which haven't fired are listed. "
                + "If not an integer, print how many times a specific production has fired.")
        private String param = null;
        
        @Override
        public void run()
        {
            Integer topN = null;
            String prodName = null;
            
            // Determine if the parameter provided is an integer or string
            if (param != null)
            {
                try
                {
                    topN = Integer.valueOf(param);
                }
                catch (NumberFormatException e)
                {
                    prodName = param;
                }
            }
            
            if (topN != null)
            {
                if (topN == 0)
                {
                    // Find and print all productions that have not fired yet
                    List<Production> productionsNotFired = new ArrayList<Production>();
                    for (Production p : parent.agent.getProductions().getProductions(null))
                    {
                        if (p.getFiringCount() == 0)
                        {
                            productionsNotFired.add(p);
                        }
                    }
                    printResults(productionsNotFired, Integer.MAX_VALUE);
                }
                else
                {
                    printTopProductions(topN);
                }
            }
            else if (prodName != null)
            {
                printSingleProduction(prodName);
            }
            else
            {
                if (countChunks || countDefault || countFired || countRL || countTemplates || countUser)
                {
                    parent.agent.getPrinter().startNewLine().print("Option(s) not "
                            + "implemented yet. Displaying all firing counts:");
                }
                printTopProductions(Integer.MAX_VALUE);
            }
        }
        
        private void printResults(List<Production> productions, int n)
        {
            final Printer printer = parent.agent.getPrinter();
            for (int i = 0; i < n && i < productions.size(); ++i)
            {
                final Production p = productions.get(i);
                printer.startNewLine().print("%5d:  %s", p.getFiringCount(), p.getName());
            }
        }
        
        private void printSingleProduction(String name)
        {
            final Production p = parent.agent.getProductions().getProduction(name);
            if (p == null)
            {
                parent.agent.getPrinter().startNewLine().print("No production named '" + name + "'");
            }
            else
            {
                printResults(Arrays.asList(p), 1);
            }
        }
        
        private void printTopProductions(int n)
        {
            final List<Production> prods = parent.agent.getProductions().getProductions(null);
            Collections.sort(prods, new Comparator<Production>()
            {
                @Override
                public int compare(Production o1, Production o2)
                {
                    final long d = o2.getFiringCount() - o1.getFiringCount();
                    if (d < 0)
                    {
                        return -1;
                    }
                    else if (d > 0)
                    {
                        return 1;
                    }
                    return 0;
                }
            });
            printResults(prods, n);
        }
    }
}

