package org.jsoar.tcl;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class MaxElaborationsCommand implements Command
{
    /**
     * 
     */
    private final SoarTclInterface ifc;

    /**
     * @param soarTclInterface
     */
    MaxElaborationsCommand(SoarTclInterface soarTclInterface)
    {
        ifc = soarTclInterface;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length > 2)
        {
            throw new TclNumArgsException(interp, 0, args, "[value]");
        }

        if(args.length == 1)
        {
            ifc.getAgent().getPrinter().print("%d", ifc.getAgent().consistency.getMaxElaborations());
        }
        else
        {
            ifc.getAgent().consistency.setMaxElaborations(Integer.parseInt(args[1].toString()));
        }
    }
}