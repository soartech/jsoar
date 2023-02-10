/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author ray
 */
public class WaterfallTests
{
    private Agent agent;
    private SoarCommandInterpreter ifc;
    
    private void sourceTestFile(String name) throws SoarException
    {
        ifc.source(getClass().getResource("/" + WaterfallTests.class.getName().replace('.', '/') + "_" + name));
    }
    
    private void runTest(String testName, int expectedDecisions) throws Exception
    {
        sourceTestFile(testName + ".soar");
        
        agent.getTrace().disableAll();
        // agent.trace.setEnabled(Category.TRACE_CONTEXT_DECISIONS_SYSPARAM, true);
        // agent.trace.setEnabled(false);
        final RhsFunctionHandler oldHalt = agent.getRhsFunctions().getHandler("halt");
        assertNotNull(oldHalt);
        
        final boolean halted[] = { false };
        final boolean failed[] = { false };
        
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("halt")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("failed")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                failed[0] = true;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("succeeded")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                halted[0] = true;
                failed[0] = false;
                return oldHalt.execute(rhsContext, arguments);
            }
        });
        
        agent.runForever();
        assertTrue(halted[0], testName + " functional test did not halt");
        assertFalse(failed[0], testName + " functional test failed");
        if(expectedDecisions >= 0)
        {
            assertEquals(expectedDecisions, agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue()); // deterministic!
        }
        
        ifc.eval("stats");
    }
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        agent = new Agent();
        agent.getTrace().enableAll();
        ifc = agent.getInterpreter();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception
    {
        agent.getPrinter().flush();
        agent.dispose();
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWaterfall() throws Exception
    {
        runTest("testWaterfall", 2);
        assertEquals(4, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        assertEquals(5, agent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWaterfallUnbound() throws Exception
    {
        runTest("testWaterfallUnbound", 2);
        assertEquals(4, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        assertEquals(5, agent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWaterfallFiveStates() throws Exception
    {
        runTest("testWaterfallFiveStates", 8);
        assertEquals(10, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        assertEquals(16, agent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWaterfallBlocksWorldHRL() throws Exception
    {
        runTest("testBlocksWorldHRL", -1);
    }
}
