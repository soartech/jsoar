package org.jsoar.tcl;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class SrandCommand implements Command
{
    /**
     * 
     */
    private final SoarTclInterface ifc;

    /**
     * @param soarTclInterface
     */
    SrandCommand(SoarTclInterface soarTclInterface)
    {
        ifc = soarTclInterface;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length > 2)
        {
            throw new TclNumArgsException(interp, 0, args, "[seed]");
        }

        long seed = 0;
        if(args.length == 1)
        {
            seed = System.nanoTime();
        }
        else
        {
            seed = Long.parseLong(args[1].toString());
        }
        ifc.agent.getRandom().setSeed(seed);
    }
}