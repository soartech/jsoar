/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 22, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;

/**
 * Event fired each time around the agent event loop. The same effect could
 * be achieved with one of the phase events, but this is meant to be a
 * generic loop event independent of any particulars of the soar decision
 * cycle.
 * 
 * @author ray
 */
public class RunLoopEvent extends AbstractAgentEvent
{
    /**
     * @param agent the agent
     */
    public RunLoopEvent(Agent agent)
    {
        super(agent);
    }

}
