/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.Wme;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.symbols.Symbol;

/**
 * 
 * intantiations.h:84
 * 
 * @author ray
 */
public class Instantiation
{
    public Production prod; 
    public Instantiation next, prev; /* dll of inst's from same prod */

    // TODO struct token_struct *rete_token;       /* used by Rete for retractions */
    Wme rete_wme;                         /* ditto */
    Condition top_of_instantiated_conditions;
    Condition bottom_of_instantiated_conditions;
    // TODO not_struct *nots;
    // TODO preference *preferences_generated;    /* header for dll of prefs */
    Symbol match_goal;                   /* symbol, or NIL if none */
    int /*goal_stack_level*/ match_goal_level;    /* level, or ATTRIBUTE_IMPASSE_LEVEL */
    boolean okay_to_variablize;
    boolean in_ms;  /* TRUE iff this inst. is still in the match set */
    int /*tc_number*/ backtrace_number;
    boolean GDS_evaluated_already;

}
