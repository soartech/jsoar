/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 19, 2008
 */
package org.jsoar.kernel.rhs.functions;


import static org.junit.Assert.*;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.events.CycleCountInput;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class InterruptTest
{
    private Agent agent;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }

    @Test(timeout=3000)
    public void testInterrupt() throws Exception
    {
        new CycleCountInput(agent.getInputOutput(), agent.getEventManager());
        this.agent.decider.setWaitsnc(true);
        this.agent.loadProduction("testInterrupt (state <s> ^superstate nil ^io.input-link.cycle-count 45) --> (interrupt)");
        
        this.agent.decisionCycle.runForever();
        
        assertEquals("*** Interrupt from production testInterrupt ***", this.agent.decisionCycle.getReasonForStop());
        assertEquals(46, this.agent.decisionCycle.d_cycle_count);
    }
}
