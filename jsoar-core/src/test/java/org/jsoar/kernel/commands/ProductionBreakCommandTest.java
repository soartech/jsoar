/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2010
 */
package org.jsoar.kernel.commands;


import static org.junit.Assert.*;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ProductionBreakCommandTest
{
    private Agent agent;

    @Before
    public void setUp() throws Exception
    {
        agent = new Agent();
    }

    @After
    public void tearDown() throws Exception
    {
        agent.dispose();
        agent = null;
    }
    
    @Test
    public void testCanEnableTracingOnARule() throws Exception
    {
        loadRules();
        
        final Production p = agent.getProductions().getProduction("b");
        assertFalse(p.isBreakpointEnabled());
        agent.getInterpreter().eval("pbreak --on b");
        assertTrue(p.isBreakpointEnabled());
        agent.getInterpreter().eval("pbreak --off b");
        assertFalse(p.isBreakpointEnabled());
    }
    
    @Test
    public void testCanListTracedRules() throws Exception
    {
        loadRules();
        agent.getInterpreter().eval("pbreak --on b");
        agent.getInterpreter().eval("pbreak --on c");
        final String result = agent.getInterpreter().eval("pbreak");
        assertEquals("b\nc", result);
    }
    
    private void loadRules() throws Exception
    {
        agent.getProductions().loadProduction("b (state <s> ^superstate nil) --> (write hi)");
        agent.getProductions().loadProduction("a (state <s> ^superstate nil) --> (write hi)");
        agent.getProductions().loadProduction("c (state <s> ^superstate nil) --> (write hi)");
    }

}
