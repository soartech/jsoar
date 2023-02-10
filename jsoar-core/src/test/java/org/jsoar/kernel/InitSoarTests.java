/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.timing.ExecutionTimer;
import org.junit.jupiter.api.Test;

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
            assertEquals(0, t.getTotalMicroseconds(), "Timer " + t.getName() + "microseconds is not zero");
            assertEquals(0, t.getTotalSeconds(), "Timer " + t.getName() + "seconds is not zero");
        }
        
        // confirm all productions are reset
        for(Production p : agent.getProductions().getProductions(null))
        {
            assertEquals(0, p.getFiringCount(), "Production " + p.getName() + " firing count is not zero");
            assertNull(p.instantiations, "Production " + p.getName() + " has instantiations");
            assertTrue(p.rlRuleInfo == null || p.rlRuleInfo.rl_update_count == 0.0, "Production " + p.getName() + " has non-zero RL update count");
        }
        
        // confirm all stats are reset
        // note that some of these values appear inconsistent (e.g., wme_addition_count=6, while max_wm_size=0)
        // but this is because some of these properties are only updated after the agent runs
        final PropertyManager props = agent.getProperties();
        assertEquals(1, props.get(SoarProperties.D_CYCLE_COUNT), "D_CYCLE_COUNT is not 1");
        assertEquals(0, props.get(SoarProperties.DECISION_PHASES_COUNT), "DECISION_PHASES_COUNT is non-zero");
        assertEquals(0, props.get(SoarProperties.E_CYCLE_COUNT), "E_CYCLE_COUNT is non-zero");
        assertEquals(0, props.get(SoarProperties.PE_CYCLE_COUNT), "PE_CYCLE_COUNT is non-zero");
        assertEquals(0, props.get(SoarProperties.INNER_E_CYCLE_COUNT), "INNER_E_CYCLE_COUNT is non-zero");
        assertEquals(0, props.get(SoarProperties.PRODUCTION_FIRING_COUNT), "PRODUCTION_FIRING_COUNT is non-zero");
        assertEquals(13, props.get(SoarProperties.WME_ADDITION_COUNT), "WME_ADDITION_COUNT is not 13");
        assertEquals(0, props.get(SoarProperties.WME_REMOVAL_COUNT), "WME_REMOVAL_COUNT is non-zero");
        assertEquals(0, props.get(SoarProperties.NUM_WM_SIZES_ACCUMULATED), "NUM_WM_SIZES_ACCUMULATED is non-zero");
        assertEquals(0, props.get(SoarProperties.CUMULATIVE_WM_SIZE), "CUMULATIVE_WM_SIZE is non-zero");
        ;
        assertEquals(0, props.get(SoarProperties.MAX_WM_SIZE), "MAX_WM_SIZE is non-zero");
        
        // confirm current phase is input
        assertEquals(Phase.INPUT, agent.getCurrentPhase(), "Current phase is not input");
        
        // confirm proper wmes are in rete
        assertEquals(13, agent.getAllWmesInRete().size(), "Wrong number of wmes in rete");
        // BADBAD: really? is this the best way to check which wmes are in the rete?
        for(Wme w : agent.getAllWmesInRete())
        {
            assertTrue(
                    (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("io") && w.getValue().toString().equals("I1"))
                            || (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("reward-link") && w.getValue().toString().equals("R1"))
                            || (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("epmem") && w.getValue().toString().equals("E1"))
                            || (w.getIdentifier().toString().equals("E1") && w.getAttribute().toString().equals("command") && w.getValue().toString().equals("C1"))
                            || (w.getIdentifier().toString().equals("E1") && w.getAttribute().toString().equals("present-id") && w.getValue().toString().equals("1"))
                            || (w.getIdentifier().toString().equals("E1") && w.getAttribute().toString().equals("result") && w.getValue().toString().equals("R2"))
                            || (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("smem") && w.getValue().toString().equals("S2"))
                            || (w.getIdentifier().toString().equals("S2") && w.getAttribute().toString().equals("result") && w.getValue().toString().equals("R3"))
                            || (w.getIdentifier().toString().equals("S2") && w.getAttribute().toString().equals("command") && w.getValue().toString().equals("C2"))
                            || (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("superstate") && w.getValue().toString().equals("nil"))
                            || (w.getIdentifier().toString().equals("S1") && w.getAttribute().toString().equals("type") && w.getValue().toString().equals("state"))
                            || (w.getIdentifier().toString().equals("I1") && w.getAttribute().toString().equals("input-link") && w.getValue().toString().equals("I2"))
                            || (w.getIdentifier().toString().equals("I1") && w.getAttribute().toString().equals("output-link") && w.getValue().toString().equals("I3")),
                    "Unexpected wme in rete");
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
        testInitSoar();
    }
    
    @Test
    public void testBlocksWorldOperatorSubgoalingInit() throws Exception
    {
        String testName = "testBlocksWorldOperatorSubgoalingInit";
        runTest(testName, 5);
        testInitSoar();
        runTestExecute(testName, 3);
        testInitSoar();
    }
    
    @Test
    public void testCountTestInit() throws Exception
    {
        String testName = "testCountTestInit";
        runTest(testName, 45047);
        testInitSoar();
        runTestExecute(testName, 25032);
        testInitSoar();
    }
}
