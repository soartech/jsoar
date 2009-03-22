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

/**
 * @author ray
 */
public class ThreadedAgentProxyTest
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

    @Test(timeout=5000)
    public void testShutdownDoesntHangIfAgentIsRunningForever() throws Exception
    {
        ThreadedAgentProxy proxy = new ThreadedAgentProxy(new Agent());
        proxy.getAgent().getProperties().set(SoarProperties.WAITSNC, true);
        proxy.getAgent().getTrace().setWatchLevel(0);
        proxy.initialize();
        proxy.runForever();
        Thread.sleep(500);
        proxy.shutdown();
    }
}
