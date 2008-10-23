/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;

/**
 * <p>callback.h:53:AFTER_DECISION_CYCLE_CALLBACK
 * 
 * @author ray
 */
public class AfterDecisionCycleEvent extends AbstractAgentEvent
{
    private final Phase phase;
    
    /**
     * @param agent the agent
     * @param phase the current phase
     */
    public AfterDecisionCycleEvent(Agent agent, Phase phase)
    {
        super(agent);
        this.phase = phase;
    }

    /**
     * @return the current phase
     */
    public Phase getPhase()
    {
        return phase;
    }
}
