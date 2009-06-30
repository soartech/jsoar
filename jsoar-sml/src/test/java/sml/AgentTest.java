/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 29, 2009
 */
package sml;

import static org.junit.Assert.*;

import org.jsoar.kernel.RunType;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.util.ByRef;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.Test;

import sml.Kernel.RhsFunctionInterface;


/**
 * @author ray
 */
public class AgentTest
{

    @Test public void testExecuteCommandLine()
    {
        final Kernel kernel = Kernel.CreateKernelInCurrentThread();
        final Agent agent = kernel.CreateAgent("testExecuteCommandLine");
        agent.ExecuteCommandLine("sp {test (state <s> ^superstate nil) --> (write |hi|)}");
        assertTrue("Expected command to load production", agent.IsProductionLoaded("test"));
    }
    
    @Test public void testConstructInputWithSML()
    {
        final Kernel kernel = Kernel.CreateKernelInCurrentThread();
        final Agent agent = kernel.CreateAgent("testExecuteCommandLine");
        
        final ByRef<Boolean> matched = ByRef.create(false);
        final RhsFunctionInterface match = new RhsFunctionInterface() {

            @Override
            public String rhsFunctionHandler(int eventID, Object data,
                    String agentName, String functionName, String argument)
            {
                matched.value = true;
                return null;
            }};
        
        kernel.AddRhsFunction("match", match, null);
        
        agent.agent.getEvents().addListener(InputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                Identifier root = agent.CreateIdWME(agent.GetInputLink(), "root");
                agent.CreateStringWME(root, "name", "felix");
                agent.CreateIntWME(root, "age", 45);
                agent.CreateFloatWME(root, "gpa", 2.6);
            }});
        
        agent.ExecuteCommandLine("sp {test " +
        		"(state <s> ^superstate nil ^io.input-link.root <r>)" +
        		"(<r> ^name felix ^age 45 ^gpa 2.6)" +
        		"-->" +
        		"(<s> ^foo (exec match))}");
        agent.agent.runFor(1, RunType.DECISIONS);
        assertTrue(matched.value);
    }
}
