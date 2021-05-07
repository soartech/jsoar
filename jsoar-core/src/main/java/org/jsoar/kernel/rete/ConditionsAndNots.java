/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.rhs.Action;

/**
 * "struct" that receives the result of {@link Rete#p_node_to_conditions_and_nots(ReteNode, Token,
 * org.jsoar.kernel.memory.WmeImpl, boolean)}. Replacement for return by reference used in csoar.
 *
 * @author ray
 */
public class ConditionsAndNots {
  public Condition top;
  public Condition bottom;
  public NotStruct nots;
  public Action actions;
}
