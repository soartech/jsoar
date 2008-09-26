package org.jsoar.kernel;

import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;

/**
 * init_soar.h:127:top_level_phase
 * 
 * @author ray
 */
public enum Phase
{
    INPUT_PHASE("input"), 
    PROPOSE_PHASE("propose"),
    DECISION_PHASE("decision"),
    APPLY_PHASE("apply"),
    OUTPUT_PHASE("output"),
    PREFERENCE_PHASE("preference"), 
    WM_PHASE("working memory");
        
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
        trace.print(Category.TRACE_PHASES_SYSPARAM, "\n--- " + getTraceName(startOfPhase) + " phase ---\n");
    }
}