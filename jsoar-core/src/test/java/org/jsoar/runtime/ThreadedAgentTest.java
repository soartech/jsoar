/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 20, 2008
 */
package org.jsoar.runtime;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author ray
 */
public class ThreadedAgentTest
{
    private final List<SoarEventListener> listeners = new ArrayList<SoarEventListener>();
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        for(SoarEventListener listener : listeners)
        {
            ThreadedAgent.getEventManager().removeListener(null, listener);
        }
        
        for(ThreadedAgent agent : ThreadedAgent.getAll())
        {
            agent.dispose();
        }
    }
    
    @Test
    public void testMultipleCallsToAttachReturnSameInstance() throws Exception
    {
        final Agent agent = new Agent();
        assertNull(ThreadedAgent.find(agent));
        final ThreadedAgent proxy1 = ThreadedAgent.attach(agent);
        final ThreadedAgent proxy2 = ThreadedAgent.attach(agent);
        assertSame(proxy1, proxy2);
        assertSame(proxy1, ThreadedAgent.find(agent));
    }

    @Test(timeout=5000)
    public void testShutdownDoesntHangIfAgentIsRunningForever() throws Exception
    {
        ThreadedAgent proxy = ThreadedAgent.attach(new Agent());
        proxy.getProperties().set(SoarProperties.WAITSNC, true);
        proxy.getTrace().setWatchLevel(0);
        proxy.initialize();
        proxy.runForever();
        Thread.sleep(500);
        proxy.detach();
    }
    
    @Test
    public void testAttachedEventIsFired() throws Exception
    {
        final AtomicReference<ThreadedAgent>  gotIt = new AtomicReference<ThreadedAgent>();
        final SoarEventListener listener = new SoarEventListener()
        {
            @Override
            public void onEvent(SoarEvent event)
            {
                gotIt.set(((ThreadedAgentAttachedEvent) event).getAgent());
            }
        };
        listeners.add(listener);
        ThreadedAgent.getEventManager().addListener(ThreadedAgentAttachedEvent.class, listener);
        final ThreadedAgent agent = ThreadedAgent.create();
        assertSame(agent, gotIt.get());
    }
    
    @Test
    public void testDetachedEventIsFired() throws Exception
    {
        final AtomicReference<ThreadedAgent>  gotIt = new AtomicReference<ThreadedAgent>();
        final SoarEventListener listener = new SoarEventListener()
        {
            @Override
            public void onEvent(SoarEvent event)
            {
                gotIt.set(((ThreadedAgentDetachedEvent) event).getAgent());
            }
        };
        listeners.add(listener);
        ThreadedAgent.getEventManager().addListener(ThreadedAgentDetachedEvent.class, listener);
        final ThreadedAgent agent = ThreadedAgent.create();
        assertNull(gotIt.get());
        agent.dispose();
        assertSame(agent, gotIt.get());
    }
}
