/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;

/**
 * callback.h:53:BEFORE_INIT_SOAR_CALLBACK
 * 
 * @author ray
 */
public class BeforeInitSoarEvent extends AbstractAgentEvent
{
    /**
     * @param agent
     */
    public BeforeInitSoarEvent(Agent agent)
    {
        super(agent);
    }

}
