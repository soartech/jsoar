/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Concatenates arguments into a single string
 *
 * <p>sml_RhsFunction.cpp:97:sml::ConcatRhsFunction::Execute
 *
 * @author ray
 */
public class Concat extends AbstractRhsFunctionHandler {
  /**
   * Concatenate the given list of symbols into a string using default, unescaped string
   * representation of each symbol.
   *
   * @param arguments List of symbols
   * @return Concatenated symbols as a string
   */
  public static String concat(@NonNull List<Symbol> arguments) {
    return arguments.stream().map(Objects::toString).collect(Collectors.joining());
  }

  public Concat() {
    super("concat");
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
   */
  @Override
  public Symbol execute(@NonNull RhsFunctionContext context, List<Symbol> arguments)
      throws RhsFunctionException {
    return context.getSymbols().createString(concat(arguments));
  }
}
