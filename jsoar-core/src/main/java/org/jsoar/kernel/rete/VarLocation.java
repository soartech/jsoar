/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.symbols.Variable;

/**
 * Variable location. Objects of this class are immutable
 *
 * <p>rete.cpp:255:var_location
 *
 * @author ray
 */
public class VarLocation {
  final int levels_up; // 0=current node's alphamem, 1=parent's, etc.
  final int field_num; // 0=id, 1=attr, 2=value

  public static final VarLocation DEFAULT = new VarLocation(0, 0);

  /**
   * This routine finds the most recent place a variable was bound. It does this simply by looking
   * at the top of the binding stack for that variable. If there is any binding, its location is
   * stored in the parameter *result, and the function returns TRUE. If no binding is found, the
   * function returns FALSE.
   *
   * <p>rete.cpp:2373:find_var_location
   *
   * @param var
   * @param current_depth
   * @return a new var location for the given variable and depth, {@code null} if the variable is
   *     not bound
   */
  static VarLocation find(Variable var, /* rete_node_level */ int current_depth) {
    if (!var.var_is_bound()) {
      return null;
    }
    int dummy = var.rete_binding_locations.peek();

    return new VarLocation(
        current_depth - Variable.dummy_to_varloc_depth(dummy),
        Variable.dummy_to_varloc_field_num(dummy));
  }

  /**
   * @param levels_up
   * @param field_num
   */
  protected VarLocation(int levels_up, int field_num) {
    this.levels_up = levels_up;
    this.field_num = field_num;
  }

  /**
   * rete.cpp:263:var_locations_equal
   *
   * @param v1
   * @param v2
   * @return true if the two var locations are equal
   */
  public static boolean var_locations_equal(VarLocation v1, VarLocation v2) {
    return (((v1).levels_up == (v2).levels_up) && ((v1).field_num == (v2).field_num));
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return levels_up + ":" + field_num;
  }
}
