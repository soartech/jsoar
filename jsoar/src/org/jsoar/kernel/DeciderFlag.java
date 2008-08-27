/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

/**
                   Decider Flags

   The decider often needs to mark symbols with
   certain flags, usually to record that the symbols
   are in certain sets or have a certain status.
   The "common.decider_flag" field on symbols is
   used for this, and is set to one of the following
   flag values.  (Usually only two or three of these
   values are used at once, and the meaning should
   be clear from the code.)
   
 * @author ray
 */
public enum DeciderFlag
{
    NOTHING_DECIDER_FLAG,     /* Warning: code relies in this being 0 */
    CANDIDATE_DECIDER_FLAG,
    CONFLICTED_DECIDER_FLAG,
    FORMER_CANDIDATE_DECIDER_FLAG,
    BEST_DECIDER_FLAG,
    WORST_DECIDER_FLAG,
    UNARY_INDIFFERENT_DECIDER_FLAG,
    ALREADY_EXISTING_WME_DECIDER_FLAG,
    UNARY_PARALLEL_DECIDER_FLAG,
    /* REW: 2003-01-02 Behavior Variability Kernel Experiments 
       A new preference type: unary indifferent + constant (probability) value
    */
    UNARY_INDIFFERENT_CONSTANT_DECIDER_FLAG

}
