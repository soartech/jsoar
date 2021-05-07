/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 27, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveNegationCondition;
import org.jsoar.kernel.lhs.NegativeCondition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.Tests;
import org.jsoar.kernel.lhs.ThreeFieldCondition;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;

/**
 * rete.cpp:2508
 *
 * <p>rete.cpp:2553:deallocate_node_varnames - not needed in Java
 *
 * @author ray
 */
class NodeVarNames {
  private static final boolean ENABLED =
      !Boolean.valueOf(System.getProperty("jsoar.discardVarNames", "false"));

  public static class ThreeFieldVarNames {
    final Object id_varnames;
    final Object attr_varnames;
    final Object value_varnames;

    private ThreeFieldVarNames(Object idVarnames, Object attrVarnames, Object valueVarnames) {
      id_varnames = idVarnames;
      attr_varnames = attrVarnames;
      value_varnames = valueVarnames;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "<" + id_varnames + ", " + attr_varnames + ", " + value_varnames + ">";
    }
  }

  final NodeVarNames parent;
  // union varname_data_union {
  final ThreeFieldVarNames fields;
  final NodeVarNames bottom_of_subconditions;
  // } data;

  private NodeVarNames(NodeVarNames parent, Object idVars, Object attrVars, Object valueVars) {
    this.parent = parent;
    this.fields = new ThreeFieldVarNames(idVars, attrVars, valueVars);
    this.bottom_of_subconditions = null;
  }

  static NodeVarNames newInstance(
      NodeVarNames parent, Object idVars, Object attrVars, Object valueVars) {
    return new NodeVarNames(parent, idVars, attrVars, valueVars);
  }

  private NodeVarNames(NodeVarNames parent, NodeVarNames bottom_of_subconditions) {
    this.parent = parent;
    this.fields = null;
    this.bottom_of_subconditions = bottom_of_subconditions;
  }

  static NodeVarNames createForNcc(NodeVarNames parent, NodeVarNames bottom_of_subconditions) {
    return new NodeVarNames(parent, bottom_of_subconditions);
  }

  /** rete.cpp:2611:make_nvn_for_posneg_cond */
  static NodeVarNames make_nvn_for_posneg_cond(ThreeFieldCondition cond, NodeVarNames parent_nvn) {
    if (!ENABLED) {
      return null;
    }

    final ListHead<Variable> vars_bound = ListHead.newInstance();

    /* --- fill in varnames for id test --- */
    final Object idVars = VarNames.add_unbound_varnames_in_test(cond.id_test, null);

    /* --- add sparse bindings for id, then get attr field varnames --- */
    Tests.bind_variables_in_test(cond.id_test, 0, 0, false, vars_bound);
    final Object attrVars = VarNames.add_unbound_varnames_in_test(cond.attr_test, null);

    /* --- add sparse bindings for attr, then get value field varnames --- */
    Tests.bind_variables_in_test(cond.attr_test, 0, 0, false, vars_bound);
    final Object valueVars = VarNames.add_unbound_varnames_in_test(cond.value_test, null);

    /* --- Pop the variable bindings for these conditions --- */
    Variable.pop_bindings_and_deallocate_list_of_variables(vars_bound);

    return newInstance(parent_nvn, idVars, attrVars, valueVars);
  }

  /** rete.cpp:2642:get_nvn_for_condition_list */
  static NodeVarNames get_nvn_for_condition_list(Condition cond_list, NodeVarNames parent_nvn) {
    if (!ENABLED) {
      return null;
    }

    NodeVarNames New = null;
    ListHead<Variable> vars = ListHead.newInstance();

    for (Condition cond = cond_list; cond != null; cond = cond.next) {
      PositiveCondition pc = cond.asPositiveCondition();
      if (pc != null) {
        New = make_nvn_for_posneg_cond(pc, parent_nvn);

        // Add sparse variable bindings for this condition
        Tests.bind_variables_in_test(pc.id_test, 0, 0, false, vars);
        Tests.bind_variables_in_test(pc.attr_test, 0, 0, false, vars);
        Tests.bind_variables_in_test(pc.value_test, 0, 0, false, vars);
      }
      NegativeCondition nc = cond.asNegativeCondition();
      if (nc != null) {
        New = make_nvn_for_posneg_cond(nc, parent_nvn);
      }
      ConjunctiveNegationCondition ncc = cond.asConjunctiveNegationCondition();
      if (ncc != null) {
        New = new NodeVarNames(parent_nvn, get_nvn_for_condition_list(ncc.top, parent_nvn));
      }

      parent_nvn = New;
    }

    // Pop the variable bindings for these conditions
    Variable.pop_bindings_and_deallocate_list_of_variables(vars);

    return parent_nvn;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return fields + "/" + bottom_of_subconditions;
  }
}
