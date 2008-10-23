/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 22, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;

/**
 * Event fired after a production is added to the agent
 * 
 * @author ray
 */
public class ProductionAddedEvent extends AbstractProductionEvent
{

    /**
     * Construct a new event
     * 
     * @param agent The agent
     * @param production The production added
     */
    public ProductionAddedEvent(Agent agent, Production production)
    {
        super(agent, production);
    }

}
