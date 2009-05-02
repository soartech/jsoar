package org.jsoar.tcl;

import org.jsoar.runtime.ThreadedAgent;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclObject;

/**
 * http://winter.eecs.umich.edu/soarwiki/Run
 * 
 * <p>Simple implementation of stop-soar command. Must be manually installed.
 * 
 * @author ray
 */
public final class StopCommand implements Command
{
    private final ThreadedAgent threadedAgent;
    
    public StopCommand(ThreadedAgent threadedAgent)
    {
        this.threadedAgent = threadedAgent;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        threadedAgent.stop();
    }
}