/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;

/**
 * callback.h:54:AFTER_INIT_SOAR_CALLBACK
 * 
 * @author ray
 */
public class AfterInitSoarEvent extends AbstractAgentEvent
{
    /**
     * @param agent
     */
    public AfterInitSoarEvent(Agent agent)
    {
        super(agent);
    }

}
