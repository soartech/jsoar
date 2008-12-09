package org.jsoar.tcl;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class WatchCommand implements Command
{
    /**
     * 
     */
    private final SoarTclInterface ifc;

    /**
     * @param soarTclInterface
     */
    WatchCommand(SoarTclInterface soarTclInterface)
    {
        ifc = soarTclInterface;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length != 2)
        {
            throw new TclNumArgsException(interp, 2, args, "level");
        }
        
        try
        {
            int level = Integer.valueOf(args[1].toString());
            ifc.agent.getTrace().setWatchLevel(level);
        }
        catch(NumberFormatException e)
        {
            throw new TclException(interp, args[1] + " is not a valid number");
        }
        catch(IllegalArgumentException e)
        {
            throw new TclException(interp, e.getMessage());
        }
    }
}