/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.Formatter;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/** @author ray */
public class RelationalTest extends ComplexTest {
  public static final int NOT_EQUAL_TEST = 1; /* various relational tests */
  public static final int LESS_TEST = 2;
  public static final int GREATER_TEST = 3;
  public static final int LESS_OR_EQUAL_TEST = 4;
  public static final int GREATER_OR_EQUAL_TEST = 5;
  public static final int SAME_TYPE_TEST = 6;

  // TODO Make these final so the object can be immutable. Eliminate copies, etc
  public int type;
  public SymbolImpl referent;

  /**
   * Reverse the direction of a relational test
   *
   * <p>reorder.cpp:320
   *
   * @param type the type of relational test
   * @return the reverse of the given test
   */
  public static int reverse_direction_of_relational_test(int type) {
    switch (type) {
      case RelationalTest.NOT_EQUAL_TEST:
        return RelationalTest.NOT_EQUAL_TEST;
      case RelationalTest.LESS_TEST:
        return RelationalTest.GREATER_TEST;
      case RelationalTest.GREATER_TEST:
        return RelationalTest.LESS_TEST;
      case RelationalTest.LESS_OR_EQUAL_TEST:
        return RelationalTest.GREATER_OR_EQUAL_TEST;
      case RelationalTest.GREATER_OR_EQUAL_TEST:
        return RelationalTest.LESS_OR_EQUAL_TEST;
      case RelationalTest.SAME_TYPE_TEST:
        return RelationalTest.SAME_TYPE_TEST;
      default:
        throw new IllegalArgumentException("Unknown RelationalTest type " + type);
    }
  }

  /**
   * @param type
   * @param referent
   */
  public RelationalTest(int type, SymbolImpl referent) {
    this.type = type;
    this.referent = referent;
  }

  private RelationalTest(RelationalTest other) {
    this.type = other.type;
    this.referent = other.referent;
  }

  public RelationalTest asRelationalTest() {
    return this;
  }

  public RelationalTest withNewReferent(SymbolImpl referent) {
    if (referent == this.referent) {
      return this;
    }
    return new RelationalTest(type, referent);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Test#copy()
   */
  @Override
  public Test copy() {
    return new RelationalTest(this);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Test#addAllVariables(int, java.util.List)
   */
  @Override
  public void addAllVariables(Marker tc_number, ListHead<Variable> var_list) {
    Variable var = referent.asVariable();
    if (var != null) {
      var.markIfUnmarked(tc_number, var_list);
    }
  }

  /* (non-Javadoc)
   * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
   */
  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    String op = "???";
    switch (type) {
      case RelationalTest.NOT_EQUAL_TEST:
        op = "<>";
        break;
      case RelationalTest.LESS_TEST:
        op = "<";
        break;
      case RelationalTest.GREATER_TEST:
        op = ">";
        break;
      case RelationalTest.LESS_OR_EQUAL_TEST:
        op = "<=";
        break;
      case RelationalTest.GREATER_OR_EQUAL_TEST:
        op = ">=";
        break;
      case RelationalTest.SAME_TYPE_TEST:
        op = "<=>";
        break;
      default:
        throw new IllegalArgumentException("Unknown RelationalTest type " + type);
    }
    formatter.format("%s %s", op, referent);
  }
}
