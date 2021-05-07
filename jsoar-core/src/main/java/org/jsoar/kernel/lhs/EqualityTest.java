/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/** @author ray */
public abstract class EqualityTest extends Test {
  /* (non-Javadoc)
   * @see org.jsoar.kernel.Test#copy()
   */
  @Override
  public Test copy() {
    return this; // new EqualityTest(sym);
  }

  /**
   * gdatastructs.h:395:referent_of_equality_test
   *
   * @return the referent of this equality test
   */
  public abstract SymbolImpl getReferent();

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Test#asBlankTest()
   */
  @Override
  public EqualityTest asEqualityTest() {
    return this;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Test#addAllVariables(int, java.util.List)
   */
  @Override
  public void addAllVariables(Marker tc_number, ListHead<Variable> var_list) {
    final SymbolImpl sym = getReferent();
    Variable var = sym != null ? sym.asVariable() : null;
    if (var != null) {
      var.markIfUnmarked(tc_number, var_list);
    }
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Test#addBoundVariables(int, java.util.List)
   */
  @Override
  public void addBoundVariables(Marker tc_number, ListHead<Variable> var_list) {
    final SymbolImpl sym = getReferent();
    Variable var = sym != null ? sym.asVariable() : null;
    if (var != null) {
      var.markIfUnmarked(tc_number, var_list);
    }
  }
}
