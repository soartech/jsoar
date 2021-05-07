/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import org.jsoar.kernel.symbols.Symbol;

/**
 * checks if the first argument is "eq" to the second argument if it is then it returns the third
 * argument else the fourth. It is useful in similar situations to the "?" notation in C. Contrary
 * to earlier belief, all 4 arguments are required.
 *
 * <p>rhsfun.cpp:441:ifeq_rhs_function_code
 *
 * @author ray
 */
public class IfEq extends AbstractRhsFunctionHandler {
  public IfEq() {
    super("ifeq", 4, 4);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
   */
  @Override
  public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments)
      throws RhsFunctionException {
    RhsFunctions.checkArgumentCount(this, arguments);

    return arguments.get(0) == arguments.get(1) ? arguments.get(2) : arguments.get(3);
  }
}
