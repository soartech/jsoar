/*
 * Copyright (c) 2012 Soar Technology, Inc
 *
 * Created on September 7, 2012
 */
package org.jsoar.kernel.events;

import android.test.AndroidTestCase;

import org.jsoar.kernel.RunType;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * @author jon.voigt
 */
public class StartEventTest extends AndroidTestCase
{
    private ThreadedAgent agent;

    @Override
    public void setUp() throws Exception
    {
        agent = ThreadedAgent.create(getClass().getName(), getContext());
    }

    @Override
    public void tearDown() throws Exception
    {
        if (agent != null)
        {
            agent.dispose();
        }
    }

    public void testEventFires() throws Exception
    {
        // The event to start fires at some point after the call to runFor
        // and therefore requires synchronization for the assert.
        final BlockingQueue<Boolean> q = new SynchronousQueue<Boolean>();
        
        agent.getEvents().addListener(StartEvent.class, new SoarEventListener()
        {
            @Override
            public void onEvent(SoarEvent event)
            {
                assertEquals(event.getClass(), StartEvent.class);
                try
                {
                    // This blocks until q.take below
                    q.put(Boolean.TRUE);
                }
                catch (InterruptedException ignored)
                {
                }
            }
        });

        agent.runFor(1, RunType.ELABORATIONS);
        
        // This blocks until q.put above
        assertTrue(q.take());
    }
}
