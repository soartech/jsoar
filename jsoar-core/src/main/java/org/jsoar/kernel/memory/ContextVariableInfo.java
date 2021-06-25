/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.kernel.memory;

import java.util.List;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Return structure used by {@link Agent#getContextVariableInfo(String)}
 *
 * <p>utilities.cpp:132:get_context_var_info
 *
 * @author ray
 */
public class ContextVariableInfo {
  private final Identifier goal;
  private final Symbol attribute;
  private final Symbol value;

  /**
   * Note: client code should use {@link Agent#getContextVariableInfo(String)}
   *
   * <p>utilities.cpp:132:get_context_var_info
   *
   * @return information about the given context variable
   * @see Agent#getContextVariableInfo(String)
   */
  public static ContextVariableInfo get(
      PredefinedSymbols predefinedSyms,
      IdentifierImpl top_goal,
      IdentifierImpl bottom_goal,
      String variable) {
    Symbol attribute;
    var levelsUp = 0;

    var v = predefinedSyms.getSyms().find_variable(variable);
    if (v == predefinedSyms.s_context_variable) {
      levelsUp = 0;
      attribute = predefinedSyms.state_symbol;
    } else if (v == predefinedSyms.o_context_variable) {
      levelsUp = 0;
      attribute = predefinedSyms.operator_symbol;
    } else if (v == predefinedSyms.ss_context_variable) {
      levelsUp = 1;
      attribute = predefinedSyms.state_symbol;
    } else if (v == predefinedSyms.so_context_variable) {
      levelsUp = 1;
      attribute = predefinedSyms.operator_symbol;
    } else if (v == predefinedSyms.sss_context_variable) {
      levelsUp = 2;
      attribute = predefinedSyms.state_symbol;
    } else if (v == predefinedSyms.sso_context_variable) {
      levelsUp = 2;
      attribute = predefinedSyms.operator_symbol;
    } else if (v == predefinedSyms.ts_context_variable) {
      levelsUp = top_goal != null ? bottom_goal.getLevel() - top_goal.getLevel() : 0;
      attribute = predefinedSyms.state_symbol;
    } else if (v == predefinedSyms.to_context_variable) {
      levelsUp = top_goal != null ? bottom_goal.getLevel() - top_goal.getLevel() : 0;
      attribute = predefinedSyms.operator_symbol;
    } else {
      return new ContextVariableInfo(null, null, null);
    }

    IdentifierImpl g = bottom_goal;
    while (g != null && levelsUp != 0) {
      g = g.goalInfo.higher_goal;
      levelsUp--;
    }

    if (g == null) {
      return new ContextVariableInfo(g, attribute, null);
    }

    Symbol value = null;
    if (attribute == predefinedSyms.state_symbol) {
      value = g;
    } else {
      List<WmeImpl> wmes = g.goalInfo.operator_slot.getWmes();
      value = wmes.isEmpty() ? null : wmes.get(0).getValue();
    }
    return new ContextVariableInfo(g, attribute, value);
  }

  private ContextVariableInfo(Identifier goal, Symbol attribute, Symbol value) {
    this.goal = goal;
    this.attribute = attribute;
    this.value = value;
  }

  public Identifier getGoal() {
    return goal;
  }

  public Symbol getAttribute() {
    return attribute;
  }

  public Symbol getValue() {
    return value;
  }
}
