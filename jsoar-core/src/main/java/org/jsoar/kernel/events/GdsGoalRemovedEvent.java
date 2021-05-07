/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 31, 2010
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.Goal;
import org.jsoar.kernel.memory.Wme;

/**
 * Event fired when a goal is removed because of an invalid GDS. This event is fired <b>after</b>
 * the goal has been removed.
 *
 * @author ray
 * @see Decider#gds_invalid_so_remove_goal(org.jsoar.kernel.memory.WmeImpl, String)
 */
public class GdsGoalRemovedEvent extends AbstractAgentEvent {
  private final Goal goal;
  private final Wme cause;

  /**
   * @param agent
   * @param goal the goal that was removed
   * @param cause the wme in the goal's GDS that caused the goal to be removed
   */
  public GdsGoalRemovedEvent(Agent agent, Goal goal, Wme cause) {
    super(agent);

    this.goal = goal;
    this.cause = cause;
  }

  /** @return the goal that was removed */
  public Goal getGoal() {
    return goal;
  }

  /** @return the wme in the goal's GDS that caused the goal to be removed */
  public Wme getCause() {
    return cause;
  }
}
