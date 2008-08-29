/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.List;

import org.jsoar.kernel.PreferenceType;
import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public abstract class Action
{
    public Action next;
    public PreferenceType preference_type;
    public ActionSupport support = ActionSupport.UNKNOWN_SUPPORT;
    boolean already_in_tc;  /* used only by compile-time o-support calcs */
    
    public static void addAllVariables(Action head, int tc_number, List<Variable> var_list)
    {
        for(Action a = head; a != null; a = a.next)
        {
            a.addAllVariables(tc_number, var_list);
        }
    }
    
    public abstract void addAllVariables(int tc_number, List<Variable> var_list);

    public MakeAction asMakeAction()
    {
        return null;
    }
    public FunctionAction asFunctionAction()
    {
        return null;
    }

    /**
     * Tests whether two RHS's (i.e., action lists) are the same (except
     * for function calls).  This is used for finding duplicate productions.
     * 
     * rete.cpp:3374:same_rhs
     * 
     * @param rhs1
     * @param rhs2
     * @return
     */
    public static boolean same_rhs(Action rhs1, Action rhs2)
    {
        /*
         * --- Scan through the two RHS's; make sure there's no function calls,
         * and make sure the actions are all the same. ---
         */
        /*
         * --- Warning: this relies on the representation of rhs_value's: two of
         * the same funcall will not be equal (==), but two of the same symbol,
         * reteloc, or unboundvar will be equal (==). ---
         */

        Action a1 = rhs1;
        Action a2 = rhs2;

        while (a1 != null && a2 != null)
        {
            if (a1.asFunctionAction() != null)
                return false;
            if (a2.asFunctionAction() != null)
                return false;
            if (a1.preference_type != a2.preference_type)
                return false;

            MakeAction ma1 = a1.asMakeAction();
            MakeAction ma2 = a2.asMakeAction();
            if (ma1.id != ma2.id)
                return false; // TODO: Assumes equal syms
            if (ma1.attr != ma2.attr)
                return false; // TODO: Assumes equal syms
            if (ma1.value != ma2.value)
                return false; // TODO: Assumes equal syms
            if (ma1.preference_type.isBinary())
            {
                if (ma1.referent != ma2.referent)
                {
                    return false;
                }
            }
            a1 = a1.next;
            a2 = a2.next;
        }

        /* --- If we reached the end of one RHS but not the other, then
           they must be different --- */
        if (a1 != a2)
            return false;

        /* --- If we got this far, the RHS's must be identical. --- */
        return true;
    }
}
