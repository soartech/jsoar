/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 19, 2009
 */
package org.jsoar.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class WaitRhsFunctionTest
{
    private ThreadedAgent agent;
    
    @BeforeEach
    void setUp()
    {
        this.agent = ThreadedAgent.create();
    }
    
    @AfterEach
    void tearDown()
    {
        this.agent.detach();
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testDoesNotWaitIfAsynchInputIsReadyAndInputPhaseHasntRunYet() throws Exception
    {
        // Skip first input phase
        agent.executeAndWait(() ->
        {
            agent.runFor(2, RunType.PHASES);
            return null;
        }, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        
        // Now load a production that waits and mark input ready
        this.agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait)");
        this.agent.getInputOutput().asynchronousInputReady();
        
        // Now run some more. The agent shouldn't wait because input ready came before input phase
        
        agent.executeAndWait(() ->
        {
            agent.runFor(2, RunType.DECISIONS);
            return null;
        }, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        
        // Test will timeout on failure
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWaitingPropertyIsSetAndWaitExitsWhenAsynchInputIsReady() throws Exception
    {
        this.agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait)");
        
        assertSame(WaitInfo.NOT_WAITING, this.agent.getProperties().get(SoarProperties.WAIT_INFO));
        
        agent.runFor(2, RunType.DECISIONS);
        
        // Wait for the wait to start
        WaitInfo waitInfo = this.agent.getProperties().get(SoarProperties.WAIT_INFO);
        while(!waitInfo.waiting)
        {
            Thread.sleep(50);
            waitInfo = this.agent.getProperties().get(SoarProperties.WAIT_INFO);
        }
        assertSame(agent.getProductions().getProduction("test"), waitInfo.cause);
        assertEquals(Long.MAX_VALUE, waitInfo.timeout);
        
        // Knock it out of wait with asynch input
        agent.getInputOutput().asynchronousInputReady();
        
        // Wait for wait to stop
        while(this.agent.getProperties().get(SoarProperties.WAIT_INFO).waiting)
        {
            Thread.sleep(50);
        }
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWaitIsInterruptedIfAgentIsStopped() throws Exception
    {
        this.agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait)");
        
        assertFalse(this.agent.getProperties().get(SoarProperties.WAIT_INFO).waiting);
        
        agent.runForever();
        
        // Wait for the wait to start
        while(!this.agent.getProperties().get(SoarProperties.WAIT_INFO).waiting)
        {
            Thread.sleep(50);
        }
        
        // Ask the agent to stop
        agent.stop();
        
        // Wait for wait to stop
        while(this.agent.getProperties().get(SoarProperties.WAIT_INFO).waiting)
        {
            Thread.sleep(50);
        }
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testNoWaitIfAgentHalts() throws Exception
    {
        // The (halt) should trump the (wait)
        this.agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait) (halt)");
        
        assertFalse(this.agent.getProperties().get(SoarProperties.WAIT_INFO).waiting);
        
        final AtomicBoolean signalled = new AtomicBoolean(false);
        final Object signal = "testNoWaitIfAgentHalts";
        
        agent.execute(() ->
        {
            agent.runForever();
            return null;
        },
                result ->
                {
                    synchronized (signal)
                    {
                        signal.notifyAll();
                        signalled.set(true);
                    }
                });
        
        if(!signalled.get())
        {
            synchronized (signal)
            {
                signal.wait();
            }
        }
    }
    
    @Test // (timeout=10000)
    public void testShortestWaitInDecisionCycleIsUsed() throws Exception
    {
        this.agent.getProductions().loadProduction("forever (state <s> ^superstate nil) --> (wait)");
        this.agent.getProductions().loadProduction("middle (state <s> ^superstate nil) --> (wait 15000)");
        this.agent.getProductions().loadProduction("short (state <s> ^superstate nil) --> (wait 5000)");
        this.agent.getProductions().loadProduction("long (state <s> ^superstate nil) --> (wait 25000)");
        
        assertFalse(this.agent.getProperties().get(SoarProperties.WAIT_INFO).waiting);
        
        agent.runForever();
        
        // Wait for the wait to start
        WaitInfo waitInfo = this.agent.getProperties().get(SoarProperties.WAIT_INFO);
        while(!waitInfo.waiting)
        {
            Thread.sleep(50);
            waitInfo = this.agent.getProperties().get(SoarProperties.WAIT_INFO);
        }
        assertEquals(5000, waitInfo.timeout);
        assertSame(agent.getProductions().getProduction("short"), waitInfo.cause);
        
        // Ask the agent to stop
        agent.stop();
        
        // Wait for wait to stop
        while(this.agent.getProperties().get(SoarProperties.WAIT_INFO).waiting)
        {
            Thread.sleep(50);
        }
    }
    
}
