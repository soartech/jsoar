package org.jsoar.tcl;

import org.jsoar.kernel.RunType;
import org.jsoar.runtime.ThreadedAgent;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * http://winter.eecs.umich.edu/soarwiki/Run
 * 
 * <p>Simple implementation of Run command. Must be manually installed.
 * 
 * @author ray
 */
public final class RunCommand implements Command
{
    private final ThreadedAgent threadedAgent;
    
    public RunCommand(ThreadedAgent threadedAgent)
    {
        this.threadedAgent = threadedAgent;
    }
    
    private int getCount(Interp interp, int i, TclObject[] args) throws TclException
    {
        final String arg = args[i].toString();
        if(i + 1 >= args.length)
        {
            throw new TclException(interp, "No count argument for " + arg + " option");
        }
        final String countString = args[i+1].toString();
        try
        {
            int n = Integer.parseInt(countString);
            if(n < 1)
            {
                throw new TclException(interp, "Expected count larger than 0 for " + arg + " option");
            }
            return n;
        }
        catch(NumberFormatException e)
        {
            throw new TclException(interp, "Expected integer for run count, got '" + countString + "'");
        }
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        RunType type = RunType.FOREVER;
        int count = 0;
        for(int i = 1; i < args.length; ++i)
        {
            final String arg = args[i].toString();
            if("-d".equals(arg) || "--decision".equals(arg))
            {
                type = RunType.DECISIONS;
                count = getCount(interp, i++, args);
            }
            else if("-e".equals(arg) || "--elaboration".equals(arg))
            {
                type = RunType.ELABORATIONS;
                count = getCount(interp, i++, args);
            }
            else if("-p".equals(arg) || "--phase".equals(arg))
            {
                type = RunType.PHASES;
                count = getCount(interp, i++, args);
            }
            else if("-f".equals(arg) || "--forever".equals(arg))
            {
                type = RunType.FOREVER;
            }
            else
            {
                throw new TclException(interp, "Unknow option '" + arg + "'");
            }
        }
        
        threadedAgent.runFor(count, type);
        
    }
}