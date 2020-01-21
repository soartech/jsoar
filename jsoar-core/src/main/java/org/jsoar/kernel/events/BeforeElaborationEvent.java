/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;

/**
 * <p>callback.h:53:BEFORE_ELABORATION_CALLBACK
 * 
 * @author ray
 */
public class BeforeElaborationEvent extends AbstractAgentEvent
{
    /**
     * @param agent the agent
     */
    public BeforeElaborationEvent(Agent agent)
    {
        super(agent);
    }

}
