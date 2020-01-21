/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2010
 */
package org.jsoar.kernel.commands;


import static org.junit.Assert.*;

import java.io.StringWriter;

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
        agent.getInterpreter().eval("production watch --on b");
        assertTrue(p.isTraceFirings());
        agent.getInterpreter().eval("production watch --off b");
        assertFalse(p.isTraceFirings());
    }
    
    @Test
    public void testCanListTracedRules() throws Exception
    {
        loadRules();
        agent.getInterpreter().eval("production watch --on b");
        agent.getInterpreter().eval("production watch --on c");
        
        final StringWriter result = new StringWriter();
        agent.getPrinter().pushWriter(result);
        agent.getInterpreter().eval("production watch");
        agent.getPrinter().popWriter();
        assertEquals("b\nc", result.toString());
    }
    
    private void loadRules() throws Exception
    {
        agent.getProductions().loadProduction("b (state <s> ^superstate nil) --> (write hi)");
        agent.getProductions().loadProduction("a (state <s> ^superstate nil) --> (write hi)");
        agent.getProductions().loadProduction("c (state <s> ^superstate nil) --> (write hi)");
    }

}
