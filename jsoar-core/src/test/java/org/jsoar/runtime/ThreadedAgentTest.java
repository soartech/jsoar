/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 20, 2008
 */
package org.jsoar.runtime;


import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author ray
 */
public class ThreadedAgentTest
{

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
}
