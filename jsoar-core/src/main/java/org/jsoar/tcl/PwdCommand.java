/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.tcl;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
public class PwdCommand implements Command
{
    private final SourceCommand sourceCommand;
    
    /**
     * @param sourceCommand
     */
    public PwdCommand(SourceCommand sourceCommand)
    {
        this.sourceCommand = sourceCommand;
    }


    /* (non-Javadoc)
     * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
     */
    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length != 1)
        {
            throw new TclNumArgsException(interp, 0, args, "");
        }
        
        interp.setResult(sourceCommand.getWorkingDirectory().getAbsolutePath().replace('\\', '/'));
    }

}
