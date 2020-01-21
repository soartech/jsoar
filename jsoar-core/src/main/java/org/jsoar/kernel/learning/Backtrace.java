/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 28, 2008
 */
package org.jsoar.kernel.learning;

import org.jsoar.kernel.lhs.Condition;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * <p>backtrace.h:35:backtrace_struct
 * 
 * @author ray
 */
public class Backtrace
{
    boolean result;           /* 1 when this is a result of the chunk */
    Condition trace_cond;     /* The (local) condition being traced */
    String   prod_name;       /* The production's name */
    Condition grounds;        /* The list of conds for the LHS of chunk */
    Condition potentials;     /* The list of conds which aren't linked */
    Condition locals;         /* Conds in the subgoal -- need to BT */
    Condition negated;        /* Negated conditions (sub/super) */
    Backtrace next_backtrace; /* Pointer to next in this list */

}
