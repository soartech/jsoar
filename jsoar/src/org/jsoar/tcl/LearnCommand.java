package org.jsoar.tcl;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class LearnCommand implements Command
{
    /**
     * 
     */
    private final SoarTclInterface ifc;

    /**
     * @param soarTclInterface
     */
    LearnCommand(SoarTclInterface soarTclInterface)
    {
        ifc = soarTclInterface;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length != 2)
        {
            throw new TclNumArgsException(interp, 2, args, "[--on|--off]");
        }
        
        if("--on".equals(args[1].toString()))
        {
            ifc.agent.chunker.setLearningOn(true);
        }
        else if("--off".equals(args[1].toString()))
        {
            ifc.agent.chunker.setLearningOn(false);
        }
        else
        {
            throw new TclException(interp, "Option must be --on or --off");
        }
    }
}