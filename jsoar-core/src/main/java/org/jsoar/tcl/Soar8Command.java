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
final class Soar8Command implements Command
{
    Soar8Command()
    {
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        throw new TclException(interp, 
                "soar8 command is not supported by jsoar. " +
                "jsoar does not support Soar 7 mode.");
    }
}