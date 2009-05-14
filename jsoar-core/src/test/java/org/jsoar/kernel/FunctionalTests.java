/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctions;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class FunctionalTests
{
    private Agent agent;
    private SoarTclInterface ifc;

    private void sourceTestFile(String name) throws SoarTclException
    {
        ifc.sourceResource("/" + FunctionalTests.class.getName().replace('.', '/')  + "_" + name);
    }
    
    private void runTest(String testName, int expectedDecisions) throws SoarTclException
    {
        sourceTestFile(testName + ".soar");
        
        agent.getTrace().disableAll();
        //agent.trace.setEnabled(Category.TRACE_CONTEXT_DECISIONS_SYSPARAM, true);
        //agent.trace.setEnabled(false);
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        final boolean halted[] = { false };
        final boolean failed[] = { false };
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                return oldHalt.execute(rhsContext, arguments);
            }});
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("failed") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                failed[0] = true;
                return oldHalt.execute(rhsContext, arguments);
            }});
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("succeeded") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                failed[0] = false;
                return oldHalt.execute(rhsContext, arguments);
            }});
        
        agent.runForever();
        assertTrue(testName + " functional test did not halt", halted[0]);
        assertFalse(testName + " functional test failed", failed[0]);
        if(expectedDecisions >= 0)
        {
            assertEquals(expectedDecisions, agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue()); // deterministic!
        }
        
        ifc.eval("stats");
    }
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        agent = new Agent();
        agent.getTrace().enableAll();
        ifc = SoarTclInterface.findOrCreate(agent);
        agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        agent.getPrinter().flush();
        SoarTclInterface.dispose(ifc);
    }

    @Test
    public void testBasicElaborationAndMatch() throws Exception
    {
        sourceTestFile("testBasicElaborationAndMatch.soar");
        
        final Set<String> matches = new HashSet<String>();
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("matched") {

            @Override
            public SymbolImpl execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                RhsFunctions.checkArgumentCount(getName(), arguments, 2, 2);
                
                matches.add(arguments.get(0).toString() + "_" + arguments.get(1).toString());
                return null;
            }});
        
        agent.runFor(1, RunType.DECISIONS);
        
        assertTrue(matches.contains("J1_0"));
        assertTrue(matches.contains("J2_1"));
        assertEquals(2, matches.size());
        
    }
    
    @Test
    public void testBasicElaborationAndMatch2() throws Exception
    {
        sourceTestFile("testBasicElaborationAndMatch2.soar");
        
        final Set<String> matches = new HashSet<String>();
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("matched") {

            @Override
            public SymbolImpl execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                RhsFunctions.checkArgumentCount(getName(), arguments, 1, 1);
                
                matches.add(arguments.get(0).toString());
                return null;
            }});
        
        agent.runFor(1, RunType.DECISIONS);
        
        assertTrue(matches.contains("monitor*contents"));
        assertTrue(matches.contains("elaborate*free"));
        assertEquals(2, matches.size());
        
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
        assertFalse(dc.isHitMaxElaborations()); //  TODO replace with callback?
        
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
    
    @Test(timeout=10000)
    public void testTowersOfHanoi() throws Exception
    {
        runTest("testTowersOfHanoi", 2048);
    }
    
    @Test(timeout=10000)
    public void testTowersOfHanoiFast() throws Exception
    {
        runTest("testTowersOfHanoiFast", 2047);
    }
    
    @Test(timeout=10000)
    public void testEightPuzzle() throws Exception
    {
        runTest("testEightPuzzle", -1);
    }
    
    @Test(/*timeout=10000*/)
    public void testJustifications() throws Exception
    {
        runTest("testJustifications", 2);
        Production j = agent.getProductions().getProduction("justification-1");
        assertNull(j);
    }
    
    @Test(timeout=10000)
    public void testChunks() throws Exception
    {
        runTest("testChunks", 2);
        
        // Verify that the chunk was created correctly
        JSoarTest.verifyProduction(agent, 
                "chunk-1*d2*opnochange*1", 
                ProductionType.CHUNK, 
                "sp {chunk-1*d2*opnochange*1\n" +
                "    :chunk\n" +
                "    (state <s1> ^operator <o1>)\n" +
                "    (<o1> ^name onc)\n" +
                "    -->\n" +
                "    (<s1> ^result true +)\n" +
                "}\n", false);
    }
    
    @Test(timeout=10000)
    public void testChunks2() throws Exception
    {
        runTest("testChunks2", -1);
    }
    
    @Test(timeout=10000)
    public void testChunks2WithLearning() throws Exception
    {
        ifc.eval("learn --on");
        runTest("testChunks2", -1);
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
        runTest("testBlocksWorldLookAhead", -1);
    }
    
    @Test(timeout=10000)
    public void testBlocksWorldLookAhead2() throws Exception
    {
        runTest("testBlocksWorldLookAhead2", 29);
    }
    
    @Test(timeout=10000)
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
        runTest("testBlocksWorldLookAheadRandom", -1);
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

}
