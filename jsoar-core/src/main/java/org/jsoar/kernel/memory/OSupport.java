/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 8, 2008
 */
package org.jsoar.kernel.memory;

import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.ProductionSupport;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.Conditions;
import org.jsoar.kernel.lhs.ConjunctiveNegationCondition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.DisjunctionTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.Tests;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.ActionSupport;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;

/**
 * 
 * osupport.cpp
 * 
 * @author ray
 */
public class OSupport
{
    private final PredefinedSymbols syms;
    private final Printer printer;
    
    private static enum YesNoMaybe
    {
        YES, NO, MAYBE
    }
    
    /**
     * agent.h:658:o_support_tc
     */
    private Marker o_support_tc;
    
    /**
     * agent.h:659:rhs_prefs_from_instantiation
     */
    private final ListHead<Preference> rhs_prefs_from_instantiation = ListHead.newInstance();
    
    
    /**
     * @param syms
     * @param printer
     */
    public OSupport(PredefinedSymbols syms, Printer printer)
    {
        this.syms = syms;
        this.printer = printer;
    }

    /**
     * osupport.cpp:63:add_to_os_tc_if_needed
     * 
     * @param sym
     */
    private void add_to_os_tc_if_needed(SymbolImpl sym)
    {
        IdentifierImpl id = sym.asIdentifier();
        if (id != null)
        {
            add_to_os_tc(id, false);
        }
    }

    /**
     * SBH 4/14/93
     * For NNPSCM, we must exclude the operator slot from the transitive closure of a state.
     * Do that by passing a boolean argument, "isa_state" to this routine.
     * If it isa_state, check for the operator slot before the recursive call.
     * 
     * <p>osupport.cpp:84:add_to_os_tc
     * 
     * @param id
     * @param isa_state
     */
    private void add_to_os_tc(IdentifierImpl id, boolean isa_state)
    {
        // if id is already in the TC, exit; else mark it as in the TC
        if (id.tc_number == o_support_tc)
        {
            return;
        }

        id.tc_number = o_support_tc;

        // scan through all preferences and wmes for all slots for this id
        for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
        {
            add_to_os_tc_if_needed(w.value);
        }
        for (ListItem<Slot> sit = id.slots.first; sit != null; sit = sit.next)
        {
            final Slot s = sit.item;
            if ((!isa_state) || (s.attr != syms.operator_symbol))
            {
                for (Preference pref = s.getAllPreferences(); pref != null; pref = pref.nextOfSlot)
                {
                    add_to_os_tc_if_needed(pref.value);
                    if (pref.type.isBinary())
                        add_to_os_tc_if_needed(pref.referent);
                }
                for (WmeImpl w = s.getWmes(); w != null; w = w.next)
                {
                    add_to_os_tc_if_needed(w.value);
                }
            }
        } /* end of for slots loop */
        // now scan through RHS prefs and look for any with this id
        for (ListItem<Preference> pit = rhs_prefs_from_instantiation.first; pit != null; pit = pit.next)
        {
            final Preference pref = pit.item;
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
     *                 Run-Time O-Support Calculation
     *
     * This routine calculates o-support for each preference for the given
     * instantiation, filling in pref.o_supported (true or false) on each one.
     *
     * <p>The following predicates are used for support calculations.  In the
     * following, "lhs has some elt. ..." means the lhs has some id or value
     * at any nesting level.
     *
     * <pre>
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
     * </pre>
     * 
     * <pre>
     * BUGBUG the code does a check of whether the lhs tests the match state via
     *       looking just at id and value fields of top-level positive cond's.
     *       It doesn't look at the attr field, or at any negative or NCC's.
     *       I'm not sure whether this is right or not.  (It's a pretty
     *       obscure case, though.)
     * </pre>
     * 
     * <p>osupport.cpp:267:calculate_support_for_instantiation_preferences
     * 
     * @param inst
     * @param top_goal
     * @param operand2_mode
     */
    public void calculate_support_for_instantiation_preferences(Instantiation inst, final IdentifierImpl top_goal)
    {
        WmeImpl w;
        Condition c;
        Action act;
        boolean o_support, op_elab;
        boolean operator_proposal;
        int pass;
        WmeImpl lowest_goal_wme;

        // TODO: verbose
        // if (thisAgent.soar_verbose_flag == true) {
        // printf("\n in calculate_support_for_instantiation_preferences:");
        // xml_generate_verbose(thisAgent, "in calculate_support_for_instantiation_preferences:");
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
        else if (inst.prod.declared_support == ProductionSupport.UNDECLARED)
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
                    if (syms.operator_symbol == ma.attr.asSymbolValue().sym && 
                        act.preference_type == PreferenceType.ACCEPTABLE)
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
                                    // former o_support_calculation_type test site
                                    // iff RHS has only operator elaborations then it's IE_PROD,
                                    // otherwise PE_PROD, so look for non-op-elabs in the actions KJC 1/00
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
                                            else if (/*o_support_calculation_type == 4 &&*/ reteLoc != null
                                                    && w.value == reteLoc.lookupSymbol(inst.rete_token, w))
                                            {
                                                op_elab = true;
                                            }
                                            else
                                            {
                                                // this is not an operator elaboration
                                                o_support = true;
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }

        /* KJC 01/00: Warn if operator elabs mixed w/ applications */
        // former o_support_calculation_type (3 or 4)  test site
        if (o_support)
        {
            if (op_elab)
            {
                // former o_support_calculation_type (4)  test site
                // warn user about mixed actions
                printer.warn("\nWARNING: operator elaborations mixed with operator applications\n" +
                		"get i_support in prod %s", inst.prod.getName());

                o_support = false;
            }
        }

        // assign every preference the correct support
        for (ListItem<Preference> pref = inst.preferences_generated.first; pref != null; pref = pref.next)
        {
            pref.item.o_supported = o_support;
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
    private YesNoMaybe test_is_for_symbol(Test t, SymbolImpl sym)
    {
        if (Tests.isBlank(t))
        {
            return YesNoMaybe.MAYBE;
        }

        EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            SymbolImpl referent = eq.getReferent();
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
     * <p>Note: this uses the TC routines and clobbers any existing TC.
     * 
     * <p>BUGBUG should follow ^object links up the goal stack if possible
     * 
     * <p>osupport.cpp:796:find_known_goals
     * 
     * @param lhs
     * @return
     */
    private ListHead<Variable> find_known_goals(Condition lhs)
    {
        final Marker tc = DefaultMarker.create();
        final ListHead<Variable> vars = ListHead.newInstance();
        for (Condition c = lhs; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc == null)
            {
                continue;
            }
            if (Tests.test_includes_goal_or_impasse_id_test(pc.id_test, true, false))
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
     * <p>Note: this uses the TC routines and clobbers any existing TC.
     * 
     * <p>osupport.cpp:825:find_compile_time_match_goal
     * 
     * @param lhs
     * @param known_goals
     * @return
     */
    private Variable find_compile_time_match_goal(Condition lhs, ListHead<Variable> known_goals)
    {
        // find root variables 
        final Marker tc = DefaultMarker.create();
        final ListHead<Variable> roots = Conditions.collect_root_variables(lhs, tc, null, null);

        // intersect roots with known_goals, producing root_goals
        final ListHead<Variable> root_goals = ListHead.newInstance();
        int num_root_goals = 0; // ListHead.size() is slow.
        for (ListItem<Variable> v = roots.first; v != null; v = v.next)
        {
            if (known_goals.contains(v.item))
            {
                root_goals.push(v.item);
                num_root_goals++;
            }
        }

        // if more than one goal, remove any with "^object nil"
        if (num_root_goals > 1)
        {
            for (Condition cond = lhs; cond != null; cond = cond.next)
            {
                PositiveCondition pc = cond.asPositiveCondition();
                if (pc != null && (test_is_for_symbol(pc.attr_test, syms.superstate_symbol) == YesNoMaybe.YES)
                        && (test_is_for_symbol(pc.value_test, syms.nil_symbol) == YesNoMaybe.YES))
                {

                    ListItem<Variable> it = root_goals.first;
                    while (it != null)
                    {
                        final Variable sym = it.item;
                        final ListItem<Variable> next = it.next;
                        if (test_is_for_symbol(pc.id_test, sym) == YesNoMaybe.YES)
                        {
                            // remove sym from the root_goals list
                            it.remove(root_goals);
                            num_root_goals--;
                            if (num_root_goals == 1)
                            {
                                break; // be sure not to remove them all 
                            }
                        }
                        it = next;
                    } /* end of for (c) loop */
                    if (num_root_goals == 1)
                        break; /* be sure not to remove them all */
                }
            } /* end of for (cond) loop */
        }

        // if there's only one root goal, that's it!
        if (num_root_goals == 1)
        {
            return root_goals.getFirstItem();
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
     * <p>Note: this uses the TC routines and clobbers any existing TC.
     * 
     * <p>osupport.cpp:896:find_thing_off_goal
     * 
     * @param lhs
     * @param goal
     * @param attr
     * @return
     */
    private SymbolImpl find_thing_off_goal(Condition lhs, Variable goal, SymbolImpl attr)
    {
        for (Condition c = lhs; c != null; c = c.next)
        {
            PositiveCondition pc = c.asPositiveCondition();
            if (pc == null) continue;
            if (test_is_for_symbol(pc.id_test, goal) != YesNoMaybe.YES) continue;
            if (test_is_for_symbol(pc.attr_test, attr) != YesNoMaybe.YES) continue;
            if (c.test_for_acceptable_preference) continue;
            
            final Marker tc = DefaultMarker.create();
            ListHead<Variable> vars = ListHead.newInstance();
            pc.value_test.addBoundVariables(tc, vars);
            if (!vars.isEmpty())
            {
                return vars.getFirstItem();
            }
        }
        return null;
    }

    /**
     * This checks whether a given condition list has an equality test for a
     * given symbol in the id field of any condition (at any nesting level
     * within NCC's).
     * 
     * <p>osupport.cpp:928:condition_list_has_id_test_for_sym
     * 
     * @param conds
     * @param sym
     * @return
     */
    private boolean condition_list_has_id_test_for_sym(Condition conds, SymbolImpl sym)
    {
        for (; conds != null; conds = conds.next)
        {
            ThreeFieldCondition tfc = conds.asThreeFieldCondition(); // Positive or negative
            if (tfc != null)
            {
                if (Tests.test_includes_equality_test_for_symbol(tfc.id_test, sym))
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
    private boolean match_state_tests_non_operator_slot(Condition conds, SymbolImpl match_state)
    {
        YesNoMaybe ynm;

        for (; conds != null; conds = conds.next)
        {
            ThreeFieldCondition tfc = conds.asThreeFieldCondition();
            if (tfc != null)
            {
                if (Tests.test_includes_equality_test_for_symbol(tfc.id_test, match_state))
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
     * <p>osupport.cpp:986:add_tc_through_lhs_and_rhs
     * 
     * @param lhs
     * @param rhs
     * @param tc
     * @param id_list
     * @param var_list
     */
    private void add_tc_through_lhs_and_rhs(Condition lhs, Action rhs, Marker tc, ListHead<IdentifierImpl> id_list,
            ListHead<Variable> var_list)
    {

        for (Condition c = lhs; c != null; c = c.next)
        {
            c.already_in_tc = false;
        }
        for (Action a = rhs; a != null; a = a.next)
        {
            a.already_in_tc = false;
        }

        // keep trying to add new stuff to the tc
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
  SymbolImpl  match_operator;
  YesNoMaybe lhs_oa_support, lhs_oc_support, lhs_om_support;
  Action a;
  Condition cond;
  YesNoMaybe ynm;
  Marker tc;

  // initialize:  mark all rhs actions as "unknown"
  for (a=rhs; a!=null; a=a.next){
    MakeAction ma = a.asMakeAction();
    if (ma != null) {  a.support=ActionSupport.UNKNOWN_SUPPORT; }
  }

  // if "operator" doesn't appear in any LHS attribute slot, and there
  // are no RHS +/! makes for "operator", then nothing gets support
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
        SymbolImpl attr = rhsSym.getSym();
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


  // find known goals; RHS augmentations of goals get no support
  final ListHead<Variable> known_goals = find_known_goals (lhs);
 /* SBH: In NNPSCM, the only RHS-goal augmentations that can't get support are
    preferences for the "operator" slot. */
  for (ListItem<Variable> cIt = known_goals.first; cIt != null; cIt = cIt.next){
    final Variable c = cIt.item;
    for (a=rhs; a!=null; a=a.next) {
      MakeAction ma = a.asMakeAction();
      if (ma == null) { continue; }
      RhsSymbolValue rhsSym = ma.attr.asSymbolValue();
      if (rhsSym != null &&
          rhsSym.getSym()==syms.operator_symbol &&
          ma.id.asSymbolValue().getSym() == c) {
        a.support = ActionSupport.I_SUPPORT;
      }
    }
  }

  // find match goal, state, and operator 
  final Variable match_state = find_compile_time_match_goal (lhs, known_goals);
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
        if (ma.id.asSymbolValue().getSym() == match_state) {  continue; }
        RhsSymbolValue rhsSym = ma.attr.asSymbolValue();
        if (rhsSym != null) { /* RBD 3/29/95 */
          SymbolImpl attr = rhsSym.getSym();
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
  
  // calculate LHS support predicates
  lhs_oa_support = YesNoMaybe.MAYBE;
  if (match_operator != null){

    if ((condition_list_has_id_test_for_sym (lhs, match_operator)) &&
    (match_state_tests_non_operator_slot(lhs,match_state))){
      lhs_oa_support = YesNoMaybe.YES;
      }
  }

  lhs_oc_support = YesNoMaybe.MAYBE;
  lhs_om_support = YesNoMaybe.MAYBE;

  /* For NNPSCM, must test that there is a test of a non-operator slot off 
     of the match_state. */
  if (match_state_tests_non_operator_slot(lhs,match_state)) 
    {
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

  if (lhs_oa_support == YesNoMaybe.YES) {    // look for RHS o-a support
    // do TC(match_state)
    tc = DefaultMarker.create();
    match_state.add_symbol_to_tc (tc, null, null);
    add_tc_through_lhs_and_rhs (lhs, rhs, tc, null, null);

    // any action with id in the TC gets support
    for (a=rhs; a!=null; a=a.next)  {

      if (a.action_is_in_tc (tc)) {
    /* SBH 7/1/94 Avoid resetting of support that was previously set to I_SUPPORT. */
    /* gap 10/6/94 If the action has an attribue of operator, then you
       don't know if it should get o-support until run time because of
       the vagaries of knowing when this is matching a context object
       or not. */
        RhsSymbolValue rhsSym = a.asMakeAction().attr.asSymbolValue();
        if (rhsSym != null &&
            (rhsSym.getSym()==syms.operator_symbol)) 
        {
            if (a.support != ActionSupport.I_SUPPORT) { a.support = ActionSupport.UNKNOWN_SUPPORT; }
        } else {
            if (a.support != ActionSupport.I_SUPPORT) { a.support = ActionSupport.O_SUPPORT; }
        }
      }
    }
  }
  
  if (lhs_oc_support == YesNoMaybe.YES) {    // look for RHS o-c support
    // do TC(rhs operators)
    tc = DefaultMarker.create();
    for (a=rhs; a!=null; a=a.next) {
      MakeAction ma = a.asMakeAction();
      if (ma == null) { continue; }
      RhsSymbolValue rhsAttrSym = ma.attr.asSymbolValue();
      if ((ma.id.asSymbolValue().getSym()==match_state) &&
          rhsAttrSym != null &&
          (rhsAttrSym.getSym()==syms.operator_symbol) &&
          ((a.preference_type==PreferenceType.ACCEPTABLE) ||
           (a.preference_type==PreferenceType.REQUIRE)) ) {
        RhsSymbolValue rhsValueSym = ma.value.asSymbolValue();
        if (rhsValueSym != null) {
            rhsValueSym.getSym().add_symbol_to_tc (tc, null, null);
    }
      }
    }
    add_tc_through_lhs_and_rhs (lhs, rhs, tc, null, null);

    // any action with id in the TC gets support
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

       // in operand, operator proposals are now only i-supported.
       if (operand2_mode) {
           // TODO Verbose
//           if (thisAgent->soar_verbose_flag == TRUE) {
//               printf("\n         operator creation: setting a->support to I_SUPPORT");
//               xml_generate_verbose(thisAgent, "operator creation: setting a->support to I_SUPPORT");
//           }

           a.support = ActionSupport.I_SUPPORT;
       }
      }
  }

  if (lhs_om_support == YesNoMaybe.YES) {    // look for RHS o-m support
    // do TC(lhs operators)
    tc = DefaultMarker.create();
    for (cond=lhs; cond!=null; cond=cond.next) {
      PositiveCondition pc = cond.asPositiveCondition();
      if (pc == null) { continue; }
      if (test_is_for_symbol (pc.id_test, match_state) == YesNoMaybe.YES){
        if (test_is_for_symbol (pc.attr_test, syms.operator_symbol) == YesNoMaybe.YES){
          pc.value_test.addBoundVariables(tc, null);
        }
      }
    }
    add_tc_through_lhs_and_rhs (lhs, rhs, tc, null, null);

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