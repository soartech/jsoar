/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import junit.framework.Assert;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.timing.ExecutionTimer;

/**
 * @author ray
 */
public class InitSoarTests extends FunctionalTestHarness
{
    
    public void testInitSoar() throws Exception
    {
        agent.initialize();
        
        // confirm all timers are zero
        for(ExecutionTimer t : agent.getAllTimers())
        {
            Assert.assertTrue("Timer " + t.getName() + "microseconds is not zero", t.getTotalMicroseconds() == 0);
            Assert.assertTrue("Timer " + t.getName() + "seconds is not zero", t.getTotalSeconds() == 0);
        }
        
        // confirm all productions are reset
        for(Production p : agent.getProductions().getProductions(null))
        {
            Assert.assertTrue("Production " + p.getName() + " firing count is not zero", p.getFiringCount() == 0);
            Assert.assertTrue("Production " + p.getName() + " has instantiations", p.instantiations == null);
            Assert.assertTrue("Production " + p.getName() + " has non-zero RL update count",
                       p.rlRuleInfo == null || p.rlRuleInfo.rl_update_count == 0.0);
        }
        
        // confirm all stats are reset
        // note that some of these values appear inconsistent (e.g., wme_addition_count=6, while max_wm_size=0)
        // but this is because some of these properties are only updated after the agent runs
        final PropertyManager props = agent.getProperties();
        Assert.assertTrue("D_CYCLE_COUNT is not 1", props.get(SoarProperties.D_CYCLE_COUNT) == 1);
        Assert.assertTrue("DECISION_PHASES_COUNT is non-zero", props.get(SoarProperties.DECISION_PHASES_COUNT) == 0);
        Assert.assertTrue("E_CYCLE_COUNT is non-zero", props.get(SoarProperties.E_CYCLE_COUNT) == 0);
        Assert.assertTrue("PE_CYCLE_COUNT is non-zero", props.get(SoarProperties.PE_CYCLE_COUNT) == 0);
        Assert.assertTrue("INNER_E_CYCLE_COUNT is non-zero", props.get(SoarProperties.INNER_E_CYCLE_COUNT) == 0);
        Assert.assertTrue("PRODUCTION_FIRING_COUNT is non-zero", props.get(SoarProperties.PRODUCTION_FIRING_COUNT) == 0);
        Assert.assertTrue("WME_ADDITION_COUNT is not 13", props.get(SoarProperties.WME_ADDITION_COUNT) == 13);
        Assert.assertTrue("WME_REMOVAL_COUNT is non-zero", props.get(SoarProperties.WME_REMOVAL_COUNT) == 0);
        Assert.assertTrue("NUM_WM_SIZES_ACCUMULATED is non-zero", props.get(SoarProperties.NUM_WM_SIZES_ACCUMULATED) == 0);
        Assert.assertTrue("CUMULATIVE_WM_SIZE is non-zero", props.get(SoarProperties.CUMULATIVE_WM_SIZE) == 0);
        Assert.assertTrue("MAX_WM_SIZE is non-zero", props.get(SoarProperties.MAX_WM_SIZE) == 0);
        
        // confirm current phase is input
        Assert.assertTrue("Current phase is not input", agent.getCurrentPhase() == Phase.INPUT);

        // confirm proper wmes are in rete
        Assert.assertTrue("Wrong number of wmes in rete", agent.getAllWmesInRete().size() == 13);
        // BADBAD: really? is this the best way to check which wmes are in the rete?
        for(Wme w : agent.getAllWmesInRete())
        {
            Assert.assertTrue("Unexpected wme in rete",
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
                || (w.getIdentifier().toString().equals("I1") && w.getAttribute().toString().equals("output-link") && w.getValue().toString().equals("I3")) );
        }    
    }
    
    public void testTowersOfHanoiInit() throws Exception
    {
        String testName = "testTowersOfHanoiInit";
        int expectedDecisions = 2048;
        runTest(testName, expectedDecisions);
        testInitSoar();
        runTestExecute(testName, expectedDecisions);
        testInitSoar();
    }
    
    public void testBlocksWorldOperatorSubgoalingInit() throws Exception
    {
        String testName = "testBlocksWorldOperatorSubgoalingInit";
        runTest(testName, 5);
        testInitSoar();
        runTestExecute(testName, 3);
        testInitSoar();
    }
    
    public void testCountTestInit() throws Exception
    {
        String testName = "testCountTestInit";
        runTest(testName, 45047);
        testInitSoar();
        runTestExecute(testName, 25032);
        testInitSoar();
    }
}
