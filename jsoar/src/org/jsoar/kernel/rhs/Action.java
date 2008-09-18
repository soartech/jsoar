/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.LinkedList;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public abstract class Action
{
    public Action next;
    public PreferenceType preference_type;
    public ActionSupport support = ActionSupport.UNKNOWN_SUPPORT;
    public boolean already_in_tc;  /* used only by compile-time o-support calcs */
    
    public static void addAllVariables(Action head, int tc_number, LinkedList<Variable> var_list)
    {
        for(Action a = head; a != null; a = a.next)
        {
            a.addAllVariables(tc_number, var_list);
        }
    }
    
    public abstract void addAllVariables(int tc_number, LinkedList<Variable> var_list);

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
                return false;
            if (ma1.attr != ma2.attr)
                return false;
            if (ma1.value != ma2.value)
                return false;
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

    /**
     * production.cpp:1428:action_is_in_tc
     * 
     * @param tc
     * @return
     */
    public boolean action_is_in_tc(int tc)
    {
        // TODO Implement action_is_in_tc in sub-classes
        throw new UnsupportedOperationException("Not implemented");

        //return false;
    }

    /**
     * production.cpp:1353:add_action_to_tc
     * 
     * @param tc
     * @param id_list
     * @param var_list
     */
    public void add_action_to_tc(int tc, LinkedList<Identifier> id_list, LinkedList<Variable> var_list)
    {
        // Do nothing by default
        
        // TODO: Implement add_action_to_tc in sub-classes
        throw new UnsupportedOperationException("Not implemented");
    }
    

    /**
     * 
     * TODO This function doesn't belong here. Circular dependency on rete package
     * TODO This function should be polymorphic on Action
     * 
     * rete.cpp:4297:copy_action_list_and_substitute_varnames
     * 
     * @param rete
     * @param actions
     * @param cond
     * @return
     */
    public static Action copy_action_list_and_substitute_varnames(Rete rete, Action actions, Condition cond)
    {
        Action prev = null;
        Action first = null;
        Action old = actions;
        Action New = null;
        while (old != null)
        {
            if (old instanceof MakeAction)
            {
                MakeAction oldMake = (MakeAction) old;
                MakeAction newMake = new MakeAction();

                newMake.id = (RhsSymbolValue) RhsValue.copy_rhs_value_and_substitute_varnames(rete, oldMake.id, cond,
                        's');
                newMake.attr = RhsValue.copy_rhs_value_and_substitute_varnames(rete, oldMake.attr, cond, 'a');
                char first_letter = newMake.attr.getFirstLetter();
                newMake.value = RhsValue
                        .copy_rhs_value_and_substitute_varnames(rete, oldMake.value, cond, first_letter);
                if (old.preference_type.isBinary())
                {
                    newMake.referent = RhsValue.copy_rhs_value_and_substitute_varnames(rete, oldMake.referent, cond,
                            first_letter);
                }

                New = newMake;

            }
            else if (old instanceof FunctionAction)
            {
                FunctionAction oldFunc = (FunctionAction) old;
                FunctionAction newFunc = new FunctionAction((RhsFunctionCall) RhsValue
                        .copy_rhs_value_and_substitute_varnames(rete, oldFunc.call, cond, 'v'));

                New = newFunc;
            }
            else
            {
                throw new IllegalStateException("Unknown action type: " + old);
            }

            if (prev != null)
                prev.next = New;
            else
                first = New;
            prev = New;

            New.preference_type = old.preference_type;
            New.support = old.support;

            old = old.next;
        }
        
        if (prev != null)
            prev.next = null;
        else
            first = null;
        return first;
    }

}
