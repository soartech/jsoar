/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 19, 2008
 */
package org.jsoar.kernel.rhs.functions;


import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.io.CycleCountInput;

/**
 * @author ray
 */
public class InterruptTest extends AndroidTestCase
{
    private Agent agent;

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void setUp() throws Exception
    {
        this.agent = new Agent(getContext());
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void tearDown() throws Exception
    {
    }

    public void testInterrupt() throws Exception
    {
        new CycleCountInput(agent.getInputOutput());
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        this.agent.getProductions().loadProduction("testInterrupt (state <s> ^superstate nil ^io.input-link.cycle-count 45) --> (interrupt)");
        
        this.agent.runForever();
        
        assertEquals("*** Interrupt from production testInterrupt ***", this.agent.getReasonForStop());
        assertEquals(45, agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue());
    }
}
