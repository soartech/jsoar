package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import com.google.common.base.Joiner;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/**
 * This is the implementation of the "production-watch" command.
 * @author austin.brehob
 */
public class ProductionWatchCommand implements SoarCommand
{
    private Agent agent;
    
    public ProductionWatchCommand(Agent agent)
    {
        this.agent = agent;
    }
    
    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new ProductionWatch(agent), args);
        
        return "";
    }
    
    @Command(name="production-watch", description="Alters the set of watched productions",
            subcommands={HelpCommand.class})
    static public class ProductionWatch implements Runnable
    {
        private Agent agent;
        
        public ProductionWatch(Agent agent)
        {
            this.agent = agent;
        }
        
        @Option(names={"on", "-e", "--on", "--enable"}, description="Enables watching of given productions")
        List<String> productionsToEnable = null;
        
        @Option(names={"off", "-d", "--off", "--disable"}, description="Disables watching of given productions")
        List<String> productionsToDisable = null;

        @Override
        public void run()
        {
            if (productionsToEnable == null && productionsToDisable == null)
            {
                List<String> tracedRuleNames = collectAndSortTracedRuleNames();
                if (tracedRuleNames.size() == 0)
                {
                    agent.getPrinter().startNewLine().print("No watched productions found.");
                }
                else
                {
                    agent.getPrinter().startNewLine().print(Joiner.on('\n').join(collectAndSortTracedRuleNames()));
                }
            }
            if (productionsToEnable != null)
            {
                for (String name : productionsToEnable)
                {
                    final Production p = agent.getProductions().getProduction(name);
                    if (p != null)
                    {
                        p.setTraceFirings(true);
                    }
                    else
                    {
                        agent.getPrinter().startNewLine().print("No production named '" + name + "'");
                    }
                }
            }
            if (productionsToDisable != null)
            {
                for (String name : productionsToDisable)
                {
                    final Production p = agent.getProductions().getProduction(name);
                    if (p != null)
                    {
                        p.setTraceFirings(false);
                    }
                    else
                    {
                        agent.getPrinter().startNewLine().print("No production named '" + name + "'");
                    }
                }
            }
        }
        
        private List<String> collectAndSortTracedRuleNames()
        {
            final List<String> result = new ArrayList<String>();
            for(Production p : agent.getProductions().getProductions(null))
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
