/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel;

import java.util.Map;
import org.jsoar.kernel.rhs.functions.Debug;
import org.jsoar.runtime.ThreadedAgent;

/**
 * Interface for an object that knows how to instantiate and attach a debugger to a JSoar agent.
 *
 * @author ray
 * @see Agent#setDebuggerProvider(DebuggerProvider)
 * @see Agent#getDebuggerProvider()
 * @see Debug
 */
public interface DebuggerProvider {
  /**
   * Name of the property that holds the action ({@link CloseAction}) to take when the debugger
   * "closes".
   */
  public static final String CLOSE_ACTION = "closeAction";

  public static enum CloseAction {
    /** The debugger should clean up and exit the Java VM. */
    EXIT,
    /**
     * The debugger should detach from the agent and dispose of itself. The agent is left running.
     */
    DETACH,
    /** The debugger should dispose the agent and itself, but not exit the Java VM. */
    DISPOSE
  }

  /**
   * Add properties to this debugger provider. Properties control various aspect of the behavior of
   * the debugger that is created by {@link #openDebugger(Agent)}. For instance, the behavior of the
   * debugger when it is "closed" by the user.
   *
   * <p>The properties are merged with the current properties of the provider. This method is
   * thread-safe.
   *
   * @param props new properties to be merged in with existing ones.
   */
  void setProperties(Map<String, Object> props);

  /** @return a copy of the current properties of this provider */
  Map<String, Object> getProperties();

  /**
   * Opens a debugger and attaches it to the given agent.
   *
   * @param agent the agent
   * @throws SoarException
   */
  void openDebugger(Agent agent) throws SoarException;

  /**
   * Opens a debugger and attaches it to the given agent. Waits until the debugger is fully
   * initialized before proceeding.
   *
   * <p>Note that care must be taken to avoid deadlocks if the debugger is initialized on a
   * different thread such as the Swing event dispatch thread. For example, if this method was
   * called from the the agent thread of {@link ThreadedAgent} there would almost certainly be
   * deadlock.
   *
   * @param agent the agent
   * @throws SoarException if an error occurs during initialization
   * @throws InterruptedException if the thread is interrupted while waiting for the debugger to
   *     initialize
   */
  void openDebuggerAndWait(Agent agent) throws SoarException, InterruptedException;
}
