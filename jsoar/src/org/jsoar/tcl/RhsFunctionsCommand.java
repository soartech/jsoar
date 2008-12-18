package org.jsoar.tcl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.tracing.Printer;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * Command that prints out all registered RHS functions
 * 
 * @author ray
 */
final class RhsFunctionsCommand implements Command
{
    private final SoarTclInterface ifc;

    /**
     * @param ifc the owning Tcl interface
     */
    RhsFunctionsCommand(SoarTclInterface ifc)
    {
        this.ifc = ifc;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        final Agent agent = ifc.getAgent();
        final Printer p = agent.getPrinter();
        
        p.startNewLine();
        
        final List<RhsFunctionHandler> handlers = agent.getRhsFunctions().getHandlers();
        Collections.sort(handlers, new Comparator<RhsFunctionHandler>(){

            @Override
            public int compare(RhsFunctionHandler a, RhsFunctionHandler b)
            {
                return a.getName().compareTo(b.getName());
            }});
        
        for(RhsFunctionHandler f : handlers)
        {
            int max = f.getMaxArguments();
            p.print("%20s (%d, %s)%n", f.getName(), f.getMinArguments(), max == Integer.MAX_VALUE ? "*" : Integer.toString(max));
        }
    }
}