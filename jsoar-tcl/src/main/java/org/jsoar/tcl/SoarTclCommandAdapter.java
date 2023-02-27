/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 6, 2009
 */
package org.jsoar.tcl;

import org.jsoar.kernel.SoarException;
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
    private final String[] alias;
    private final SoarTclInterface soarTclInterface;
    
    /**
     * @param inner
     */
    public SoarTclCommandAdapter(SoarCommand inner, SoarTclInterface soarTclInterface)
    {
        this(inner, new String[0], soarTclInterface);
    }
    
    /**
     * Note that the alias support here is very weak -- we only support replacing a single command name with an array of strings
     * That is, we don't support aliasing subcommands
     */
    public SoarTclCommandAdapter(SoarCommand inner, String[] alias, SoarTclInterface soarTclInterface)
    {
        this.inner = inner;
        this.alias = alias;
        this.soarTclInterface = soarTclInterface;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
     */
    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        String[] stringArgs;
        if(alias.length > 0)
        {
            // if there is an alias, then replace the command name with the alias
            stringArgs = new String[args.length - 1 + alias.length];
            for(int i = 0; i < alias.length; ++i)
            {
                stringArgs[i] = alias[i].toString();
            }
            // skip the first index, which is the command name that we're replacing
            for(int stringArgsIndex = alias.length, argsIndex = 1; argsIndex < args.length; ++stringArgsIndex, ++argsIndex)
            {
                stringArgs[stringArgsIndex] = args[argsIndex].toString();
            }
            
        }
        else
        {
            stringArgs = new String[args.length];
            for(int i = 0; i < args.length; ++i)
            {
                stringArgs[i] = args[i].toString();
            }
        }
        
        try
        {
            interp.setResult(inner.execute(soarTclInterface.getContext(), stringArgs));
        }
        catch(SoarException e)
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
