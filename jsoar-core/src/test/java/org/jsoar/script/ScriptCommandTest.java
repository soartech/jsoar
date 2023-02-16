/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 21, 2010
 */
package org.jsoar.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.util.commands.DefaultSoarCommandContext;
import org.jsoar.util.events.SoarEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScriptCommandTest
{
    private Agent agent;
    private ScriptCommand command;
    
    public static class TestEvent implements SoarEvent
    {
    }
    
    @BeforeEach
    void setUp() throws Exception
    {
        agent = new Agent();
        command = new ScriptCommand(agent);
    }
    
    @AfterEach
    void tearDown() throws Exception
    {
    }
    
    @Test
    void testThrowsAnExceptionForUnknownScriptEngines()
    {
        assertThrows(SoarException.class, () -> command.execute(DefaultSoarCommandContext.empty(), new String[] { "script", "unknown-script-engine" }));
    }
    
    @Test
    void testCanEvalScriptCode() throws Exception
    {
        final String result = command.execute(DefaultSoarCommandContext.empty(), new String[] { "script", "javascript", "'hi there'" });
        assertEquals("hi there", result);
    }
    
    @Test
    void testInstallsRhsFunctionHandler() throws Exception
    {
        // Initialize javascript engine
        command.execute(DefaultSoarCommandContext.empty(), new String[] { "script", "javascript" });
        
        final RhsFunctionManager rhsFuncs = agent.getRhsFunctions();
        assertNotNull(rhsFuncs);
        
        final RhsFunctionHandler handler = rhsFuncs.getHandler("javascript");
        assertNotNull(handler);
    }
    
    @Test
    void testCanCleanupRegisteredListenersWhenReset() throws Exception
    {
        final Agent agent = new Agent("testCanCleanupRegisteredListenersWhenReset");
        try
        {
            // Initialize javascript engine and register a handler for our test
            // event. It throws an exception.
            agent.getInterpreter().eval("script javascript { soar.onEvent('org.jsoar.script.ScriptCommandTest$TestEvent', function() { throw 'Failed'; }); }");
            // reset javascript engine
            agent.getInterpreter().eval("script --reset javascript");
            
            // Now if everything went right, firing the test event should have
            // no effect
            agent.getEvents().fireEvent(new TestEvent());
        }
        finally
        {
            agent.dispose();
        }
    }
    
    @Test
    void testCanCleanupRegistersRhsFunctionsWhenReset() throws Exception
    {
        final Agent agent = new Agent("testCanCleanupRegistersRhsFunctionsWhenReset");
        try
        {
            // Initialize javascript engine and register a handler for our test
            // function. It throws an exception.
            agent.getInterpreter().eval("script javascript {\n" +
                    "soar.rhsFunction( { name: 'cleanup-test', \n" +
                    "   execute: function(context, args) { throw 'Failed'; } " +
                    "});\n" +
                    "\n}");
            
            // reset javascript engine
            agent.getInterpreter().eval("script --reset javascript");
            
            // Now if everything went right, firing the test event should have
            // no effect
            assertNull(agent.getRhsFunctions().getHandler("cleanup-test"));
        }
        finally
        {
            agent.dispose();
        }
    }
}
