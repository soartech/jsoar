/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on April 29, 2009
 */
package org.jsoar.tcl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.tracing.Printer;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * http://winter.eecs.umich.edu/soarwiki/Firing-counts
 * 
 * @author ray
 */
final class FiringCountsCommand implements Command
{
    private final SoarTclInterface ifc;

    FiringCountsCommand(SoarTclInterface soarTclInterface)
    {
        ifc = soarTclInterface;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length > 2)
        {
            throw new TclNumArgsException(interp, 0, args, "n");
        }
        
        try
        {
            final int n = args.length == 1 ? Integer.MAX_VALUE : Integer.valueOf(args[1].toString());
            if(n < 1)
            {
                throw new IllegalArgumentException("Numeric argument must be 1 or more, got " + n);
            }
            printTopProductions(interp, n);
        }
        catch(NumberFormatException e)
        {
            final String name = args[1].toString();
            printSingleProduction(interp, name);
        }
        catch(IllegalArgumentException e)
        {
            throw new TclException(interp, e.getMessage());
        }
    }

    private void printResults(List<Production> productions, int n)
    {
        final Printer printer = ifc.getAgent().getPrinter();
        for(int i = 0; i < n && i < productions.size(); ++i)
        {
            final Production p = productions.get(i);
            printer.startNewLine().print("%5d:  %s", p.firing_count, p.getName());
        }
    }
    
    private void printSingleProduction(Interp interp, String name) throws TclException
    {
        final Production p = ifc.getAgent().getProductions().getProduction(name);
        if(p == null)
        {
            throw new TclException(interp, "No production named '" + name + "'");
        }
        printResults(Arrays.asList(p), 1);
    }

    private void printTopProductions(Interp interp, int n)
    {
        final List<Production> prods = ifc.getAgent().getProductions().getProductions(null);
        Collections.sort(prods, new Comparator<Production> () {

            @Override
            public int compare(Production o1, Production o2)
            {
                return o2.firing_count - o1.firing_count;
            }});
        printResults(prods, n);
    }
}