/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 22, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;

/**
 * Abstract base class for production-related events.
 * 
 * @author ray
 */
public abstract class AbstractProductionEvent extends AbstractAgentEvent
{
    private final Production production;
    
    /**
     * @param agent The agent
     * @param production The production
     */
    public AbstractProductionEvent(Agent agent, Production production)
    {
        super(agent);
        this.production = production;
    }

    /**
     * @return the production
     */
    public Production getProduction()
    {
        return production;
    }
}
