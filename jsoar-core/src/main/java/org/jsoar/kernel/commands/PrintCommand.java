/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WorkingMemoryPrinter;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.OptionProcessor;
import org.jsoar.util.commands.SoarCommand;

import com.google.common.collect.Lists;

/**
 * http://winter.eecs.umich.edu/soarwiki/Print
 * 
 * @author ray
 * @author voigtjr
 */
public class PrintCommand implements SoarCommand
{
    private static enum Options
    {
        // EXACT is implied by the way the printer is implemented.
        ALL, CHUNKS, DEFAULTS, DEPTH, FILE_NAME, FULL, INTERNAL, JUSTIFICATIONS, 
        NAME, OPERATORS, RL, STACK, STATES, TEMPLATE, TREE, USER, VARPRINT;
    }
    
    private final Agent agent;
    private final WorkingMemoryPrinter wmp = new WorkingMemoryPrinter();
    private int defaultDepth = 1;
    private final OptionProcessor<Options> options = OptionProcessor.create();
    
    private int depth = 1;
    
    public PrintCommand(Agent agent)
    {
        this.agent = agent;
        
        options.newOption(Options.ALL, "all").register();
        options.newOption(Options.CHUNKS, "chunks").register();
        options.newOption(Options.DEFAULTS, "Defaults").register();
        options.newOption(Options.DEPTH, "depth").requiredArg().register();
        options.newOption(Options.FILE_NAME, "Filename").register();
        options.newOption(Options.FULL, "full").register();
        options.newOption(Options.INTERNAL, "internal").register();
        options.newOption(Options.JUSTIFICATIONS, "justifications").register();
        options.newOption(Options.NAME, "name").register();
        options.newOption(Options.OPERATORS, "operators").register();
        options.newOption(Options.RL, "rl").register();
        options.newOption(Options.STACK, "stack").register();
        options.newOption(Options.STATES, "States").register();
        options.newOption(Options.TEMPLATE, "Template").register();
        options.newOption(Options.TREE, "tree").register();
        options.newOption(Options.USER, "user").register();
        options.newOption(Options.VARPRINT, "varprint").register();
    }
    
    public void setDefaultDepth(int depth)
    {
        if(depth <= 0)
        {
            throw new IllegalArgumentException("depth must be greater than 0");
        }
        this.defaultDepth = depth;
    }
    
    public int getDefaultDepth()
    {
        return this.defaultDepth;
    }
    
    private List<Production> collectProductions()
    {
        final ProductionManager pm = agent.getProductions();
        final List<Production> result = new ArrayList<Production>();
        
        if(options.has(Options.CHUNKS)) result.addAll(pm.getProductions(ProductionType.CHUNK));
        if(options.has(Options.USER)) result.addAll(pm.getProductions(ProductionType.USER));
        if(options.has(Options.DEFAULTS)) result.addAll(pm.getProductions(ProductionType.DEFAULT));
        if(options.has(Options.TEMPLATE)) result.addAll(pm.getProductions(ProductionType.TEMPLATE));
        if(options.has(Options.JUSTIFICATIONS)) result.addAll(pm.getProductions(ProductionType.JUSTIFICATION));

        return result;
    }
    
    @Override
    public String execute(String[] args) throws SoarException
    {
        agent.getPrinter().startNewLine();
        
        this.depth = defaultDepth;
        List<String> nonOpts = options.process(Lists.newArrayList(args));
        
        if (options.has(Options.DEPTH))
        {
            try
            {
                depth = Integer.parseInt(options.getArgument(Options.DEPTH));
                if(depth < 0)
                {
                    throw new SoarException("--depth must be positive");
                }
            }
            catch(NumberFormatException e)
            {
                throw new SoarException("Invalid --depth value: " + options.getArgument(Options.DEPTH));
            }
        }
        
        if (options.has(Options.VARPRINT))
            throw new SoarException("--varprint not implemented yet");
        
        // New in Soar 8.6.3: if no args or options given, print all prods
        if(args.length == 1)
            options.set(Options.ALL);

        if(options.has(Options.STACK))
        {
            agent.printStackTrace(options.has(Options.STATES), options.has(Options.OPERATORS));
            agent.getPrinter().print("\n").flush();
            return "";
        }
        
        if (!nonOpts.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String arg : nonOpts)
            {
                if (first)
                    first = false;
                else
                    sb.append(" ");
                sb.append(arg); 
            }
            String argString = sb.toString();

            // symbol? pattern?
            Symbol arg = agent.readIdentifierOrContextVariable(argString);
            if(arg != null || argString.charAt(0) == '(')
            {
                agent.getPrinter().startNewLine();
                wmp.setInternal(options.has(Options.INTERNAL));
                
                // these are ignored if pattern
                wmp.setDepth(depth);
                wmp.setTree(options.has(Options.TREE));
                
                try {
                    wmp.print(agent, agent.getPrinter(), arg, argString);
                } catch(Exception e) {
                    throw new SoarException(e.toString());
                }

                agent.getPrinter().flush();
                return "";
            }
            
            // timetag?
            try 
            {
                int tt = Integer.parseInt(argString);
                // TODO: make this less naive
                for (Wme wme : agent.getAllWmesInRete())
                {
                    if (wme.getTimetag() == tt)
                    {
                        agent.getPrinter().startNewLine();
                        agent.getPrinter().print(wme.toString());
                        agent.getPrinter().flush();
                        return "";
                    }
                }
                throw new SoarException("No wme " + tt + " in working memory.");
            } 
            catch (NumberFormatException ignored)
            {
            }
            
            // production?
            // Default with arg is full print (productions)
            if (!options.has(Options.NAME))
                options.set(Options.FULL);

            agent.getPrinter().startNewLine();
            Production p = agent.getProductions().getProduction(argString);
            if(p != null)
                do_print_for_production(p);
            else
                agent.getPrinter().print("No production named " + argString);
            agent.getPrinter().flush();
            return "";
        }
        
        if (options.has(Options.ALL))
        {
            options.set(Options.CHUNKS);
            options.set(Options.DEFAULTS);
            options.set(Options.JUSTIFICATIONS);
            options.set(Options.USER);
            options.set(Options.TEMPLATE);
        }
        
        agent.getPrinter().startNewLine();
        for(Production p : collectProductions())
        {
            do_print_for_production(p);
        }
        
        if(options.has(Options.RL))
        {
            for(Production p : agent.getProductions().getProductions(null))
            {
                if (p.rl_rule)
                    do_print_for_production(p);
            }
        }
        agent.getPrinter().flush();
        return "";
    }
    
    private void do_print_for_production(Production prod)
    {
        final Printer p = agent.getPrinter();
        
        if (options.has(Options.FILE_NAME))
        {
            if (options.has(Options.FULL))
                p.print("# source file: ", prod.getLocation());
            
            p.print("%s", prod.getLocation());

            if (options.has(Options.FULL))
                p.print("\n");
            else
                p.print(": ");
        }

        if (options.has(Options.FULL))
            prod.print(p, options.has(Options.INTERNAL));
        else
        {
            p.print("%s ", prod.getName());

            if (prod.rl_rule)
            {
                p.print("%f  ", prod.rl_update_count);
                p.print("%s", prod.action_list.asMakeAction().referent);
            }
        }
        p.print("\n");
    }
    
}