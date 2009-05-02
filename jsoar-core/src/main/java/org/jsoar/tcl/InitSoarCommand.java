package org.jsoar.tcl;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * Command that prints out all property values
 * 
 * @author ray
 */
final class InitSoarCommand implements Command
{
    private final SoarTclInterface ifc;

    /**
     * @param ifc the owning Tcl interface
     */
    InitSoarCommand(SoarTclInterface ifc)
    {
        this.ifc = ifc;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        ifc.getAgent().initialize();
        ifc.getAgent().getPrinter().startNewLine().print("Agent reinitialized\n").flush();
    }
}