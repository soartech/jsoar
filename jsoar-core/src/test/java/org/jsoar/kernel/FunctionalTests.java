/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rhs.ReordererException;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.timing.ExecutionTimer;
import org.junit.Test;

/**
 * @author ray
 */
public class FunctionalTests extends FunctionalTestHarness
{
    @Test
    public void testBasicElaborationAndMatch() throws Exception
    {
        runTest("testBasicElaborationAndMatch", 1);
    }
    
    @Test
    public void testTowersOfHanoiProductionThatCrashesRete() throws Exception
    {
        // 9/24/2008 - This production caused a crash in the initial match of the
        // production. Nothing to test other than that no exceptions are thrown.
        agent.getProductions().loadProduction("towers-of-hanoi*propose*initialize\n" +
        "   (state <s> ^superstate nil\n" +
        "             -^name)\n" +
        "-->\n" +
        "   (<s> ^operator <o> +)\n" +
        "   (<o> ^name initialize-toh)");
    }
    
    @Test
    public void testTowersOfHanoiProductionThatCausesMaxElaborations() throws Exception
    {
        // 9/24/2008 - This production caused a crash in the initial match of the
        // production. Nothing to test other than that no exceptions are thrown.
        agent.getProductions().loadProduction("towers-of-hanoi*propose*initialize\n" +
        "   (state <s> ^superstate nil\n" +
        "             -^name)\n" +
        "-->\n" +
        "   (<s> ^operator <o> +)\n" +
        "   (<o> ^name initialize-toh)");
        
        agent.getProperties().set(SoarProperties.MAX_ELABORATIONS, 5);
        agent.runFor(1, RunType.DECISIONS);
        final DecisionCycle dc = Adaptables.adapt(agent, DecisionCycle.class);
        assertFalse(dc.isHitMaxElaborations());
        
    }    
    
    @Test(timeout=5000)
    public void testWaterJug() throws Exception
    {
        runTest("testWaterJug", -1);
    }
    @Test(timeout=10000)
    public void testWaterJugLookAhead() throws Exception
    {
        runTest("testWaterJugLookAhead", -1);
    }
    @Test(timeout=10000)
    public void testWaterJugHierarchy() throws Exception
    {
        runTest("testWaterJugHierarchy", -1);
    }
    
    @Test
    public void testTowersOfHanoi() throws Exception
    {
        runTest("testTowersOfHanoi", 2048);
    }
    
    @Test
    public void testTowersOfHanoiFast() throws Exception
    {
        runTest("testTowersOfHanoiFast", 2047);
    }
    
    @Test(timeout=10000)
    public void testEightPuzzle() throws Exception
    {
        runTest("testEightPuzzle", -1);
    }
    
    @Test(timeout=10000)
    public void testBlocksWorld() throws Exception
    {
        runTest("testBlocksWorld", -1);
    }
    
    @Test(timeout=10000)
    public void testBlocksWorldOperatorSubgoaling() throws Exception
    {
        runTest("testBlocksWorldOperatorSubgoaling", -1);
    }
    
    @Test(timeout=10000)
    public void testBlocksWorldLookAhead() throws Exception
    {
        String testName = "testBlocksWorldLookAhead";
        runTestSetup(testName);
        agent.getRandom().setSeed(1);
        runTestExecute(testName, 27);
    }
    
    @Test
    public void testBlocksWorldLookAhead2() throws Exception
    {
        String testName = "testBlocksWorldLookAhead";
        runTestSetup(testName);
        agent.getRandom().setSeed(100000000002L);
        runTestExecute(testName, 29);
    }
    
    @Test
    public void testBlocksWorldLookAheadWithMaxNoChangeBug() throws Exception
    {
        // This tests for a bug in the chunking caused by a bug in add_cond_to_tc()
        // where the id and attr test for positive conditions were added to the tc
        // rather than id and *value*. The first chunk constructed was incorrect
        runTest("testBlocksWorldLookAheadWithMaxNoChangeBug", 15);
        assertEquals(72, agent.getProductions().getProductions(ProductionType.DEFAULT).size());
        assertEquals(15, agent.getProductions().getProductions(ProductionType.USER).size());
        assertEquals(4, agent.getProductions().getProductions(ProductionType.CHUNK).size());
        
        // Make sure the chunk was built correctly.
        JSoarTest.verifyProduction(agent, 
                "chunk-1*d10*opnochange*1", 
                ProductionType.CHUNK,
                "sp {chunk-1*d10*opnochange*1\n" +
                "    :chunk\n" +
                "    (state <s1> ^operator <o1>)\n" +
                "    (<o1> -^default-desired-copy yes)\n" +
                "    (<o1> ^name evaluate-operator)\n" +
                "    (<o1> ^superproblem-space <s2>)\n" +
                "    (<s2> ^name move-blocks)\n" +
                "    (<o1> ^evaluation <e1>)\n" +
                "    (<s1> ^evaluation <e1>)\n" +
                "    (<o1> ^superstate <s3>)\n" +
                "    (<s3> ^name blocks-world)\n" +
                "    (<s3> ^object <o2>)\n" +
                "    (<o2> ^type block)\n" +
                "    (<e1> ^desired <d1>)\n" +
                "    (<o1> ^superoperator <s4>)\n" +
                "    (<s4> ^moving-block { <m1> <> <o2> })\n" +
                "    (<s3> ^object <m1>)\n" +
                "    (<s4> ^destination <d2>)\n" +
                "    (<s3> ^ontop <o3>)\n" +
                "    (<o3> ^top-block <o2>)\n" +
                "    (<o3> ^bottom-block { <b1> <> <d2> <> <m1> })\n" +
                "    (<s3> ^ontop <o4>)\n" +
                "    (<o4> ^top-block <m1>)\n" +
                "    (<o4> ^bottom-block <b1>)\n" +
                "    (<s3> ^ontop <o5>)\n" +
                "    (<o5> ^top-block <d2>)\n" +
                "    (<o5> ^bottom-block <b1>)\n" +
                "    (<d1> ^ontop <o6>)\n" +
                "    (<o6> ^top-block <o2>)\n" +
                "    (<o6> ^bottom-block <m1>)\n" +
                "    (<d1> ^ontop { <o7> <> <o6> })\n" +
                "    (<o7> ^top-block <m1>)\n" +
                "    (<o7> ^bottom-block <d2>)\n" +
                "    (<d1> ^ontop { <o8> <> <o7> <> <o6> })\n" +
                "    (<o8> ^top-block <d2>)\n" +
                "    (<o8> ^bottom-block <b1>)\n" +
                "    -->\n" +
                "    (<e1> ^symbolic-value success +)\n" +
                "}\n", true);

    }
    
    @Test(timeout=10000)
    public void testBlocksWorldLookAheadRandom() throws Exception
    {
        runTest("testBlocksWorldLookAhead", -1);
    }
    
    @Test(timeout=30000)
    public void testArithmetic() throws Exception
    {
        runTest("testArithmetic", -1);
        assertTrue(agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue() > 40000);
    }
    
    @Test(timeout=80000)
    public void testCountTest() throws Exception
    {
        runTest("testCountTest", -1);
        assertEquals(42, agent.getProductions().getProductions(ProductionType.USER).size());
        assertEquals(15012, agent.getProductions().getProductions(ProductionType.CHUNK).size());
        assertEquals(45047, agent.getProperties().get(SoarProperties.DECISION_PHASES_COUNT).intValue());
        assertEquals(115136, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        assertEquals(40039, agent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        assertEquals(120146, agent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
    }

    @Test 
    public void testForBadBug517Fix() throws Exception
    {
        // Original fix for bug 517 (ca. r299) caused a bug in the following production.
        // This tests against the regression.
        agent.getProductions().loadProduction("test (state <s> ^a <val> -^a {<val> < 1}) -->");
        JSoarTest.verifyProduction(agent, 
            "test", 
            ProductionType.USER,
            "sp {test\n"+
            "    (state <s> ^a <val>)\n" +
            "    (<s> -^a { <val> < 1 })\n"+
            "    -->\n"+
            "    \n"+
            "}\n", 
            true);
        
    }

    @Test
    public void testTemplateVariableNameBug1121() throws Exception
    {
        runTest("testTemplateVariableNameBug1121", 1);
        assertEquals(4, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
    }
    
    @Test
    public void testNegatedConjunctiveTestUnbound() throws Exception
    {
    	boolean success;
    	
    	// these should all fail in reorder
    	success = false;
    	try {
    		agent.getProductions().loadProduction("test (state <s> ^superstate nil -^foo { <> <bad> }) -->");
    	} catch (ReordererException e) {
    		// <bad> is unbound referent in value test
    		success = true;
    	}
    	assertTrue(success);
    	
    	success = false;
    	try {
    		agent.getProductions().loadProduction("test (state <s> ^superstate nil -^{ <> <bad> } <s>) -->");
    	} catch (ReordererException e) {
    		// <bad> is unbound referent in attr test
    		success = true;
    	}
    	assertTrue(success);
    	
    	success = false;
    	try {
    		agent.getProductions().loadProduction("test (state <s> ^superstate nil -^foo { <> <b> }) -{(<s> ^bar <b>) (<s> -^bar { <> <b>})} -->");
    	} catch (ReordererException e) {
    		// <b> is unbound referent in test, defined in ncc out of scope
    		success = true;
    	}
    	assertTrue(success);
    	
    	success = false;
    	try {
    		agent.getProductions().loadProduction("test  (state <s> ^superstate <d> -^foo { <> <b> }) -{(<s> ^bar <b>) (<s> -^bar { <> <d>})} -->");
    	} catch (ReordererException e) {
    		// <d> is unbound referent in value test in ncc
    		success = true;
    	}
    	assertTrue(success);
    	
    	// these should succeed
   		agent.getProductions().loadProduction("test (state <s> ^superstate <d>) -{(<s> ^bar <b>) (<s> -^bar { <> <d>})} -->");
   		agent.getProductions().loadProduction("test (state <s> ^superstate nil) -{(<s> ^bar <d>) (<s> -^bar { <> <d>})} -->");
    }
    
    @Test(timeout=10000)
    public void testPreferenceSemantics() throws Exception
    {
        runTest("testPreferenceSemantics", -1);
    }
    
    @Test
    public void testTieImpasse() throws Exception
    {
        runTest("testTieImpasse", 1);
    }
    
    @Test
    public void testConflictImpasse() throws Exception
    {
        runTest("testConflictImpasse", 1);
    }
    
    @Test
    public void testConstraintFailureImpasse() throws Exception
    {
        runTest("testConstraintFailureImpasse", 1);
    }
    
    @Test
    public void testOperatorNoChangeImpasse() throws Exception
    {
        runTest("testOperatorNoChangeImpasse", 2);
    }
    
    @Test
    public void testStateNoChangeImpasse() throws Exception
    {
        runTest("testStateNoChangeImpasse", 1);
    }
    
    @Test
    public void testInitialState() throws Exception
    {
        runTest("testInitialState", 1);
    }
    
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
        String testName = "testTowersOfHanoi";
        int expectedDecisions = 2048;
        runTest(testName, expectedDecisions);
        testInitSoar();
        runTestExecute(testName, expectedDecisions);
    }
    
    @Test
    public void testORejectsFirst() throws Exception
    {
        runTest("testORejectsFirst", 1);
    }

}
