/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 18, 2009
 */
package org.jsoar.kernel;

import java.io.StringReader;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveNegationCondition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.Tests;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.parser.ParserContext;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.parser.original.OriginalParser;
import org.jsoar.kernel.rete.ConditionsAndNots;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.adaptables.Adaptables;

/**
 * sml_KernelHelpers.cpp::ProductionFind
 *
 * @author ray
 */
public class ProductionFinder {
  public static enum Options {
    LHS,
    RHS
  }

  private static class Binding {
    private final Symbol from, to;

    Binding(Symbol from, Symbol to) {
      this.from = from;
      this.to = to;
    }

    public String toString() {
      return String.format("%s -> %s", from, to);
    }
  }

  private final EnumSet<Options> options = EnumSet.allOf(Options.class);
  private final Agent agnt;
  private final ParserContext parserContext =
      new ParserContext() {
        @Override
        public Object getAdapter(Class<?> klass) {
          return agnt.getAdapter(klass);
        }
      };

  /**
   * Construct a new production finder for the given agent
   *
   * @param agent the agent
   */
  public ProductionFinder(Agent agent) {
    this.agnt = agent;
  }

  /** @return a reference to a modifiable set of options for the finder */
  public EnumSet<Options> options() {
    return options;
  }

  /**
   * Find productions that match the given pattern.
   *
   * @param pattern the pattern
   * @param productions list of productions to search
   * @return List of matching productions
   * @throws ParserException if the pattern does not parse
   */
  public List<Production> find(String pattern, Collection<Production> productions)
      throws ParserException {
    LinkedList<Production> current_pf_list = new LinkedList<Production>();

    if (options.contains(Options.LHS)) {
      read_pattern_and_get_matching_productions(pattern, productions, current_pf_list);
    }

    if (options.contains(Options.RHS)) {
      read_rhs_pattern_and_get_matching_productions(pattern, productions, current_pf_list);
    }

    return current_pf_list;
  }

  private Symbol get_binding(Symbol f, ListHead<Binding> bindings) {
    for (Binding b : bindings) {
      if (b.from == f) {
        return b.to;
      }
    }
    return null;
  }

  private boolean symbols_are_equal_with_bindings(
      SymbolImpl s1, SymbolImpl s2, ListHead<Binding> bindings) {
    /* SBH/MVP 7-5-94 */
    if (s1 == s2 && s1.asVariable() == null) return true;

    /* "*" matches everything. */
    if (s1.asString() != null && s1.asString().getValue().equals("*")) return true;
    if (s2.asString() != null && s2.asString().getValue().equals("*")) return true;

    if (s1.asVariable() == null || s2.asVariable() == null) return false;

    /* Both are variables */
    Symbol bvar = get_binding(s1, bindings);
    if (bvar == null) {
      bindings.push(new Binding(s1, s2));
      return true;
    } else if (bvar == s2) {
      return true;
    } else return false;
  }

  private boolean actions_are_equal_with_bindings(
      Action a1, Action a2, ListHead<Binding> bindings) {
    if (a2.asFunctionAction() != null) return false;

    // Both are make_actions.
    final MakeAction m1 = a1.asMakeAction();
    final MakeAction m2 = a2.asMakeAction();

    if (m1.preference_type != m2.preference_type) return false;

    if (!symbols_are_equal_with_bindings(
        m1.id.asSymbolValue().getSym(), m2.id.asSymbolValue().getSym(), bindings)) return false;

    if (m1.attr.asSymbolValue() != null && m2.attr.asSymbolValue() != null) {
      if (!symbols_are_equal_with_bindings(
          m1.attr.asSymbolValue().getSym(), m2.attr.asSymbolValue().getSym(), bindings)) {
        return false;
      }
    }

    // Values are different. They are rhs_value's.

    if (m1.value.asSymbolValue() != null && m2.value.asSymbolValue() != null) {
      if (symbols_are_equal_with_bindings(
          m1.value.asSymbolValue().getSym(), m2.value.asSymbolValue().getSym(), bindings)) {
        return true;
      } else {
        return false;
      }
    }
    if (m1.value.asFunctionCall() != null && m2.value.asFunctionCall() != null) {
      return false;
    }
    return false;
  }

  private void reset_old_binding_point(
      ListHead<Binding> bindings, ListItem<Binding> current_binding_point) {
    while (bindings.first != current_binding_point) {
      bindings.pop();
    }
  }

  private boolean tests_are_equal_with_bindings(Test t1, Test test2, ListHead<Binding> bindings) {

    // t1 is from the pattern given to "pf"; t2 is from a production's condition list.
    if (Tests.isBlank(t1)) return !Tests.isBlank(test2);

    // If the pattern doesn't include "(state", but the test from the production
    // does, strip it out of the production's.
    final Test t2;
    if ((!Tests.test_includes_goal_or_impasse_id_test(t1, true, false))
        && Tests.test_includes_goal_or_impasse_id_test(test2, true, false)) {
      t2 =
          Tests.copy_test_removing_goal_impasse_tests(
              test2, ByRef.create(false), ByRef.create(false));
    } else {
      t2 = Tests.copy(test2); /* DJP 4/3/96 -- Always make t2 into a copy */
    }

    if (t1.asEqualityTest() != null) {
      if (!(!Tests.isBlank(t2) && t2.asEqualityTest() != null)) {
        return false;
      } else {
        if (symbols_are_equal_with_bindings(
            t1.asEqualityTest().getReferent(), t2.asEqualityTest().getReferent(), bindings)) {
          return true;
        } else {
          return false;
        }
      }
    }

    if (t1.asGoalIdTest() != null && t2.asGoalIdTest() != null) return true;
    if (t1.asImpasseIdTest() != null && t2.asImpasseIdTest() != null) return true;
    if (t1.asDisjunctionTest() != null && t2.asDisjunctionTest() != null) {
      return t1.asDisjunctionTest()
          .disjunction_list
          .equals(t2.asDisjunctionTest().disjunction_list);
    }
    if (t1.asConjunctiveTest() != null && t2.asConjunctiveTest() != null) {
      final ConjunctiveTest ct1 = t1.asConjunctiveTest();
      final ConjunctiveTest ct2 = t2.asConjunctiveTest();
      if (ct1.conjunct_list.size() != ct2.conjunct_list.size()) return false;
      final Iterator<Test> it1 = ct1.conjunct_list.iterator();
      final Iterator<Test> it2 = ct2.conjunct_list.iterator();
      while (it1.hasNext()) {
        if (!tests_are_equal_with_bindings(it1.next(), it2.next(), bindings)) {
          return false;
        }
      }
      return true;
    }

    // relational tests other than equality
    return symbols_are_equal_with_bindings(
        t1.asRelationalTest().referent, t2.asRelationalTest().referent, bindings);
  }

  private boolean conditions_are_equal_with_bindings(
      Condition c1, Condition c2, ListHead<Binding> bindings) {
    final ThreeFieldCondition tfc1 = c1.asThreeFieldCondition();
    final ThreeFieldCondition tfc2 = c2.asThreeFieldCondition();
    if (tfc1 != null && tfc2 != null) {
      if (!tests_are_equal_with_bindings(tfc1.id_test, tfc2.id_test, bindings)) return false;
      if (!tests_are_equal_with_bindings(tfc1.attr_test, tfc2.attr_test, bindings)) return false;

      if (!tests_are_equal_with_bindings(tfc1.value_test, tfc2.value_test, bindings)) return false;
      if (tfc1.test_for_acceptable_preference != tfc2.test_for_acceptable_preference) return false;
      return true;
    }

    final ConjunctiveNegationCondition ncc1 = c1.asConjunctiveNegationCondition();
    final ConjunctiveNegationCondition ncc2 = c2.asConjunctiveNegationCondition();
    if (ncc1 != null && ncc2 != null) {
      for (c1 = ncc1.top, c2 = ncc2.top; ((c1 != null) && (c2 != null)); c1 = c1.next, c2 = c2.next)
        if (!conditions_are_equal_with_bindings(c1, c2, bindings)) return false;
      if (c1 == c2) return true; /* make sure they both hit end-of-list */
      return false;
    }
    return false; /* unreachable, but without it, gcc -Wall warns here */
  }

  private void read_pattern_and_get_matching_productions(
      String pattern, Collection<Production> productions, List<Production> current_pf_list)
      throws ParserException {
    final OriginalParser parser = new OriginalParser();
    final Condition clist = parser.parseLeftHandSide(parserContext, new StringReader(pattern));

    final Rete rete = Adaptables.adapt(agnt, Rete.class);
    for (Production prod : productions) {
      // Now the complicated part.
      // This is basically a full graph-match.  Like the rete.  Yikes!
      // Actually it's worse, because there are so many different KINDS of
      // conditions (negated, >/<, acc prefs, ...)
      // Is there some way I could *USE* the rete for this?  -- for lhs
      // positive conditions, possibly.  Put some fake stuff into WM
      // (i.e. with make-wme's), see what matches all of them, and then
      // yank out the fake stuff.  But that won't work for RHS or for
      // negateds.

      // Also note that we need bindings for every production.  Very expensive
      // (but don't necc. need to save them -- maybe can just print them as we go).

      boolean match = true;
      final ConditionsAndNots cans =
          rete.p_node_to_conditions_and_nots(prod.getReteNode(), null, null, false);
      final ListHead<Binding> bindings = ListHead.newInstance();
      ListItem<Binding> current_binding_point = null;

      for (Condition c = clist; c != null; c = c.next) {
        boolean match_this_c = false;
        current_binding_point = bindings.first;

        for (Condition pc = cans.top; pc != null; pc = pc.next) {
          if (conditions_are_equal_with_bindings(c, pc, bindings)) {
            match_this_c = true;
            break;
          } else {
            // Remove new, incorrect bindings.
            reset_old_binding_point(bindings, current_binding_point);
            bindings.first = current_binding_point;
          }
        }
        if (!match_this_c) {
          match = false;
          break;
        }
      }
      if (match) {
        current_pf_list.add(prod);
        /* TODO
        if (show_bindings) {
            print_with_symbols(agnt, "%y, with bindings:\n",prod->name);
            print_binding_list(agnt, bindings);
        }
        else
            print_with_symbols(agnt, "%y\n",prod->name);
            */
      }
    }
  }

  private void read_rhs_pattern_and_get_matching_productions(
      String pattern, Collection<Production> productions, List<Production> current_pf_list)
      throws ParserException {
    final Action alist =
        new OriginalParser().parseRightHandSide(parserContext, new StringReader(pattern));
    final Rete rete = Adaptables.adapt(agnt, Rete.class);

    for (Production prod : productions) {
      boolean match = true;

      ListHead<Binding> bindings = ListHead.newInstance();

      final ConditionsAndNots cans =
          rete.p_node_to_conditions_and_nots(prod.getReteNode(), null, null, true);
      for (Action a = alist; a != null; a = a.next) {
        boolean match_this_a = false;
        ListItem<Binding> current_binding_point = bindings.first;

        for (Action pa = cans.actions; pa != null; pa = pa.next) {
          if (actions_are_equal_with_bindings(a, pa, bindings)) {
            match_this_a = true;
            break;
          } else {
            // Remove new, incorrect bindings.
            reset_old_binding_point(bindings, current_binding_point);
            bindings.first = current_binding_point;
          }
        }
        if (!match_this_a) {
          match = false;
          break;
        }
      }

      if (match) {
        current_pf_list.add(prod);
        /* TODO
        if (show_bindings)
        {
            print_with_symbols(agnt, "%y, with bindings:\n",prod->name);
            print_binding_list(agnt,bindings);
        }
        else
        {
            print_with_symbols(agnt,"%y\n",prod->name);
        }
        */
      }
    }
  }
}
