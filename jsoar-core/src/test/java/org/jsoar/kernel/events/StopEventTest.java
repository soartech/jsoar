/*
 * Copyright (c) 2012 Soar Technology, Inc
 *
 * Created on September 7, 2012
 */
package org.jsoar.kernel.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import org.jsoar.kernel.RunType;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author jon.voigt */
public class StopEventTest {
  private ThreadedAgent agent;

  @Before
  public void setUp() throws Exception {
    agent = ThreadedAgent.create(getClass().getName());
  }

  @After
  public void tearDown() throws Exception {
    if (agent != null) {
      agent.dispose();
    }
  }

  @Test(timeout = 2000)
  public void testEventFires() throws Exception {
    // The event to start fires at some point after the call to runFor
    // and therefore requires synchronization for the assert.
    final BlockingQueue<Boolean> q = new SynchronousQueue<Boolean>();

    agent
        .getEvents()
        .addListener(
            StopEvent.class,
            new SoarEventListener() {
              @Override
              public void onEvent(SoarEvent event) {
                assertEquals(event.getClass(), StopEvent.class);
                try {
                  // This blocks until q.take below
                  q.put(Boolean.TRUE);
                } catch (InterruptedException ignored) {
                }
              }
            });

    agent.runFor(1, RunType.ELABORATIONS);

    // This blocks until q.put above
    assertTrue(q.take());
  }
}
