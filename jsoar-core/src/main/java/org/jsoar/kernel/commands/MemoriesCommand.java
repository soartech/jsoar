/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on June 25, 2009
 */
package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "memories" command.
 * 
 * @author ray
 */
public final class MemoriesCommand implements SoarCommand
{
    private final Agent agent;

    public MemoriesCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        Production single = null;
        int count = Integer.MAX_VALUE;
        final EnumSet<ProductionType> types = EnumSet.noneOf(ProductionType.class);
        
        for(int i = 1; i < args.length; ++i)
        {
            final String arg = args[i];
            if("-d".equals(arg) || "--default".equals(arg))
            {
                types.add(ProductionType.DEFAULT);
            }
            else if("-c".equals(arg) || "--chunks".equals(arg))
            {
                types.add(ProductionType.CHUNK);
            }
            else if("-u".equals(arg) || "--user".equals(arg))
            {
                types.add(ProductionType.USER);
            }
            else if("-j".equals(arg) || "--justifications".equals(arg))
            {
                types.add(ProductionType.JUSTIFICATION);
            }
            else if("-T".equals(arg) || "--template".equals(arg))
            {
                types.add(ProductionType.TEMPLATE);
            }
            else
            {
                try
                {
                    count = Integer.parseInt(arg);
                    if(count < 1)
                    {
                        throw new SoarException("Count argument must be greater than 0, got " + count);
                    }
                }
                catch (NumberFormatException e)
                {
                    single = agent.getProductions().getProduction(arg);
                    if(single == null)
                    {
                        throw new SoarException("No production named '" + arg + "'");
                    }
                }
                
            }
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
        
        return "";
    }

    private void printResults(List<Production> productions, int n)
    {
        final Printer printer = agent.getPrinter();
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
            prods.addAll(agent.getProductions().getProductions(type));
        }
        
        Collections.sort(prods, new Comparator<Production> () {

            @Override
            public int compare(Production o1, Production o2)
            {
                return o2.getReteTokenCount() - o1.getReteTokenCount();
            }});
        printResults(prods, n);
    }
}