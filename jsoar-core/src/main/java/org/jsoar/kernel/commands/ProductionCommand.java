package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionFinder;
import org.jsoar.kernel.ProductionFinder.Options;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.StringTools;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.PicocliSoarCommand;

import com.google.common.base.Joiner;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * This is the implementation of the "production" command.
 * 
 * @author austin.brehob
 */
public class ProductionCommand extends PicocliSoarCommand
{
    
    public ProductionCommand(Agent agent)
    {
        super(agent, new ProductionC(agent));
    }
    
    @Override
    public ProductionC getCommand()
    {
        return (ProductionC) super.getCommand();
    }
    
    @Command(name = "production", description = "Commands related to altering and printing production info", subcommands = { HelpCommand.class,
            ProductionCommand.Break.class,
            ProductionCommand.Excise.class,
            ProductionCommand.Find.class,
            ProductionCommand.FiringCounts.class,
            ProductionCommand.Matches.class,
            ProductionCommand.MemoryUsage.class,
            ProductionCommand.OptimizeAttribute.class,
            ProductionCommand.Watch.class })
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
                            "=======================================================\n" +
                            "Not yet implemented\n");
        }
    }
    
    @Command(name = "break", description = "Stops the Soar decision cycle when the given rule fires", subcommands = { HelpCommand.class })
    static public class Break implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        @Option(names = { "-c", "--clear" }, description = "Clear :interrupt flag from a production")
        String prodToClear;
        
        @Option(names = { "-p", "--print" }, defaultValue = "false", description = "Print which production rules "
                + "have had their :interrupt flags set")
        boolean printBreaks;
        
        @Option(names = { "-s", "--set" }, description = "Set interrupt flag on a production rule")
        String prodToBreak;
        
        @Parameters(index = "0", arity = "0..1", description = "Production rule on which to set :interrupt flag")
        private String prodName;
        
        @Override
        public void run()
        {
            String name = null;
            boolean flag = false;
            
            // Determine the production on which to set/clear the :interrupt flag
            if(prodToClear != null)
            {
                name = prodToClear;
            }
            else if(prodToBreak != null)
            {
                name = prodToBreak;
                flag = true;
            }
            else if(prodName != null)
            {
                name = prodName;
                flag = true;
            }
            
            if(name != null)
            {
                Production p = parent.agent.getProductions().getProduction(name);
                if(p != null)
                {
                    p.setBreakpointEnabled(flag);
                }
                else
                {
                    parent.agent.getPrinter().startNewLine().print("No production named '" + name + "'");
                }
            }
            else
            {
                parent.agent.getPrinter().print(Joiner.on('\n').join(collectAndSortRuleNames()));
            }
        }
        
        private List<String> collectAndSortRuleNames()
        {
            ProductionManager pm = parent.agent.getProductions();
            final List<String> result = new ArrayList<String>();
            
            for(Production p : pm.getProductions(null))
            {
                if(p.isBreakpointEnabled())
                {
                    result.add(p.getName());
                }
            }
            Collections.sort(result);
            return result;
        }
    }
    
    @Command(name = "excise", description = "Excises specified productions", subcommands = { HelpCommand.class })
    static public class Excise implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        @Option(names = { "-a", "--all" }, defaultValue = "false", description = "Remove all productions from "
                + "memory and perform an init-soar command")
        boolean exciseAll;
        
        @Option(names = { "-c", "--chunks" }, defaultValue = "false", description = "Remove all chunks (learned productions) from memory")
        boolean exciseChunks;
        
        @Option(names = { "-d", "--default" }, defaultValue = "false", description = "Remove all default productions from memory")
        boolean exciseDefault;
        
        @Option(names = { "-n", "--never-fired" }, defaultValue = "false", description = "Excise rules that have a firing count of 0")
        boolean exciseNeverFired;
        
        @Option(names = { "-r", "--rl" }, defaultValue = "false", description = "Excise Soar-RL rules")
        boolean exciseRL;
        
        @Option(names = { "-t", "--task" }, defaultValue = "false", description = "Remove chunks, "
                + "justifications, and user productions from memory")
        boolean exciseTasks;
        
        @Option(names = { "-T", "--templates" }, defaultValue = "false", description = "Excise Soar-RL templates")
        boolean exciseTemplates;
        
        @Option(names = { "-u", "--user" }, defaultValue = "false", description = "Remove all user productions "
                + "(but not chunks or default rules) from memory")
        boolean exciseUser;
        
        @Parameters(index = "0", arity = "0..1", description = "Remove the specific production with this name")
        private String prodName;
        
        @Override
        public void run()
        {
            ProductionManager pm = parent.agent.getProductions();
            
            if(prodName != null)
            {
                // If the production of the given name exists, excise it
                Production p = pm.getProduction(prodName);
                if(p == null)
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
                if(exciseAll)
                {
                    toExcise.addAll(pm.getProductions(null));
                    doInit = true;
                }
                if(exciseChunks)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.CHUNK));
                }
                if(exciseDefault)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.DEFAULT));
                }
                if(exciseNeverFired)
                {
                    for(Production p : pm.getProductions(null))
                    {
                        if(p.getFiringCount() == 0)
                        {
                            toExcise.add(p);
                        }
                    }
                }
                if(exciseRL)
                {
                    final Set<Production> maybeExcise = new LinkedHashSet<Production>();
                    maybeExcise.addAll(pm.getProductions(ProductionType.DEFAULT));
                    maybeExcise.addAll(pm.getProductions(ProductionType.USER));
                    maybeExcise.addAll(pm.getProductions(ProductionType.CHUNK));
                    
                    for(Production p : toExcise)
                    {
                        if(p.rlRuleInfo != null)
                        {
                            toExcise.add(p);
                        }
                    }
                }
                if(exciseTasks)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.CHUNK));
                    toExcise.addAll(pm.getProductions(ProductionType.JUSTIFICATION));
                    toExcise.addAll(pm.getProductions(ProductionType.USER));
                }
                if(exciseTemplates)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.TEMPLATE));
                }
                if(exciseUser)
                {
                    toExcise.addAll(pm.getProductions(ProductionType.USER));
                }
                
                for(Production p : toExcise)
                {
                    pm.exciseProduction(p, false);
                }
                
                if(exciseRL)
                {
                    // cli_excise.cpp:DoExcise
                    Adaptables.adapt(parent.agent, ReinforcementLearning.class).rl_initialize_template_tracking();
                }
                
                // Initialize the agent if the "-a" option is provided
                if(doInit)
                {
                    parent.agent.initialize();
                }
                
                parent.agent.getPrinter().startNewLine().print(String.format(
                        "\n%d production%s excised.", toExcise.size(), toExcise.size() == 1 ? "" : "s"));
            }
        }
    }
    
    @Command(name = "find", description = "Find productions by condition or action patterns", subcommands = { HelpCommand.class })
    static public class Find implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        @Option(names = { "-l", "--lhs" }, defaultValue = "false", description = "Match pattern only "
                + "against the conditions of productions (default)")
        boolean matchLHS;
        
        @Option(names = { "-r", "--rhs" }, defaultValue = "false", description = "Match pattern against the actions of productions")
        boolean matchRHS;
        
        @Option(names = { "-c", "--chunks" }, defaultValue = "false", description = "Look only for chunks that match the pattern")
        boolean matchChunks;
        
        @Option(names = { "-n", "--nochunks" }, defaultValue = "false", description = "Disregard chunks when looking for the pattern")
        boolean matchNoChunks;
        
        @Option(names = { "-s", "--show-bindings" }, defaultValue = "false", description = "Show the bindings associated with a wildcard pattern")
        boolean showBindings;
        
        @Parameters(arity = "1", description = "Any pattern that can appear in productions")
        String[] arrPattern;
        
        @Override
        public void run()
        {
            final ProductionFinder finder = new ProductionFinder(parent.agent);
            finder.options().clear();
            
            if(matchRHS)
            {
                finder.options().add(Options.RHS);
            }
            else
            {
                finder.options().add(Options.LHS);
            }
            
            if(showBindings)
            {
                parent.agent.getPrinter().startNewLine().print("Option not yet supported");
                return;
            }
            
            final String pattern = StringTools.join(Arrays.asList(arrPattern), " ");
            final Collection<Production> productions = collectProductions(matchChunks, matchNoChunks);
            
            try
            {
                List<Production> result = finder.find(pattern, productions);
                printResults(pattern, result);
            }
            catch(ParserException e)
            {
                parent.agent.getPrinter().startNewLine().print(e.getMessage());
            }
        }
        
        private void printResults(final String pattern, List<Production> result)
        {
            final Printer printer = parent.agent.getPrinter();
            printer.startNewLine();
            if(result.isEmpty())
            {
                printer.print("No productions match '%s'\n", pattern);
            }
            else
            {
                for(Production p : result)
                {
                    printer.print("%s\n", p.getName());
                }
            }
        }
        
        // Obtains all productions filtered by chunks, nochunks, or neither
        private Collection<Production> collectProductions(final boolean chunks, final boolean nochunks)
        {
            final Collection<Production> productions = parent.agent.getProductions().getProductions(null).stream()
                    .filter(p -> 
                    {
                        final ProductionType type = p.getType();
                        return (type == ProductionType.CHUNK && chunks) ||
                                (type != ProductionType.CHUNK && nochunks) ||
                                (!chunks && !nochunks);
                    }
                    ).collect(Collectors.toList());
                    
            return productions;
        }
    }
    
    @Command(name = "firing-counts", description = "Print the number of times productions have fired", subcommands = { HelpCommand.class })
    static public class FiringCounts implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        // TODO: Implement these options
        // In CSoar, when multiple options are provided, only productions that fit into all categories
        // are displayed along with their firing counts
        @Option(names = { "-a", "--all" }, defaultValue = "false", description = "Print how many times all productions fired")
        boolean countAll;
        
        @Option(names = { "-c", "--chunks" }, defaultValue = "false", description = "Print how many times chunks (learned rules) fired")
        boolean countChunks;
        
        @Option(names = { "-d", "--default" }, defaultValue = "false", description = "Print how many times default productions fired")
        boolean countDefault;
        
        @Option(names = { "-f", "--fired" }, defaultValue = "false", description = "Prints only rules that have fired")
        boolean countFired;
        
        @Option(names = { "-r", "--rl" }, defaultValue = "false", description = "Print how many times Soar-RL rules fired")
        boolean countRL;
        
        @Option(names = { "-T", "--templates" }, defaultValue = "false", description = "Print how many times Soar-RL templates fired")
        boolean countTemplates;
        
        @Option(names = { "-u", "--user" }, defaultValue = "false", description = "Print how many times user productions fired")
        boolean countUser;
        
        @Parameters(index = "0", arity = "0..1", description = "If an integer, list the top n productions; "
                + "if n is 0, only the productions which haven't fired are listed. "
                + "If not an integer, print how many times a specific production has fired.")
        private String param;
        
        @Override
        public void run()
        {
            Integer topN = null;
            String prodName = null;
            
            // Determine if the parameter provided is an integer or string
            if(param != null)
            {
                try
                {
                    topN = Integer.valueOf(param);
                }
                catch(NumberFormatException e)
                {
                    prodName = param;
                }
            }
            
            if(topN != null)
            {
                if(topN == 0)
                {
                    // Find and print all productions that have not fired yet
                    List<Production> productionsNotFired = new ArrayList<Production>();
                    for(Production p : parent.agent.getProductions().getProductions(null))
                    {
                        if(p.getFiringCount() == 0)
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
            else if(prodName != null)
            {
                printSingleProduction(prodName);
            }
            else
            {
                if(countChunks || countDefault || countFired || countRL || countTemplates || countUser)
                {
                    parent.agent.getPrinter().startNewLine().print("Option(s) not "
                            + "implemented yet. Displaying all firing counts:");
                }
                printTopProductions(Integer.MAX_VALUE);
            }
        }
        
        private void printResults(List<Production> productions, int n)
        {
            if(productions.size() > 0 && n > 0)
            {
                Production p = productions.get(0);
                final Printer printer = parent.agent.getPrinter();
                
                // don't print a newline for the first one, but do print one before each subsequent one
                printer.print("%5d:  %s", p.getFiringCount(), p.getName());
                
                for(int i = 1; i < n && i < productions.size(); ++i)
                {
                    p = productions.get(i);
                    printer.startNewLine().print("%5d:  %s", p.getFiringCount(), p.getName());
                }
            }
        }
        
        private void printSingleProduction(String name)
        {
            final Production p = parent.agent.getProductions().getProduction(name);
            if(p == null)
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
                    if(d < 0)
                    {
                        return -1;
                    }
                    else if(d > 0)
                    {
                        return 1;
                    }
                    return 0;
                }
            });
            printResults(prods, n);
        }
    }
    
    @Command(name = "matches", description = "Print the list of productions that will "
            + "retract or fire in the next propose or apply phase", subcommands = { HelpCommand.class })
    static public class Matches implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        @Option(names = { "-c", "--counts" }, defaultValue = "false", description = "Same as default implementation")
        boolean includeCounts;
        
        @Option(names = { "-n", "--names" }, defaultValue = "false", description = "Same as default implementation")
        boolean includeNames;
        
        @Option(names = { "-t", "--timetags" }, defaultValue = "false", description = "Also print the "
                + "timetags of the wmes at the first failing condition")
        boolean includeTimetags;
        
        @Option(names = { "-w", "--wmes" }, defaultValue = "false", description = "Also print the full wmes, not just "
                + "the timetags, at the first failing condition")
        boolean includeWmes;
        
        @Option(names = { "-a", "--assertions" }, defaultValue = "false", description = "List only productions about to fire")
        boolean onlyAssertions;
        
        @Option(names = { "-r", "--retractions" }, defaultValue = "false", description = "List only productions about to retract")
        boolean onlyRetractions;
        
        @Option(names = { "-i", "--internal" }, defaultValue = "false", description = "Also print some internal "
                + "information when a production name is provided")
        boolean includeInternalInfo;
        
        @Parameters(index = "0", arity = "0..1", description = "Print partial match information for the named production")
        private String prodName;
        
        @Override
        public void run()
        {
            WmeTraceType wmeTraceType = WmeTraceType.NONE;
            EnumSet<MatchSetTraceType> mstt = EnumSet.allOf(MatchSetTraceType.class);
            
            if(includeWmes)
            {
                wmeTraceType = WmeTraceType.FULL;
            }
            else if(includeTimetags)
            {
                wmeTraceType = WmeTraceType.TIMETAG;
            }
            
            if(onlyAssertions)
            {
                mstt.clear();
                mstt.add(MatchSetTraceType.MS_ASSERT);
            }
            else if(onlyRetractions)
            {
                mstt.clear();
                mstt.add(MatchSetTraceType.MS_RETRACT);
            }
            
            if(prodName != null)
            {
                // Print partial matches for the named production
                Production p = parent.agent.getProductions().getProduction(prodName);
                if(p == null)
                {
                    parent.agent.getPrinter().startNewLine().print("No production '" + prodName + "'");
                }
                else if(p.getReteNode() == null)
                {
                    parent.agent.getPrinter().startNewLine().print("Production '" + prodName + "' is not in rete");
                }
                else
                {
                    p.printPartialMatches(parent.agent.getPrinter(), wmeTraceType, includeInternalInfo);
                }
            }
            else
            {
                parent.agent.printMatchSet(parent.agent.getPrinter(), wmeTraceType, mstt);
                parent.agent.getPrinter().flush();
            }
        }
    }
    
    @Command(name = "memory-usage", description = "Print memory usage for partial matches", subcommands = { HelpCommand.class })
    static public class MemoryUsage implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        @Option(names = { "-c", "--chunks" }, defaultValue = "false", description = "Print memory usage of chunks")
        boolean filterByChunks;
        
        @Option(names = { "-d", "--default" }, defaultValue = "false", description = "Print memory usage of default productions")
        boolean filterByDefault;
        
        @Option(names = { "-j", "--justifications" }, defaultValue = "false", description = "Print memory usage of justifications")
        boolean filterByJustify;
        
        @Option(names = { "-T", "--templates" }, defaultValue = "false", description = "Print how many times Soar-RL templates fired")
        boolean filterByTemplates;
        
        @Option(names = { "-u", "--user" }, defaultValue = "false", description = "Print memory usage of user-defined productions")
        boolean filterByUser;
        
        @Parameters(index = "0", arity = "0..1", description = "If an integer, list the top n productions according to "
                + "memory usage. If not an integer, print memory usage for a specific production.")
        private String param;
        
        @Override
        public void run()
        {
            Integer topN = null;
            String prodName = null;
            
            Production single = null;
            int count = Integer.MAX_VALUE;
            final EnumSet<ProductionType> types = EnumSet.noneOf(ProductionType.class);
            
            // Determine if the parameter provided is an integer or string
            if(param != null)
            {
                try
                {
                    topN = Integer.valueOf(param);
                }
                catch(NumberFormatException e)
                {
                    prodName = param;
                }
            }
            
            if(topN != null)
            {
                count = topN;
                if(count < 1)
                {
                    parent.agent.getPrinter().startNewLine().print("Count argument "
                            + "must be greater than 0, got " + count);
                    return;
                }
            }
            else if(prodName != null)
            {
                single = parent.agent.getProductions().getProduction(prodName);
                if(single == null)
                {
                    parent.agent.getPrinter().startNewLine().print("No production named '" + prodName + "'");
                    return;
                }
            }
            else if(filterByDefault)
            {
                types.add(ProductionType.DEFAULT);
            }
            else if(filterByChunks)
            {
                types.add(ProductionType.CHUNK);
            }
            else if(filterByUser)
            {
                types.add(ProductionType.USER);
            }
            else if(filterByJustify)
            {
                types.add(ProductionType.JUSTIFICATION);
            }
            else if(filterByTemplates)
            {
                types.add(ProductionType.TEMPLATE);
            }
            
            if(single != null)
            {
                printResults(Arrays.asList(single), 1);
            }
            else
            {
                if(types.isEmpty())
                {
                    types.addAll(EnumSet.allOf(ProductionType.class));
                }
                printTopProductions(types, count);
            }
        }
        
        private void printResults(List<Production> productions, int n)
        {
            final Printer printer = parent.agent.getPrinter();
            for(int i = 0; i < n && i < productions.size(); ++i)
            {
                final Production p = productions.get(i);
                printer.startNewLine().print("%5d:  %s", p.getReteTokenCount(), p.getName());
            }
        }
        
        private void printTopProductions(EnumSet<ProductionType> types, int n)
        {
            final List<Production> prods = new ArrayList<Production>();
            for(ProductionType type : types)
            {
                prods.addAll(parent.agent.getProductions().getProductions(type));
            }
            
            Collections.sort(prods, new Comparator<Production>()
            {
                @Override
                public int compare(Production o1, Production o2)
                {
                    return o2.getReteTokenCount() - o1.getReteTokenCount();
                }
            });
            printResults(prods, n);
        }
    }
    
    @Command(name = "optimize-attribute", description = "Declare a symbol to be multi-attributed so "
            + "the rule can be matched more efficiently", subcommands = { HelpCommand.class })
    static public class OptimizeAttribute implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        @Parameters(index = "0", description = "Any Soar attribute")
        private String attribute;
        
        @Parameters(index = "1", description = "Estimate of degree of simultaneous values for attribute")
        private Integer degree;
        
        @Override
        public void run()
        {
            final StringSymbol attr = parent.agent.getSymbols().createString(attribute);
            final int cost = degree;
            parent.agent.getMultiAttributes().setCost(attr, cost);
        }
    }
    
    @Command(name = "watch", description = "Alters the set of watched productions", subcommands = { HelpCommand.class })
    static public class Watch implements Runnable
    {
        @ParentCommand
        ProductionC parent; // injected by picocli
        
        @Option(names = { "on", "-e", "--on", "--enable" }, description = "Enables watching of given productions")
        List<String> productionsToEnable;
        
        @Option(names = { "off", "-d", "--off", "--disable" }, description = "Disables watching of given productions")
        List<String> productionsToDisable;
        
        @Override
        public void run()
        {
            if(productionsToEnable == null && productionsToDisable == null)
            {
                List<String> tracedRuleNames = collectAndSortTracedRuleNames();
                if(tracedRuleNames.size() == 0)
                {
                    parent.agent.getPrinter().startNewLine().print("No watched productions found.");
                }
                else
                {
                    parent.agent.getPrinter().print(Joiner.on('\n').join(collectAndSortTracedRuleNames()));
                }
            }
            if(productionsToEnable != null)
            {
                for(String name : productionsToEnable)
                {
                    final Production p = parent.agent.getProductions().getProduction(name);
                    if(p != null)
                    {
                        p.setTraceFirings(true);
                    }
                    else
                    {
                        parent.agent.getPrinter().startNewLine().print("No production named '" + name + "'");
                    }
                }
            }
            if(productionsToDisable != null)
            {
                for(String name : productionsToDisable)
                {
                    final Production p = parent.agent.getProductions().getProduction(name);
                    if(p != null)
                    {
                        p.setTraceFirings(false);
                    }
                    else
                    {
                        parent.agent.getPrinter().startNewLine().print("No production named '" + name + "'");
                    }
                }
            }
        }
        
        private List<String> collectAndSortTracedRuleNames()
        {
            final List<String> result = new ArrayList<String>();
            for(Production p : parent.agent.getProductions().getProductions(null))
            {
                if(p.isTraceFirings())
                {
                    result.add(p.getName());
                }
            }
            Collections.sort(result);
            return result;
        }
    }
}
