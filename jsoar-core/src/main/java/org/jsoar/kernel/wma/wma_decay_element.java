/*
 * Copyright (c) 2013 Bob Marinier <marinier@gmail.com>
 *
 * Created on Feb 8, 2013
 */
package org.jsoar.kernel.wma;

import org.jsoar.kernel.memory.Wme;

/** attached to o-supported WMEs to keep track of its activation. */
public class wma_decay_element {
  // the wme that this element goes with
  Wme this_wme;

  // when a WME is removed from working memory, the data
  // structure is not necessarily deallocated right away
  // because its reference count has not fallen to zero.
  // This flag indicates that the WME is in this "limbo" state.
  boolean just_removed;

  // notes the awkward period between first activation
  // and dealing with history changes
  boolean just_created;

  // how many times this wme has been referenced so far
  // this cycle
  long num_references;

  // when and how often this wme has been referenced in recent
  // history.
  wma_history touches = new wma_history();

  // if forgetting is enabled, this tells us when we think
  // we need to forget this wme
  long forget_cycle;
}
