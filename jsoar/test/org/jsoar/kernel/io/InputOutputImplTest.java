/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.io;


import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.events.SoarEvent;
import org.jsoar.kernel.events.SoarEventListener;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class InputOutputImplTest extends JSoarTest
{
    private Agent agent;
    private SoarTclInterface ifc;
    
    private static class MatchFunction extends AbstractRhsFunctionHandler
    {
        boolean called = false;
        
        public MatchFunction() { super("match"); }
        
        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
         */
        @Override
        public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
        {
            called = true;
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
        ifc = new SoarTclInterface(agent);
        agent.getRhsFunctions().registerHandler(match = new MatchFunction());
        agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        ifc.dispose();
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
                        add("flag", null);
            }});
        
        sourceTestFile("testBasicInput.soar");
        agent.decisionCycle.run_for_n_decision_cycles(3);
        assertEquals(2, listenerCallCount[0]);
        if(!match.called)
        {
            ifc.eval("matches testBasicInput");
        }
        assertTrue(match.called);
    }
}
