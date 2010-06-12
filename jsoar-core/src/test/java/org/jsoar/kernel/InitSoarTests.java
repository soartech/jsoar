/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertTrue;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.timing.ExecutionTimer;
import org.junit.Test;

/**
 * @author ray
 */
public class InitSoarTests extends FunctionalTestHarness
{
    
    @Test
    public void testInitSoar() throws Exception
    {
        agent.initialize();
        
        // confirm all timers are zero
        for(ExecutionTimer t : agent.getAllTimers())
        {
            assertTrue("Timer " + t.getName() + "microseconds is not zero", t.getTotalMicroseconds() == 0);
            assertTrue("Timer " + t.getName() + "seconds is not zero", t.getTotalSeconds() == 0);
        }
        
        // confirm all productions are reset
        for(Production p : agent.getProductions().getProductions(null))
        {
            assertTrue("Production " + p.getName() + " firing count is not zero", p.getFiringCount() == 0);
            assertTrue("Production " + p.getName() + " has instantiations", p.instantiations == null);
            assertTrue("Production " + p.getName() + " has non-zero RL update count", p.rl_update_count == 0.0);
        }
        
        // confirm all stats are reset
        // note that some of these values appear inconsistent (e.g., wme_addition_count=6, while max_wm_size=0)
        // but this is because some of these properties are only updated after the agent runs
        final PropertyManager props = agent.getProperties();
        assertTrue("D_CYCLE_COUNT is not 1", props.get(SoarProperties.D_CYCLE_COUNT) == 1);
        assertTrue("DECISION_PHASES_COUNT is non-zero", props.get(SoarProperties.DECISION_PHASES_COUNT) == 0);
        assertTrue("E_CYCLE_COUNT is non-zero", props.get(SoarProperties.E_CYCLE_COUNT) == 0);
        assertTrue("PE_CYCLE_COUNT is non-zero", props.get(SoarProperties.PE_CYCLE_COUNT) == 0);
        assertTrue("INNER_E_CYCLE_COUNT is non-zero", props.get(SoarProperties.INNER_E_CYCLE_COUNT) == 0);
        assertTrue("PRODUCTION_FIRING_COUNT is non-zero", props.get(SoarProperties.PRODUCTION_FIRING_COUNT) == 0);
        assertTrue("WME_ADDITION_COUNT is not 6", props.get(SoarProperties.WME_ADDITION_COUNT) == 6);
        assertTrue("WME_REMOVAL_COUNT is non-zero", props.get(SoarProperties.WME_REMOVAL_COUNT) == 0);
        assertTrue("NUM_WM_SIZES_ACCUMULATED is non-zero", props.get(SoarProperties.NUM_WM_SIZES_ACCUMULATED) == 0);
        assertTrue("CUMULATIVE_WM_SIZE is non-zero", props.get(SoarProperties.CUMULATIVE_WM_SIZE) == 0);
        assertTrue("MAX_WM_SIZE is non-zero", props.get(SoarProperties.MAX_WM_SIZE) == 0);
        
        // confirm current phase is input
        assertTrue("Current phase is not input", agent.getCurrentPhase() == Phase.INPUT);

        // confirm proper wmes are in rete
        assertTrue("Wrong number of wmes in rete", agent.getAllWmesInRete().size() == 6);
        // BADBAD: really? is this the best way to check which wmes are in the rete?
        for(Wme w : agent.getAllWmesInRete())
        {
            assertTrue("Unexpected wme in rete", 
                   (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("io") && w.getValue().toString().equals("I1"))
                || (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("reward-link") && w.getValue().toString().equals("R1"))
                || (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("superstate") && w.getValue().toString().equals("nil"))
                || (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("type") && w.getValue().toString().equals("state"))
                || (w.getIdentifier().toString().equals("I1") && w.getAttribute().toString().equals("input-link") && w.getValue().toString().equals("I2"))
                || (w.getIdentifier().toString().equals("I1") && w.getAttribute().toString().equals("output-link") && w.getValue().toString().equals("I3")) );
        }    
    }
    
    @Test
    public void testTowersOfHanoiInit() throws Exception
    {
        String testName = "testTowersOfHanoiInit";
        int expectedDecisions = 2048;
        runTest(testName, expectedDecisions);
        testInitSoar();
        runTestExecute(testName, expectedDecisions);
    }
    
    @Test
    public void testBlocksWorldOperatorSubgoalingInit() throws Exception
    {
        String testName = "testBlocksWorldOperatorSubgoalingInit";
        runTest(testName, 5);
        testInitSoar();
        runTestExecute(testName, 3);
    }
}
