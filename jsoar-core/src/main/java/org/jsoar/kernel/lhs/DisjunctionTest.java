/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.Formatter;
import java.util.List;
import org.jsoar.kernel.symbols.SymbolImpl;

/** @author ray */
public class DisjunctionTest extends ComplexTest {
  public final List<SymbolImpl> disjunction_list;

  /**
   * Construct a new disjunction test from an <b>unmodifiable</b> list of symbols.
   *
   * @param disjunction An <b>unmodifiable</b> list of symbols
   */
  public DisjunctionTest(List<SymbolImpl> disjunction) {
    this.disjunction_list = disjunction;
  }

  public DisjunctionTest asDisjunctionTest() {
    return this;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Test#copy()
   */
  @Override
  public Test copy() {
    return this;
  }
  /* (non-Javadoc)
   * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
   */
  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    formatter.format("<< ");
    for (SymbolImpl s : disjunction_list) {
      formatter.format("%s ", s);
    }
    formatter.format(">>");
  }
}
