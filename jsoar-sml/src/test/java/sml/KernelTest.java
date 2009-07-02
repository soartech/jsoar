/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 29, 2009
 */
package sml;

import static org.junit.Assert.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import sml.Kernel.RhsFunctionInterface;


/**
 * @author ray
 */
public class KernelTest
{
    @Test public void testCreateAgent()
    {
        final Kernel kernel = Kernel.CreateKernelInCurrentThread();
        final Agent agent = kernel.CreateAgent("testCreateAgent");
        assertNotNull(agent);
        assertEquals("testCreateAgent", agent.GetAgentName());
        assertSame(agent, kernel.GetAgent("testCreateAgent"));
        assertSame(agent, kernel.GetAgentByIndex(0));
        assertEquals(1, kernel.GetNumberAgents());
    }
    
    @Test public void testAddRhsFunction()
    {
        final AtomicReference<String> args = new AtomicReference<String>();
        final RhsFunctionInterface func = new RhsFunctionInterface() {

            @Override
            public String rhsFunctionHandler(int eventID, Object data,
                    String agentName, String functionName, String argument)
            {
                args.set(argument);
                return "";
            }
        };
        
        final Kernel kernel = Kernel.CreateKernelInCurrentThread();
        final Agent agent = kernel.CreateAgent("testCreateAgent");
        
        kernel.AddRhsFunction("test", func, null);
        agent.ExecuteCommandLine("sp {testAddRhsFunction (state <s> ^superstate nil) --> (<s> ^foo (exec test a b c d))}");
        agent.RunSelf(1);
        assertEquals("abcd", args.get());
        
        // test additional agents get it...
        final Agent agent2 = kernel.CreateAgent("testCreateAgent2");
        
        args.set(null);
        agent2.ExecuteCommandLine("sp {testAddRhsFunction2 (state <s> ^superstate nil) --> (<s> ^foo (exec test a b c d))}");
        agent2.RunSelf(1);
        assertEquals("abcd", args.get());
    }
    
    @Test public void testRunAllAgents()
    {
        final Kernel kernel = Kernel.CreateKernelInNewThread();
        final Agent a = kernel.CreateAgent("a");
        final Agent b = kernel.CreateAgent("b");
        
        kernel.ExecuteCommandLine("waitsnc --on", "a");
        kernel.ExecuteCommandLine("waitsnc --on", "b");
        kernel.ExecuteCommandLine("watch 0", "a");
        kernel.ExecuteCommandLine("watch 0", "b");
        
        kernel.RunAllAgents(500);
        
        assertEquals(501, a.GetDecisionCycleCounter());
        assertEquals(501, b.GetDecisionCycleCounter());
        
    }
    
    @Test(timeout=10000) public void testRunAllAgentsForever()
    {
        final Kernel kernel = Kernel.CreateKernelInNewThread();
        final String[] names = new String[] {"a", "b", "c", "d", "e" };
        
        for(String name : names)
        {
            kernel.CreateAgent(name);
            kernel.ExecuteCommandLine("waitsnc --on", name);
            kernel.ExecuteCommandLine("watch 0", name);
        }
        
        new Timer("testRunAllAgentForever timer").schedule(new TimerTask() {

            @Override
            public void run()
            {
                kernel.StopAllAgents();
            }}, 2000);
        kernel.RunAllAgentsForever();
        
        assertFalse(kernel.IsSoarRunning());
    }
    
    @Test public void testKernelUpdateEvent()
    {
        final Kernel kernel = Kernel.CreateKernelInNewThread();
        final AtomicInteger count = new AtomicInteger(0);
        kernel.RegisterForUpdateEvent(smlUpdateEventId.smlEVENT_AFTER_ALL_GENERATED_OUTPUT, new Kernel.UpdateEventInterface() {

            @Override
            public void updateEventHandler(int eventID, Object data,
                    Kernel kernel, int runFlags)
            {
                final AtomicInteger c = (AtomicInteger) data;
                c.addAndGet(1);
            }}, count);
        final String[] names = new String[] {"a", "b", "c", "d", "e" };
        
        for(String name : names)
        {
            kernel.CreateAgent(name);
            kernel.ExecuteCommandLine("waitsnc --on", name);
            kernel.ExecuteCommandLine("watch 0", name);
        }
        
        kernel.RunAllAgents(500);
        assertEquals(500, count.get());
        
        assertFalse(kernel.IsSoarRunning());
    }
}
