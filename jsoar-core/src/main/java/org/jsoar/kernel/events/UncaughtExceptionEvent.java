/*
 * Copyright (c) 2010  Dave Ray <daveray@gmail.com>
 *
 * Created on January 28, 2010
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.runtime.ThreadedAgent;

/**
 * Event fired when an unhandled exception is caught in the agent thread. Note
 * that this event will never be fired by a raw {@link Agent} since there is no
 * concept of an agent thread. This event will typically be fired by a manager
 * object such as {@link ThreadedAgent}.
 * 
 * @author ray
 */
public class UncaughtExceptionEvent extends AbstractAgentEvent
{
    private final Throwable exception;
    
    /**
     * Construct a new event for the given agent and exception
     * 
     * @param agent the agent
     * @param exception the uncaught exception
     */
    public UncaughtExceptionEvent(Agent agent, Throwable exception)
    {
        super(agent);
        
        this.exception = exception;
    }
    
    /**
     * @return the uncaught exception
     */
    public Throwable getException()
    {
        return exception;
    }
}
