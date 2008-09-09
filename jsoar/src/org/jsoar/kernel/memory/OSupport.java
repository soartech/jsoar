/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 8, 2008
 */
package org.jsoar.kernel.memory;

import org.jsoar.kernel.ProductionSupport;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveNegationCondition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.TestTools;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
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
    private final SymbolFactory syms;
    private final SymConstant operator_symbol;
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
    public OSupport(SymbolFactory syms, SymConstant operator_symbol)
    {
        this.syms = syms;
        this.operator_symbol = operator_symbol;
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
            if ((!isa_state) || (s.attr != operator_symbol))
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
                if ((!isa_state) || (pref.attr != operator_symbol))
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
        o_support_tc = syms.get_new_tc_number();
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
                        && TestTools.test_includes_equality_test_for_symbol(tfc.attr_test, operator_symbol))
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
     * osupport.cpp:267:calculate_support_for_instantiation_preferences
     * 
     * @param inst
     * @param top_goal
     * @param operand2_mode
     * @param o_support_calculation_type
     */
    public void calculate_support_for_instantiation_preferences(Instantiation inst, final Identifier top_goal,
            final boolean operand2_mode, final int o_support_calculation_type)
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
                                    if ((w.attr == operator_symbol) && (w.acceptable == false)
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
                    && (pref.attr == operator_symbol)
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
            if ((w.id == match_state) && (w.attr != operator_symbol))
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
            if ((w.id == match_goal) && (w.attr == operator_symbol))
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
                    if (!((pref.attr == operator_symbol) && (is_state_id(top_goal, pref.id, match_state))))
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
                            && (pref.attr == operator_symbol)
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
                        if ((w.id == match_goal) && (w.attr == operator_symbol))
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
            if ((w.id == match_state) && (w.attr == operator_symbol))
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
            o_support_tc = syms.get_new_tc_number();
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
                        && (pref.attr == operator_symbol)
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
            if ((pref.id == match_state) && (pref.attr == operator_symbol))
            {
                pref.o_supported = false;
            }
        }
    }

}