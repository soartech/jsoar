/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 22, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;

/**
 * Event fired just before a production is excised from the agent
 * 
 * @author ray
 */
public class ProductionExcisedEvent extends AbstractProductionEvent
{

    /**
     * Construct a new event
     * 
     * @param agent the agent
     * @param production the excised production
     */
    public ProductionExcisedEvent(Agent agent, Production production)
    {
        super(agent, production);
    }

}
