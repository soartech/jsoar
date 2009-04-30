/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 19, 2009
 */
package org.jsoar.runtime;

import static org.junit.Assert.*;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WaitRhsFunctionTest
{
    private ThreadedAgent agent;
    
    @Before
    public void setUp()
    {
        this.agent = ThreadedAgent.attach(new Agent());
        this.agent.initialize();
    }
    
    @After
    public void tearDown()
    {
        this.agent.detach();
    }

    @Test(timeout=10000)
    public void testDoesNotWaitIfAsynchInputIsReadyAndInputPhaseHasntRunYet() throws Exception
    {
        this.agent.getAgent().getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait)");
        this.agent.getAgent().getInputOutput().asynchronousInputReady();
        final Object signal = new String("testDoesNotWaitIfAsynchInputIsReadyAndInputPhaseHasntRunYet");
        agent.execute(new Callable<Void>() {

            @Override
            public Void call() throws Exception
            {
                agent.runFor(2, RunType.DECISIONS);
                return null;
            }}, new CompletionHandler<Void>(){

                @Override
                public void finish(Void result)
                {
                    synchronized(signal)
                    {
                        signal.notifyAll();
                    }
                }});
        
        synchronized(signal)
        {
            signal.wait();
        }
    }
    
    @Test(timeout=10000)
    public void testWaitingPropertyIsSetAndWaitExitsWhenAsynchInputIsReady() throws Exception
    {
        this.agent.getAgent().getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait)");
        
        assertSame(WaitInfo.NOT_WAITING, this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO));
        
        agent.runFor(2, RunType.DECISIONS);
        
        // Wait for the wait to start
        WaitInfo waitInfo = this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO);
        while(!waitInfo.waiting)
        {
            Thread.sleep(50);
            waitInfo = this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO);
        }
        assertSame(agent.getAgent().getProductions().getProduction("test"), waitInfo.cause);
        assertEquals(Long.MAX_VALUE, waitInfo.timeout);
        
        // Knock it out of wait with asynch input
        agent.getAgent().getInputOutput().asynchronousInputReady();
        
        // Wait for wait to stop
        while(this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO).waiting)
        {
            Thread.sleep(50);
        }
    }
    
    @Test(timeout=10000)
    public void testWaitIsInterruptedIfAgentIsStopped() throws Exception
    {
        this.agent.getAgent().getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait)");
        
        assertFalse(this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO).waiting);
        
        agent.runForever();
        
        // Wait for the wait to start
        while(!this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO).waiting)
        {
            Thread.sleep(50);
        }
        
        // Ask the agent to stop
        agent.stop();
        
        // Wait for wait to stop
        while(this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO).waiting)
        {
            Thread.sleep(50);
        }
    }
    
    @Test(timeout=10000)
    public void testNoWaitIfAgentHalts() throws Exception
    {
        // The (halt) should trump the (wait)
        this.agent.getAgent().getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait) (halt)");
        
        assertFalse(this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO).waiting);
        
        final AtomicBoolean signalled = new AtomicBoolean(false);
        final Object signal = new String("testNoWaitIfAgentHalts");
        agent.execute(new Callable<Void>() {

            @Override
            public Void call() throws Exception
            {
                agent.runForever();
                return null;
            }}, new CompletionHandler<Void>(){

                @Override
                public void finish(Void result)
                {
                    synchronized(signal)
                    {
                        signal.notifyAll();
                        signalled.set(true);
                    }
                }});
        
        if(!signalled.get())
        {
            synchronized(signal)
            {
                signal.wait();
            }
        }
    }
    
    @Test //(timeout=10000)
    public void testShortestWaitInDecisionCycleIsUsed() throws Exception
    {
        this.agent.getAgent().getProductions().loadProduction("forever (state <s> ^superstate nil) --> (wait)");
        this.agent.getAgent().getProductions().loadProduction("middle (state <s> ^superstate nil) --> (wait 15000)");
        this.agent.getAgent().getProductions().loadProduction("short (state <s> ^superstate nil) --> (wait 5000)");
        this.agent.getAgent().getProductions().loadProduction("long (state <s> ^superstate nil) --> (wait 25000)");
        
        assertFalse(this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO).waiting);
        
        agent.runForever();
        
        // Wait for the wait to start
        WaitInfo waitInfo = this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO);
        while(!waitInfo.waiting)
        {
            Thread.sleep(50);
            waitInfo = this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO);
        }
        assertEquals(5000, waitInfo.timeout);
        assertSame(agent.getAgent().getProductions().getProduction("short"), waitInfo.cause);
        
        // Ask the agent to stop
        agent.stop();
        
        // Wait for wait to stop
        while(this.agent.getAgent().getProperties().get(SoarProperties.WAIT_INFO).waiting)
        {
            Thread.sleep(50);
        }
    }

}
