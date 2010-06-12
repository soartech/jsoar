/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.memory.WorkingMemoryPrinter;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.SoarCommand;

/**
 * http://winter.eecs.umich.edu/soarwiki/Print
 * 
 * @author ray
 */
public class PrintCommand implements SoarCommand
{
    private static enum Options
    {
        ALL, CHUNKS, DEFAULTS, INTERNAL, JUSTIFICATIONS, STACK, TREE, TEMPLATE, USER,
        STATES, OPERATORS, FULL, RL, FILE_NAME, EXACT, DEPTH;
        // TODO varprint
    }
    private final Agent agent;
    private final WorkingMemoryPrinter wmp = new WorkingMemoryPrinter();
    private int defaultDepth = 1;
    
    private final EnumSet<Options> options = EnumSet.noneOf(Options.class);
    private int depth = 1;
    
    public PrintCommand(Agent agent)
    {
        this.agent = agent;
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
    
    private static boolean has(String arg, String little, String big)
    {
        return arg.equals(little) || arg.equals(big);
    }
    
    private List<Production> collectProductions()
    {
        final ProductionManager pm = agent.getProductions();
        final List<Production> result = new ArrayList<Production>();
        
        if(options.contains(Options.ALL))
        {
            result.addAll(pm.getProductions(null));
        }
        else
        {
            if(options.contains(Options.CHUNKS)) result.addAll(pm.getProductions(ProductionType.CHUNK));
            if(options.contains(Options.USER)) result.addAll(pm.getProductions(ProductionType.USER));
            if(options.contains(Options.DEFAULTS)) result.addAll(pm.getProductions(ProductionType.DEFAULT));
            if(options.contains(Options.TEMPLATE)) result.addAll(pm.getProductions(ProductionType.TEMPLATE));
            if(options.contains(Options.JUSTIFICATIONS)) result.addAll(pm.getProductions(ProductionType.JUSTIFICATION));
        }
        
        return result;
    }
    
    private int processArgs(String[] args) throws SoarException
    {
        this.depth = defaultDepth;
        
        int i = 1;
        for(; i < args.length; ++i)
        {
            String arg = args[i];
            if(has(arg, "-d", "--depth"))
            {
            	options.add(Options.DEPTH);
                if(i + 1 == args.length)
                {
                    throw new SoarException("No argument for --depth option");
                }
                try
                {
                    depth = Integer.parseInt(args[++i].toString());
                    if(depth < 0)
                    {
                        throw new SoarException("--depth must be positive");
                    }
                }
                catch(NumberFormatException e)
                {
                    throw new SoarException("Invalid --depth value");
                }
            }
            else if(has(arg, "-t", "--tree"))
            {
                options.add(Options.TREE);
            }
            else if(has(arg, "-i", "--internal"))
            {
                options.add(Options.INTERNAL);
            }
            else if(has(arg, "-a", "--all"))
            {
                options.add(Options.ALL);
            }
            else if(has(arg, "-u", "--user"))
            {
                options.add(Options.USER);
            }
            else if(has(arg, "-c", "--chunks"))
            {
                options.add(Options.CHUNKS);
            }
            else if(has(arg, "-D", "--defaults"))
            {
                options.add(Options.DEFAULTS);
            }
            else if(has(arg, "-j", "--justifications"))
            {
                options.add(Options.JUSTIFICATIONS);
            }
            else if(has(arg, "-s", "--stack"))
            {
                options.add(Options.STACK);
            }
            else if(has(arg, "-S", "--states"))
            {
                options.add(Options.STATES);
            }
            else if(has(arg, "-o", "--operators"))
            {
                options.add(Options.OPERATORS);
            }
            else if(has(arg, "-f", "--full"))
            {
                options.add(Options.FULL);
            }
            else if(has(arg, "-T", "--template"))
            {
                options.add(Options.TEMPLATE);
            }
            else if(has(arg, "-r", "--rl"))
            {
                options.add(Options.RL);
            }
            else if(has(arg, "-e", "--exact"))
            {
            	options.add(Options.EXACT);
            }
            else if(has(arg, "-F", "--filename"))
            {
                options.add(Options.FILE_NAME);
            }
            else if(arg.startsWith("-"))
            {
                throw new SoarException("Unknow option " + arg);
            }
            else
            {
                break;
            }
        }
        // New in Soar 9.3: if no args given, print all prods
        if(i == args.length)
        {
            options.add(Options.ALL);
        }
        return i;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        agent.getPrinter().startNewLine();
        
        options.clear();
        
        int firstNonOption = processArgs(args);
        if(options.contains(Options.STACK))
        {
            agent.printStackTrace(options.contains(Options.STATES), options.contains(Options.OPERATORS));
            agent.getPrinter().print("\n").flush();
            return "";
        }
        else if(options.contains(Options.ALL) || options.contains(Options.CHUNKS) ||
                options.contains(Options.USER) || options.contains(Options.TEMPLATE) ||
                options.contains(Options.DEFAULTS))
        {
            for(Production p : collectProductions())
            {
                if(options.contains(Options.FILE_NAME))
                {
                    agent.getPrinter().print("# sourcefile : %s\n", p.getLocation());
                }
                if(options.contains(Options.FULL))
                {
                    p.print(agent.getPrinter(), options.contains(Options.INTERNAL));
                }
                else
                {
                    agent.getPrinter().print("%s   ", p.getName());
                    if(p.rl_rule)
                    {
                        agent.getPrinter().print("%f   %s", p.rl_update_count, p.action_list.asMakeAction().referent);
                    }
                }
                agent.getPrinter().print("\n");
            }
            agent.getPrinter().flush();
            return "";
        }
        else if(options.contains(Options.RL))
        {
            for(Production p : agent.getProductions().getProductions(null))
            {
                if(p.rl_rule)
                {
                    agent.getPrinter().print("%f   %s", p.rl_update_count, p.action_list.asMakeAction().referent);
                }
            }
            return "";
        }
        else if(options.contains(Options.EXACT) && (options.contains(Options.DEPTH) || options.contains(Options.TREE)))
        {
        	throw new SoarException("No depth/tree flags allowed when printing exact.");
        }
        
        if(firstNonOption == args.length)
        {
            throw new SoarException("Expected id or production name argument");
        }
        
        String argString = args[firstNonOption].toString();
        Symbol arg = agent.readIdentifierOrContextVariable(argString);
        if(arg != null || options.contains(Options.EXACT))
        {
            agent.getPrinter().startNewLine();
            wmp.setDepth(depth);
            wmp.setInternal(options.contains(Options.INTERNAL));
            wmp.setTree(options.contains(Options.TREE));
            wmp.setExact(options.contains(Options.EXACT));
            
            try {
                wmp.print(agent, agent.getPrinter(), arg, argString);
            } catch(Exception e) {
                throw new SoarException(e.toString());
            }

            agent.getPrinter().flush();
        }
        else
        {
            agent.getPrinter().startNewLine();
            Production p = agent.getProductions().getProduction(argString);
            if(p != null)
            {
                if(options.contains(Options.FILE_NAME))
                {
                    agent.getPrinter().print("# sourcefile : %s\n", p.getLocation());
                }
                p.print(agent.getPrinter(), options.contains(Options.INTERNAL));
            }
            else
            {
                agent.getPrinter().print("No production '" + argString + "'");
            }
            agent.getPrinter().flush();
        }
        return "";
    }
    
    private void do_print_for_production(Production prod)
    {
        final Printer p = agent.getPrinter();

        if (!options.contains(Options.FULL))
        {
            p.print("%s ", prod.getName());

            if (prod.rl_rule)
            {
                p.print("%f  ", prod.rl_update_count);
                p.print("%s", prod.action_list.asMakeAction().referent);
            }
        }
        if (options.contains(Options.FILE_NAME))
        {
            p.print("# sourcefile : %s", prod.getLocation());
        }
        p.print("\n");
        if (options.contains(Options.FULL))
        {
            prod.print(p, options.contains(Options.INTERNAL));
            p.print("\n");
        }
    }

}