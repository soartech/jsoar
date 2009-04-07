/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.util.events.SoarEvent;

/**
 * Base class for an event that holds a pointer to an agent
 * 
 * @author ray
 */
public abstract class AbstractAgentEvent implements SoarEvent
{
    private final Agent agent;

    /**
     * Construct a new event
     * 
     * @param agent the agent
     */
    public AbstractAgentEvent(Agent agent)
    {
        this.agent = agent;
    }

    /**
     * @return the agent that is the source of this event
     */
    public Agent getAgent()
    {
        return agent;
    }
}
