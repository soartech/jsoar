/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 29, 2009
 */
package sml;

import static org.junit.Assert.*;

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
}
