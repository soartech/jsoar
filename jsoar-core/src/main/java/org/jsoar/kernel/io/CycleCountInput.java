/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.io;

import lombok.NonNull;
import org.jsoar.kernel.events.BeforeInitSoarEvent;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.util.Arguments;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

/**
 * Adds an augmentation to the input-link with the current cycle count. The default name of the
 * attribute is "cycle-count".
 *
 * @author ray
 */
public class CycleCountInput {
  private static final int START = 1; // TODO: I think this should be 0. Why isn't it?
  private final InputOutput io;
  private final InputListener listener;
  private final InitSoarListener initListener;

  private int count = START; // TODO: this should be long, but there's no long symbols
  private InputWme wme;

  /**
   * Construct a new cycle count input object. This object will automatically register for input
   * events and update the input-link.
   *
   * @param io The I/O interface
   */
  public CycleCountInput(@NonNull InputOutput io) {
    this.io = io;
    this.listener = new InputListener();
    this.initListener = new InitSoarListener();
    this.io.getEvents().addListener(InputEvent.class, listener);
    this.io.getEvents().addListener(BeforeInitSoarEvent.class, initListener);
  }

  /**
   * Dispose this object, removing the cycle count from the input link and unregistering from the
   * event manager if necessary
   */
  public void dispose() {
    this.io.getEvents().removeListener(null, listener);
    this.io.getEvents().removeListener(null, initListener);

    // Schedule removal of wme at next input cycle.
    if (wme != null) {
      this.wme.remove();
      this.wme = null;
    }
  }

  /** Updates the input-link. This should only be called during the input cycle. */
  private void update() {
    if (wme == null) {
      wme = InputWmes.add(io, "cycle-count", count);
    } else {
      InputWmes.update(wme, count);
    }
    count++;
  }

  private class InputListener implements SoarEventListener {
    /* (non-Javadoc)
     * @see org.jsoar.kernel.events.SoarEventListener#onEvent(org.jsoar.kernel.events.SoarEvent)
     */
    @Override
    public void onEvent(SoarEvent event) {
      update();
    }
  }

  private class InitSoarListener implements SoarEventListener {
    /* (non-Javadoc)
     * @see org.jsoar.util.events.SoarEventListener#onEvent(org.jsoar.util.events.SoarEvent)
     */
    @Override
    public void onEvent(SoarEvent event) {
      wme = null;
      count = START;
    }
  }
}
