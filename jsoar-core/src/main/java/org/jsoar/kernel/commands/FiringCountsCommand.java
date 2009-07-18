/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on April 29, 2009
 */
package org.jsoar.kernel.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.SoarCommand;

/**
 * http://winter.eecs.umich.edu/soarwiki/Firing-counts
 * 
 * @author ray
 */
public final class FiringCountsCommand implements SoarCommand
{
    private final Agent agent;

    public FiringCountsCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length > 2)
        {
            throw new SoarException("Expected at least one argument.");
        }
        
        try
        {
            final int n = args.length == 1 ? Integer.MAX_VALUE : Integer.valueOf(args[1]);
            if(n < 1)
            {
                throw new IllegalArgumentException("Numeric argument must be 1 or more, got " + n);
            }
            printTopProductions(n);
        }
        catch(NumberFormatException e)
        {
            final String name = args[1];
            printSingleProduction(name);
        }
        catch(IllegalArgumentException e)
        {
            throw new SoarException(e.getMessage());
        }
        return "";
    }

    private void printResults(List<Production> productions, int n)
    {
        final Printer printer = agent.getPrinter();
        for(int i = 0; i < n && i < productions.size(); ++i)
        {
            final Production p = productions.get(i);
            printer.startNewLine().print("%5d:  %s", p.getFiringCount(), p.getName());
        }
    }
    
    private void printSingleProduction(String name) throws SoarException
    {
        final Production p = agent.getProductions().getProduction(name);
        if(p == null)
        {
            throw new SoarException("No production named '" + name + "'");
        }
        printResults(Arrays.asList(p), 1);
    }

    private void printTopProductions(int n)
    {
        final List<Production> prods = agent.getProductions().getProductions(null);
        Collections.sort(prods, new Comparator<Production> () {

            @Override
            public int compare(Production o1, Production o2)
            {
                final long d = o2.getFiringCount() - o1.getFiringCount();
                return d < 0 ? -1 : (d > 0 ? 1 : 0);
            }});
        printResults(prods, n);
    }
}