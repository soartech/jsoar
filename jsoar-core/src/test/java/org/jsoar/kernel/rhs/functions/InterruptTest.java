/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 19, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.io.CycleCountInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author ray
 */
public class InterruptTest
{
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        this.agent = new Agent();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception
    {
    }
    
    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
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
