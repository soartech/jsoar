package org.jsoar.kernel;

import org.jsoar.kernel.Trace.Category;

/**
 * init_soar.h:127:top_level_phase
 * 
 * @author ray
 */
public enum Phase
{
    INPUT_PHASE("Input Phase"), 
    PROPOSE_PHASE("Proposal"),
    DECISION_PHASE("Decision"),
    APPLY_PHASE("Application"),
    OUTPUT_PHASE("Output"),
    PREFERENCE_PHASE("Input"), 
    WM_PHASE("Working Memory");
        
    private final String traceName;
    private final String traceEndName;
    
    Phase(String traceName)
    {
        this.traceName = traceName;
        this.traceEndName = "END " + traceName;
    }
    
    public String getTraceName(boolean startOfPhase)
    {
        return startOfPhase ? traceName : traceEndName;
    }
    
    public void trace(Trace trace, boolean startOfPhase)
    {
        trace.print(Category.TRACE_PHASES_SYSPARAM, "\n--- " + getTraceName(startOfPhase) + " ---\n");
    }
}