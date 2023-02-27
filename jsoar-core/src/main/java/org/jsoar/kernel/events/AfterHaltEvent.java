/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 22, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;

/**
 * <p>callback.h::AFTER_HALT_SOAR_CALLBACK
 * 
 * @author ray
 */
public class AfterHaltEvent extends AbstractAgentEvent
{
    /**
     * @param agent the agent
     */
    public AfterHaltEvent(Agent agent)
    {
        super(agent);
    }
    
}
