/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2010
 */
package org.jsoar.kernel.commands;


import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;

public class ProductionBreakCommandTest extends AndroidTestCase
{
    private Agent agent;

    @Override
    public void setUp() throws Exception
    {
        agent = new Agent(getContext());
    }

    @Override
    public void tearDown() throws Exception
    {
        agent.dispose();
        agent = null;
    }
    
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
