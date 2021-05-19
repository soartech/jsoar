/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 19, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.symbols.VariableGenerator;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;

/** @author ray */
public class ConditionReorderer {

  /* --- estimated k-search branching factors --- */
  private static final int MAX_COST = 10000005; // cost of a disconnected condition

  private static final int BF_FOR_ACCEPTABLE_PREFS = 8; // cost of (. ^. <var> +)
  private static final int BF_FOR_VALUES = 8; // cost of (. ^. <var>)
  private static final int BF_FOR_ATTRIBUTES = 8; // cost of (. ^<var> .)

  private final VariableGenerator vars;
  private final Trace trace;
  private final MultiAttributes multiAttrs;

  private String prodName;

  /**
   * Originally this was just a field on Condition. Converted to a temporary map to save memory. It
   * just makes more sense anyway.
   */
  private final Map<Condition, List<Variable>> reorder_vars_requiring_bindings =
      new HashMap<Condition, List<Variable>>();

  private static class SavedTest {
    public SavedTest(SavedTest old_sts, SymbolImpl var, ComplexTest the_test) {
      assert var != null;
      assert the_test != null;

      this.next = old_sts;
      this.var = var;
      this.the_test = the_test;
    }

    SavedTest next;
    SymbolImpl var;
    ComplexTest the_test;
  }

  public ConditionReorderer(
      VariableGenerator vars, Trace trace, MultiAttributes multiAttrs, String prodName) {
    this.vars = vars;
    this.trace = trace;
    this.multiAttrs = multiAttrs;
    this.prodName = prodName;
  }

  /**
   * reorder.cpp:1064:reorder_lhs
   *
   * @param lhs_top
   * @param lhs_bottom
   * @param reorder_nccs
   * @throws ReordererException
   */
  public void reorder_lhs(
      ByRef<Condition> lhs_top, ByRef<Condition> lhs_bottom, boolean reorder_nccs)
      throws ReordererException {
    final Marker tc = DefaultMarker.create();
    /* don't mark any variables, since nothing is bound outside the LHS */

    final ListHead<Variable> roots =
        Conditions.collect_root_variables(lhs_top.value, tc, trace.getPrinter(), prodName);

    /*
     * SBH/MVP 6-24-94 Fix to include only root "STATE" test in the LHS of a
     * chunk.
     */
    if (!roots.isEmpty()) {
      remove_isa_state_tests_for_non_roots(lhs_top, lhs_bottom, roots);
    }

    /* MVP 6-8-94 - fix provided by Bob */
    if (roots.isEmpty()) {

      for (Condition cond = lhs_top.value; cond != null; cond = cond.next) {
        var pc = cond.asPositiveCondition();
        if (pc != null && (Tests.test_includes_goal_or_impasse_id_test(pc.id_test, true, false))) {
          pc.id_test.addBoundVariables(tc, roots);
          if (!roots.isEmpty()) {
            break;
          }
        }
      }
    }

    if (roots.isEmpty()) {
      final var message =
          String.format("Error: in production %s,\n" + " The LHS has no roots.\n", prodName);
      trace.getPrinter().print(message);
      throw new ReordererException(message);
    }

    fill_in_vars_requiring_bindings(lhs_top.value, tc);
    reorder_condition_list(lhs_top, lhs_bottom, roots, tc, reorder_nccs);
    remove_vars_requiring_bindings(lhs_top.value);
    check_negative_relational_test_bindings(lhs_top.value, DefaultMarker.create());
  }

  /**
   * Reorder the given list of conditions. The "top_of_conds" and "bottom_of_conds" arguments are
   * destructively modified to reflect the reordered conditions. The "bound_vars_tc_number" should
   * reflect the variables bound outside the given condition list. The "reorder_nccs" flag indicates
   * whether it is necessary to recursively reorder the subconditions of NCC's. (For newly built
   * chunks, this is never necessary.)
   *
   * <p>reorder.cpp:979:reorder_condition_list
   *
   * @param top_of_conds
   * @param bottom_of_conds
   * @param roots
   * @param tc
   * @param reorder_nccs
   */
  private void reorder_condition_list(
      ByRef<Condition> top_of_conds,
      ByRef<Condition> bottom_of_conds,
      ListHead<Variable> roots,
      Marker tc,
      boolean reorder_nccs) {
    final SavedTest saved_tests = simplify_condition_list(top_of_conds.value);
    reorder_simplified_conditions(top_of_conds, bottom_of_conds, roots, tc, reorder_nccs);
    restore_and_deallocate_saved_tests(top_of_conds.value, tc, saved_tests);
  }

  /** reorder.cpp:398:restore_and_deallocate_saved_tests */
  private void restore_and_deallocate_saved_tests(
      Condition conds_list, Marker tc, SavedTest tests_to_restore) {
    final ListHead<Variable> new_vars = ListHead.newInstance();
    for (Condition cond = conds_list; cond != null; cond = cond.next) {
      if (cond.asPositiveCondition() != null) {
        var pc = cond.asPositiveCondition();

        ByRef<Test> id_test = ByRef.create(pc.id_test);
        tests_to_restore = restore_saved_tests_to_test(id_test, true, tc, tests_to_restore, false);
        pc.id_test = id_test.value;

        pc.id_test.addBoundVariables(tc, new_vars);

        ByRef<Test> attr_test = ByRef.create(pc.attr_test);
        tests_to_restore =
            restore_saved_tests_to_test(attr_test, false, tc, tests_to_restore, false);
        pc.attr_test = attr_test.value;

        pc.attr_test.addBoundVariables(tc, new_vars);

        ByRef<Test> value_test = ByRef.create(pc.value_test);
        tests_to_restore =
            restore_saved_tests_to_test(value_test, false, tc, tests_to_restore, false);
        pc.value_test = value_test.value;

        pc.value_test.addBoundVariables(tc, new_vars);
      }
    }
    if (tests_to_restore != null) {
      final var p = trace.getPrinter();
      if (p.isPrintWarnings()) {
        p.warn(
            "\nWarning: in production %s,\n ignoring test(s) whose referent is unbound:\n",
            prodName);
        // TODO print_saved_test_list (thisAgent, tests_to_restore);
      }
    }
    Variable.unmark(new_vars);
  }

  /** reorder.cpp:339 */
  private SavedTest restore_saved_tests_to_test(
      ByRef<Test> t,
      boolean is_id_field,
      Marker bound_vars_tc_number,
      SavedTest tests_to_restore,
      boolean neg) {
    SavedTest prev_st = null, next_st = null;
    SavedTest st = tests_to_restore;
    while (st != null) {
      next_st = st.next;
      var added_it = false;

      if ((is_id_field
              && (st.the_test.asGoalIdTest() != null || st.the_test.asImpasseIdTest() != null))
          || st.the_test.asDisjunctionTest() != null) {
        if (Tests.test_includes_equality_test_for_symbol(t.value, st.var)) {
          t.value = Tests.add_new_test_to_test_if_not_already_there(t.value, st.the_test, neg);
          added_it = true;
        }
      }
      var rt = st.the_test.asRelationalTest();
      if (rt != null) // relational test other than equality
      {
        SymbolImpl referent = rt.referent;
        if (Tests.test_includes_equality_test_for_symbol(t.value, st.var)) {
          if (symbol_is_constant_or_marked_variable(referent, bound_vars_tc_number)
              || (st.var == referent)) {
            t.value = Tests.add_new_test_to_test_if_not_already_there(t.value, st.the_test, neg);
            added_it = true;
          }
        } else if (Tests.test_includes_equality_test_for_symbol(t.value, referent)) {
          if (symbol_is_constant_or_marked_variable(st.var, bound_vars_tc_number)
              || (st.var == referent)) {

            rt.type = RelationalTest.reverse_direction_of_relational_test(rt.type);
            rt.referent = st.var;
            st.var = referent;
            t.value = Tests.add_new_test_to_test_if_not_already_there(t.value, st.the_test, neg);
            added_it = true;
          }
        }
      }

      if (added_it) {
        if (prev_st != null) {
          prev_st.next = next_st;
        } else {
          tests_to_restore = next_st;
        }
        // symbol_remove_ref (thisAgent, st->var);
        // free_with_pool (&thisAgent->saved_test_pool, st);
      } else {
        prev_st = st;
      }
      st = next_st;
    } /* end of while (st) */
    return tests_to_restore;
  }

  /** reorder.cpp:53 */
  private static boolean symbol_is_constant_or_marked_variable(
      SymbolImpl referent, Marker bound_vars_tc_number) {
    var var = referent.asVariable();
    return var == null || var.tc_number == bound_vars_tc_number;
  }

  /** reorder.cpp:828:reorder_simplified_conditions */
  private void reorder_simplified_conditions(
      ByRef<Condition> top_of_conds,
      ByRef<Condition> bottom_of_conds,
      ListHead<Variable> roots,
      Marker bound_vars_tc_number,
      boolean reorder_nccs) {
    // Originally condition::reorder_next_min_cost. Moved here to save memory, reduce
    // coupling, etc.
    final Map<Condition, Condition> reorder_next_min_cost = new HashMap<Condition, Condition>();

    Condition remaining_conds = top_of_conds.value; // header of dll
    Condition first_cond = null;
    Condition last_cond = null;
    final ListHead<Variable> new_vars = ListHead.newInstance();
    Condition chosen;

    /*
     * repeat: scan through remaining_conds rate each one if tie, call
     * lookahead routine add min-cost item to conds
     */

    while (remaining_conds != null) {
      /* --- find min-cost set --- */
      Condition min_cost_conds = null;
      var min_cost = 0;
      var cost = 0;
      for (Condition cond = remaining_conds; cond != null; cond = cond.next) {
        cost = cost_of_adding_condition(cond, bound_vars_tc_number, roots);
        if (min_cost_conds == null || cost < min_cost) {
          min_cost = cost;
          min_cost_conds = cond;
          reorder_next_min_cost.put(cond, null);
        } else if (cost == min_cost) {
          reorder_next_min_cost.put(cond, min_cost_conds);
          min_cost_conds = cond;
        }
        /*
         * if (min_cost <= 1) break; This optimization needs to be
         * removed, otherwise the tie set is not created. Without the
         * tie set we can't check the canonical order.
         */
      }
      // if min_cost==MAX_COST, print error message
      if (min_cost == MAX_COST) {
        trace
            .getPrinter()
            .warn(
                "Warning: in production %s,\n" + " The LHS conditions are not all connected.\n",
                prodName);

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
      // if more than one min-cost item, and cost>1, do lookahead
      if (min_cost > 1 && reorder_next_min_cost.get(min_cost_conds) != null) {
        min_cost = MAX_COST + 1;
        for (Condition cond = min_cost_conds, next_cond = reorder_next_min_cost.get(cond);
            cond != null;
            cond = next_cond, next_cond = (cond != null ? reorder_next_min_cost.get(cond) : null)) {
          cost = find_lowest_cost_lookahead(remaining_conds, cond, bound_vars_tc_number, roots);
          if (cost < min_cost) {
            min_cost = cost;
            min_cost_conds = cond;
            reorder_next_min_cost.put(cond, null);
          } else {
            /*******************************************************
             * These code segments find the condition in the tie set
             * with the smallest value in the canonical order. This
             * ensures that productions with the same set of
             * conditions are ordered the same. Except if the
             * variables are assigned differently.
             ******************************************************/
            if (cost == min_cost && cond.asPositiveCondition() != null) {
              if (canonical_cond_greater(min_cost_conds, cond)) {
                min_cost = cost;
                min_cost_conds = cond;
                reorder_next_min_cost.put(cond, null);
              }
            }
          }
        }
      }
      /** **************************************************************** */
      if (min_cost == 1 && reorder_next_min_cost.get(min_cost_conds) != null) {
        for (Condition cond = min_cost_conds;
            cond != null;
            cond = reorder_next_min_cost.get(cond)) {
          if (cond.asPositiveCondition() != null
              && min_cost_conds.asPositiveCondition() != null
              && canonical_cond_greater(min_cost_conds, cond)) {
            min_cost = cost;
            min_cost_conds = cond;
          } else if (cond.asPositiveCondition() == null
              && min_cost_conds.asPositiveCondition() != null) {
            min_cost = cost;
            min_cost_conds = cond;
          }
        }
      }
      /** **************************************************************** */

      /* --- install the first item in the min-cost set --- */
      chosen = min_cost_conds;
      remaining_conds = Condition.removeFromList(remaining_conds, chosen);
      if (first_cond == null) {
        first_cond = chosen;
      }
      last_cond = Condition.insertAtEnd(last_cond, chosen);

      // if a conjunctive negation, recursively reorder its conditions
      var ncc = chosen.asConjunctiveNegationCondition();
      if (ncc != null && reorder_nccs) {
        final ListHead<Variable> ncc_roots =
            Conditions.collect_root_variables(
                ncc.top, bound_vars_tc_number, trace.getPrinter(), prodName);
        final ByRef<Condition> top = ByRef.create(ncc.top);
        final ByRef<Condition> bottom = ByRef.create(ncc.bottom);
        reorder_condition_list(top, bottom, ncc_roots, bound_vars_tc_number, reorder_nccs);
        ncc.top = top.value;
        ncc.bottom = bottom.value;
      }

      // update set of bound variables for newly added condition
      chosen.addBoundVariables(bound_vars_tc_number, new_vars);

      // if all roots are bound, set roots=NIL: don't need 'em anymore
      if (!roots.isEmpty()) {
        var allBound = true;
        for (ListItem<Variable> v = roots.first; v != null; v = v.next) {
          if (v.item.tc_number != bound_vars_tc_number) {
            allBound = false;
            break;
          }
        }
        if (allBound) {
          roots.clear();
        }
      }
    } /* end of while (remaining_conds) */

    Variable.unmark(new_vars); // unmark_variables_and_free_list
    // (thisAgent, new_vars);
    top_of_conds.value = first_cond;
    bottom_of_conds.value = last_cond;
  }

  /** production.cpp:503:canonical_test */
  private int canonical_test(Test t) {
    final var NON_EQUAL_TEST_RETURN_VAL = 0; /* some unusual number */

    if (Tests.isBlank(t)) {
      return NON_EQUAL_TEST_RETURN_VAL;
    }

    var eq = t.asEqualityTest();
    if (eq != null) {
      SymbolImpl sym = eq.getReferent();
      if (sym.asString() != null || sym.asInteger() != null || sym.asDouble() != null) {
        return sym.getHash();
      }
      return NON_EQUAL_TEST_RETURN_VAL;
    }
    return NON_EQUAL_TEST_RETURN_VAL;
  }

  /**
   * Extensive discussion in reorder.cpp
   *
   * <p>reorder.cpp:536
   */
  private boolean canonical_cond_greater(Condition c1, Condition c2) {
    int test_order_1, test_order_2;

    if ((test_order_1 = canonical_test(c1.asPositiveCondition().attr_test))
        < (test_order_2 = canonical_test(c2.asPositiveCondition().attr_test))) {
      return true;
    } else if (test_order_1 == test_order_2
        && canonical_test(c1.asPositiveCondition().value_test)
            < canonical_test(c2.asPositiveCondition().value_test)) {
      return true;
    }
    return false;
  }

  /**
   * Return an estimate of the "cost" of the lowest-cost condition that could be added next, IF the
   * given "chosen" condition is added first.
   *
   * <p>reorder.cpp:787:find_lowest_cost_lookahead
   */
  private int find_lowest_cost_lookahead(
      Condition candidates,
      Condition chosen,
      Marker tc,
      ListHead<Variable> root_vars_not_bound_yet) {
    ListHead<Variable> new_vars = ListHead.newInstance();
    chosen.addBoundVariables(tc, new_vars);

    int min_cost = MAX_COST + 1;
    for (Condition c = candidates; c != null; c = c.next) {
      if (c == chosen) continue;

      int cost = cost_of_adding_condition(c, tc, root_vars_not_bound_yet);
      if (cost < min_cost) {
        min_cost = cost;
        if (cost <= 1) {
          break;
        }
      }
    }
    Variable.unmark(new_vars);

    return min_cost;
  }

  /**
   * Return an estimate of the "cost" of the given condition. The current TC should be the set of
   * previously bound variables; "root_vars_not_bound_yet" should be the set of other root
   * variables.
   *
   * <p>reorder.cpp:716:cost_of_adding_condition
   */
  private int cost_of_adding_condition(
      Condition cond, Marker tc, ListHead<Variable> root_vars_not_bound_yet) {
    int result;

    /* --- handle the common simple case quickly up front --- */
    final var pc = cond.asPositiveCondition();
    if (root_vars_not_bound_yet.isEmpty()
        && pc != null
        && !Tests.isBlank(pc.id_test)
        && !Tests.isBlank(pc.attr_test)
        && !Tests.isBlank(pc.value_test)
        && pc.id_test.asEqualityTest() != null
        && pc.attr_test.asEqualityTest() != null
        && pc.value_test.asEqualityTest() != null) {

      if (!symbol_is_constant_or_marked_variable(pc.id_test.asEqualityTest().getReferent(), tc)) {
        return MAX_COST;
      }
      if (symbol_is_constant_or_marked_variable(pc.attr_test.asEqualityTest().getReferent(), tc)) {
        result = multiAttrs.getCost(pc.attr_test.asEqualityTest().getReferent(), 1);
      } else {
        result = BF_FOR_ATTRIBUTES;
      }

      if (!symbol_is_constant_or_marked_variable(
          pc.value_test.asEqualityTest().getReferent(), tc)) {
        if (pc.test_for_acceptable_preference) {
          result = result * BF_FOR_ACCEPTABLE_PREFS;
        } else {
          result = result * BF_FOR_VALUES;
        }
      }
      return result;
    } /* --- end of common simple case --- */

    if (pc != null) {
      /* --- for pos cond's, check what's bound, etc. --- */
      if (!test_covered_by_bound_vars(pc.id_test, tc, root_vars_not_bound_yet)) {
        return MAX_COST;
      }
      if (test_covered_by_bound_vars(pc.attr_test, tc, root_vars_not_bound_yet)) {
        result = 1;
      } else {
        result = BF_FOR_ATTRIBUTES;
      }
      if (!test_covered_by_bound_vars(pc.value_test, tc, root_vars_not_bound_yet)) {
        if (pc.test_for_acceptable_preference) {
          result = result * BF_FOR_ACCEPTABLE_PREFS;
        } else {
          result = result * BF_FOR_VALUES;
        }
      }
      return result;
    }

    // negated or NC conditions: just check whether all variables requiring
    // bindings are actually bound. If so, return 1, else return MAX_COST
    for (Variable v : reorder_vars_requiring_bindings.get(cond)) {
      if (v.tc_number != tc) {
        return MAX_COST;
      }
    }
    return 1;
  }

  /**
   * Return TRUE iff the given test is covered by the previously bound variables. The set of
   * previously bound variables is given by the current TC, PLUS any variables in the list
   * "extra_vars."
   *
   * <p>reorder.cpp:669:test_covered_by_bound_vars
   */
  private boolean test_covered_by_bound_vars(Test t, Marker tc, ListHead<Variable> extra_vars) {
    if (Tests.isBlank(t)) {
      return false;
    }

    var eq = t.asEqualityTest();
    if (eq != null) {
      SymbolImpl referent = eq.getReferent();
      if (symbol_is_constant_or_marked_variable(referent, tc)) {
        return true;
      }
      return extra_vars.contains(referent);
    }

    var ct = t.asConjunctiveTest();
    if (ct != null) {
      for (Test child : ct.conjunct_list) {
        if (test_covered_by_bound_vars(child, tc, extra_vars)) {
          return true;
        }
      }
    }
    return false;
  }

  /** reorder.cpp:303 */
  private SavedTest simplify_condition_list(Condition conds_list) {
    SavedTest sts = null;
    for (Condition c = conds_list; c != null; c = c.next) {
      if (c.asPositiveCondition() != null) {
        var pc = c.asPositiveCondition();

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

  /** reorder.cpp:223 */
  private SavedTest simplify_test(ByRef<Test> t, SavedTest old_sts) {
    if (Tests.isBlank(t.value)) {
      SymbolImpl sym = vars.generate_new_variable("dummy-");
      t.value = SymbolImpl.makeEqualityTest(sym);
      return old_sts;
    }

    if (t.value.asEqualityTest() != null) {
      return old_sts;
    }

    var ct = t.value.asConjunctiveTest();
    if (ct != null) {
      // look at subtests for an equality test
      SymbolImpl sym = null;
      for (Test subtest : ct.conjunct_list) {
        final var eq = subtest.asEqualityTest();
        if (eq != null) {
          sym = eq.getReferent();
        }
      }
      // if no equality test was found, generate a variable for it
      if (sym == null) {
        sym = vars.generate_new_variable("dummy-");
        var newTest = SymbolImpl.makeEqualityTest(sym);
        ct.conjunct_list.add(0, newTest); // push(newTest);
      }
      // scan through, create saved_test for subtests except equality
      final Iterator<Test> it = ct.conjunct_list.iterator();
      while (it.hasNext()) {
        final var subtest = it.next();
        if (subtest.asEqualityTest() == null) {
          // create saved_test, splice this cons out of conjunct_list
          final var saved = new SavedTest(old_sts, sym, subtest.asComplexTest());

          old_sts = saved;

          it.remove();
        }
      }
    } else {
      // goal/impasse, disjunction, and non-equality relational tests
      final var var = vars.generate_new_variable("dummy-");
      final var New = SymbolImpl.makeEqualityTest(var);
      final var saved = new SavedTest(old_sts, var, t.value.asComplexTest());

      old_sts = saved;
      t.value = New;
      // *t = make_equality_test_without_adding_reference (sym);
    }
    return old_sts;
  }

  /**
   * reorder.cpp:554:remove_vars_requiring_bindings
   *
   * @param cond_list
   */
  private void remove_vars_requiring_bindings(Condition cond_list) {
    // scan through negated and NC cond's, remove lists from them
    for (Condition c = cond_list; c != null; c = c.next) {
      final var pc = c.asPositiveCondition();
      if (pc == null) {
        reorder_vars_requiring_bindings.get(c).clear();
      }
      final var ncc = c.asConjunctiveNegationCondition();
      if (ncc != null) {
        remove_vars_requiring_bindings(ncc.top);
      }
    }
  }

  /**
   * reorder.cpp:1040:remove_isa_state_tests_for_non_roots
   *
   * @param lhs_top
   * @param lhs_bottom
   * @param roots
   */
  private void remove_isa_state_tests_for_non_roots(
      ByRef<Condition> lhs_top, ByRef<Condition> lhs_bottom, ListHead<Variable> roots) {
    ByRef<Boolean> a = ByRef.create(false);
    ByRef<Boolean> b = ByRef.create(false);

    for (Condition cond = lhs_top.value; cond != null; cond = cond.next) {
      var pc = cond.asPositiveCondition();
      if (pc != null
          && pc.id_test.asComplexTest() != null
          && Tests.test_includes_goal_or_impasse_id_test(pc.id_test, true, false)
          && !Tests.test_tests_for_root(pc.id_test, roots)) {
        pc.id_test = Tests.copy_test_removing_goal_impasse_tests(pc.id_test, a, b);
      }
    }
  }

  /**
   * reorder.cpp:532:fill_in_vars_requiring_bindings
   *
   * @param cond_list
   * @param tc
   */
  private void fill_in_vars_requiring_bindings(Condition cond_list, Marker tc) {
    // add anything bound in a positive condition at this level
    ListHead<Variable> new_bound_vars = ListHead.newInstance();
    for (Condition c = cond_list; c != null; c = c.next) {
      var pc = c.asPositiveCondition();
      if (pc != null) {
        pc.addBoundVariables(tc, new_bound_vars);
      }
    }

    // scan through negated and NC cond's, fill in stuff
    for (Condition c = cond_list; c != null; c = c.next) {
      var pc = c.asPositiveCondition();
      if (pc == null) {
        reorder_vars_requiring_bindings.put(
            c, collect_vars_tested_by_cond_that_are_bound(c, tc, new LinkedList<Variable>()));
      }
      var ncc = c.asConjunctiveNegationCondition();
      if (ncc != null) {
        fill_in_vars_requiring_bindings(ncc.top, tc);
      }
    }

    Variable.unmark(new_bound_vars);
  }

  /**
   * reorder.cpp:509:collect_vars_tested_by_cond_that_are_bound
   *
   * <p>TODO: Check whether null for start_list can ever occur
   *
   * @param cond
   * @param tc
   * @param starting_list
   */
  private LinkedList<Variable> collect_vars_tested_by_cond_that_are_bound(
      Condition cond, Marker tc, @NonNull LinkedList<Variable> starting_list) {

    var ncc = cond.asConjunctiveNegationCondition();
    if (ncc != null) {
      for (Condition c = ncc.top; c != null; c = c.next) {
        collect_vars_tested_by_cond_that_are_bound(c, tc, starting_list);
      }
    }
    // Positive and Negative conditions
    var tfc = cond.asThreeFieldCondition();
    if (tfc != null) {
      collect_vars_tested_by_test_that_are_bound(tfc.id_test, tc, starting_list);
      collect_vars_tested_by_test_that_are_bound(tfc.attr_test, tc, starting_list);
      collect_vars_tested_by_test_that_are_bound(tfc.value_test, tc, starting_list);
    }

    return starting_list;
  }

  /**
   * reorder.cpp:468:collect_vars_tested_by_test_that_are_bound
   *
   * <p>TODO: Check whether null for start_list can ever occur
   *
   * @param t
   * @param tc
   * @param starting_list
   */
  private void collect_vars_tested_by_test_that_are_bound(
      Test t, Marker tc, @NonNull LinkedList<Variable> starting_list) {

    if (Tests.isBlank(t)) {
      return;
    }

    var eq = t.asEqualityTest();
    if (eq != null) {
      var referent = eq.getReferent().asVariable();
      if (referent != null && referent.tc_number == tc && !starting_list.contains(referent)) {
        starting_list.push(referent);
      }
      return;
    }

    var ct = t.asConjunctiveTest();
    if (ct != null) {
      for (Test c : ct.conjunct_list) {
        collect_vars_tested_by_test_that_are_bound(c, tc, starting_list);
      }
      return;
    }
    var rt = t.asRelationalTest();
    if (rt != null) {
      var referent = rt.referent.asVariable();
      if (referent != null && referent.tc_number == tc && !starting_list.contains(referent)) {
        starting_list.add(referent);
      }
    }

    // Do nothing for GoalId, Impasse, or disjunction
  }

  /* -------------------------------------------------------------
  ------------------------------------------------------------- */
  /**
   * reorder.cpp:1121:check_negative_relational_test_bindings
   *
   * <p>check_unbound_negative_relational_test_referents check_negative_relational_test_bindings
   *
   * <p>These two functions are for fixing bug 517. The bug stems from two different code paths
   * being used to check the bound variables after reordering the left hand side; one for positive
   * conditions and one for negated conditions.
   *
   * <p>Specifically, the old system would let unbound referents of non-equality relational tests
   * continue past the reordering until the production addition failed as the bad production was
   * added to the rete.
   *
   * <p>These two productions specifically check that all referents of non-equality relational tests
   * are bound and return false if an unbound referent is discovered.
   *
   * <p>There may be a faster way of checking for this inside of the existing calls to
   * fill_in_vars_requiring_bindings and reorder_condition_list, but my last attempt at fixing it
   * there failed.
   *
   * <p>Example bad production: {@code sp {test (state <s> ^superstate nil -^foo {<> <bar>}) --> }}
   *
   * @param cond_list
   * @param tc
   */
  private void check_negative_relational_test_bindings(Condition cond_list, Marker tc)
      throws ReordererException {
    ListHead<Variable> bound_vars =
        ListHead
            .newInstance(); // this list necessary pop variables bound inside ncc's out of scope on
    // return

    /* --- add anything bound in a positive condition at this level --- */
    /* --- recurse in to NCCs --- */
    for (Condition c = cond_list; c != null; c = c.next) {
      var pc = c.asPositiveCondition();
      if (pc != null) {
        PositiveCondition.addBoundVariables(c, tc, bound_vars);
      } else {
        var ncc = c.asConjunctiveNegationCondition();
        if (ncc != null) {
          check_negative_relational_test_bindings(ncc.top, tc);
        }
      }
    }

    /* --- find referents of non-equality tests in conjunctive tests in negated conditions ---*/
    for (Condition c = cond_list; c != null; c = c.next) {
      var nc = c.asNegativeCondition();
      if (nc != null) {
        check_unbound_negative_relational_test_referents(nc.id_test, tc);
        check_unbound_negative_relational_test_referents(nc.attr_test, tc);
        check_unbound_negative_relational_test_referents(nc.value_test, tc);
      }
    }

    // unmark anything bound on this level
    Variable.unmark(bound_vars);
  }

  /**
   * reorder.cpp:1080:check_unbound_negative_relational_test_referents
   *
   * @see org.jsoar.kernel.lhs.ConditionReorderer#check_negative_relational_test_bindings
   * @param t
   * @param tc
   */
  private void check_unbound_negative_relational_test_referents(Test t, Marker tc)
      throws ReordererException {
    // we only care about relational tests other than equality
    if (Tests.isBlank(t)) {
      return;
    }
    final var eq = t.asEqualityTest();
    if (eq != null) {
      return;
    }

    final var ct = t.asConjunctiveTest();
    if (ct != null) {
      // we do need to loop over conjunctive tests, however
      for (Test subtest : ct.conjunct_list) {
        check_unbound_negative_relational_test_referents(subtest, tc);
      }
    }

    final var rt = t.asRelationalTest();
    if (rt != null) {
      /* --- relational tests other than equality --- */
      var referent = rt.referent.asVariable();
      if (referent != null && referent.tc_number != tc) {
        var message =
            String.format(
                "Error: production %s has an unbound referent in negated relational test %s",
                this.prodName, t);
        trace.getPrinter().print(message);
        throw new ReordererException(message);
      }
    }
  }
}
