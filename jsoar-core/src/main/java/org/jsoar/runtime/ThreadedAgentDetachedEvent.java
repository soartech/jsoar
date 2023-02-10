/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.runtime;

import org.jsoar.util.events.SoarEvent;

/**
 * Event fired when threaded agents are detached/destroyed.
 * 
 * @author ray
 */
public class ThreadedAgentDetachedEvent implements SoarEvent
{
    private final ThreadedAgent agent;
    
    /**
     * @param agent
     */
    public ThreadedAgentDetachedEvent(ThreadedAgent agent)
    {
        this.agent = agent;
    }
    
    /**
     * @return the agent
     */
    public ThreadedAgent getAgent()
    {
        return agent;
    }
}
