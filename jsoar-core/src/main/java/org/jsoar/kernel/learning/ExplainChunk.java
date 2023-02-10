/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 28, 2008
 */
package org.jsoar.kernel.learning;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.Conditions;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.tracing.Printer;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * <p>For each chunk (or justification) take a copy of its conds and actions, and
 * the list of productions which were backtraced through in creating it. Also
 * keep a list of all of the grounds (WMEs in the supergoal) which were tested
 * as the chunk was formed.
 * 
 * <p>explain.h:38:explain_chunk_struct
 * 
 * @author ray
 */
public class ExplainChunk
{
    String name = ""; /* Name of this chunk/justification */
    Condition conds; /* Variablized list of conditions */
    Action actions; /* Variablized list of actions */
    Backtrace backtrace; /* List of back traced productions */
    ExplainChunk next_chunk; /* Next chunk in the list */
    Condition all_grounds; /*
                            * All conditions which go to LHS --
                            * must be in same order as the chunk's
                            * conditions.
                            */
    
    /**
     * Find the numbered condition in the chunk.
     * 
     * <p>explain.cpp:284:find_ground
     * 
     */
    Condition find_ground(final Printer printer, int number)
    {
        Condition ground = null;
        for(Condition cond = all_grounds; cond != null; cond = cond.next)
        {
            number--;
            if(number == 0)
                ground = cond;
        }
        if(number > 0)
        {
            printer.print("Could not find this condition.\n");
            return null;
        }
        return ground;
    }
    
    /**
     * <p>explain.cpp:305:explain_trace_chunk
     * 
     */
    void explain_trace_chunk(final Printer printer)
    {
        printer.print("Chunk : %s\n", name);
        Backtrace prod = backtrace;
        while(prod != null)
        {
            printer.print("Backtrace production : %s\n", prod.prod_name);
            printer.print("Result : %s\n", prod.result);
            if(prod.trace_cond != null)
            {
                printer.print("Trace condition : %s", prod.trace_cond);
            }
            else
                printer.print("The result preference is not stored, sorry.\n");
            printer.print("\nGrounds:\n");
            Conditions.print_list_of_conditions(printer, prod.grounds);
            printer.print("\nPotentials:\n");
            Conditions.print_list_of_conditions(printer, prod.potentials);
            printer.print("\nLocals:\n");
            Conditions.print_list_of_conditions(printer, prod.locals);
            printer.print("\nNegateds:\n");
            Conditions.print_list_of_conditions(printer, prod.negated);
            prod = prod.next_backtrace;
            printer.print("\n\n");
        }
    }
}
