/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.StringSymbol;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class MultiAttrCommand implements Command
{
    /**
     * 
     */
    private final Agent agent;

    /**
     * @param soarTclInterface
     */
    MultiAttrCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length != 3)
        {
            throw new TclNumArgsException(interp, 0, args, "attr cost");
        }
        
        StringSymbol attr = agent.getSymbols().createString(args[1].toString());
        int cost = Integer.valueOf(args[2].toString());
        agent.getMultiAttributes().setCost(attr, cost);
    }
}