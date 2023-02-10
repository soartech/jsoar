/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 21, 2009
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.runtime.ThreadedAgent;

/**
 * Event fired when an agent is stopped. Note that this event will never be fired
 * by a raw {@link Agent} since there is no concept of run control there other
 * than running by phases. This event will typically be fired by a manager
 * object such as {@link ThreadedAgent}
 * 
 * @author ray
 */
public class StopEvent extends AbstractAgentEvent
{
    
    /**
     * Construct a new stop event for the given agent
     * 
     * @param agent the agent
     */
    public StopEvent(Agent agent)
    {
        super(agent);
    }
    
}
