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
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctions;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class WaterfallTests
{
    private Agent agent;
    private SoarTclInterface ifc;

    private void sourceTestFile(String name) throws SoarTclException
    {
        ifc.sourceResource("/" + WaterfallTests.class.getName().replace('.', '/')  + "_" + name);
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

    @Test(timeout=1000)
    public void testWaterfall() throws Exception
    {
        runTest("testWaterfall", 2);
        assertEquals(4, agent.decisionCycle.e_cycle_count);
        assertEquals(5, agent.decisionCycle.inner_e_cycle_count);
    }
    @Test(timeout=1000)
    public void testWaterfallUnbound() throws Exception
    {
        runTest("testWaterfallUnbound", 2);
        assertEquals(4, agent.decisionCycle.e_cycle_count);
        assertEquals(5, agent.decisionCycle.inner_e_cycle_count);
    }
    @Test(timeout=1000)
    public void testWaterfallFiveStates() throws Exception
    {
        runTest("testWaterfallFiveStates", 8);
        assertEquals(10, agent.decisionCycle.e_cycle_count);
        assertEquals(16, agent.decisionCycle.inner_e_cycle_count);
    }
    @Test(timeout=20000)
    public void testWaterfallBlocksWorldHRL() throws Exception
    {
        runTest("testBlocksWorldHRL", -1);
    }
}
