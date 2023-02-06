/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2010
 */
package org.jsoar.kernel.rete;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.RunType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class SoarReteListenerTest
{
    private Agent agent;

    @BeforeEach
    public void setUp() throws Exception
    {
        agent = new Agent("SoarReteListenerTest");
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        agent.dispose();
        agent = null;
    }

    @Test
    public void testInterruptsTheAgentWhenTheInterruptFlagIsSet() throws Exception
    {
        agent.getProductions().loadProduction(
        "propose*init\n" +
        "(state <s> ^superstate nil\n" +
        "          -^name)\n" +
        "-->\n" +
        "(<s> ^operator.name init)\n" +
        "");
        
        agent.getProductions().loadProduction(
        "apply*init\n" +
        ":interrupt\n" +
        "(state <s> ^operator.name init)\n" +
        "-->\n" +
        "(<s> ^name done)\n" +
        "");
        
        agent.runFor(500, RunType.DECISIONS);
        
        // The rule should not have actually fired, just matched
        assertTrue(agent.getReasonForStop().contains("apply*init"));
        final Production p = agent.getProductions().getProduction("apply*init");
        assertEquals(0L, p.getFiringCount());
    }
    
    @Test
    public void testInterruptsTheAgentWhenBreakpointIsEnabled() throws Exception
    {
        agent.getProductions().loadProduction(
        "propose*init\n" +
        "(state <s> ^superstate nil\n" +
        "          -^name)\n" +
        "-->\n" +
        "(<s> ^operator.name init)\n" +
        "");
        
        agent.getProductions().loadProduction(
        "apply*init\n" +
        "(state <s> ^operator.name init)\n" +
        "-->\n" +
        "(<s> ^name done)\n" +
        "");
        final Production p = agent.getProductions().getProduction("apply*init");
        p.setBreakpointEnabled(true);
        
        agent.runFor(500, RunType.DECISIONS);
        
        // The rule should not have actually fired, just matched
        assertTrue(agent.getReasonForStop().contains("apply*init"));
        assertEquals(0L, p.getFiringCount());
    }
}
