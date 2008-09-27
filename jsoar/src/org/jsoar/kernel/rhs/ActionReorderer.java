/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 19, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.LinkedList;

import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class ActionReorderer
{
    private final Printer printer;
    private final String prodName;
    
    /*
     * =====================================================================
     * 
     * Reordering for RHS Actions
     * 
     * Whenever a new identifier is created, we need to know (at creation time)
     * what level of the goal stack it's at. If the <newid> appears in the
     * attribute or value slot of a make, we just give it the same level as
     * whatever's in the id slot of that make. But if <newid> appears in the id
     * slot of a make, we can't tell what level it goes at.
     * 
     * To avoid this problem, we reorder the list of RHS actions so that when
     * the actions are executed (in the resulting order), each <newid> is
     * encountered in an attribute or value slot *before* it is encountered in
     * an id slot.
     * 
     * Furthermore, we make sure all arguments to function calls are bound
     * before the function call is executed.
     * 
     * Reorder_action_list() does the reordering. Its parameter action_list is
     * reordered in place (destructively modified). It also requires at entry
     * that the variables bound on the LHS are marked. The function returns TRUE
     * if successful, FALSE if it was unable to produce a legal ordering.
     * =====================================================================
     */

    /**
     * Construct a new reorderer
     * 
     * @param printer Printer used for errors
     * @param prodName Name of production
     */
    public ActionReorderer(Printer printer, String prodName)
    {
        this.printer = printer;
        this.prodName = prodName;
    }

    /**
     * reorder.cpp:86:reorder_action_list
     * 
     * @param action_list
     * @param lhs_tc
     * @throws ReordererException on error
     */
    public void reorder_action_list(ByRef<Action> action_list, int lhs_tc) throws ReordererException
    {
        LinkedList<Variable> new_bound_vars = new LinkedList<Variable>();
        Action remaining_actions = action_list.value;
        Action first_action = null;
        Action last_action = null;
        Action prev_a = null;

        while (remaining_actions != null)
        {
            // scan through remaining_actions, look for one that's legal
            prev_a = null;
            Action a = remaining_actions;
            while (true)
            {
                if (a == null)
                {
                    break; /* looked at all candidates, but none were legal */
                }
                if (legal_to_execute_action(a, lhs_tc))
                {
                    break;
                }
                prev_a = a;
                a = a.next;
            }
            if (a == null)
            {
                break;
            }
            // move action a from remaining_actions to reordered list
            if (prev_a != null)
            {
                prev_a.next = a.next;
            }
            else
            {
                remaining_actions = a.next;
            }
            a.next = null;
            if (last_action != null)
            {
                last_action.next = a;
            }
            else
            {
                first_action = a;
            }
            last_action = a;
            
            // add new variables from a to new_bound_vars
            Action.addAllVariables(a, lhs_tc, new_bound_vars);
        }

        if (remaining_actions != null)
        {
            /* --- reconstruct list of all actions --- */
            if (last_action != null)
                last_action.next = remaining_actions;
            else
                first_action = remaining_actions;

            // There are remaining_actions but none can be legally added
            String message = String.format("Error: production %s has a bad RHS--\n" +
                          " Either it creates structure not connected to anything\n" +  
                          " else in WM, or it tries to pass an unbound variable as\n" + 
                          " an argument to a function.\n", prodName);
            printer.error(message);
            
            throw new ReordererException(message);
        }

        /* --- unmark variables that we just marked --- */
        for (Variable var : new_bound_vars)
        {
            var.unmark();
        }

        /* --- return final result --- */
        action_list.value = first_action;

    }

    /**
     * reorder.cpp:164:legal_to_execute_action
     * 
     * @param a
     * @param tc
     * @return
     */
    private boolean legal_to_execute_action(Action a, int tc)
    {
        MakeAction ma = a.asMakeAction();
        if (ma != null)
        {
            if (!all_variables_in_rhs_value_bound(ma.id, tc))
            {
                return false;
            }
            if (ma.attr.asFunctionCall() != null
                    && (!all_variables_in_rhs_value_bound(ma.attr, tc)))
            {
                return false;
            }
            if (ma.value.asFunctionCall() != null
                    && (!all_variables_in_rhs_value_bound(ma.value, tc)))
            {
                return false;
            }
            if (a.preference_type.isBinary()
                    && ma.referent.asFunctionCall() != null
                    && (!all_variables_in_rhs_value_bound(ma.referent, tc)))
            {
                return false;
            }
            return true;
        }
        /* --- otherwise it's a function call; make sure args are all bound --- */
        return all_variables_in_rhs_value_bound(a.asFunctionAction().getCall(),
                tc);
    }

    private boolean all_variables_in_rhs_value_bound(RhsValue rv, int tc)
    {

        RhsFunctionCall fc = rv.asFunctionCall();

        if (fc != null)
        {
            /* --- function calls --- */
            for (RhsValue arg : fc.getArguments())
            {
                if (!all_variables_in_rhs_value_bound(arg, tc))
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            /* --- ordinary (symbol) rhs values --- */
            Variable var = rv.asSymbolValue().getSym().asVariable();
            if (var != null)
            {
                return var.tc_number == tc;
            }
            return true;
        }
    }

}
