/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2008
 */
package org.jsoar.kernel.events;

import org.jsoar.kernel.Agent;

/**
 * callback.h:53:AFTER_ELABORATION_CALLBACK
 *
 * @author ray
 */
public class AfterElaborationEvent extends AbstractAgentEvent {
  /** @param agent the agent */
  public AfterElaborationEvent(Agent agent) {
    super(agent);
  }
}
