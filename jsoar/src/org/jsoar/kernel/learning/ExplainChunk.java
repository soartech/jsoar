/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 28, 2008
 */
package org.jsoar.kernel.learning;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.rhs.Action;

/**
 * For each chunk (or justification) take a copy of its conds and actions, and
 * the list of productions which were backtraced through in creating it. Also
 * keep a list of all of the grounds (WMEs in the supergoal) which were tested
 * as the chunk was formed.
 * 
 * <p>
 * explain.h:38:explain_chunk_struct
 * 
 * @author ray
 */
public class ExplainChunk
{
    String name = "";                      /* Name of this chunk/justification */
    Condition conds;                    /* Variablized list of conditions */
    Action actions;                     /* Variablized list of actions */
    // TODOstruct backtrace_struct *backtrace;  /* List of back traced productions */
    ExplainChunk next_chunk; /* Next chunk in the list */
    Condition all_grounds;             /* All conditions which go to LHS -- 
                                           must be in same order as the chunk's 
                                           conditions. */

}
