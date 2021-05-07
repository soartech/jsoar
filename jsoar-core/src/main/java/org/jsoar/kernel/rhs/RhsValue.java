/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.Formattable;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/**
 * rhs_value_to_string is handled by Formattable implementation
 *
 * <p>TODO make this an interface
 *
 * @author ray
 */
public interface RhsValue extends Formattable {
  public RhsSymbolValue asSymbolValue();

  public RhsFunctionCall asFunctionCall();

  public ReteLocation asReteLocation();

  public UnboundVariable asUnboundVariable();

  /**
   * Returns a copy of this rhs value. If a value is immutable it may return <code>this</code>.
   *
   * @return A copy of this rhs value
   */
  public RhsValue copy();

  public char getFirstLetter();

  /**
   * Finding all variables from rhs_value's, actions, and action lists
   *
   * <p>These routines collect all the variables in rhs_value's, etc. Their "var_list" arguments
   * should either be NIL or else should point to the header of the list of marked variables being
   * constructed.
   *
   * <p>Warning: These are part of the reorderer and handle only productions in non-reteloc, etc.
   * format. They don't handle reteloc's or RHS unbound variables.
   *
   * <p>production.cpp:1223:add_all_variables_in_rhs_value
   *
   * @param tc_number
   * @param var_list
   */
  public void addAllVariables(Marker tc_number, ListHead<Variable> var_list);
}
