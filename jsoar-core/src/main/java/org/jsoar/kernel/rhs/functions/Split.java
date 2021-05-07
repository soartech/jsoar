/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.kernel.rhs.functions;

import com.google.common.collect.Iterators;
import java.util.List;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.symbols.Symbol;

/**
 * String split RHS function implementation.
 *
 * <p>{@code (split <string> <regex>) }
 *
 * <p>For example, {@code <s> ^result (split |string to split| | |))} will result in:
 *
 * <pre>{@code
 * ^result
 *   ^value string
 *   ^next
 *     ^value to
 *     ^next
 *       ^value split
 * }</pre>
 *
 * <p>The behavior of the function is equivalent to {@link java.lang.String#split(String, int)},
 * i.e. {@code "string to split".split(" ", -1);}
 *
 * @author ray
 */
public class Split extends AbstractRhsFunctionHandler {
  public Split() {
    super("split", 2, 2);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
   */
  @Override
  public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
      throws RhsFunctionException {
    RhsFunctions.checkArgumentCount(this, arguments);

    final String target = arguments.get(0).toString();
    final String regex = arguments.get(1).toString();
    return Wmes.createLinkedList(context, Iterators.forArray(target.split(regex, -1)));
  }
}
