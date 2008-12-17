/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.WorkingMemoryPrinter;
import org.jsoar.kernel.symbols.Symbol;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
public class PrintCommand implements Command
{
    private final SoarTclInterface ifc;
    private final WorkingMemoryPrinter wmp = new WorkingMemoryPrinter();
    
    /**
     * @param agent
     */
    public PrintCommand(SoarTclInterface ifc)
    {
        this.ifc = ifc;
    }
    
    private int processArgs(Interp interp, TclObject[] args) throws TclException
    {
        int i = 1;
        for(; i < args.length; ++i)
        {
            String arg = args[i].toString();
            if("-d".equals(arg) || "--depth".equals(arg))
            {
                if(i + 1 == args.length)
                {
                    throw new TclException(interp, "No argument for --depth option");
                }
                try
                {
                    int depth = Integer.parseInt(args[++i].toString());
                    if(depth < 0)
                    {
                        throw new TclException(interp, "--depth must be positive");
                    }
                    wmp.setDepth(depth);
                }
                catch(NumberFormatException e)
                {
                    throw new TclException(interp, "Invalid --depth value");
                }
            }
            else if("-t".equals(arg) || "--tree".equals(arg))
            {
                wmp.setTree(true);
            }
            else if("-i".equals(arg) || "--internal".equals(arg))
            {
                wmp.setInternal(true);
            }
            else if(arg.startsWith("-"))
            {
                throw new TclException(interp, "Unknow option " + arg);
            }
            else
            {
                break;
            }
        }
        return i;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        wmp.setDefaults();
        
        int firstNonOption = processArgs(interp, args);
        if(firstNonOption == args.length)
        {
            throw new TclException(interp, "Expected id argument");
        }
        final Agent agent = ifc.getAgent();
        
        String argString = args[firstNonOption].toString();
        Symbol arg = agent.readIdentifierOrContextVariable(argString);
        if(arg != null)
        {
            agent.getPrinter().startNewLine();
            wmp.print(agent.syms, agent.getPrinter(), arg);
            agent.getPrinter().flush();
        }
        else
        {
            agent.getPrinter().startNewLine();
            Production p = agent.getProductions().getProduction(argString);
            if(p != null)
            {
                p.print_production(agent.getPrinter(), wmp.isInternal());
            }
            else
            {
                agent.getPrinter().print("No production '" + argString + "'");
            }
        }
    }
}