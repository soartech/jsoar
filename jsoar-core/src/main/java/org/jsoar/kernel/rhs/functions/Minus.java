package org.jsoar.kernel.rhs.functions;

import java.util.List;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * Takes one or more int_constant or float_constant arguments.
 *
 * <ul>
 *   <li>If 0 arguments, returns NIL (error).
 *   <li>If 1 argument (x), returns -x.
 *   <li>If {@literal >=2} arguments (x, y1, ..., yk), returns x - y1 - ... - yk.
 * </ul>
 *
 * <p>rhsfun_math.cpp:125:minus_rhs_function_code
 */
public final class Minus extends AbstractRhsFunctionHandler {
  public Minus() {
    super("-", 1, Integer.MAX_VALUE);
  }

  @Override
  public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
      throws RhsFunctionException {
    RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);
    RhsFunctions.checkArgumentCount(this, arguments);

    final SymbolFactory syms = context.getSymbols();

    Symbol arg = arguments.get(0);
    if (arguments.size() == 1) {
      IntegerSymbol i = arg.asInteger();

      return i != null
          ? syms.createInteger(-i.getValue())
          : syms.createDouble(-arg.asDouble().getValue());
    }

    long i = 0;
    double f = 0;
    boolean float_found = false;
    IntegerSymbol ic = arg.asInteger();
    if (ic != null) {
      i = ic.getValue();
    } else {
      float_found = true;
      f = arg.asDouble().getValue();
    }
    for (int index = 1; index < arguments.size(); ++index) {
      arg = arguments.get(index);

      ic = arg.asInteger();
      if (ic != null) {
        if (float_found) {
          f -= ic.getValue();
        } else {
          i -= ic.getValue();
        }
      } else {
        if (float_found) {
          f -= arg.asDouble().getValue();
        } else {
          float_found = true;
          f = i - arg.asDouble().getValue();
        }
      }
    }

    return float_found ? syms.createDouble(f) : syms.createInteger(i);
  }
}
