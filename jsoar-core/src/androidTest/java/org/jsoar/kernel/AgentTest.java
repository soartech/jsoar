/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 30, 2010
 */
package org.jsoar.kernel;

import android.test.AndroidTestCase;

import org.jsoar.kernel.symbols.SymbolFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author ray
 */
public class AgentTest extends AndroidTestCase
{
    private Agent agent;

    public void setUp() throws Exception
    {
        agent = new Agent(getContext());
    }

    public void tearDown() throws Exception
    {
        agent.dispose();
    }

    public void testDefaultStopPhaseIsApply()
    {
        assertEquals(Phase.APPLY, agent.getStopPhase());
    }

    public void testSetStopPhaseSetsTheStopPhaseProperty()
    {
        agent.setStopPhase(Phase.DECISION);
        assertEquals(Phase.DECISION, agent.getStopPhase());
        assertEquals(Phase.DECISION, agent.getProperties().get(SoarProperties.STOP_PHASE));
    }

    public void testGetGoalStack()
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
