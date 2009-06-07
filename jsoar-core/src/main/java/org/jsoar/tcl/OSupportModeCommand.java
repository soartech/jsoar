/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class OSupportModeCommand implements Command
{
    OSupportModeCommand()
    {
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        throw new TclException(interp, 
                "o-support-mode command is not supported. " +
                "jsoar's default behavior is equivalent to o-support mode 4 in" +
                "the standard Soar 9.0.0 release.");
    }
}