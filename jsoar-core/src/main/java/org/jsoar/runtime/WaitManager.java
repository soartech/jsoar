/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 21, 2009
 */
package org.jsoar.runtime;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NonNull;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.events.AfterDecisionCycleEvent;
import org.jsoar.kernel.events.AsynchronousInputReadyEvent;
import org.jsoar.kernel.events.PhaseEvents.AfterInput;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.properties.PropertyProvider;

/**
 * Manages "wait" state for an agent. See {@link WaitRhsFunction}. Note that a wait manager is
 * automatically created by ThreadedAgent so it should never be necessary to instantiate this
 * class.
 *
 * @author ray
 * @see WaitRhsFunction
 */
public class WaitManager {

  private ThreadedAgent agent;
  private boolean inputReady = false;
  private SoarEventListener inputReadyListener;
  private final AsynchronousInputReadyCommand inputReadyCommand =
      new AsynchronousInputReadyCommand();
  private SoarEventListener afterInputListener;
  private SoarEventListener afterDecisionCycleListener;
  private WaitInfo requestedWaitInfo = WaitInfo.NOT_WAITING;
  private final AtomicReference<WaitInfo> waitInfo =
      new AtomicReference<>(WaitInfo.NOT_WAITING);
  private final PropertyProvider<WaitInfo> waitInfoProp =
      new PropertyProvider<WaitInfo>() {

        @Override
        public WaitInfo get() {
          return waitInfo.get();
        }

        @Override
        public WaitInfo set(WaitInfo value) {
          throw new IllegalArgumentException("Can't set wait_info property");
        }
      };

  /**
   * Attach this wait manager to the given agent. An agent should have at most one wait manager
   * attached.
   *
   * @param agent the agent
   */
  public void attach(@NonNull ThreadedAgent agent) {
    if (this.agent != null) {
      throw new IllegalStateException("Already attached to agent");
    }
    this.agent = agent;

    // Listen for input ready events
    this.agent
        .getEvents()
        .addListener(
            AsynchronousInputReadyEvent.class,
            inputReadyListener =
                new SoarEventListener() {

                  @Override
                  public void onEvent(SoarEvent event) {
                    setNewInputAvailable();
                  }
                });

    // Listen for end of decision cycle event. This is where we actually do the wait
    // if one has been requested. Since the next phase will be the input phase,
    // we don't have to worry about additional waits blocking it and we'll conveniently
    // go straight to input when an asynch input ready event knocks us out of a wait.
    this.agent
        .getEvents()
        .addListener(
            AfterDecisionCycleEvent.class,
            afterDecisionCycleListener =
                new SoarEventListener() {

                  @Override
                  public void onEvent(SoarEvent event) {
                    doWait();
                  }
                });

    this.agent
        .getEvents()
        .addListener(
            AfterInput.class,
            afterInputListener =
                new SoarEventListener() {

                  @Override
                  public void onEvent(SoarEvent event) {
                    inputReady = false;
                  }
                });

    // Set up "waiting" property
    this.agent.getProperties().setProvider(SoarProperties.WAIT_INFO, waitInfoProp);
  }

  /**
   * Detach this wait manager from the agent
   */
  public void detach() {
    if (agent != null) {
      agent.getEvents().removeListener(null, inputReadyListener);
      agent.getEvents().removeListener(null, afterInputListener);
      agent.getEvents().removeListener(null, afterDecisionCycleListener);
      agent = null;
    }
  }

  /**
   * @return the agent this manager is attach to, or {@code null} if not attached.
   */
  public ThreadedAgent getAgent() {
    return agent;
  }

  /**
   * Returns the current wait state of the agent. This is equivalent to accessing the agent property
   * {@link SoarProperties#WAIT_INFO}.
   *
   * @return the current wait state of the agent
   */
  public WaitInfo getWaitState() {
    return waitInfoProp.get();
  }

  /**
   * Request a wait with the given wait info. This allows external code to invoke the same mechanism
   * as the "wait" RHS function
   *
   * @param newWaitInfo new wait info
   */
  public void requestWait(WaitInfo newWaitInfo) {
    if (!this.requestedWaitInfo.waiting || newWaitInfo.timeout < requestedWaitInfo.timeout) {
      requestedWaitInfo = newWaitInfo;
    }
  }

  /**
   * Wake the agent up if it is currently sleeping
   */
  public void requestResume() {
    if (this.agent != null) {
      this.agent.getInputOutput().asynchronousInputReady();
    }
  }

  private void doWait() {
    if (!requestedWaitInfo.waiting) // no wait requested
    {
      inputReady = false;
      return;
    }

    // Update the wait property
    waitInfo.set(requestedWaitInfo);

    final long start = System.currentTimeMillis();
    final BlockingQueue<Runnable> commands = agent.getCommandQueue();
    boolean done = isDoneWaiting();
    while (!done) {
      try {
        final long remaining = requestedWaitInfo.timeout - (System.currentTimeMillis() - start);
        if (remaining <= 0) {
          done = true;
        }

        final Runnable command = commands.poll(remaining, TimeUnit.MILLISECONDS);
        if (command != null) {
          command.run();

          done = isDoneWaiting();
        } else {
          done = true; // timeout
        }
      } catch (InterruptedException e) {
        done = true;
        Thread.currentThread().interrupt(); // Reset the interrupt status for higher levels!
        break;
      }
    }
    requestedWaitInfo = WaitInfo.NOT_WAITING; // clear the wait

    inputReady = false;
    waitInfo.set(WaitInfo.NOT_WAITING);
  }

  private synchronized void setNewInputAvailable() {
    // This will break out of the poll below
    if (agent.isAgentThread()) {
      inputReady = true;
    } else {
      agent.execute(inputReadyCommand, null);
    }
  }

  private synchronized boolean isDoneWaiting() {
    return agent.getAgent().getReasonForStop() != null
        || inputReady
        || Thread.currentThread().isInterrupted();
  }

  private class AsynchronousInputReadyCommand implements Callable<Void> {

    @Override
    public Void call() throws Exception {
      inputReady = true;
      return null;
    }
  }
}
