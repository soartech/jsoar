/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.io;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.events.InputCycleEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.events.OutputEvent.OutputMode;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.StandaloneRhsFunctionHandler;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterators;

/**
 * @author ray
 */
public class InputOutputImplTest extends JSoarTest
{
    private Agent agent;
    private SoarTclInterface ifc;
    
    private static class MatchFunction extends StandaloneRhsFunctionHandler
    {
        boolean called = false;
        List<List<Symbol>> calls = new ArrayList<List<Symbol>>();
        
        public MatchFunction() { super("match"); }
        
        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
         */
        @Override
        public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
        {
            called = true;
            calls.add(new ArrayList<Symbol>(arguments));
            return null;
        }
    }
    
    private MatchFunction match;
    
    private void sourceTestFile(String name) throws SoarTclException
    {
        ifc.sourceResource("/" + getClass().getName().replace('.', '/')  + "_" + name);
    }
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        
        agent = new Agent();
        ifc = SoarTclInterface.findOrCreate(agent);
        agent.getRhsFunctions().registerHandler(match = new MatchFunction());
        agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        SoarTclInterface.dispose(ifc);
    }

    @Test
    public void testBasicInput() throws Exception
    {
        final int listenerCallCount[] = { 0 };
        agent.getEventManager().addListener(InputCycleEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                listenerCallCount[0]++;
                
                InputBuilder builder = InputBuilder.create(agent.io);
                builder.push("location").markId("L1").
                            add("x", 3).
                            add("y", 4).
                            add("name", "hello").
                            pop().
                        push("location").markId("L2").
                            add("x", 5).
                            add("y", 6).
                            add("name", "goodbye").
                            link("a link", "L1").
                            pop().
                        add(99, "integer attribute").
                        add(3.0, "double attribute").
                        add("flag", Symbols.NEW_ID);
            }});
        
        sourceTestFile("testBasicInput.soar");
        agent.runFor(3, RunType.DECISIONS);
        assertEquals(2, listenerCallCount[0]);
        if(!match.called)
        {
            ifc.eval("matches testBasicInput");
        }
        assertTrue(match.called);
    }
    
    @Test
    public void testAddAndRemoveInputWme() throws Exception
    {
        final Wme[] wme = { null };
        agent.getEventManager().addListener(InputCycleEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                if(wme[0] == null)
                {
                    InputBuilder builder = InputBuilder.create(agent.io);
                    builder.add("location", "ann arbor").markWme("wme");
                    wme[0] = builder.getWme("wme");
                }
                else
                {
                    agent.io.removeInputWme(wme[0]);
                }
            }});
        
        sourceTestFile("testAddAndRemoveInputWme.soar");
        
        // First the "removed" production will fire because the WME isn't present yet
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(1, match.calls.size());
        assertEquals("removed", match.calls.get(0).get(0).toString());
        
        // Next the "added" production will fire because the WME has been added
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(2, match.calls.size());
        assertEquals("added", match.calls.get(1).get(0).toString());
        
        // Finally, "removed" fires again after the WME has been removed
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(3, match.calls.size());
        assertEquals("removed", match.calls.get(2).get(0).toString());
        
    }
    
    @Test
    public void testBasicOutput() throws Exception
    {
        
        final List<Set<Wme>> outputs = new ArrayList<Set<Wme>>();
        
        agent.getEventManager().addListener(InputCycleEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                if(agent.decisionCycle.d_cycle_count == 2)
                {
                    InputBuilder builder = InputBuilder.create(agent.io);
                    builder.add("retract-output", "*yes*");
                }
            }});
        agent.getEventManager().addListener(OutputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                OutputEvent oe = (OutputEvent) event;
                
                if(oe.getMode() == OutputMode.MODIFIED_OUTPUT_COMMAND)
                {
                    outputs.add(new HashSet<Wme>(oe.getWmes()));
                }
            }});
        
        sourceTestFile("testBasicOutput.soar");
        agent.runFor(3, RunType.DECISIONS);
        
        assertEquals(2, outputs.size());
        
        // Initially we'll have an output command with 5 WMEs, the output-link plus
        // the 4 created in our test production
        final Set<Wme> d1 = outputs.get(0);
        assertEquals(5, d1.size());
        final Identifier ol = agent.getInputOutput().getOutputLink();
        final Wme struct = Wmes.find(d1.iterator(), Wmes.newMatcher(agent.getSymbols(), ol, "struct", null));
        assertNotNull(struct);
        assertNotNull(Wmes.find(d1.iterator(), Wmes.newMatcher(agent.getSymbols(), struct.getValue().asIdentifier(), "name", "hello")));
        assertNotNull(Wmes.find(d1.iterator(), Wmes.newMatcher(agent.getSymbols(), ol, "a", 1)));
        assertNotNull(Wmes.find(d1.iterator(), Wmes.newMatcher(agent.getSymbols(), ol, "b", 2)));
        
        // After retract-output is added in the second decision cycle above, the
        // test production will retract and we'll be left with only the output-link
        // wme
        final Set<Wme> d2 = outputs.get(1);
        assertEquals(1, d2.size());
    }
    
    @Test
    public void testGetPendingCommands() throws Exception
    {
        final InputOutput io = agent.getInputOutput();
        new CycleCountInput(io, agent.getEventManager());
        sourceTestFile("testGetPendingCommands.soar");
        
        // Run one decision to get cycle count going
        agent.runFor(1, RunType.DECISIONS);
        
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(1, io.getPendingCommands().size());
        assertEquals(1, Iterators.size(io.getOutputLink().getWmes()));
        assertNotNull(Wmes.find(io.getPendingCommands().iterator(), Wmes.newMatcher(agent.getSymbols(), io.getOutputLink(), "first")));
        
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(1, io.getPendingCommands().size());
        assertEquals(2, Iterators.size(io.getOutputLink().getWmes()));
        assertNotNull(Wmes.find(io.getPendingCommands().iterator(), Wmes.newMatcher(agent.getSymbols(), io.getOutputLink(), "second")));
        
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(1, io.getPendingCommands().size());
        assertEquals(3, Iterators.size(io.getOutputLink().getWmes()));
        assertNotNull(Wmes.find(io.getPendingCommands().iterator(), Wmes.newMatcher(agent.getSymbols(), io.getOutputLink(), "third")));
        
        agent.runFor(1, RunType.DECISIONS);
        assertEquals(0, io.getPendingCommands().size());
        assertEquals(3, Iterators.size(io.getOutputLink().getWmes()));
    }
}
