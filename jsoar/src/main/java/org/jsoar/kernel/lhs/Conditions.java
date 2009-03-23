/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.Iterator;
import java.util.LinkedList;

import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.ListItem;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;

/**
 * Utility methods for working with LHS conditions
 * 
 * @author ray
 */
public class Conditions
{
    private Conditions() {}

    /**
     * This routine finds the root variables in a given condition list. The
     * caller should setup the current tc to be the set of variables bound
     * outside the given condition list. (This should normally be an empty TC,
     * except when the condition list is the subconditions of an NCC.) If the
     * "allow_printing_warnings" flag is TRUE, then this routine makes sure each
     * root variable is accompanied by a goal or impasse id test, and prints a
     * warning message if it isn't.
     * 
     * <p>reorder.cpp:580:collect_root_variables
     * 
     * @param cond_list
     * @param tc
     * @param printer (originally allow_printing_warnings) if not <code>null</code>
     *      warnings will be printed to this printer
     * @return
     */
    public static ListHead<Variable> collect_root_variables(Condition cond_list, int tc, Printer printer, String name_of_production_being_reordered)
    {
        // find everthing that's in the value slot of some condition
        ListHead<Variable> new_vars_from_value_slot = ListHead.newInstance();
        for (Condition cond = cond_list; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                pc.value_test.addBoundVariables(tc, new_vars_from_value_slot);
            }
        }
    
        // now see what else we can add by throwing in the id slot
        ListHead<Variable> new_vars_from_id_slot = ListHead.newInstance();
        for (Condition cond = cond_list; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                pc.id_test.addBoundVariables(tc, new_vars_from_id_slot);
            }
        }
    
        // unmark everything we just marked
        Variable.unmark(new_vars_from_value_slot);
        new_vars_from_value_slot = null; // unmark and deallocate list
        Variable.unmark(new_vars_from_id_slot);
    
        // make sure each root var has some condition with goal/impasse
        if (printer != null && printer.isPrintWarnings())
        {
            for (ListItem<Variable> var = new_vars_from_id_slot.first; var != null; var = var.next)
            {
                boolean found_goal_impasse_test = false;
                for (Condition cond = cond_list; cond != null; cond = cond.next)
                {
                    PositiveCondition pc = cond.asPositiveCondition();
                    if (pc == null)
                        continue;
                    if (Tests.test_includes_equality_test_for_symbol(pc.id_test, var.item))
                    {
                        if (Tests.test_includes_goal_or_impasse_id_test(pc.id_test, true, true))
                        {
                            found_goal_impasse_test = true;
                            break;
                        }
                    }
                }
                if (!found_goal_impasse_test)
                {
                    printer.warn("\nWarning: On the LHS of production %s, identifier %s is not " +
                    		     "connected to any goal or impasse.\n",
                                 name_of_production_being_reordered, var.item);
    
                }
            }
        }
    
        return new_vars_from_id_slot;
    }

    /**
     * print.cpp:1103:print_list_of_conditions
     * 
     * @param printer
     * @param cond
     */
    public static void print_list_of_conditions(Printer printer, Condition cond)
    {
        while (cond != null)
        {
            if (printer.getOutputColumn() >= printer.getColumnsPerLine() - 20)
                printer.print("\n      %s\n", cond);
            cond = cond.next;
        }
    }

    /**
     * <p>print.cpp:488:print_condition_list
     * 
     * @param printer
     * @param conds
     * @param indent
     * @param internal
     */
    public static void print_condition_list(Printer printer, Condition conds, int indent, boolean internal)
    {
        if (conds == null)
            return;
    
        // build dl_list of all the actions
        LinkedList<Condition> conds_not_yet_printed = new LinkedList<Condition>();
    
        for (Condition c = conds; c != null; c = c.next)
        {
            conds_not_yet_printed.add(c);
        }
    
        // main loop: find all conds for first id, print them together
        boolean did_one_line_already = false;
        while (!conds_not_yet_printed.isEmpty())
        {
            if (did_one_line_already)
            {
                printer.print("\n").spaces(indent);
            }
            else
            {
                did_one_line_already = true;
            }
    
            final Condition c = conds_not_yet_printed.pop();
            final ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                printer.print("-{");
                print_condition_list(printer, ncc.top, indent + 2, internal);
                printer.print("}");
                continue;
            }
    
            // normal pos/neg conditions
            ThreeFieldCondition tfc = c.asThreeFieldCondition();
            ByRef<Boolean> removed_goal_test = ByRef.create(false);
            ByRef<Boolean> removed_impasse_test = ByRef.create(false);
    
            Test id_test = Tests.copy_test_removing_goal_impasse_tests(tfc.id_test, removed_goal_test,
                    removed_impasse_test);
            Test id_test_to_match = Tests.copy_of_equality_test_found_in_test(id_test);
    
            // collect all cond's whose id test matches this one, removing them
            // from the conds_not_yet_printed list
            LinkedList<ThreeFieldCondition> conds_for_this_id = new LinkedList<ThreeFieldCondition>();
            conds_for_this_id.add(tfc);
            if (!internal)
            {
                Iterator<Condition> it = conds_not_yet_printed.iterator();
                while (it.hasNext())
                {
                    final Condition n = it.next();
    
                    // pick_conds_with_matching_id_test
                    ThreeFieldCondition ntfc = n.asThreeFieldCondition();
                    if (ntfc != null && Tests.tests_are_equal(id_test_to_match, ntfc.id_test))
                    {
                        conds_for_this_id.add(ntfc);
                        it.remove();
                    }
                }
            }
    
            // print the collected cond's all together
            printer.print(" (");
            if (removed_goal_test.value)
            {
                printer.print("state ");
            }
    
            if (removed_impasse_test.value)
            {
                printer.print("impasse ");
            }
    
            printer.print("%s", id_test);
    
            while (!conds_for_this_id.isEmpty())
            {
                final ThreeFieldCondition tc = conds_for_this_id.pop();
    
                { // build and print attr/value test for condition c
                    final StringBuilder gs = new StringBuilder();
                    gs.append(" ");
                    if (tc.asNegativeCondition() != null)
                    {
                        gs.append("-");
                    }
    
                    gs.append("^");
                    gs.append(String.format("%s", tc.attr_test));
                    if (!Tests.isBlank(tc.value_test))
                    {
                        gs.append(String.format(" %s", tc.value_test));
                        if (tc.test_for_acceptable_preference)
                        {
                            gs.append(" +");
                        }
                    }
                    if (printer.getOutputColumn() + gs.length() >= printer.getColumnsPerLine())
                    {
                        printer.print("\n").spaces(indent + 6);
                    }
                    printer.print(gs.toString());
                }
            }
            printer.print(")");
        }
    }
    
    
}
