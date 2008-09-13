/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 8, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.ProductionSupport;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConditionReorderer;
import org.jsoar.kernel.lhs.ConjunctiveNegationCondition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.DisjunctionTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.TestTools;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.ActionSupport;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * 
 * osupport.cpp
 * 
 * @author ray
 */
public class OSupport
{
    private final PredefinedSymbols syms;
    
    /**
     * agent.h:687:o_support_calculation_type
     */
    public int o_support_calculation_type = 4;

    private static enum YesNoMaybe
    {
        YES, NO, MAYBE
    }
    
    /**
     * agent.h:658:o_support_tc
     */
    private int o_support_tc;
    
    /**
     * agent.h:659:rhs_prefs_from_instantiation
     */
    private ListHead<Preference> rhs_prefs_from_instantiation = new ListHead<Preference>();
    
    
    /**
     * @param syms
     * @param operator_symbol
     */
    public OSupport(PredefinedSymbols syms)
    {
        this.syms = syms;
    }

    /**
     * osupport.cpp:63:add_to_os_tc_if_needed
     * 
     * @param sym
     */
    private void add_to_os_tc_if_needed(Symbol sym)
    {
        Identifier id = sym.asIdentifier();
        if (id != null)
        {
            add_to_os_tc(id, false);
        }
    }

    /**
     * osupport.cpp:72:add_to_os_tc_if_id
     * 
     * @param sym
     * @param flag
     */
    private void add_to_os_tc_if_id(Symbol sym, boolean flag)
    {
        Identifier id = sym.asIdentifier();
        if (id != null)
        {
            add_to_os_tc(id, flag);
        }
    }

    /**
     * SBH 4/14/93
     * For NNPSCM, we must exclude the operator slot from the transitive closure of a state.
     * Do that by passing a boolean argument, "isa_state" to this routine.
     * If it isa_state, check for the operator slot before the recursive call.
     * 
     * osupport.cpp:84:add_to_os_tc
     * @param id
     * @param isa_state
     */
    private void add_to_os_tc(Identifier id, boolean isa_state)
    {
        // if id is already in the TC, exit; else mark it as in the TC
        if (id.tc_number == o_support_tc)
        {
            return;
        }

        id.tc_number = o_support_tc;

        // scan through all preferences and wmes for all slots for this id
        for (Wme w : id.input_wmes)
        {
            add_to_os_tc_if_needed(w.value);
        }
        for (Slot s : id.slots)
        {
            if ((!isa_state) || (s.attr != syms.operator_symbol))
            {
                for (Preference pref : s.all_preferences)
                {
                    add_to_os_tc_if_needed(pref.value);
                    if (pref.type.isBinary())
                        add_to_os_tc_if_needed(pref.referent);
                }
                for (Wme w : s.wmes)
                {
                    add_to_os_tc_if_needed(w.value);
                }
            }
        } /* end of for slots loop */
        /* --- now scan through RHS prefs and look for any with this id --- */
        for (Preference pref : rhs_prefs_from_instantiation)
        {
            if (pref.id == id)
            {
                if ((!isa_state) || (pref.attr != syms.operator_symbol))
                {
                    add_to_os_tc_if_needed(pref.value);
                    if (pref.type.isBinary())
                    {
                        add_to_os_tc_if_needed(pref.referent);
                    }
                }
            }
        }
        /* We don't need to worry about goal/impasse wmes here, since o-support tc's
           never start there and there's never a pointer to a goal or impasse from
           something else. */
    }
    
    /**
     * osupport.cpp:122:begin_os_tc
     * 
     * @param rhs_prefs_or_nil
     */
    private void begin_os_tc(AsListItem<Preference> rhs_prefs_or_nil)
    {
        o_support_tc = syms.getSyms().get_new_tc_number();
        rhs_prefs_from_instantiation.first = rhs_prefs_or_nil;
    }
    
    /**
     * After a TC has been marked with the above routine, these utility routines
     * are used for checking whether certain things are in the TC.
     * Test_has_id_in_os_tc() checks whether a given test contains an equality
     * test for any identifier in the TC, other than the identifier
     * "excluded_sym". Id_or_value_of_condition_list_is_in_os_tc() checks
     * whether any id or value test in the given condition list (including
     * id/value tests inside NCC's) has a test for an id in the TC. In the case
     * of value tests, the id is not allowed to be "sym_excluded_from_value".
     * 
     * osupport.cpp:140:test_has_id_in_os_tc
     * 
     * @param t
     * @param excluded_sym
     * @return
     */
    private boolean test_has_id_in_os_tc(Test t, Symbol excluded_sym)
    {
        if (t.isBlank())
        {
            return false;
        }
        
        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Identifier referent = eq.getReferent().asIdentifier();
            if (referent != null)
            {
                if (referent.tc_number == o_support_tc)
                {
                    if (referent != excluded_sym)
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            for (Test c : ct.conjunct_list)
            {
                if (test_has_id_in_os_tc(c, excluded_sym))
                {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * osupport.cpp:163:id_or_value_of_condition_list_is_in_os_tc
     * 
     * @param conds
     * @param sym_excluded_from_value
     * @param match_state_to_exclude_test_of_the_operator_off_of
     * @return
     */
    private boolean id_or_value_of_condition_list_is_in_os_tc(Condition conds, Symbol sym_excluded_from_value,
            Symbol match_state_to_exclude_test_of_the_operator_off_of)
    {
        /*
         * RBD 8/19/94 Under NNPSCM, when we use this routine to look for
         * "something off the state", we want to exclude tests of (match_state
         * ^operator _).
         */
        for (; conds != null; conds = conds.next)
        {
            // TODO: The original switch statement here was a little tricky. 
            // I think I got the gist of it though.
            
            // Positive or negative condition
            ThreeFieldCondition tfc = conds.asThreeFieldCondition();
            if (tfc != null)
            {
                if (TestTools.test_includes_equality_test_for_symbol(tfc.id_test,
                        match_state_to_exclude_test_of_the_operator_off_of)
                        && TestTools.test_includes_equality_test_for_symbol(tfc.attr_test, syms.operator_symbol))
                {
                    return false;
                }
                if (test_has_id_in_os_tc(tfc.id_test, null))
                {
                    return true;
                }
                if (test_has_id_in_os_tc(tfc.value_test, sym_excluded_from_value))
                {
                    return true;
                }
            }
            ConjunctiveNegationCondition ncc = conds.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                if (id_or_value_of_condition_list_is_in_os_tc(ncc.top, sym_excluded_from_value,
                        match_state_to_exclude_test_of_the_operator_off_of))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * GAP 10-6-94
     * 
     * This routine checks to see if the identifier is one of the context
     * objects i.e. it is the state somewhere in the context stack. This is used
     * to ensure that O-support is not given to context objects in super-states.
     * 
     * osupport.cpp:207:is_state_id
     * 
     * @param top_goal Originally retrieved from agent struct.
     * @param sym
     * @param match_state
     * @return
     */
    private boolean is_state_id(Identifier top_goal, Symbol sym, Symbol match_state)
    {
        for (Identifier c = top_goal; c != match_state; c = c.lower_goal)
        {
            if (sym == c)
            {
                return true;
            }
        }

        return sym == match_state;
    }
    
    /**
     *                 Run-Time O-Support Calculation
     *
     * This routine calculates o-support for each preference for the given
     * instantiation, filling in pref.o_supported (true or false) on each one.
     *
     * The following predicates are used for support calculations.  In the
     * following, "lhs has some elt. ..." means the lhs has some id or value
     * at any nesting level.
     *
     *  lhs_oa_support:
     *    (1) does lhs test (match_goal ^operator match_operator NO) ?
     *    (2) mark TC (match_operator) using TM;
     *        does lhs has some elt. in TC but != match_operator ?
     *    (3) mark TC (match_state) using TM;
     *        does lhs has some elt. in TC ?
     *  lhs_oc_support:
     *    (1) mark TC (match_state) using TM;
     *        does lhs has some elt. in TC but != match_state ?
     *  lhs_om_support:
     *    (1) does lhs tests (match_goal ^operator) ?
     *    (2) mark TC (match_state) using TM;
     *        does lhs has some elt. in TC but != match_state ?
     *
     *  rhs_oa_support:
     *    mark TC (match_state) using TM+RHS;
     *    if pref.id is in TC, give support
     *  rhs_oc_support:
     *    mark TC (inst.rhsoperators) using TM+RHS;
     *    if pref.id is in TC, give support
     *  rhs_om_support:
     *    mark TC (inst.lhsoperators) using TM+RHS;
     *    if pref.id is in TC, give support
     *
     * BUGBUG the code does a check of whether the lhs tests the match state via
     *       looking just at id and value fields of top-level positive cond's.
     *       It doesn't look at the attr field, or at any negative or NCC's.
     *       I'm not sure whether this is right or not.  (It's a pretty
     *       obscure case, though.)
     * 
     * TODO Re-check braces in this method :(
     * 
     * osupport.cpp:267:calculate_support_for_instantiation_preferences
     * 
     * @param inst
     * @param top_goal
     * @param operand2_mode
     */
    public void calculate_support_for_instantiation_preferences(Instantiation inst, final Identifier top_goal,
            final boolean operand2_mode)
    {
        Identifier match_goal, match_state, match_operator;
        Wme match_operator_wme;
        boolean lhs_tests_operator_installed;
        boolean lhs_tests_operator_acceptable_or_installed;
        boolean lhs_is_known_to_test_something_off_match_state;
        boolean lhs_is_known_to_test_something_off_match_operator;
        boolean rhs_does_an_operator_creation;
        boolean oc_support_possible;
        boolean om_support_possible;
        boolean oa_support_possible;
        Wme w;
        Condition lhs, c;

        /* RCHONG: begin 10.11 */

        Action act;
        boolean o_support, op_elab;
        boolean operator_proposal;
        int pass;
        Wme lowest_goal_wme;

        /* RCHONG: end 10.11 */

        /* REW: begin 09.15.96 */

        if (operand2_mode)
        {
            // TODO: verbose
            // if (thisAgent.soar_verbose_flag == true) {
            // printf("\n in calculate_support_for_instantiation_preferences:");
            // xml_generate_verbose(thisAgent, "in
            // calculate_support_for_instantiation_preferences:");
            // }
            o_support = false;
            op_elab = false;

            if (inst.prod.declared_support == ProductionSupport.DECLARED_O_SUPPORT)
            {
                o_support = true;
            }
            else if (inst.prod.declared_support == ProductionSupport.DECLARED_I_SUPPORT)
            {
                o_support = false;
            }
            else if (inst.prod.declared_support == ProductionSupport.UNDECLARED_SUPPORT)
            {
                /*
                 * check if the instantiation is proposing an operator. if it
                 * is, then this instantiation is i-supported.
                 */

                operator_proposal = false;
                for (act = inst.prod.action_list; act != null; act = act.next)
                {
                    MakeAction ma = act.asMakeAction();
                    if (ma != null && ma.attr.asSymbolValue() != null)
                    {

                        // TODO: Is toString() correct here?
                        if (ma.attr.asSymbolValue().sym.toString().equals("operator")
                                && (act.preference_type == PreferenceType.ACCEPTABLE_PREFERENCE_TYPE))
                        {
                            operator_proposal = true;
                            o_support = false;
                            break;
                        }
                    }
                }

                if (operator_proposal == false)
                {
                    /*
                     * an operator wasn't being proposed, so now we need to test
                     * if the operator is being tested on the LHS.
                     * 
                     * i'll need to make two passes over the wmes that pertain
                     * to this instantiation. the first pass looks for the
                     * lowest goal identifier. the second pass looks for a wme
                     * of the form:
                     *  (<lowest-goal-id> ^operator ...)
                     * 
                     * if such a wme is found, then this o-support = true; false
                     * otherwise.
                     * 
                     * this code is essentially identical to that in
                     * p_node_left_addition() in rete.c.
                     * 
                     * BUGBUG this check only looks at positive conditions. we
                     * haven't really decided what testing the absence of the
                     * operator will do. this code assumes that such a
                     * productions (instantiation) would get i-support.
                     */

                    lowest_goal_wme = null;

                    for (pass = 0; pass != 2; pass++)
                    {

                        for (c = inst.top_of_instantiated_conditions; c != null; c = c.next)
                        {
                            PositiveCondition pc = c.asPositiveCondition();
                            if (pc != null)
                            {
                                w = pc.bt.wme_;

                                if (pass == 0)
                                {
                                    if (w.id.isa_goal == true)
                                    {
                                        if (lowest_goal_wme == null)
                                        {
                                            lowest_goal_wme = w;
                                        }
                                        else
                                        {
                                            if (w.id.level > lowest_goal_wme.id.level)
                                            {
                                                lowest_goal_wme = w;
                                            }
                                        }
                                    }

                                }
                                else
                                {
                                    if ((w.attr == syms.operator_symbol) && (w.acceptable == false)
                                            && (w.id == lowest_goal_wme.id))
                                    {
                                        if (o_support_calculation_type == 3 || o_support_calculation_type == 4)
                                        {

                                            /*
                                             * iff RHS has only operator
                                             * elaborations then it's IE_PROD,
                                             * otherwise PE_PROD, so look for
                                             * non-op-elabs in the actions KJC
                                             * 1/00
                                             */
                                            for (act = inst.prod.action_list; act != null; act = act.next)
                                            {
                                                MakeAction ma = act.asMakeAction();
                                                if (ma != null)
                                                {
                                                    RhsSymbolValue symVal = ma.id.asSymbolValue();
                                                    ReteLocation reteLoc = ma.id.asReteLocation();
                                                    if (symVal != null && symVal.sym == w.value)
                                                    {
                                                        op_elab = true;
                                                    }
                                                    else if (o_support_calculation_type == 4
                                                            && reteLoc != null
                                                            && w.value == Rete.get_symbol_from_rete_loc(reteLoc
                                                                    .getLevelsUp(), reteLoc.getFieldNum(),
                                                                    inst.rete_token, w))
                                                    {
                                                        op_elab = true;
                                                    }
                                                    else
                                                    {
                                                        /*
                                                         * this is not an
                                                         * operator elaboration
                                                         */
                                                        o_support = true;
                                                    }
                                                }
                                            }
                                        }
                                        else
                                        {
                                            o_support = true;
                                            break;
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }

            /* KJC 01/00: Warn if operator elabs mixed w/ applications */
            if ((o_support_calculation_type == 3 || o_support_calculation_type == 4) && (o_support == true))
            {
                if (op_elab == true)
                {
                    /* warn user about mixed actions */
                    if (o_support_calculation_type == 3)
                    {

                        // TODO: Warn user about mixed actions
                        // print_with_symbols(thisAgent, "\nWARNING: operator
                        // elaborations mixed with operator applications\nget
                        // o_support in prod %y", inst.prod.name);
                        //                
                        // growable_string gs =
                        // make_blank_growable_string(thisAgent);
                        // add_to_growable_string(thisAgent, &gs, "WARNING:
                        // operator elaborations mixed with operator
                        // applications\nget o_support in prod ");
                        // add_to_growable_string(thisAgent, &gs,
                        // symbol_to_string(thisAgent, inst.prod.name, true,
                        // 0, 0));
                        // xml_generate_warning(thisAgent,
                        // text_of_growable_string(gs));
                        // free_growable_string(thisAgent, gs);

                        o_support = true;
                    }
                    else if (o_support_calculation_type == 4)
                    {
                        // TODO: Warn user about mixed actions
                        // print_with_symbols(thisAgent, "\nWARNING: operator
                        // elaborations mixed with operator applications\nget
                        // i_support in prod %y", inst.prod.name);
                        //
                        // growable_string gs =
                        // make_blank_growable_string(thisAgent);
                        // add_to_growable_string(thisAgent, &gs, "WARNING:
                        // operator elaborations mixed with operator
                        // applications\nget i_support in prod ");
                        // add_to_growable_string(thisAgent, &gs,
                        // symbol_to_string(thisAgent, inst.prod.name, true,
                        // 0, 0));
                        // xml_generate_warning(thisAgent,
                        // text_of_growable_string(gs));
                        // free_growable_string(thisAgent, gs);

                        o_support = false;
                    }
                }
            }

            /*
             * assign every preference the correct support
             */

            for (Preference pref : inst.preferences_generated)
            {
                pref.o_supported = o_support;
            }

            return; // goto o_support_done;
        }

        /* REW: end 09.15.96 */

        /* --- initialize by giving everything NO o_support --- */
        for (Preference pref : inst.preferences_generated)
        {
            pref.o_supported = false;
        }

        /* --- find the match goal, match state, and match operator --- */
        match_goal = inst.match_goal;
        if (match_goal == null)
        {
            return; // goto o_support_done; // nothing gets o-support

        }

        match_state = match_goal;

        match_operator_wme = match_goal.operator_slot.wmes.getFirstItem();
        if (match_operator_wme != null)
        {
            match_operator = match_operator_wme.value.asIdentifier();
        }
        else
        {
            match_operator = null;
        }

        lhs = inst.top_of_instantiated_conditions;
        ListHead<Preference> rhs = inst.preferences_generated;

        /* --- scan through rhs to look for various things --- */
        rhs_does_an_operator_creation = false;

        for (Preference pref : rhs)
        {
            if ((pref.id == match_goal)
                    && (pref.attr == syms.operator_symbol)
                    && ((pref.type == PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) || (pref.type == PreferenceType.REQUIRE_PREFERENCE_TYPE)))
            {
                rhs_does_an_operator_creation = true;
            }
        }

        /* --- scan through lhs to look for various tests --- */
        lhs_tests_operator_acceptable_or_installed = false;
        lhs_tests_operator_installed = false;
        lhs_is_known_to_test_something_off_match_state = false;
        lhs_is_known_to_test_something_off_match_operator = false;

        for (c = lhs; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc == null)
            {
                continue;
            }
            w = pc.bt.wme_;
            /*
             * For NNPSCM, count something as "off the match state" only if it's
             * not the OPERATOR.
             */
            if ((w.id == match_state) && (w.attr != syms.operator_symbol))
            {
                lhs_is_known_to_test_something_off_match_state = true;
            }
            if (w.id == match_operator)
            {
                lhs_is_known_to_test_something_off_match_operator = true;
            }
            if (w == match_operator_wme)
            {
                lhs_tests_operator_installed = true;
            }
            if ((w.id == match_goal) && (w.attr == syms.operator_symbol))
            {
                lhs_tests_operator_acceptable_or_installed = true;
            }
        }

        /* --- calcluate lhs support flags --- */
        oa_support_possible = lhs_tests_operator_installed;
        oc_support_possible = rhs_does_an_operator_creation;
        om_support_possible = lhs_tests_operator_acceptable_or_installed;

        if ((!oa_support_possible) && (!oc_support_possible) && (!om_support_possible))
        {
            return; // goto o_support_done;
        }

        if (!lhs_is_known_to_test_something_off_match_state)
        {
            begin_os_tc(null);
            add_to_os_tc_if_id(match_state, true);
            if (!id_or_value_of_condition_list_is_in_os_tc(lhs, match_state, match_state))
            {
                oc_support_possible = false;
                om_support_possible = false;
            }
        }

        if (oa_support_possible)
        {
            if (!lhs_is_known_to_test_something_off_match_operator)
            {
                begin_os_tc(null);
                add_to_os_tc_if_id(match_operator, false);
                if (!id_or_value_of_condition_list_is_in_os_tc(lhs, match_operator, null))
                    oa_support_possible = false;
            }
        }

        /* --- look for rhs oa support --- */
        if (oa_support_possible)
        {
            begin_os_tc(rhs.first);
            add_to_os_tc_if_id(match_state, true);
            for (Preference pref : rhs)
            {
                if (pref.id.tc_number == o_support_tc)
                {
                    /* RBD 8/19/94 added extra NNPSCM test -- ^operator augs on the state
                                                              don't get o-support */
                    /* AGR 639 begin 94.11.01 */
                    /* gap 10/6/94 You need to check the id on all preferences that have
                       an attribute of operator to see if this is an operator slot of a
                       context being modified. */
                    if (!((pref.attr == syms.operator_symbol) && (is_state_id(top_goal, pref.id, match_state))))
                    {
                        /* AGR 639 end */
                        pref.o_supported = true;
                    }
                }
            }

            /* --- look for rhs oc support --- */
            if (oc_support_possible)
            {
                begin_os_tc(rhs.first);
                for (Preference pref : rhs)
                {
                    if ((pref.id == match_goal)
                            && (pref.attr == syms.operator_symbol)
                            && ((pref.type == PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) || (pref.type == PreferenceType.REQUIRE_PREFERENCE_TYPE)))
                    {
                        add_to_os_tc_if_id(pref.value, false);
                    }
                }
                for (Preference pref : rhs)
                {
                    /* SBH 6/23/94 */
                    if ((pref.id.tc_number == o_support_tc) && pref.id != match_state)
                    {
                        /* SBH: Added 2nd test to avoid circular assignment of o-support
                           to augmentations of the state: in, e.g.
                           (sp p2
                                 (state <g> ^problem-space)(state <ig> ^problem-space.name top-ps)
                              -.
                              (<g> ^operator <o>)(<o> ^name opx ^circular-goal-test <ig>))
                           Here, the op acc. pref would get o-support (it's in the transitive
                           closure); this test rules it out.
                           
                           BUGBUG: this is not fully general; it does not rule out assiging
                           o-support to substructures of the state that are in the TC of an
                           operator creation; e.g.
                           (sp p2
                                 (state <g> ^problem-space)(state <ig> ^problem-space.name top-ps)
                              -.
                              (<g> ^operator <o> ^other <x>)
                              (<o> ^name opx ^circular-goal-test <ig>)
                              (<x> ^THIS-GETS-O-SUPPORT T))
                         */
                        /* end SBH 6/23/94 */
                        pref.o_supported = true;
                    }
                }
            }

            /* --- look for rhs om support --- */
            if (om_support_possible)
            {
                begin_os_tc(rhs.first);
                for (c = inst.top_of_instantiated_conditions; c != null; c = c.next)
                {
                    PositiveCondition pc = c.asPositiveCondition();
                    if (pc != null)
                    {
                        w = pc.bt.wme_;
                        if ((w.id == match_goal) && (w.attr == syms.operator_symbol))
                        {
                            add_to_os_tc_if_id(w.value, false);
                        }
                    }
                }
                for (Preference pref : rhs)
                {
                    if (pref.id.tc_number == o_support_tc)
                    {
                        pref.o_supported = true;
                    }
                }
            }

        }
    }

    /**
     *    Run-Time O-Support Calculation:  Doug Pearson's Scheme
     *
     * This routine calculates o-support for each preference for the given
     * instantiation, filling in pref.o_supported (true or false) on each one.
     *
     * This is basically Doug's original scheme (from email August 16, 1994)
     * modified by John's response (August 17) points #2 (don't give o-c
     * support unless pref in TC of RHS op.) and #3 (all support calc's should
     * be local to a goal).  In detail:
     *
     * For a particular preference p=(id ^attr ...) on the RHS of an
     * instantiation [LHS,RHS]:
     *
     * RULE #1 (Context pref's): If id is the match state and attr="operator", 
     * then p does NOT get o-support.  This rule overrides all other rules.
     *
     * RULE #2 (O-A support):  If LHS includes (match-state ^operator ...),
     * then p gets o-support.
     *
     * RULE #3 (O-M support):  If LHS includes (match-state ^operator ... +),
     * then p gets o-support.
     *
     * RULE #4 (O-C support): If RHS creates (match-state ^operator ... +/!),
     * and p is in TC(RHS-operators, RHS), then p gets o-support.
     *
     * Here "TC" means transitive closure; the starting points for the TC are 
     * all operators the RHS creates an acceptable/require preference for (i.e., 
     * if the RHS includes (match-state ^operator such-and-such +/!), then 
     * "such-and-such" is one of the starting points for the TC).  The TC
     * is computed only through the preferences created by the RHS, not
     * through any other existing preferences or WMEs.
     *
     * If none of rules 1-4 apply, then p does NOT get o-support.
     *
     * Note that rules 1 through 3 can be handled in linear time (linear in 
     * the size of the LHS and RHS); rule 4 can be handled in time quadratic 
     * in the size of the RHS (and typical behavior will probably be linear).
     * 
     * osupport.cpp:661:dougs_calculate_support_for_instantiation_preferences
     * 
     * @param inst
     */
    void dougs_calculate_support_for_instantiation_preferences(Instantiation inst)
    {
        Condition lhs = inst.top_of_instantiated_conditions;
        ListHead<Preference> rhs = inst.preferences_generated;
        Identifier match_state = inst.match_goal;

        /* --- First, check whether rule 2 or 3 applies. --- */
        boolean rule_2_or_3 = false;
        Condition c;
        Wme w;
        for (c = lhs; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc == null)
            {
                continue;
            }
            w = c.bt.wme_;
            if ((w.id == match_state) && (w.attr == syms.operator_symbol))
            {
                rule_2_or_3 = true;
                break;
            }
        }

        /* --- Initialize all pref's according to rules 2 and 3 --- */
        for (Preference pref : rhs)
        {
            pref.o_supported = rule_2_or_3;
        }

        /* --- If they didn't apply, check rule 4 --- */
        if (!rule_2_or_3)
        {
            o_support_tc = syms.getSyms().get_new_tc_number();
            /*
             * BUGBUG With Doug's scheme, o_support_tc no longer needs to be a
             * global variable -- it could simply be local to this procedure
             */
            boolean anything_added = false;
            /*
             * --- look for RHS operators, add 'em (starting points) to the TC
             * ---
             */
            for (Preference pref : rhs)
            {
                if ((pref.id == match_state)
                        && (pref.attr == syms.operator_symbol)
                        && ((pref.type == PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) || (pref.type == PreferenceType.REQUIRE_PREFERENCE_TYPE))
                        && (pref.value.asIdentifier() != null))
                {
                    pref.value.asIdentifier().tc_number = o_support_tc;
                    anything_added = true;
                }
            }
            /* --- Keep adding stuff to the TC until nothing changes anymore --- */
            while (anything_added)
            {
                anything_added = false;
                for (Preference pref : rhs)
                {
                    if (pref.id.tc_number != o_support_tc)
                    {
                        continue;
                    }
                    if (pref.o_supported)
                    {
                        continue; /* already added this thing */
                    }
                    pref.o_supported = true;
                    anything_added = true;
                    Identifier idValue = pref.value.asIdentifier();
                    if (idValue != null)
                    {
                        idValue.tc_number = o_support_tc;
                    }
                    Identifier referentId = pref.type.isBinary() ? pref.referent.asIdentifier() : null;
                    if (referentId != null)
                    {
                        referentId.tc_number = o_support_tc;
                    }
                }
            }
        }

        /* --- Finally, use rule 1, which overrides all the other rules. --- */
        for (Preference pref : rhs)
        {
            if ((pref.id == match_state) && (pref.attr == syms.operator_symbol))
            {
                pref.o_supported = false;
            }
        }
    }


    /**
     * This function determines whether a given symbol could be the match for a
     * given test. It returns YES if the symbol is the only symbol that could
     * pass the test (i.e., the test *forces* that symbol to be present in WM),
     * NO if the symbol couldn't possibly pass the test, and MAYBE if it can't
     * tell for sure. The symbol may be a variable; the test may contain
     * variables.
     * 
     * osupport.cpp:747:test_is_for_symbol
     * 
     * @param t
     * @param sym
     * @return
     */
    private YesNoMaybe test_is_for_symbol(Test t, Symbol sym)
    {
        if (t.isBlank())
        {
            return YesNoMaybe.MAYBE;
        }

        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            Symbol referent = eq.getReferent();
            if (referent == sym)
            {
                return YesNoMaybe.YES;
            }
            if (referent.asVariable() != null)
            {
                return YesNoMaybe.MAYBE;
            }
            if (sym.asVariable() != null)
            {
                return YesNoMaybe.MAYBE;
            }
            return YesNoMaybe.NO;
        }

        DisjunctionTest dt = t.asDisjunctionTest();
        if (dt != null)
        {
            if (sym.asVariable() != null)
            {
                return YesNoMaybe.MAYBE;
            }
            if (dt.disjunction_list.contains(sym))
            {
                return YesNoMaybe.MAYBE;
            }
            return YesNoMaybe.NO;
        }
        ConjunctiveTest ct = t.asConjunctiveTest();
        if (ct != null)
        {
            boolean maybe_found = false;
            for (Test c : ct.conjunct_list)
            {
                YesNoMaybe temp = test_is_for_symbol(c, sym);
                if (temp == YesNoMaybe.YES)
                {
                    return YesNoMaybe.YES;
                }
                if (temp == YesNoMaybe.MAYBE)
                {
                    maybe_found = true;
                }
            }
            if (maybe_found)
            {
                return YesNoMaybe.MAYBE;
            }
            return YesNoMaybe.NO;
        }
        // goal/impasse tests, relational tests other than equality
        return YesNoMaybe.MAYBE;
    }
    
    /**
     * This routine looks at the LHS and returns a list of variables that are
     * certain to be bound to goals.
     * 
     * Note: this uses the TC routines and clobbers any existing TC.
     * 
     * BUGBUG should follow ^object links up the goal stack if possible
     * 
     * osupport.cpp:796:find_known_goals
     * 
     * @param lhs
     * @return
     */
    private LinkedList<Variable> find_known_goals(Condition lhs)
    {
        int tc = syms.getSyms().get_new_tc_number();
        LinkedList<Variable> vars = new LinkedList<Variable>();
        for (Condition c = lhs; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc == null)
            {
                continue;
            }
            if (TestTools.test_includes_goal_or_impasse_id_test(pc.id_test, true, false))
            {
                pc.id_test.addBoundVariables(tc, vars);
            }
        }
        return vars;
    }

    /**
     * Given the LHS and a list of known goals (i.e., variables that must be
     * bound to goals at run-time), this routine tries to determine which
     * variable will be the match goal. If successful, it returns that variable;
     * if it can't tell which variable will be the match goal, it returns NIL.
     * 
     * Note: this uses the TC routines and clobbers any existing TC.
     * 
     * osupport.cpp:825:find_compile_time_match_goal
     * 
     * @param lhs
     * @param known_goals
     * @return
     */
    private Variable find_compile_time_match_goal(Condition lhs, List<Variable> known_goals)
    {
        /* --- find root variables --- */
        int tc = syms.getSyms().get_new_tc_number();
        List<Variable> roots = ConditionReorderer.collect_root_variables(lhs, tc, false);

        /* --- intersect roots with known_goals, producing root_goals --- */
        LinkedList<Variable> root_goals = new LinkedList<Variable>();
        int num_root_goals = 0; // TODO Just use root_goals.size()?
        for (Variable v : roots)
        {
            if (known_goals.contains(v))
            {
                root_goals.push(v);
                num_root_goals++;
            }
        }

        /* --- if more than one goal, remove any with "^object nil" --- */
        if (num_root_goals > 1)
        {
            for (Condition cond = lhs; cond != null; cond = cond.next)
            {
                PositiveCondition pc = cond.asPositiveCondition();
                if (pc != null && (test_is_for_symbol(pc.attr_test, syms.superstate_symbol) == YesNoMaybe.YES)
                        && (test_is_for_symbol(pc.value_test, syms.nil_symbol) == YesNoMaybe.YES))
                {

                    Iterator<Variable> it = root_goals.iterator();
                    while (it.hasNext())
                    {
                        Variable sym = it.next();
                        if (test_is_for_symbol(pc.id_test, sym) == YesNoMaybe.YES)
                        {
                            // remove sym from the root_goals list
                            it.remove();
                            num_root_goals--;
                            if (num_root_goals == 1)
                            {
                                break; // be sure not to remove them all 
                            }
                        }
                    } /* end of for (c) loop */
                    if (num_root_goals == 1)
                        break; /* be sure not to remove them all */
                }
            } /* end of for (cond) loop */
        }

        // --- if there's only one root goal, that's it!
        if (num_root_goals == 1)
        {
            return root_goals.getFirst();
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Given the LHS and a the match goal variable, this routine looks for a
     * positive condition testing (goal ^attr) for the given attribute "attr".
     * If such a condition exists, and the value field contains an equality test
     * for a variable, then that variable is returned. (If more than one such
     * variable exists, one is chosen arbitrarily and returned.) Otherwise the
     * function returns NIL.
     * 
     * Note: this uses the TC routines and clobbers any existing TC.
     * 
     * osupport.cpp:896:find_thing_off_goal
     * 
     * @param lhs
     * @param goal
     * @param attr
     * @return
     */
    private Symbol find_thing_off_goal(Condition lhs, Variable goal, Symbol attr)
    {
        for (Condition c = lhs; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc == null) continue;
            if (test_is_for_symbol(pc.id_test, goal) != YesNoMaybe.YES) continue;
            if (test_is_for_symbol(pc.attr_test, attr) != YesNoMaybe.YES) continue;
            if (c.test_for_acceptable_preference) continue;
            
            int tc = syms.getSyms().get_new_tc_number();
            LinkedList<Variable> vars = new LinkedList<Variable>();
            pc.value_test.addBoundVariables(tc, vars);
            if (!vars.isEmpty())
            {
                return vars.getFirst();
            }
        }
        return null;
    }

    /**
     * This checks whether a given condition list has an equality test for a
     * given symbol in the id field of any condition (at any nesting level
     * within NCC's).
     * 
     * osupport.cpp:928:condition_list_has_id_test_for_sym
     * 
     * @param conds
     * @param sym
     * @return
     */
    private boolean condition_list_has_id_test_for_sym(Condition conds, Symbol sym)
    {
        for (; conds != null; conds = conds.next)
        {
            ThreeFieldCondition tfc = conds.asThreeFieldCondition(); // Positive
                                                                        // or
                                                                        // negative
            if (tfc != null)
            {
                if (TestTools.test_includes_equality_test_for_symbol(tfc.id_test, sym))
                {
                    return true;
                }
            }
            ConjunctiveNegationCondition ncc = conds.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                if (condition_list_has_id_test_for_sym(ncc.top, sym))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * osupport.cpp:953:match_state_tests_non_operator_slot
     * 
     * @param conds
     * @param match_state
     * @return
     */
    private boolean match_state_tests_non_operator_slot(Condition conds, Symbol match_state)
    {
        YesNoMaybe ynm;

        for (; conds != null; conds = conds.next)
        {
            ThreeFieldCondition tfc = conds.asThreeFieldCondition();
            if (tfc != null)
            {
                if (TestTools.test_includes_equality_test_for_symbol(tfc.id_test, match_state))
                {
                    ynm = test_is_for_symbol(tfc.attr_test, syms.operator_symbol);
                    if (ynm == YesNoMaybe.NO)
                    {
                        return true;
                    }
                }
            }
            ConjunctiveNegationCondition ncc = conds.asConjunctiveNegationCondition();
            if (ncc != null)
            {
                if (match_state_tests_non_operator_slot(ncc.top, match_state))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This enlarges a given TC by adding to it any connected conditions in the
     * LHS or actions in the RHS.
     * 
     * osupport.cpp:986:add_tc_through_lhs_and_rhs
     * 
     * @param lhs
     * @param rhs
     * @param tc
     * @param id_list
     * @param var_list
     */
    private void add_tc_through_lhs_and_rhs(Condition lhs, Action rhs, int tc, LinkedList<Identifier> id_list,
            LinkedList<Variable> var_list)
    {

        for (Condition c = lhs; c != null; c = c.next)
        {
            c.already_in_tc = false;
        }
        for (Action a = rhs; a != null; a = a.next)
        {
            a.already_in_tc = false;
        }

        /* --- keep trying to add new stuff to the tc --- */
        while (true)
        {
            boolean anything_changed = false;
            for (Condition c = lhs; c != null; c = c.next)
            {
                if (!c.already_in_tc)
                {
                    if (c.cond_is_in_tc(tc))
                    {
                        c.add_cond_to_tc(tc, id_list, var_list);
                        c.already_in_tc = true;
                        anything_changed = true;
                    }
                }
            }
            for (Action a = rhs; a != null; a = a.next)
            {
                if (!a.already_in_tc)
                {
                    if (a.action_is_in_tc(tc))
                    {
                        a.add_action_to_tc(tc, id_list, var_list);
                        a.already_in_tc = true;
                        anything_changed = true;
                    }
                }
            }
            if (!anything_changed)
                break;
        }
    }

    
/* -----------------------------------------------------------------------
                   Calculate Compile Time O-Support

   This takes the LHS and RHS, and fills in the a->support field in each
   RHS action with either UNKNOWN_SUPPORT, O_SUPPORT, or I_SUPPORT.
   (Actually, it only does this for MAKE_ACTION's--for FUNCALL_ACTION's,
   the support doesn't matter.)
----------------------------------------------------------------------- */

public void calculate_compile_time_o_support (Condition lhs, Action rhs, boolean operand2_mode) {
  Symbol  match_operator;
  YesNoMaybe lhs_oa_support, lhs_oc_support, lhs_om_support;
  Action a;
  Condition cond;
  YesNoMaybe ynm;
  int tc;

  // TODO Re-check braces here
  /* --- initialize:  mark all rhs actions as "unknown" --- */
  for (a=rhs; a!=null; a=a.next){
      MakeAction ma = a.asMakeAction();
    if (ma != null) {  a.support=ActionSupport.UNKNOWN_SUPPORT; }
  }

  /* --- if "operator" doesn't appear in any LHS attribute slot, and there
         are no RHS +/! makes for "operator", then nothing gets support --- */
  boolean operator_found = false;
  boolean possible_operator_found = false;
  for (cond=lhs; cond!=null; cond=cond.next) {
    PositiveCondition pc = cond.asPositiveCondition();  
    if (pc == null) {  continue; }
    ynm = test_is_for_symbol (pc.attr_test, syms.operator_symbol);
    if (ynm==YesNoMaybe.YES) { operator_found = possible_operator_found = true; break; }
    if (ynm==YesNoMaybe.MAYBE) { possible_operator_found = true; }
  }
  if (! operator_found){
    for (a=rhs; a!=null; a=a.next) {
      MakeAction ma = a.asMakeAction();
      if (ma == null) { continue; }
      RhsSymbolValue rhsSym = ma.attr.asSymbolValue();
      if (rhsSym != null) { /* RBD 3/29/95 general RHS attr's */
        Symbol attr = rhsSym.getSym();
        if (attr==syms.operator_symbol)
          { operator_found = possible_operator_found = true; break; }
        if (attr.asVariable() != null){
          possible_operator_found = true;
        }
      } else {
        possible_operator_found = true; // for funcall, must play it safe
      }
    }
  }
  if (! possible_operator_found) {
    for (a=rhs; a!=null; a=a.next) {
      if (a.asMakeAction() != null) { a.support=ActionSupport.I_SUPPORT; }
    }
    return;
  }


  /* --- find known goals; RHS augmentations of goals get no support --- */
  LinkedList<Variable> known_goals = find_known_goals (lhs);
 /* SBH: In NNPSCM, the only RHS-goal augmentations that can't get support are
    preferences for the "operator" slot. */
  for (Variable c : known_goals){
    for (a=rhs; a!=null; a=a.next) {
      MakeAction ma = a.asMakeAction();
      if (ma == null) { continue; }
      RhsSymbolValue rhsSym = ma.attr.asSymbolValue();
      if (rhsSym != null &&  /* RBD 3/29/95 */
          rhsSym.getSym()==syms.operator_symbol &&
          (ma.id.getSym() == c)) {
        a.support = ActionSupport.I_SUPPORT;
      }
    }
  }

  /* --- find match goal, state, and operator --- */
  Variable match_state = find_compile_time_match_goal (lhs, known_goals);
  if (match_state == null) { return; }
  
  match_operator = find_thing_off_goal (lhs, match_state, syms.operator_symbol);
  /* --- If when checking (above) for "operator" appearing anywhere, we
     found a possible operator but not a definite operator, now go back and
     see if the possible operator was actually the match goal or match state;
     if so, it's not a possible operator.  (Note:  by "possible operator" I
     mean something appearing in the *attribute* field that might get bound
     to the symbol "operator".)  --- */
  if (possible_operator_found && !operator_found) {
    possible_operator_found = false;
    for (cond=lhs; cond!=null; cond=cond.next) {
      PositiveCondition pc = cond.asPositiveCondition();
      if (pc == null) { continue; }
      ynm = test_is_for_symbol (pc.attr_test, syms.operator_symbol);
      if ((ynm!=YesNoMaybe.NO) &&
          (test_is_for_symbol (pc.attr_test, match_state)!=YesNoMaybe.YES))
        { possible_operator_found = true; break; }
    }
    if (! possible_operator_found) {
      for (a=rhs; a!=null; a=a.next) {
        MakeAction ma = a.asMakeAction();
        if (ma == null) { continue; }
        /* we're looking for "operator" augs of goals only, and match_state
           couldn't get bound to a goal */
        if (ma.id.getSym() == match_state) {  continue; }
        RhsSymbolValue rhsSym = ma.attr.asSymbolValue();
        if (rhsSym != null) { /* RBD 3/29/95 */
          Symbol attr = rhsSym.getSym();
          if (attr.asVariable() != null && attr != match_state)
            { possible_operator_found = true; break; }
        } else { /* RBD 3/29/95 */
          possible_operator_found = true; break;
        }
      }
    }
    if (! possible_operator_found) {
      for (a=rhs; a!=null; a=a.next){
        if (a.asMakeAction() != null) { a.support=ActionSupport.I_SUPPORT; }
      }
      return;
    }
  }
  
  /* --- calculate LHS support predicates --- */
  lhs_oa_support = YesNoMaybe.MAYBE;
  if (match_operator != null){

/* SBH 7/1/94 #2 */
    if ((condition_list_has_id_test_for_sym (lhs, match_operator)) &&
    (match_state_tests_non_operator_slot(lhs,match_state))){
/* end SBH 7/1/94 #2 */

      lhs_oa_support = YesNoMaybe.YES;
      }
  }

  lhs_oc_support = YesNoMaybe.MAYBE;
  lhs_om_support = YesNoMaybe.MAYBE;

/* SBH 7/1/94 #2 */
  /* For NNPSCM, must test that there is a test of a non-operator slot off 
     of the match_state. */
  if (match_state_tests_non_operator_slot(lhs,match_state)) 
    {
/* end SBH 7/1/94 #2 */

    lhs_oc_support = YesNoMaybe.YES; 
    for (cond=lhs; cond!=null; cond=cond.next) {
      PositiveCondition pc = cond.asPositiveCondition();
      if (pc == null){ continue; }
      if (test_is_for_symbol (pc.id_test, match_state) != YesNoMaybe.YES) { continue; }
      if (test_is_for_symbol (pc.attr_test, syms.operator_symbol)
          != YesNoMaybe.YES) {
        continue;
      }
      lhs_om_support = YesNoMaybe.YES;
      break;
    }
  }     

  if (lhs_oa_support == YesNoMaybe.YES) {    /* --- look for RHS o-a support --- */
    /* --- do TC(match_state) --- */
    tc = syms.getSyms().get_new_tc_number();
    match_state.add_symbol_to_tc (tc, new LinkedList<Identifier>(), new LinkedList<Variable>());
    add_tc_through_lhs_and_rhs (lhs, rhs, tc, new LinkedList<Identifier>(), new LinkedList<Variable>());

    /* --- any action with id in the TC gets support --- */
    for (a=rhs; a!=null; a=a.next)  {

      if (a.action_is_in_tc (tc)) {
    /* SBH 7/1/94 Avoid resetting of support that was previously set to I_SUPPORT. */
    /* gap 10/6/94 If the action has an attribue of operator, then you
       don't know if it should get o-support until run time because of
       the vagaries of knowing when this is matching a context object
       or not. */
        RhsSymbolValue rhsSym = a.asMakeAction().attr.asSymbolValue();
        if (rhsSym != null &&
            (rhsSym.getSym()==syms.operator_symbol)) {
      if (a.support != ActionSupport.I_SUPPORT) { a.support = ActionSupport.UNKNOWN_SUPPORT; }
    } else {
      if (a.support != ActionSupport.I_SUPPORT) { a.support = ActionSupport.O_SUPPORT; }
    }
        /* end SBH 7/1/94 */
    }
    }
  }
  
  if (lhs_oc_support == YesNoMaybe.YES) {    /* --- look for RHS o-c support --- */
    /* --- do TC(rhs operators) --- */
    tc = syms.getSyms().get_new_tc_number();
    for (a=rhs; a!=null; a=a.next) {
      MakeAction ma = a.asMakeAction();
      if (ma == null) { continue; }
      RhsSymbolValue rhsAttrSym = ma.attr.asSymbolValue();
      if ((ma.id.getSym()==match_state) &&
          rhsAttrSym != null &&
          (rhsAttrSym.getSym()==syms.operator_symbol) &&
          ((a.preference_type==PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) ||
           (a.preference_type==PreferenceType.REQUIRE_PREFERENCE_TYPE)) ) {
        RhsSymbolValue rhsValueSym = ma.value.asSymbolValue();
        if (rhsValueSym != null) {
            rhsValueSym.getSym().add_symbol_to_tc (tc, new LinkedList<Identifier>(),new LinkedList<Variable>());
    }
      }
    }
    add_tc_through_lhs_and_rhs (lhs, rhs, tc, new LinkedList<Identifier>(), new LinkedList<Variable>());

    /* --- any action with id in the TC gets support --- */
    for (a=rhs; a!=null; a=a.next)  {


      if (a.action_is_in_tc (tc)) {

    /* SBH 6/7/94:
       Make sure the action is not already marked as "I_SUPPORT".  This
       avoids giving o-support in the case where the operator
       points back to the goal, thus adding the goal to the TC,
       thus adding the operator proposal itself to the TC; thus
       giving o-support to an operator proposal.
    */
    if (a.support != ActionSupport.I_SUPPORT) { a.support = ActionSupport.O_SUPPORT; }
    /* End SBH 6/7/94 */


       /* REW: begin 09.15.96 */
       /*
       in operand, operator proposals are now only i-supported.
       */

       if (operand2_mode) {
           // TODO Verbose
//           if (thisAgent->soar_verbose_flag == TRUE) {
//               printf("\n         operator creation: setting a->support to I_SUPPORT");
//               xml_generate_verbose(thisAgent, "operator creation: setting a->support to I_SUPPORT");
//           }

           a.support = ActionSupport.I_SUPPORT;
       }
       /* REW: end   09.15.96 */

      }
  }

  if (lhs_om_support == YesNoMaybe.YES) {    /* --- look for RHS o-m support --- */
    /* --- do TC(lhs operators) --- */
    tc = syms.getSyms().get_new_tc_number();
    for (cond=lhs; cond!=null; cond=cond.next) {
      PositiveCondition pc = cond.asPositiveCondition();
      if (pc == null) { continue; }
      if (test_is_for_symbol (pc.id_test, match_state) == YesNoMaybe.YES){
        if (test_is_for_symbol (pc.attr_test, syms.operator_symbol) == YesNoMaybe.YES){
          pc.value_test.addBoundVariables(tc, new LinkedList<Variable>());
        }
      }
    }
    add_tc_through_lhs_and_rhs (lhs, rhs, tc, new LinkedList<Identifier>(), new LinkedList<Variable>());

    /* --- any action with id in the TC gets support --- */
    for (a=rhs; a!=null; a=a.next) {

      if (a.action_is_in_tc (tc)) {
    /* SBH 7/1/94 Avoid resetting of support that was previously set to I_SUPPORT. */
    if (a.support != ActionSupport.I_SUPPORT) { a.support = ActionSupport.O_SUPPORT; }
    /* end SBH 7/1/94 */
      }
    }
  }
}

}
}