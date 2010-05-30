/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 30, 2010
 */
package org.jsoar.kernel;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class AgentTest
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
    }

    @Test
    public void testDefaultStopPhaseIsInput()
    {
        assertEquals(Phase.INPUT, agent.getStopPhase());
    }
    
    @Test
    public void testSetStopPhaseSetsTheStopPhaseProperty()
    {
        agent.setStopPhase(Phase.DECISION);
        assertEquals(Phase.DECISION, agent.getStopPhase());
        assertEquals(Phase.DECISION, agent.getProperties().get(SoarProperties.STOP_PHASE));
    }

}
