/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2010
 */
package org.jsoar.kernel.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProductionBreakCommandTest
{
    private Agent agent;
    
    @BeforeEach
    void setUp() throws Exception
    {
        agent = new Agent();
    }
    
    @AfterEach
    void tearDown() throws Exception
    {
        agent.dispose();
        agent = null;
    }
    
    @Test
    void testCanEnableTracingOnARule() throws Exception
    {
        loadRules();
        
        final Production p = agent.getProductions().getProduction("b");
        assertFalse(p.isBreakpointEnabled());
        agent.getInterpreter().eval("production break --set b");
        assertTrue(p.isBreakpointEnabled());
        agent.getInterpreter().eval("production break --clear b");
        assertFalse(p.isBreakpointEnabled());
    }
    
    @Test
    void testCanListTracedRules() throws Exception
    {
        loadRules();
        agent.getInterpreter().eval("production break --set b");
        agent.getInterpreter().eval("production break --set c");
        StringWriter result = new StringWriter();
        agent.getPrinter().pushWriter(result);
        agent.getInterpreter().eval("production break");
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
