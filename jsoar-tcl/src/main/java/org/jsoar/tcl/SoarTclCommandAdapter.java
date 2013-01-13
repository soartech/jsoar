/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.tcl;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.commands.SoarCommand;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * Adapts from Tcl Command interface to {@link SoarCommand} interface.
 * 
 * @author ray
 */
public class SoarTclCommandAdapter implements Command
{
    private final SoarCommand inner;
    
    /**
     * @param inner
     */
    public SoarTclCommandAdapter(SoarCommand inner)
    {
        this.inner = inner;
    }

    /* (non-Javadoc)
     * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
     */
    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        final String[] stringArgs = new String[args.length];
        for(int i = 0; i < args.length; ++i) { stringArgs[i] = args[i].toString(); }
        try
        {
            interp.setResult(inner.execute(/* TODO SoarCommandContext */ DefaultSoarCommandContext.empty(), stringArgs));
        }
        catch (SoarException e)
        {
            throw new TclException(interp, e.getMessage());
        }
    }
    
    /**
     * @return the {@link SoarCommand} backing this Tcl command.
     */
    public SoarCommand getSoarCommand()
    {
        return inner;
    }
}
