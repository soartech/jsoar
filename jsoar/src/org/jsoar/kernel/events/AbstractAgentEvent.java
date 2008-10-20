/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.util.events.SoarEvent;

/**
 * @author ray
 */
public abstract class AbstractAgentEvent implements SoarEvent
{
    private final Agent agent;

    /**
     * @param agent
     */
    public AbstractAgentEvent(Agent agent)
    {
        this.agent = agent;
    }

    /**
     * @return the agent
     */
    public Agent getAgent()
    {
        return agent;
    }
}
