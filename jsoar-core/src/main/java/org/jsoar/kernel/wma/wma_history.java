/*
 * Copyright (c) 2013 Bob Marinier <marinier@gmail.com>
 *
 * Created on Feb 8, 2013
 */

package org.jsoar.kernel.wma;

public class wma_history {
  /** This is the size of the reference history. wma.h:29:WMA_DECAY_HISTORY */
  public static final int WMA_DECAY_HISTORY = 10;

  wma_cycle_reference access_history[] = new wma_cycle_reference[WMA_DECAY_HISTORY];
  int next_p;
  int history_ct;

  long history_references;
  long total_references;
  long first_reference;

  wma_history() {
    for (int i = 0; i < WMA_DECAY_HISTORY; ++i) {
      this.access_history[i] = new wma_cycle_reference();
    }
  }
}
