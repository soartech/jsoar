/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 19, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.VariableGenerator;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class ConditionReorderer
{

    /* --- estimated k-search branching factors --- */
    private static final int MAX_COST = 10000005; // cost of a disconnected condition

    private static final int BF_FOR_ACCEPTABLE_PREFS = 8; // cost of (. ^. <var> +)
    private static final int BF_FOR_VALUES = 8; // cost of (. ^. <var>)
    private static final int BF_FOR_ATTRIBUTES = 8; // cost of (. ^<var> .)

    private VariableGenerator vars;

    private static class SavedTest
    {
        public SavedTest(SavedTest old_sts, Symbol var, ComplexTest the_test)
        {
            Arguments.checkNotNull(var, "var");
            Arguments.checkNotNull(the_test, "the_test");

            this.next = old_sts;
            this.var = var;
            this.the_test = the_test;
        }

        SavedTest next;
        Symbol var;
        ComplexTest the_test;
    }

    public ConditionReorderer(VariableGenerator vars)
    {
        this.vars = vars;
    }
    
    public void reorder_lhs(ByRef<Condition> lhs_top, ByRef<Condition> lhs_bottom, boolean reorder_nccs)
    {
        int tc = vars.getSyms().get_new_tc_number();
        /* don't mark any variables, since nothing is bound outside the LHS */

        LinkedList<Variable> roots = collect_root_variables(lhs_top.value, tc, true);

        /*
         * SBH/MVP 6-24-94 Fix to include only root "STATE" test in the LHS of a
         * chunk.
         */
        if (!roots.isEmpty())
        {
            remove_isa_state_tests_for_non_roots(lhs_top, lhs_bottom, roots);
        }

        /* MVP 6-8-94 - fix provided by Bob */
        if (roots.isEmpty())
        {

            for (Condition cond = lhs_top.value; cond != null; cond = cond.next)
            {
                PositiveCondition pc = cond.asPositiveCondition();
                if (pc != null && (TestTools.test_includes_goal_or_impasse_id_test(pc.id_test, true, false)))
                {
                    pc.id_test.addBoundVariables(tc, roots);
                    if (!roots.isEmpty())
                    {
                        break;
                    }
                }
            }
        }

        if (roots.isEmpty())
        {
            // TODO: Warning
            throw new IllegalStateException("LHS has no roots");
            // print (thisAgent, "Error: in production %s,\n",
            // thisAgent->name_of_production_being_reordered);
            // print (thisAgent, " The LHS has no roots.\n");
            // /* hmmm... most people aren't going to understand this error
            // message */
            // return FALSE;
        }

        fill_in_vars_requiring_bindings(lhs_top.value, tc);
        reorder_condition_list(lhs_top, lhs_bottom, roots, tc, reorder_nccs);
        remove_vars_requiring_bindings(lhs_top.value);
    }

    private void reorder_condition_list(ByRef<Condition> top_of_conds, ByRef<Condition> bottom_of_conds,
            List<Variable> roots, int tc, boolean reorder_nccs)
    {
        SavedTest saved_tests = simplify_condition_list(top_of_conds.value);
        reorder_simplified_conditions(top_of_conds, bottom_of_conds, roots, tc, reorder_nccs);
        restore_and_deallocate_saved_tests(top_of_conds.value, tc, saved_tests);

    }

    /**
     * 
     * reorder.cpp:405
     * 
     * @param value
     * @param tc
     * @param saved_tests
     */
    private void restore_and_deallocate_saved_tests(Condition conds_list, int tc, SavedTest tests_to_restore)
    {
        LinkedList<Variable> new_vars = new LinkedList<Variable>();
        for (Condition cond = conds_list; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc == null)
            {
                continue;
            }
            ByRef<Test> id_test = ByRef.create(pc.id_test);
            tests_to_restore = restore_saved_tests_to_test(id_test, true, tc, tests_to_restore);
            pc.id_test = id_test.value;

            pc.id_test.addBoundVariables(tc, new_vars);

            ByRef<Test> attr_test = ByRef.create(pc.attr_test);
            tests_to_restore = restore_saved_tests_to_test(attr_test, false, tc, tests_to_restore);
            pc.attr_test = attr_test.value;

            pc.attr_test.addBoundVariables(tc, new_vars);

            ByRef<Test> value_test = ByRef.create(pc.value_test);
            tests_to_restore = restore_saved_tests_to_test(value_test, false, tc, tests_to_restore);
            pc.value_test = value_test.value;

            pc.value_test.addBoundVariables(tc, new_vars);
        }
        if (tests_to_restore != null)
        {
            if (true /* TODO thisAgent->sysparams[PRINT_WARNINGS_SYSPARAM] */)
            {
                // TODO: Warning
                // print (thisAgent, "\nWarning: in production %s,\n",
                // thisAgent->name_of_production_being_reordered);
                // print (thisAgent, " ignoring test(s) whose referent is
                // unbound:\n");
                // print_saved_test_list (thisAgent, tests_to_restore);
                // // TODO: XML tagged output -- how to create this string?
                // // KJC TODO: need a tagged output version of
                // print_saved_test_list
                //
                // // XML generation
                // growable_string gs = make_blank_growable_string(thisAgent);
                // add_to_growable_string(thisAgent, &gs, "Warning: in
                // production ");
                // add_to_growable_string(thisAgent, &gs,
                // thisAgent->name_of_production_being_reordered);
                // add_to_growable_string(thisAgent, &gs, "\n ignoring test(s)
                // whose referent is unbound:");
                // //TODO: fill in XML print_saved_test_list. Possibile methods
                // include:
                // // 1) write a version which adds to a growable string
                // // 2) write a version which generates XML tags/attributes, so
                // we get "typed" output for this warning
                // // i.e. "<warning><string value="beginning of
                // message"></string><test att="val"></test><string value="rest
                // of message"></string></warning>
                // xml_generate_warning(thisAgent, text_of_growable_string(gs));
                //
                // free_growable_string(thisAgent, gs);
            }
            /* ought to deallocate the saved tests, but who cares */
        }
        Variable.unmark(new_vars); // unmark_variables_and_free_list
                                    // (thisAgent, new_vars);
    }

    /**
     * 
     * reorder.cpp:339
     * 
     * @param id_test
     * @param b
     * @param tc
     * @param tests_to_restore
     * @return
     */
    private SavedTest restore_saved_tests_to_test(ByRef<Test> t, boolean is_id_field, int bound_vars_tc_number,
            SavedTest tests_to_restore)
    {
        SavedTest prev_st = null, next_st = null;
        SavedTest st = tests_to_restore;
        while (st != null)
        {
            next_st = st.next;
            boolean added_it = false;

            if ((is_id_field && (st.the_test.asGoalIdTest() != null || st.the_test.asImpasseIdTest() != null))
                    || st.the_test.asDisjunctionTest() != null)
            {
                if (TestTools.test_includes_equality_test_for_symbol(t.value, st.var))
                {
                    TestTools.add_new_test_to_test_if_not_already_there(t, st.the_test);
                    added_it = true;
                }
            }
            RelationalTest rt = st.the_test.asRelationalTest();
            if (rt != null) // relational test other than equality
            {
                Symbol referent = rt.referent;
                if (TestTools.test_includes_equality_test_for_symbol(t.value, st.var))
                {
                    if (symbol_is_constant_or_marked_variable(referent, bound_vars_tc_number) || (st.var == referent))
                    {
                        TestTools.add_new_test_to_test_if_not_already_there(t, st.the_test);
                        added_it = true;
                    }
                }
                else if (TestTools.test_includes_equality_test_for_symbol(t.value, referent))
                {
                    if (symbol_is_constant_or_marked_variable(st.var, bound_vars_tc_number) || (st.var == referent))
                    {

                        rt.type = RelationalTest.reverse_direction_of_relational_test(rt.type);
                        rt.referent = st.var;
                        st.var = referent;
                        TestTools.add_new_test_to_test_if_not_already_there(t, st.the_test);
                        added_it = true;
                    }
                }
            }

            if (added_it)
            {
                if (prev_st != null)
                {
                    prev_st.next = next_st;
                }
                else
                {
                    tests_to_restore = next_st;
                }
                // symbol_remove_ref (thisAgent, st->var);
                // free_with_pool (&thisAgent->saved_test_pool, st);
            }
            else
            {
                prev_st = st;
            }
            st = next_st;
        } /* end of while (st) */
        return tests_to_restore;
    }

    /**
     * 
     * reorder.cpp:53
     * 
     * @param referent
     * @param bound_vars_tc_number
     * @return
     */
    private static boolean symbol_is_constant_or_marked_variable(Symbol referent, int bound_vars_tc_number)
    {
        Variable var = referent.asVariable();
        return var == null || var.tc_number == bound_vars_tc_number;
    }

    /**
     * 
     * reorder.cpp:836
     * 
     * @param top_of_conds
     * @param bottom_of_conds
     * @param roots
     * @param tc
     * @param reorder_nccs
     */
    private void reorder_simplified_conditions(ByRef<Condition> top_of_conds, ByRef<Condition> bottom_of_conds,
            List<Variable> roots, int bound_vars_tc_number, boolean reorder_nccs)
    {
        Condition remaining_conds = top_of_conds.value; // header of dll
        Condition first_cond = null;
        Condition last_cond = null;
        LinkedList<Variable> new_vars = new LinkedList<Variable>();
        Condition chosen;

        /*
         * repeat: scan through remaining_conds rate each one if tie, call
         * lookahead routine add min-cost item to conds
         */

        while (remaining_conds != null)
        {
            /* --- find min-cost set --- */
            Condition min_cost_conds = null;
            int min_cost = 0;
            int cost = 0;
            for (Condition cond = remaining_conds; cond != null; cond = cond.next)
            {
                cost = cost_of_adding_condition(cond, bound_vars_tc_number, roots);
                if (min_cost_conds == null || cost < min_cost)
                {
                    min_cost = cost;
                    min_cost_conds = cond;
                    cond.reorder.next_min_cost = null;
                }
                else if (cost == min_cost)
                {
                    cond.reorder.next_min_cost = min_cost_conds;
                    min_cost_conds = cond;
                }
                /*
                 * if (min_cost <= 1) break; This optimization needs to be
                 * removed, otherwise the tie set is not created. Without the
                 * tie set we can't check the canonical order.
                 */
            }
            /* --- if min_cost==MAX_COST, print error message --- */
            if (min_cost == MAX_COST /*
                                         * TODO &&
                                         * thisAgent->sysparams[PRINT_WARNINGS_SYSPARAM]
                                         */)
            {
                // TODO WARNING
                // print (thisAgent, "Warning: in production %s,\n",
                // thisAgent->name_of_production_being_reordered);
                // print (thisAgent, " The LHS conditions are not all
                // connected.\n");
                // /* BUGBUG I'm not sure whether this can ever happen. */
                //
                // // XML geneneration
                // growable_string gs = make_blank_growable_string(thisAgent);
                // add_to_growable_string(thisAgent, &gs, "Warning: in
                // production ");
                // add_to_growable_string(thisAgent, &gs,
                // thisAgent->name_of_production_being_reordered);
                // add_to_growable_string(thisAgent, &gs, "\n The LHS conditions
                // are not all connected.");
                // xml_generate_warning(thisAgent, text_of_growable_string(gs));
                // free_growable_string(thisAgent, gs);

            }
            /* --- if more than one min-cost item, and cost>1, do lookahead --- */
            if (min_cost > 1 && min_cost_conds.reorder.next_min_cost != null)
            {
                min_cost = MAX_COST + 1;
                for (Condition cond = min_cost_conds, next_cond = cond.reorder.next_min_cost; cond != null; cond = next_cond, next_cond = (cond != null ? cond.reorder.next_min_cost
                        : null))
                {
                    cost = find_lowest_cost_lookahead(remaining_conds, cond, bound_vars_tc_number, roots);
                    if (cost < min_cost)
                    {
                        min_cost = cost;
                        min_cost_conds = cond;
                        cond.reorder.next_min_cost = null;
                    }
                    else
                    {
                        /*******************************************************
                         * These code segments find the condition in the tie set
                         * with the smallest value in the canonical order. This
                         * ensures that productions with the same set of
                         * conditions are ordered the same. Except if the
                         * variables are assigned differently.
                         ******************************************************/
                        if (cost == min_cost && cond.asPositiveCondition() != null)
                        {
                            if (canonical_cond_greater(min_cost_conds, cond))
                            {
                                min_cost = cost;
                                min_cost_conds = cond;
                                cond.reorder.next_min_cost = null;
                            }
                        }
                    }
                    /** **************************************************************** */

                }
            }
            /** **************************************************************** */
            if (min_cost == 1 && min_cost_conds.reorder.next_min_cost != null)
            {
                for (Condition cond = min_cost_conds; cond != null; cond = cond.reorder.next_min_cost)
                {
                    if (cond.asPositiveCondition() != null && min_cost_conds.asPositiveCondition() != null
                            && canonical_cond_greater(min_cost_conds, cond))
                    {
                        min_cost = cost;
                        min_cost_conds = cond;
                    }
                    else if (cond.asPositiveCondition() == null && min_cost_conds.asPositiveCondition() != null)
                    {
                        min_cost = cost;
                        min_cost_conds = cond;
                    }
                }
            }
            /** **************************************************************** */

            /* --- install the first item in the min-cost set --- */
            chosen = min_cost_conds;
            remaining_conds = Condition.removeFromList(remaining_conds, chosen);
            if (first_cond == null)
            {
                first_cond = chosen;
            }
            last_cond = Condition.insertAtEnd(last_cond, chosen);

            /*
             * --- if a conjunctive negation, recursively reorder its conditions
             * ---
             */
            ConjunctiveNegationCondition ncc = chosen.asConjunctiveNegationCondition();
            if (ncc != null && reorder_nccs)
            {
                List<Variable> ncc_roots = collect_root_variables(ncc.top, bound_vars_tc_number, true);
                ByRef<Condition> top = ByRef.create(ncc.top);
                ByRef<Condition> bottom = ByRef.create(ncc.bottom);
                reorder_condition_list(top, bottom, ncc_roots, bound_vars_tc_number, reorder_nccs);
                ncc.top = top.value;
                ncc.bottom = bottom.value;
            }

            /* --- update set of bound variables for newly added condition --- */
            chosen.addBoundVariables(bound_vars_tc_number, new_vars);

            /*
             * --- if all roots are bound, set roots=NIL: don't need 'em anymore
             * ---
             */
            if (!roots.isEmpty())
            {
                boolean allBound = true;
                for (Variable v : roots)
                {
                    if (v.tc_number != bound_vars_tc_number)
                    {
                        allBound = false;
                        break;
                    }
                }
                if (allBound)
                {
                    roots.clear();
                }
            }

        } /* end of while (remaining_conds) */

        Variable.unmark(new_vars); // unmark_variables_and_free_list
                                    // (thisAgent, new_vars);
        top_of_conds.value = first_cond;
        bottom_of_conds.value = last_cond;
    }

    /**
     * reorder.cpp:508
     * 
     * @param t
     * @return
     */
    private int canonical_test(Test t)
    {
        final int NON_EQUAL_TEST_RETURN_VAL = 0; /* some unusual number */

        if (t.isBlank())
        {
            return NON_EQUAL_TEST_RETURN_VAL;
        }

        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Symbol sym = eq.getReferent();
            if (sym.asSymConstant() != null || sym.asIntConstant() != null || sym.asFloatConstant() != null)
            {
                return sym.hashCode(); // TODO: hash_id???
            }
            return NON_EQUAL_TEST_RETURN_VAL;
        }
        return NON_EQUAL_TEST_RETURN_VAL;
    }

    /**
     * Extensive discussion in reorder.cpp
     * 
     * reorder.cpp:536
     * 
     * @param c1
     * @param c2
     * @return
     */
    private boolean canonical_cond_greater(Condition c1, Condition c2)
    {
        int test_order_1, test_order_2;

        if ((test_order_1 = canonical_test(c1.asPositiveCondition().attr_test)) < (test_order_2 = canonical_test(c2
                .asPositiveCondition().attr_test)))
        {
            return true;
        }
        else if (test_order_1 == test_order_2
                && canonical_test(c1.asPositiveCondition().value_test) < canonical_test(c2.asPositiveCondition().value_test))
        {
            return true;
        }
        return false;
    }

    /**
     * Return an estimate of the "cost" of the lowest-cost condition that could
     * be added next, IF the given "chosen" condition is added first.
     * 
     * reorder.cpp:796
     * 
     * @param candidates
     * @param chosen
     * @param tc
     * @param root_vars_not_bound_yet
     * @return
     */
    private int find_lowest_cost_lookahead(Condition candidates, Condition chosen, int tc,
            List<Variable> root_vars_not_bound_yet)
    {
        LinkedList<Variable> new_vars = new LinkedList<Variable>();
        chosen.addBoundVariables(tc, new_vars);

        int min_cost = MAX_COST + 1;
        for (Condition c = candidates; c != null; c = c.next)
        {
            if (c == chosen)
                continue;

            int cost = cost_of_adding_condition(c, tc, root_vars_not_bound_yet);
            if (cost < min_cost)
            {
                min_cost = cost;
                if (cost <= 1)
                {
                    break;
                }
            }
        }
        Variable.unmark(new_vars); // unmark_variables_and_free_list
                                    // (thisAgent, new_vars);
        return min_cost;
    }

    /**
     * Return an estimate of the "cost" of the given condition. The current TC
     * should be the set of previously bound variables;
     * "root_vars_not_bound_yet" should be the set of other root variables.
     * 
     * reorder.cpp:724
     * 
     * @param cond
     * @param bound_vars_tc_number
     * @param roots
     * @return
     */
    private int cost_of_adding_condition(Condition cond, int tc, List<Variable> root_vars_not_bound_yet)
    {
        int result;

        /* --- handle the common simple case quickly up front --- */
        PositiveCondition pc = cond.asPositiveCondition();
        if (root_vars_not_bound_yet.isEmpty() && pc != null && pc.id_test.asEqualityTest() != null
                && pc.attr_test.asEqualityTest() != null && pc.value_test.asEqualityTest() != null
                && !pc.id_test.isBlank() && !pc.attr_test.isBlank() && !pc.value_test.isBlank())
        {

            if (!symbol_is_constant_or_marked_variable(pc.id_test.asEqualityTest().getReferent(), tc))
            {
                return MAX_COST;
            }
            if (symbol_is_constant_or_marked_variable(pc.attr_test.asEqualityTest().getReferent(), tc))
            {
                result = get_cost_of_possible_multi_attribute(pc.attr_test.asEqualityTest().getReferent());
            }
            else
            {
                result = BF_FOR_ATTRIBUTES;
            }

            if (!symbol_is_constant_or_marked_variable(pc.value_test.asEqualityTest().getReferent(), tc))
            {
                if (cond.test_for_acceptable_preference)
                {
                    result = result * BF_FOR_ACCEPTABLE_PREFS;
                }
                else
                {
                    result = result * BF_FOR_VALUES;
                }
            }
            return result;
        } /* --- end of common simple case --- */

        if (pc != null)
        {
            /* --- for pos cond's, check what's bound, etc. --- */
            if (!test_covered_by_bound_vars(pc.id_test, tc, root_vars_not_bound_yet))
            {
                return MAX_COST;
            }
            if (test_covered_by_bound_vars(pc.attr_test, tc, root_vars_not_bound_yet))
            {
                result = 1;
            }
            else
            {
                result = BF_FOR_ATTRIBUTES;
            }
            if (!test_covered_by_bound_vars(pc.value_test, tc, root_vars_not_bound_yet))
            {
                if (cond.test_for_acceptable_preference)
                {
                    result = result * BF_FOR_ACCEPTABLE_PREFS;
                }
                else
                {
                    result = result * BF_FOR_VALUES;
                }
            }
            return result;
        }
        /*
         * --- negated or NC conditions: just check whether all variables
         * requiring bindings are actually bound. If so, return 1, else return
         * MAX_COST ---
         */
        for (Variable v : cond.reorder.vars_requiring_bindings)
        {
            if (v.tc_number != tc)
            {
                return MAX_COST;
            }
        }
        return 1;
    }

    /**
     * Returns the user set value of the expected match cost of the
     * multi-attribute, or 1 if the input symbol isn't in the user set list.
     * 
     * reorder.cpp:707
     * 
     * @param referent
     * @return
     */
    private int get_cost_of_possible_multi_attribute(Symbol referent)
    {
        // TODO: Implement this when multi_attributes is implemented
        // multi_attribute *m = thisAgent->multi_attributes;
        // while(m) {
        // if(m->symbol == sym) return m->value;
        // m = m->next;
        // }
        return 1;
    }

    /**
     * Return TRUE iff the given test is covered by the previously bound
     * variables. The set of previously bound variables is given by the current
     * TC, PLUS any variables in the list "extra_vars."
     * 
     * reorder.cpp:677
     * 
     * @param id_test
     * @param tc
     * @param root_vars_not_bound_yet
     * @return
     */
    private boolean test_covered_by_bound_vars(Test t, int tc, List<Variable> extra_vars)
    {
        if (t.isBlank())
        {
            return false;
        }

        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Symbol referent = eq.getReferent();
            if (symbol_is_constant_or_marked_variable(referent, tc))
            {
                return true;
            }
            return extra_vars.contains(referent);
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test child : ct.conjunct_list)
            {
                if (test_covered_by_bound_vars(child, tc, extra_vars))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 
     * reorder.cpp:303
     * 
     * @param conds_list
     * @return
     */
    private SavedTest simplify_condition_list(Condition conds_list)
    {
        SavedTest sts = null;
        for (Condition c = conds_list; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc != null)
            {
                ByRef<Test> id_test = ByRef.create(pc.id_test);
                sts = simplify_test(id_test, sts);
                pc.id_test = id_test.value;

                ByRef<Test> attr_test = ByRef.create(pc.attr_test);
                sts = simplify_test(attr_test, sts);
                pc.attr_test = attr_test.value;

                ByRef<Test> value_test = ByRef.create(pc.value_test);
                sts = simplify_test(value_test, sts);
                pc.value_test = value_test.value;
            }
        }
        return sts;
    }

    /**
     * reorder.cpp:223
     * 
     * @param t
     * @param old_sts
     * @return
     */
    private SavedTest simplify_test(ByRef<Test> t, SavedTest old_sts)
    {
        if (t.value.isBlank())
        {
            Symbol sym = vars.generate_new_variable("dummy-");
            t.value = new EqualityTest(sym);
            return old_sts;
        }

        if (t.value.asEqualityTest() != null)
        {
            return old_sts;
        }

        ConjunctiveTest ct = t.value.asConjunctiveTest();
        if (ct != null)
        {
            /* --- look at subtests for an equality test --- */
            Symbol sym = null;
            for (Test subtest : ct.conjunct_list)
            {
                EqualityTest eq = subtest.asEqualityTest();
                if (eq != null)
                {
                    sym = eq.getReferent();
                }
            }
            /* --- if no equality test was found, generate a variable for it --- */
            if (sym == null)
            {
                sym = vars.generate_new_variable("dummy-");
                EqualityTest newTest = new EqualityTest(sym);
                ct.conjunct_list.add(0, newTest);
            }
            /*
             * --- scan through, create saved_test for subtests except equality
             * ---
             */
            Iterator<Test> it = ct.conjunct_list.iterator();
            while (it.hasNext())
            {
                Test subtest = it.next();
                if (subtest.asEqualityTest() == null)
                {
                    /*
                     * --- create saved_test, splice this cons out of
                     * conjunct_list ---
                     */
                    SavedTest saved = new SavedTest(old_sts, sym, subtest.asComplexTest());

                    old_sts = saved;

                    it.remove();
                }
            }
        }
        else
        {
            /*
             * --- goal/impasse, disjunction, and non-equality relational tests
             * ---
             */
            Variable var = vars.generate_new_variable("dummy-");
            EqualityTest New = new EqualityTest(var);
            SavedTest saved = new SavedTest(old_sts, var, t.value.asComplexTest());

            old_sts = saved;
            t.value = New;
            // *t = make_equality_test_without_adding_reference (sym);
        }
        return old_sts;
    }

    /**
     * reorder.cpp:560
     * 
     * @param cond_list
     */
    private void remove_vars_requiring_bindings(Condition cond_list)
    {
        /* --- scan through negated and NC cond's, remove lists from them --- */
        for (Condition c = cond_list; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc != null)
            {
                pc.reorder.vars_requiring_bindings.clear();
            }
            ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                remove_vars_requiring_bindings(ncc.top);
            }
        }
    }

    /**
     * 
     * reorder.cpp:1040
     * 
     * @param lhs_top
     * @param lhs_bottom
     * @param roots
     */
    private void remove_isa_state_tests_for_non_roots(ByRef<Condition> lhs_top, ByRef<Condition> lhs_bottom,
            List<Variable> roots)
    {
        ByRef<Boolean> a = ByRef.create(false);
        ByRef<Boolean> b = ByRef.create(false);

        for (Condition cond = lhs_top.value; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null && pc.id_test.asComplexTest() != null
                    && TestTools.test_includes_goal_or_impasse_id_test(pc.id_test, true, false)
                    && !TestTools.test_tests_for_root(pc.id_test, roots))
            {
                Test temp = pc.id_test;
                pc.id_test = TestTools.copy_test_removing_goal_impasse_tests(temp, a, b);
                // deallocate_test (thisAgent, temp); /* RBD fixed memory leak
                // 3/29/95 */
            }
        }
    }

    /**
     * This routine finds the root variables in a given condition list. The
     * caller should setup the current tc to be the set of variables bound
     * outside the given condition list. (This should normally be an empty TC,
     * except when the condition list is the subconditions of an NCC.) If the
     * "allow_printing_warnings" flag is TRUE, then this routine makes sure each
     * root variable is accompanied by a goal or impasse id test, and prints a
     * warning message if it isn't.
     * 
     * TODO: This belongs somewhere else
     * 
     * reorder.cpp:589:collect_root_variables
     * 
     * @param cond_list
     * @param tc
     * @param allow_printing_warnings
     * @return
     */
    public static LinkedList<Variable> collect_root_variables(Condition cond_list, int tc, boolean allow_printing_warnings)
    {
        // find everthing that's in the value slot of some condition
        LinkedList<Variable> new_vars_from_value_slot = new LinkedList<Variable>();
        for (Condition cond = cond_list; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                pc.value_test.addBoundVariables(tc, new_vars_from_value_slot);
            }
        }

        /* --- now see what else we can add by throwing in the id slot --- */
        LinkedList<Variable> new_vars_from_id_slot = new LinkedList<Variable>();
        for (Condition cond = cond_list; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                pc.id_test.addBoundVariables(tc, new_vars_from_id_slot);
            }
        }

        /* --- unmark everything we just marked --- */
        Variable.unmark(new_vars_from_value_slot);
        new_vars_from_value_slot = null; // unmark and deallocate list
        Variable.unmark(new_vars_from_id_slot);

        /* --- make sure each root var has some condition with goal/impasse --- */
        if (allow_printing_warnings /*
                                     * TODO &&
                                     * thisAgent->sysparams[PRINT_WARNINGS_SYSPARAM]
                                     */)
        {
            for (Variable var : new_vars_from_id_slot)
            {
                boolean found_goal_impasse_test = false;
                for (Condition cond = cond_list; cond != null; cond = cond.next)
                {
                    PositiveCondition pc = cond.asPositiveCondition();
                    if (pc == null)
                        continue;
                    if (TestTools.test_includes_equality_test_for_symbol(pc.id_test, var))
                    {
                        if (TestTools.test_includes_goal_or_impasse_id_test(pc.id_test, true, true))
                        {
                            found_goal_impasse_test = true;
                            break;
                        }
                    }
                }
                if (!found_goal_impasse_test)
                {
                    // TODO: WARNING

                    // print (thisAgent, "\nWarning: On the LHS of production
                    // %s, identifier ",
                    // thisAgent->name_of_production_being_reordered);
                    // print_with_symbols (thisAgent, "%y is not connected to
                    // any goal or impasse.\n",
                    // (Symbol *)(c->first));
                    //
                    // // XML geneneration
                    // growable_string gs =
                    // make_blank_growable_string(thisAgent);
                    // add_to_growable_string(thisAgent, &gs, "Warning: On the
                    // LHS of production ");
                    // add_to_growable_string(thisAgent, &gs,
                    // thisAgent->name_of_production_being_reordered);
                    // add_to_growable_string(thisAgent, &gs, ", identifier ");
                    // add_to_growable_string(thisAgent, &gs, symbol_to_string
                    // (thisAgent, (Symbol *)(c->first), true, 0, 0));
                    // add_to_growable_string(thisAgent, &gs, " is not connected
                    // to any goal or impasse.");
                    // xml_generate_warning(thisAgent,
                    // text_of_growable_string(gs));
                    // free_growable_string(thisAgent, gs);

                }
            }
        }

        return new_vars_from_id_slot;
    }

    /**
     * 
     * reorder.cpp:532
     * 
     * @param cond_list
     * @param tc
     */
    private void fill_in_vars_requiring_bindings(Condition cond_list, int tc)
    {

        /* --- add anything bound in a positive condition at this level --- */
        LinkedList<Variable> new_bound_vars = new LinkedList<Variable>();
        for (Condition c = cond_list; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc != null)
            {
                pc.addBoundVariables(tc, new_bound_vars);
            }
        }

        /* --- scan through negated and NC cond's, fill in stuff --- */
        for (Condition c = cond_list; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc != null)
            {
                collect_vars_tested_by_cond_that_are_bound(c, tc, pc.reorder.vars_requiring_bindings);
            }
            ConjunctiveNegationCondition ncc = c.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                fill_in_vars_requiring_bindings(ncc.top, tc);
            }
        }

        Variable.unmark(new_bound_vars);
    }

    /**
     * 
     * reorder.cpp:509
     * 
     * @param cond
     * @param tc
     * @param starting_list
     */
    private void collect_vars_tested_by_cond_that_are_bound(Condition cond, int tc, List<Variable> starting_list)
    {

        ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
        if (ncc != null)
        {
            for (Condition c = ncc.top; c != null; c = c.next)
            {
                collect_vars_tested_by_cond_that_are_bound(c, tc, starting_list);
            }
        }
        // Positive and Negative conditions
        ThreeFieldCondition tfc = cond.asThreeFieldCondition();
        if (tfc != null)
        {
            collect_vars_tested_by_test_that_are_bound(tfc.id_test, tc, starting_list);
            collect_vars_tested_by_test_that_are_bound(tfc.attr_test, tc, starting_list);
            collect_vars_tested_by_test_that_are_bound(tfc.value_test, tc, starting_list);
        }
    }

    /**
     * 
     * reorder.cpp:468
     * 
     * @param t
     * @param tc
     * @param starting_list
     */
    private void collect_vars_tested_by_test_that_are_bound(Test t, int tc, List<Variable> starting_list)
    {

        if (t.isBlank())
        {
            return;
        }

        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Variable referent = eq.getReferent().asVariable();
            if (referent != null && referent.tc_number == tc && !starting_list.contains(referent))
            {
                starting_list.add(referent);
            }
            return;
        }

        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                collect_vars_tested_by_test_that_are_bound(c, tc, starting_list);
            }
            return;
        }
        RelationalTest rt = t.asRelationalTest();
        if (rt != null)
        {
            Variable referent = rt.referent.asVariable();
            if (referent != null && referent.tc_number == tc && !starting_list.contains(referent))
            {
                starting_list.add(referent);
            }
        }

        // Do nothing for GoalId, Impasse, or disjunction
    }

}
