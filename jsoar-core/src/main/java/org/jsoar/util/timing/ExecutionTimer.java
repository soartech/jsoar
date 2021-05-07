/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

/**
 * Interface for a timer in jsoar.
 *
 * @author ray
 */
public interface ExecutionTimer {
  /** Start the timer */
  void start();

  /** Pause the timer. This will update the current total time for the timer. */
  void pause();

  /** Make sure that the total time is up to date. */
  void update();

  /** Reset the total time back to 0 */
  void reset();

  /** @return The total microseconds monitored by this timer */
  long getTotalMicroseconds();

  /** @return The total seconds monitored by this timer */
  double getTotalSeconds();

  /** @return The name of this timer */
  String getName();

  /**
   * Set the name of this timer
   *
   * @param name The new name of the timer
   * @return this
   * @throws IllegalArgumentException if name is <code>null</code>
   */
  ExecutionTimer setName(String name);
}
