/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 20, 2008
 */
package org.jsoar.runtime;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.events.UncaughtExceptionEvent;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    
    @Test
    public void testAgentThreadCatchesUncaughtExceptions() throws Exception
    {
        final ThreadedAgent agent = ThreadedAgent.create();
        
        agent.execute(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                throw new IllegalStateException("Test exception thrown by testAgentThreadCatchesUnhandledExceptions");
            }
        }, null);
        
        // If the thread survived, then we should be able to successfully make this call...
        final String result = agent.executeAndWait(new Callable<String>() {

            @Override
            public String call() throws Exception
            {
                return "success";
            }}, 10000, TimeUnit.MILLISECONDS);
        
        assertEquals("success", result);
    }
    
    @Test(timeout=5000)
    public void testAgentFiresUncaughtExceptionEventWhenAnExceptionIsUncaught() throws Exception
    {
        final ThreadedAgent agent = ThreadedAgent.create();
        
        final AtomicBoolean called = new AtomicBoolean();
        final Object signal = new Object();
        agent.getEvents().addListener(UncaughtExceptionEvent.class, new SoarEventListener()
        {
            @Override
            public void onEvent(SoarEvent event)
            {
                synchronized(signal)
                {
                    called.set(true);
                    signal.notifyAll();
                }
            }
        });
        
        agent.execute(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                throw new IllegalStateException("Test exception thrown by testAgentThreadCatchesUnhandledExceptions");
            }
        }, null);
        
        synchronized(signal)
        {
            while(!called.get())
            {
                signal.wait();
            }
        }
    }
    
    /*
     * This test makes sure that we can create, start, and stop multiple
     * threaded agents at a time. It is also fishing for exception causing
     * concurrency problems.
     */
    @Test(timeout = 15000)
    public void testMultipleAgents() throws Exception
    {
        final int numAgents = 100;
        List<ThreadedAgent> agents = new ArrayList<ThreadedAgent>();
        Random rand = new Random(8389);

        // Load the rules
        String sourceName = getClass().getSimpleName() + "_testMultipleAgents.soar";
        URL sourceUrl = getClass().getResource(sourceName);
        assertNotNull("Could not find test file " + sourceName, sourceUrl);

        // Create the agents and source their rules
        for (int i = 0; i < numAgents; i++)
        {
            ThreadedAgent ta = ThreadedAgent.create();
            ta.getInterpreter().source(sourceUrl);
            agents.add(ta);
        }

        // Start the threads in a random order
        Collections.shuffle(agents, rand);
        for (ThreadedAgent ta : agents)
        {
            ta.runForever();
        }

        // Give the agents a chance to start
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException e){}

        // Make sure the agents are running
        for (ThreadedAgent ta : agents)
        {
            assertTrue("A ThreadedAgent failed to start.", ta.isRunning());
        }

        // Let the threads run for a bit longer
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException e){}

        // Stop the threads in a random order
        Collections.shuffle(agents, rand);
        // Stop the threads
        for (ThreadedAgent ta : agents)
        {
            ta.stop();
        }

        // Give the threads a chance to stop
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException e){}

        // If the agents are unhappy or unresponsive, we will timeout in this
        // loop
        while (!agents.isEmpty())
        {
            Iterator<ThreadedAgent> iter = agents.iterator();
            while (iter.hasNext())
            {
                ThreadedAgent ta = iter.next();
                // If the agent successfully stopped, remove it from the list
                if (!ta.isRunning())
                {
                    ta.dispose();
                    iter.remove();
                }
            }
        }
        // If we got here, there were not any critical issues
    }
}
