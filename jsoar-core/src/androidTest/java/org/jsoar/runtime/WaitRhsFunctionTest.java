/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 19, 2009
 */
package org.jsoar.runtime;

import android.test.AndroidTestCase;

import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WaitRhsFunctionTest extends AndroidTestCase
{
    private ThreadedAgent agent;
    
    @Override
    public void setUp()
    {
        this.agent = ThreadedAgent.create(getContext());
    }
    
    @Override
    public void tearDown()
    {
        this.agent.detach();
    }

    public void testDoesNotWaitIfAsynchInputIsReadyAndInputPhaseHasntRunYet() throws Exception
    {
        // Skip first input phase
        agent.executeAndWait(new Callable<Void>() {

            @Override
            public Void call() throws Exception
            {
                agent.runFor(2, RunType.PHASES);
                return null;
            }}, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        
        // Now load a production that waits and mark input ready
        this.agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait)");
        this.agent.getInputOutput().asynchronousInputReady();
        
        // Now run some more. The agent shouldn't wait because input ready came before input phase
        
        agent.executeAndWait(new Callable<Void>() {

            @Override
            public Void call() throws Exception
            {
                agent.runFor(2, RunType.DECISIONS);
                return null;
            }}, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        
        // Test will timeout on failure
    }
    
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
    
    public void testNoWaitIfAgentHalts() throws Exception
    {
        // The (halt) should trump the (wait)
        this.agent.getProductions().loadProduction("test (state <s> ^superstate nil) --> (wait) (halt)");
        
        assertFalse(this.agent.getProperties().get(SoarProperties.WAIT_INFO).waiting);
        
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
