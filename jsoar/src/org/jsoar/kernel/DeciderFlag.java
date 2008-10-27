/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

/**
 * The decider often needs to mark symbols with certain flags, usually to record
 * that the symbols are in certain sets or have a certain status. The
 * "common.decider_flag" field on symbols is used for this, and is set to one of
 * the following flag values. (Usually only two or three of these values are
 * used at once, and the meaning should be clear from the code.)
 * 
 * <p>decide.cpp:120:DECIDER_FLAG
 * 
 * @author ray
 */
public enum DeciderFlag
{
    /** decide.cpp:120:NOTHING_DECIDER_FLAG  */
    NOTHING,
    
    /** decide.cpp:121:CANDIDATE_DECIDER_FLAG  */
    CANDIDATE,
    
    /** decide.cpp:122:CONFLICTED_DECIDER_FLAG  */
    CONFLICTED,
    
    /** decide.cpp:123:FORMER_CANDIDATE_DECIDER_FLAG  */
    FORMER_CANDIDATE,
    
    /** decide.cpp:124:BEST_DECIDER_FLAG  */
    BEST,
    
    /** decide.cpp:125:WORST_DECIDER_FLAG  */
    WORST,
    
    /** decide.cpp:126:UNARY_INDIFFERENT_DECIDER_FLAG  */
    UNARY_INDIFFERENT,
    
    /** decide.cpp:127:ALREADY_EXISTING_WME_DECIDER_FLAG  */
    ALREADY_EXISTING_WME,
    
    /** decide.cpp:128:UNARY_PARALLEL_DECIDER_FLAG  */
    UNARY_PARALLEL,
    
    /** decide.cpp:132:UNARY_INDIFFERENT_CONSTANT_DECIDER_FLAG  */
    UNARY_INDIFFERENT_CONSTANT;

    /**
     * Helper to handle code that relies on NOTHING_DECIDER_FLAG being 0 in
     * boolean contexts in C (see above)
     * 
     * @return true if this flag is not NOTHING
     */
    public boolean isSomething()
    {
        return this != NOTHING;
    }
}