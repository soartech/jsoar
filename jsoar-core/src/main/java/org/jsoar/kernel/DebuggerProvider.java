/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel;

/**
 * Interface for an object that knows how to instantiate and attach a debugger to 
 * a JSoar agent.
 * 
 * @author ray
 */
public interface DebuggerProvider
{
    /**
     * Opens a debugger and attaches it to the given agent.
     * 
     * @param agent the agent
     * @throws SoarException 
     */
    void openDebugger(Agent agent) throws SoarException;
    
    /**
     * Opens a debugger and attaches it to the given agent. Waits until the debugger 
     * is fully initialized before proceeding.
     * 
     * <p>Note that care must be taken to avoid deadlocks if the debugger is initialized 
     * on a different thread such as the Swing event dispatch thread. For example, if this
     * method was called from the the agent thread of {@link ThreadedAgent} there would 
     * almost certainly be deadlock.
     * 
     * @param agent the agent
     * @throws SoarException if an error occurs during initialization
     * @throws InterruptedException if the thread is interrupted while waiting for the
     *      debugger to initialize
     */
    void openDebuggerAndWait(Agent agent) throws SoarException, InterruptedException;
}
