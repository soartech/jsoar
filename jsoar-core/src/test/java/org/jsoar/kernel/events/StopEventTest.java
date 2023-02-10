/*
 * Copyright (c) 2012 Soar Technology, Inc
 *
 * Created on September 7, 2012
 */
package org.jsoar.kernel.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.jsoar.kernel.RunType;
import org.jsoar.runtime.ThreadedAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author jon.voigt
 */
public class StopEventTest
{
    private ThreadedAgent agent;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        agent = ThreadedAgent.create(getClass().getName());
    }
    
    @AfterEach
    public void tearDown() throws Exception
    {
        if(agent != null)
        {
            agent.dispose();
        }
    }
    
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    public void testEventFires() throws Exception
    {
        // The event to start fires at some point after the call to runFor
        // and therefore requires synchronization for the assert.
        final BlockingQueue<Boolean> q = new SynchronousQueue<Boolean>();
        
        agent.getEvents().addListener(StopEvent.class, event -> {
            assertEquals(event.getClass(), StopEvent.class);
            try
            {
                // This blocks until q.take below
                q.put(Boolean.TRUE);
            }
            catch(InterruptedException ignored)
            {
            }
        });
        
        agent.runFor(1, RunType.ELABORATIONS);
        
        // This blocks until q.put above
        assertTrue(q.take());
    }
}
