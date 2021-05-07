/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 9, 2010
 */
package org.jsoar.soar2soar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jsoar.kernel.AgentRunController;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.runtime.ThreadedAgent;

/** @author ray */
public class Soar2SoarRunController implements AgentRunController {
  private final ThreadedAgent env;
  private List<ThreadedAgent> clients;

  public Soar2SoarRunController(ThreadedAgent env, Collection<ThreadedAgent> clients) {
    this.env = env;
    this.clients = new ArrayList<ThreadedAgent>(clients);
  }

  public void stop() {
    this.env.stop();
    for (ThreadedAgent client : clients) {
      client.stop();
    }
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.AgentRunController#getStopPhase()
   */
  @Override
  public Phase getStopPhase() {
    return env.getStopPhase();
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.AgentRunController#runFor(long, org.jsoar.kernel.RunType)
   */
  @Override
  public void runFor(long n, RunType runType) {
    env.runFor(n, runType);
    for (ThreadedAgent client : clients) {
      client.runFor(n, runType);
    }
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.AgentRunController#setStopPhase(org.jsoar.kernel.Phase)
   */
  @Override
  public void setStopPhase(Phase phase) {
    env.setStopPhase(phase);
    for (ThreadedAgent client : clients) {
      client.setStopPhase(phase);
    }
  }
}
