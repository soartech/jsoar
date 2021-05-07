/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 28, 2010
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.symbols.Symbol;

/**
 * RHS function that creates a linked list from a list of symbols.
 *
 * <p>For example:
 *
 * <pre>(list 99 bottles of beer)</pre>
 *
 * will create the following structure:
 *
 * <pre>{@code
 * ^value 99
 * ^next
 *    ^value bottles
 *    ^next
 *       ^value of
 *       ^next
 *          ^value beer
 * }</pre>
 *
 * @author ray
 */
public class ListRhsFunction extends AbstractRhsFunctionHandler {
  public ListRhsFunction() {
    super("list");
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
   */
  @Override
  public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
      throws RhsFunctionException {
    return Wmes.createLinkedList(context, arguments.iterator());
  }
}
