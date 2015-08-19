/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2010
 */
package org.jsoar.kernel.commands;


import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;

public class ProductionWatchCommandTest extends AndroidTestCase
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
        assertFalse(p.isTraceFirings());
        agent.getInterpreter().eval("pwatch --on b");
        assertTrue(p.isTraceFirings());
        agent.getInterpreter().eval("pwatch --off b");
        assertFalse(p.isTraceFirings());
    }
    
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
