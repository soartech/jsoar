/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * rhsfun_math.cpp:530:float_rhs_function_code
 *
 * @author ray
 */
public class FloatRhsFunction extends AbstractRhsFunctionHandler {
  public FloatRhsFunction() {
    super("float", 1, 1);
  }

  public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
      throws RhsFunctionException {
    RhsFunctions.checkArgumentCount(this, arguments);
    final Symbol arg = arguments.get(0);

    final DoubleSymbol doubleSym = arg.asDouble();
    if (doubleSym != null) {
      return doubleSym;
    }
    final IntegerSymbol intSym = arg.asInteger();
    if (intSym != null) {
      return context.getSymbols().createDouble(intSym.getValue());
    }
    if (arg.asIdentifier() != null) {
      throw new RhsFunctionException("Identifier passed to float RHS function: " + arg);
    }
    final String string = arg.toString();
    try {
      return context.getSymbols().createDouble(Double.parseDouble(string));
    } catch (NumberFormatException e) {
      throw new RhsFunctionException(arg + " is not a valid float.");
    }
  }
}
