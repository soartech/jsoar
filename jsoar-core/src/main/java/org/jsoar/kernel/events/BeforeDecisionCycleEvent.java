/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;

/**
 * callback.h:53:AFTER_DECISION_CYCLE_CALLBACK
 *
 * @author ray
 */
public class BeforeDecisionCycleEvent extends AbstractAgentEvent {
  private final Phase phase;

  /**
   * Construct a new event
   *
   * @param agent the agent
   * @param phase the phase
   */
  public BeforeDecisionCycleEvent(Agent agent, Phase phase) {
    super(agent);
    this.phase = phase;
  }

  /** @return the current phase */
  public Phase getPhase() {
    return phase;
  }
}
