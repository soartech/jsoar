/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionTools;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
        
        agent.trace.disableAll();
        //agent.trace.setEnabled(Category.TRACE_CONTEXT_DECISIONS_SYSPARAM, true);
        //agent.trace.setEnabled(false);
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        final boolean halted[] = { false };
        final boolean failed[] = { false };
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                return oldHalt.execute(syms, arguments);
            }});
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("failed") {

            @Override
            public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                failed[0] = true;
                return oldHalt.execute(syms, arguments);
            }});
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("succeeded") {

            @Override
            public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                failed[0] = false;
                return oldHalt.execute(syms, arguments);
            }});
        
        agent.decisionCycle.runForever();
        assertTrue(testName + " functional test did not halt", halted[0]);
        assertFalse(testName + " functional test failed", failed[0]);
        if(expectedDecisions >= 0)
        {
            assertEquals(expectedDecisions, agent.decisionCycle.d_cycle_count); // deterministic!
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
        agent.trace.enableAll();
        ifc = new SoarTclInterface(agent);
        agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        agent.getPrinter().flush();
        ifc.dispose();
    }

    @Test
    public void testBasicElaborationAndMatch() throws Exception
    {
        sourceTestFile("testBasicElaborationAndMatch.soar");
        
        final Set<String> matches = new HashSet<String>();
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("matched") {

            @Override
            public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
            {
                RhsFunctionTools.checkArgumentCount(getName(), arguments, 2, 2);
                
                matches.add(arguments.get(0).toString() + "_" + arguments.get(1).toString());
                return null;
            }});
        
        agent.decisionCycle.run_for_n_decision_cycles(1);
        
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
            public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
            {
                RhsFunctionTools.checkArgumentCount(getName(), arguments, 1, 1);
                
                matches.add(arguments.get(0).toString());
                return null;
            }});
        
        agent.decisionCycle.run_for_n_decision_cycles(1);
        
        assertTrue(matches.contains("monitor*contents"));
        assertTrue(matches.contains("elaborate*free"));
        assertEquals(2, matches.size());
        
    }
    
    @Test
    public void testTowersOfHanoiProductionThatCrashesRete() throws Exception
    {
        // 9/24/2008 - This production caused a crash in the initial match of the
        // production. Nothing to test other than that no exceptions are thrown.
        agent.loadProduction("towers-of-hanoi*propose*initialize\n" +
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
        agent.loadProduction("towers-of-hanoi*propose*initialize\n" +
        "   (state <s> ^superstate nil\n" +
        "             -^name)\n" +
        "-->\n" +
        "   (<s> ^operator <o> +)\n" +
        "   (<o> ^name initialize-toh)");
        
        agent.consistency.setMaxElaborations(5);
        agent.decisionCycle.run_for_n_decision_cycles(1);
        assertFalse(agent.consistency.isHitMaxElaborations()); //  TODO replace with callback?
        
    }    
    
    @Test(timeout=5000)
    public void testWaterJug() throws Exception
    {
        runTest("testWaterJug", -1);
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
    
    @Test(timeout=10000)
    public void testJustifications() throws Exception
    {
        runTest("testJustifications", 2);
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
    
    @Ignore
    @Test(/*timeout=10000*/)
    public void testBlocksWorldLookAhead() throws Exception
    {
        runTest("testBlocksWorldLookAhead", -1);
    }
    
    @Test(timeout=20000)
    public void testArithmetic() throws Exception
    {
        runTest("testArithmetic", -1);
        assertTrue(agent.decisionCycle.d_cycle_count > 40000);
    }
}
