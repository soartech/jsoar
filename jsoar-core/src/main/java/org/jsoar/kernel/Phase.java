package org.jsoar.kernel;

import org.jsoar.kernel.events.AbstractPhaseEvent;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.events.PhaseEvents;
import org.jsoar.kernel.events.PhaseEvents.AfterApply;
import org.jsoar.kernel.events.PhaseEvents.AfterDecision;
import org.jsoar.kernel.events.PhaseEvents.AfterInput;
import org.jsoar.kernel.events.PhaseEvents.AfterOutput;
import org.jsoar.kernel.events.PhaseEvents.AfterPropose;
import org.jsoar.kernel.events.PhaseEvents.BeforeApply;
import org.jsoar.kernel.events.PhaseEvents.BeforeDecision;
import org.jsoar.kernel.events.PhaseEvents.BeforeInput;
import org.jsoar.kernel.events.PhaseEvents.BeforeOutput;
import org.jsoar.kernel.events.PhaseEvents.BeforePropose;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;

/**
 * Represents a phase type in the Soar decision cycle.
 * 
 * <p>init_soar.h:127:top_level_phase
 * 
 * @author ray
 */
public enum Phase
{
    /**
     * The input phase of the agent where input is generated.
     * 
     * <p>init_soar.h:127:INPUT_PHASE
     * 
     * @see InputEvent
     */
    INPUT("input", BeforeInput.class, AfterInput.class),
    
    /**
     * The operator proposal phase
     * 
     * <p>init_soar.h:127:PROPOSE_PHASE
     */
    PROPOSE("propose", BeforePropose.class, AfterPropose.class),
    
    /**
     * The decision phase
     * 
     * <p>init_soar.h:127:DECISION_PHASE
     */
    DECISION("decision", BeforeDecision.class, AfterDecision.class),
    
    /**
     * The operator application phase
     * 
     * <p>init_soar.h:127:APPLY_PHASE
     */
    APPLY("apply", BeforeApply.class, AfterApply.class),
    
    /**
     * The output phase where the agent's output-link is processed.
     * 
     * <p>init_soar.h:127:OUTPUT_PHASE
     * 
     * @see OutputEvent
     */
    OUTPUT("output", BeforeOutput.class, AfterOutput.class);
    
    private final String traceName;
    private final String traceEndName;
    private final Class<? extends AbstractPhaseEvent> beforeEventType;
    private final Class<? extends AbstractPhaseEvent> afterEventType;
    
    Phase(String traceName, Class<? extends AbstractPhaseEvent> beforeEventType, Class<? extends AbstractPhaseEvent> afterEventType)
    {
        this.traceName = traceName;
        this.traceEndName = "END " + traceName;
        this.beforeEventType = beforeEventType;
        this.afterEventType = afterEventType;
    }
    
    private String getTraceName(boolean startOfPhase)
    {
        return startOfPhase ? traceName : traceEndName;
    }
    
    /**
     * Trace the start or end of this phase
     * 
     * @param trace The trace object
     * @param startOfPhase <code>true</code> for start of phase, <code>false</code>
     *     for end of phase
     */
    public void trace(Trace trace, boolean startOfPhase)
    {
        // Only log start of phase. This is what csoar debugger currently does.
        if(startOfPhase && trace.isEnabled(Category.PHASES))
        {
            trace.startNewLine().print("--- " + getTraceName(startOfPhase) + " phase ---");
        }
    }
    
    /**
     * Returns the class of the "before" event for this phase.
     * 
     * @return the class of the "before" event for this phase
     * @see PhaseEvents
     */
    public Class<? extends AbstractPhaseEvent> getBeforeEvent()
    {
        return beforeEventType;
    }
    
    /**
     * Returns the class of the "after" event for this phase
     * 
     * @return the class of the "after" event for this phase
     * @see PhaseEvents
     */
    public Class<? extends AbstractPhaseEvent> getAfterEvent()
    {
        return afterEventType;
    }
    
}
