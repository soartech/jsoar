/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.events;

import java.util.List;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.util.events.SoarEvent;

/**
 * callback.h:96:INPUT_WME_GARBAGE_COLLECTED_CALLBACK
 *
 * @author ray
 */
public class InputWmeGarbageCollectedEvent implements SoarEvent {
  private final List<WmeImpl> removedWmes;

  public InputWmeGarbageCollectedEvent(List<WmeImpl> WMEs) {
    this.removedWmes = WMEs;
  }
}
