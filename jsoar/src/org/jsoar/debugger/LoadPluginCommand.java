/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 7, 2008
 */
package org.jsoar.debugger;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
public class LoadPluginCommand implements Command
{
    private final JSoarDebugger debugger;
    
    /**
     * @param debugger
     */
    LoadPluginCommand(JSoarDebugger debugger)
    {
        this.debugger = debugger;
    }

    /* (non-Javadoc)
     * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
     */
    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length < 2)
        {
            throw new TclNumArgsException(interp, 0, args, "load-plugin <class> args...");
        }
        try
        {
            Class<?> klass = Class.forName(args[1].toString());
            JSoarDebuggerPlugin plugin = (JSoarDebuggerPlugin) klass.newInstance();
            
            String[] initArgs = new String[args.length - 2];
            for(int i = 2; i < args.length; ++i)
            {
                initArgs[i - 2] = args[i].toString();
            }
            
            plugin.initialize(debugger, initArgs);
        }
        catch (ClassNotFoundException e)
        {
            throw new TclException(interp, e.getMessage());
        }
        catch (InstantiationException e)
        {
            throw new TclException(interp, e.getMessage());
        }
        catch (IllegalAccessException e)
        {
            throw new TclException(interp, e.getMessage());
        }
        catch (ClassCastException e)
        {
            throw new TclException(interp, e.getMessage());
        }
    }

}
