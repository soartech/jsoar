/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import lombok.NonNull;
import org.jsoar.util.Arguments;

/**
 * Base class for RHS functions. Defaults to non-standalone function.
 *
 * @author ray
 */
public abstract class AbstractRhsFunctionHandler implements RhsFunctionHandler {
  private final String name;
  private final int minArgs;
  private final int maxArgs;

  /**
   * Construct a non-standalone function with any number of arguments with the given name.
   *
   * @param name Name of the function
   */
  public AbstractRhsFunctionHandler(String name) {
    this(name, 0, Integer.MAX_VALUE);
  }

  /**
   * Construct a non-standalone function with the given number of arguments and the given name.
   *
   * @param name Name of the function
   * @param minArgs Minimum number of arguments
   * @param maxArgs Maximum number of arguments
   * @throws IllegalArgumentException If name is <code>null</code> or minArgs is greater than
   *     maxArgs
   */
  public AbstractRhsFunctionHandler(@NonNull String name, int minArgs, int maxArgs) {
    Arguments.check(minArgs <= maxArgs, "minArgs > maxArgs");
    Arguments.check(minArgs >= 0, "minArgs < 0");

    this.name = name;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#getMaxArguments()
   */
  @Override
  public int getMaxArguments() {
    return maxArgs;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#getMinArguments()
   */
  @Override
  public int getMinArguments() {
    return minArgs;
  }

  @Override
  public boolean mayBeStandalone() {
    return false;
  }

  @Override
  public boolean mayBeValue() {
    return true;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.RhsFunctionHandler#getName()
   */
  @Override
  public String getName() {
    return name;
  }
}
