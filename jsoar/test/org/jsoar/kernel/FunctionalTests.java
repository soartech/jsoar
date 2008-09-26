/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionTools;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.tcl.SoarTclInterface;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tcl.lang.TclException;

/**
 * @author ray
 */
public class FunctionalTests
{
    private Agent agent;
    private SoarTclInterface ifc;

    private void sourceTestFile(String name) throws TclException
    {
        ifc.sourceResource("/" + FunctionalTests.class.getName().replace('.', '/')  + "_" + name);
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
    public void testTowersOfHanoiProductionThatCrashesRete() throws IOException
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
    public void testTowersOfHanoiProductionThatCausesMaxElaborations() throws IOException
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
        sourceTestFile("testWaterJug.soar");
        
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        final boolean halted[] = { false };
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                return oldHalt.execute(syms, arguments);
            }});
        
        agent.decisionCycle.run_for_n_decision_cycles(1000);
        assertTrue("waterjugs functional test did not halt", halted[0]);
    }
    
    @Test(timeout=10000)
    public void testTowersOfHanoi() throws Exception
    {
        sourceTestFile("testTowersOfHanoi.soar");
        
        agent.trace.setEnabled(false);
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        final boolean halted[] = { false };
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                return oldHalt.execute(syms, arguments);
            }});
        
        agent.decisionCycle.run_for_n_decision_cycles(5000);
        assertTrue("toh functional test did not halt", halted[0]);
        assertEquals(2048, agent.decisionCycle.d_cycle_count); // deterministic!
    }
    
    @Test(timeout=10000)
    public void testTowersOfHanoiFast() throws Exception
    {
        sourceTestFile("testTowersOfHanoiFast.soar");
        
        agent.trace.setEnabled(false);
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        final boolean halted[] = { false };
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt") {

            @Override
            public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                return oldHalt.execute(syms, arguments);
            }});
        
        agent.decisionCycle.run_for_n_decision_cycles(5000);
        assertTrue("toh functional test did not halt", halted[0]);
        assertEquals(2048, agent.decisionCycle.d_cycle_count); // deterministic!
    }
}
