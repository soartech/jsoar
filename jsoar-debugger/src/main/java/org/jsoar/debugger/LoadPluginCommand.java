/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 7, 2008
 */
package org.jsoar.debugger;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * @author ray
 */
public class LoadPluginCommand implements SoarCommand
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
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length < 2)
        {
            // TODO illegal arguments
            throw new SoarException(String.format("%s <class> args...", args[0]));
        }
        try
        {
            Class<?> klass = Class.forName(args[1]);
            JSoarDebuggerPlugin plugin = (JSoarDebuggerPlugin) klass.newInstance();
            
            String[] initArgs = new String[args.length - 2];
            for(int i = 2; i < args.length; ++i)
            {
                initArgs[i - 2] = args[i].toString();
            }
            
            plugin.initialize(debugger, initArgs);
            debugger.addPlugin(plugin);
            return "";
        }
        catch (ClassNotFoundException e)
        {
            throw new SoarException("Failed to find plugin class. Maybe it's not on the class path? : " + e.getMessage(), e);
        }
        catch (InstantiationException e)
        {
            throw new SoarException(e.getMessage());
        }
        catch (IllegalAccessException e)
        {
            throw new SoarException(e.getMessage());
        }
        catch (ClassCastException e)
        {
            throw new SoarException(e.getMessage());
        }
    }
    @Override
    public Object getCommand() {
        //todo - when implementing picocli, return the runnable
        return null;
    }
}
