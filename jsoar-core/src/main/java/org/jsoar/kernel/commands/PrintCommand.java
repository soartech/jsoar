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
    private final Agent agent;
    private final WorkingMemoryPrinter wmp = new WorkingMemoryPrinter();
    private final OptionProcessor options = OptionProcessor.create();

    private int defaultDepth = 1;
    private int depth = 1;
    
    private final String ALL = "all";
    private final String CHUNKS = "chunks";
    private final String DEFAULTS = "Defaults";
    private final String DEPTH = "depth";
    private final String FILE_NAME = "Filename";
    private final String FULL = "full";
    private final String INTERNAL = "internal";
    private final String JUSTIFICATIONS = "justifications";
    private final String NAME = "name";
    private final String OPERATORS = "operators";
    private final String RL = "rl";
    private final String STACK = "stack";
    private final String STATES = "States";
    private final String TEMPLATE = "Template";
    private final String TREE = "tree";
    private final String USER = "user";
    private final String VARPRINT = "varprint";

    public PrintCommand(Agent agent)
    {
        this.agent = agent;
        
        options.newOption(ALL).register();
        options.newOption(CHUNKS).register();
        options.newOption(DEFAULTS).register();
        options.newOption(DEPTH).requiredArg().register();
        options.newOption(FILE_NAME).register();
        options.newOption(FULL).register();
        options.newOption(INTERNAL).register();
        options.newOption(JUSTIFICATIONS).register();
        options.newOption(NAME).register();
        options.newOption(OPERATORS).register();
        options.newOption(RL).register();
        options.newOption(STACK).register();
        options.newOption(STATES).register();
        options.newOption(TEMPLATE).register();
        options.newOption(TREE).register();
        options.newOption(USER).register();
        options.newOption(VARPRINT).register();
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
        
        if(options.has(CHUNKS)) result.addAll(pm.getProductions(ProductionType.CHUNK));
        if(options.has(USER)) result.addAll(pm.getProductions(ProductionType.USER));
        if(options.has(DEFAULTS)) result.addAll(pm.getProductions(ProductionType.DEFAULT));
        if(options.has(TEMPLATE)) result.addAll(pm.getProductions(ProductionType.TEMPLATE));
        if(options.has(JUSTIFICATIONS)) result.addAll(pm.getProductions(ProductionType.JUSTIFICATION));

        return result;
    }
    
    @Override
    public String execute(String[] args) throws SoarException
    {
        agent.getPrinter().startNewLine();
        
        this.depth = defaultDepth;
        List<String> nonOpts = options.process(Lists.newArrayList(args));
        
        if (options.has(DEPTH))
        {
            depth = Integer.parseInt(options.get(DEPTH));
            if(depth < 0)
                throw new SoarException("--depth must be positive");
        }
        
        if (options.has(VARPRINT))
            throw new SoarException("--varprint not implemented yet");
        
        // New in Soar 8.6.3: if no args or options given, print all prods
        if(args.length == 1)
            options.set(ALL);

        if(options.has(STACK))
        {
            agent.printStackTrace(options.has(STATES), options.has(OPERATORS));
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
                wmp.setInternal(options.has(INTERNAL));
                
                // these are ignored if pattern
                wmp.setDepth(depth);
                wmp.setTree(options.has(TREE));
                
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
            if (!options.has(NAME))
                options.set(FULL);

            agent.getPrinter().startNewLine();
            Production p = agent.getProductions().getProduction(argString);
            if(p != null)
                do_print_for_production(p);
            else
                agent.getPrinter().print("No production named " + argString);
            agent.getPrinter().flush();
            return "";
        }
        
        if (options.has(ALL))
        {
            options.set(CHUNKS);
            options.set(DEFAULTS);
            options.set(JUSTIFICATIONS);
            options.set(USER);
            options.set(TEMPLATE);
        }
        
        agent.getPrinter().startNewLine();
        for(Production p : collectProductions())
        {
            do_print_for_production(p);
        }
        
        if(options.has(RL))
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
        
        if (options.has(FILE_NAME))
        {
            if (options.has(FULL))
                p.print("# source file: ", prod.getLocation());
            
            p.print("%s", prod.getLocation());

            if (options.has(FULL))
                p.print("\n");
            else
                p.print(": ");
        }

        if (options.has(FULL))
            prod.print(p, options.has(INTERNAL));
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