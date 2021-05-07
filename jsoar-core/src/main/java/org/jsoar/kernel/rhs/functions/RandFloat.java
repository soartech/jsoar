/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import java.util.Random;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.Symbol;

/**
 * {@code (rand-float)} RHS function. Use this instead of random-float for csoar compatibility.
 *
 * <p>Has the following usage:
 *
 * <ul>
 *   <li>{@code (rand-float)} - No args, returns a random double in {@code [0.0, 1.0)}.
 *   <li>{@code (rand-float N)} - Returns a random double in {@code [0.0, N)} if N is positive and a
 *       random double in {@code (N, 0]} if N is negative.
 * </ul>
 *
 * <p>Csoar compatibility notes:
 *
 * <ul>
 *   <li>Csoar's results are in a fully inclusive range: {@code [0.0, N]}
 *   <li>Csoar doesn't properly support negative arguments like jsoar does (it defaults to ignoring
 *       the argument).
 * </ul>
 *
 * @author ray
 * @see Random#nextDouble()
 */
public class RandFloat extends AbstractRhsFunctionHandler {
  private final Random random;

  /**
   * Construct a new random int RHS function and use the given random number generator.
   *
   * @param random the random number generator to use
   */
  public RandFloat(Random random) {
    super("rand-float", 0, 1);

    this.random = random;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
   */
  @Override
  public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
      throws RhsFunctionException {
    RhsFunctions.checkArgumentCount(this, arguments);

    if (arguments.size() == 0) {
      return context.getSymbols().createDouble(random.nextDouble());
    } else {
      double max;

      // argument could be an IntegerSymbol or DoubleSymbol, so check for both
      final DoubleSymbol maxSymD = arguments.get(0).asDouble();
      if (maxSymD != null) {
        max = maxSymD.getValue();
      } else {
        final IntegerSymbol maxSymI = arguments.get(0).asInteger();
        if (maxSymI != null) {
          max = maxSymI.getValue();
        } else {
          throw new RhsFunctionException(
              "rand-float: Expected double or integer for first argument, got " + arguments.get(0));
        }
      }

      return context.getSymbols().createDouble(random.nextDouble() * max);
    }
  }
}
