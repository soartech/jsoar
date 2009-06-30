/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 29, 2009
 */
package sml;

import static org.junit.Assert.*;

import org.jsoar.kernel.RunType;
import org.jsoar.util.ByRef;
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
        final ByRef<String> args = ByRef.create(null);
        final RhsFunctionInterface func = new RhsFunctionInterface() {

            @Override
            public String rhsFunctionHandler(int eventID, Object data,
                    String agentName, String functionName, String argument)
            {
                args.value = argument;
                return "";
            }
        };
        
        final Kernel kernel = Kernel.CreateKernelInCurrentThread();
        final Agent agent = kernel.CreateAgent("testCreateAgent");
        
        kernel.AddRhsFunction("test", func, null);
        agent.ExecuteCommandLine("sp {testAddRhsFunction (state <s> ^superstate nil) --> (<s> ^foo (exec test a b c d))}");
        agent.agent.runFor(1, RunType.DECISIONS);
        assertEquals("abcd", args.value);
        
        // test additional agents get it...
        final Agent agent2 = kernel.CreateAgent("testCreateAgent2");
        
        args.value = null;
        agent2.ExecuteCommandLine("sp {testAddRhsFunction2 (state <s> ^superstate nil) --> (<s> ^foo (exec test a b c d))}");
        agent2.agent.runFor(1, RunType.DECISIONS);
        assertEquals("abcd", args.value);
    }
}
