/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 30, 2010
 */
package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class AgentTest
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
    }
    
    @Test
    void testDefaultStopPhaseIsApply()
    {
        assertEquals(Phase.APPLY, agent.getStopPhase());
    }
    
    @Test
    void testSetStopPhaseSetsTheStopPhaseProperty()
    {
        agent.setStopPhase(Phase.DECISION);
        assertEquals(Phase.DECISION, agent.getStopPhase());
        assertEquals(Phase.DECISION, agent.getProperties().get(SoarProperties.STOP_PHASE));
    }
    
    @Test
    void testGetGoalStack()
    {
        agent.runFor(3, RunType.DECISIONS);
        // We start with S1. Running three steps, gives three new states, S2, S3, S4
        final List<Goal> gs = agent.getGoalStack();
        assertNotNull(gs);
        assertEquals(4, gs.size());
        final SymbolFactory syms = agent.getSymbols();
        assertEquals(Arrays.asList(syms.findIdentifier('S', 1),
                syms.findIdentifier('S', 3),
                syms.findIdentifier('S', 5),
                syms.findIdentifier('S', 7)),
                Arrays.asList(gs.get(0).getIdentifier(),
                        gs.get(1).getIdentifier(),
                        gs.get(2).getIdentifier(),
                        gs.get(3).getIdentifier()));
    }
    
}
