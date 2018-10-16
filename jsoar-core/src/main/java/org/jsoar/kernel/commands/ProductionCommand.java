package org.jsoar.kernel.commands;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
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
                         Excise.class})
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
                
                if (doInit)
                {
                    parent.agent.initialize();
                }
                
                parent.agent.getPrinter().startNewLine().print(String.format(
                        "\n%d production%s excised.", toExcise.size(), toExcise.size() == 1 ? "" : "s"));
            }
        }
    }
}
