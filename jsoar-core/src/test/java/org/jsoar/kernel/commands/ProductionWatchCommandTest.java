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

public class ProductionWatchCommandTest
{
    private Agent agent;

    @Before
    public void setUp() throws Exception
    {
        agent = new Agent();
        agent.initialize();
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
        assertFalse(p.isTraceFirings());
        agent.getInterpreter().eval("pwatch --on b");
        assertTrue(p.isTraceFirings());
        agent.getInterpreter().eval("pwatch --off b");
        assertFalse(p.isTraceFirings());
    }
    
    @Test
    public void testCanListTracedRules() throws Exception
    {
        loadRules();
        agent.getInterpreter().eval("pwatch --on b");
        agent.getInterpreter().eval("pwatch --on c");
        final String result = agent.getInterpreter().eval("pwatch");
        assertEquals("b\nc", result);
    }
    
    private void loadRules() throws Exception
    {
        agent.getProductions().loadProduction("b (state <s> ^superstate nil) --> (write hi)");
        agent.getProductions().loadProduction("a (state <s> ^superstate nil) --> (write hi)");
        agent.getProductions().loadProduction("c (state <s> ^superstate nil) --> (write hi)");
    }

}
