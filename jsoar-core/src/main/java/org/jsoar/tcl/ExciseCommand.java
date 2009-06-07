/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import java.util.LinkedHashSet;
/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on April 29, 2009
 */
import java.util.Set;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * http://winter.eecs.umich.edu/soarwiki/Excise
 * 
 * @author ray
 */
final class ExciseCommand implements Command
{
    private final Agent agent;

    ExciseCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        final Set<Production> toExcise = new LinkedHashSet<Production>();
        final ProductionManager pm = agent.getProductions();
        boolean doInit = false;
        for(int i = 1; i < args.length; ++i)
        {
            final String arg = args[i].toString();
            if("-a".equals(arg) || "--all".equals(arg))
            {
                toExcise.addAll(pm.getProductions(null));
                doInit = true;
            }
            else if("-c".equals(arg) || "--chunks".equals(arg))
            {
                toExcise.addAll(pm.getProductions(ProductionType.CHUNK));
            }
            else if("-d".equals(arg) || "--default".equals(arg))
            {
                toExcise.addAll(pm.getProductions(ProductionType.DEFAULT));
            }
            else if("-r".equals(arg) || "--rl".equals(arg))
            {
                for(Production p : pm.getProductions(ProductionType.DEFAULT))
                {
                    if(p.rl_rule)
                    {
                        toExcise.add(p);
                    }
                }
            }
            else if("-t".equals(arg) || "--task".equals(arg))
            {
                toExcise.addAll(pm.getProductions(ProductionType.CHUNK));
                toExcise.addAll(pm.getProductions(ProductionType.JUSTIFICATION));
                toExcise.addAll(pm.getProductions(ProductionType.USER));
            }
            else if("-T".equals(arg) || "--templates".equals(arg))
            {
                toExcise.addAll(pm.getProductions(ProductionType.TEMPLATE));
            }
            else if("-u".equals(arg) || "--user".equals(arg))
            {
                toExcise.addAll(pm.getProductions(ProductionType.USER));
            }
            else
            {
                final Production p = pm.getProduction(arg);
                if(p == null)
                {
                    throw new TclException(interp, "No production named '" + arg + "'");
                }
                toExcise.add(p);
            }
        }

        for(Production p : toExcise)
        {
            pm.exciseProduction(p, false);
        }
        
        // If -a is given, we do an init-soar as well
        if(doInit)
        {
            agent.initialize();
        }
        
        agent.getPrinter().startNewLine().print("%d production%s excise.", toExcise.size(), toExcise.size() == 1 ? "" : "s");
    }
}