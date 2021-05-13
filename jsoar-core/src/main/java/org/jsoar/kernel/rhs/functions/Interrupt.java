/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 19, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;
import lombok.NonNull;
import org.jsoar.kernel.DecisionCycle;
import org.jsoar.kernel.symbols.Symbol;

/**
 * rhsfun.cpp:223:interrupt_rhs_function_code
 *
 * @author ray
 */
public class Interrupt extends AbstractRhsFunctionHandler {

  private final DecisionCycle decisionCycle;

  /**
   * @param decisionCycle the agent's decision cycle object
   */
  public Interrupt(@NonNull DecisionCycle decisionCycle) {
    super("interrupt", 0, 0);

    this.decisionCycle = decisionCycle;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
   */
  @Override
  public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments)
      throws RhsFunctionException {
    RhsFunctions.checkArgumentCount(this, arguments);
    decisionCycle.interrupt(rhsContext.getProductionBeingFired().getName());
    return null;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeStandalone()
   */
  @Override
  public boolean mayBeStandalone() {
    return true;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeValue()
   */
  @Override
  public boolean mayBeValue() {
    return false;
  }
}
