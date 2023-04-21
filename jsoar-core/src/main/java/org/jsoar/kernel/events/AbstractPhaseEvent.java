/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 22, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;

/**
 * Abstract base class for a phase begin/end event. Note that this class
 * should not be registered for. Instead, listeners should register for
 * the class objects returned by {@link Phase#getBeforeEvent()} and
 * {@link Phase#getAfterEvent()}.
 * 
 * @author ray
 * @see PhaseEvents
 */
public abstract class AbstractPhaseEvent extends AbstractAgentEvent
{
    private final Phase phase;
    private final boolean before;
    
    /**
     * Construct a new event
     * 
     * @param agent the source agent
     * @param phase the phase
     * @param before if <code>true</code> this is a "before" event, otherwise
     *     it's an "after" event
     */
    protected AbstractPhaseEvent(Agent agent, Phase phase, boolean before)
    {
        super(agent);
        this.phase = phase;
        this.before = before;
    }
    
    /**
     * @return the phase associated with this event
     */
    public Phase getPhase()
    {
        return phase;
    }
    
    /**
     * @return <code>true</code> if this is a "before" event, <code>false</code>
     * if this is an "after" event
     */
    public boolean isBefore()
    {
        return before;
    }
}
