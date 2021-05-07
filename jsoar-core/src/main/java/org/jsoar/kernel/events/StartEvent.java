/*
 * Copyright (c) 2012 Soar Technology, Inc
 *
 * Created on Sept 7, 2012
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;
import org.jsoar.runtime.ThreadedAgent;

/**
 * Event fired when an agent is started. Note that this event will never be fired by a raw {@link
 * Agent} since there is no concept of run control there other than running by phases. This event
 * will typically be fired by a manager object such as {@link ThreadedAgent}
 *
 * @author jon.voigt
 */
public class StartEvent extends AbstractAgentEvent {

  /**
   * Construct a new start event for the given agent
   *
   * @param agent the agent
   */
  public StartEvent(Agent agent) {
    super(agent);
  }
}
