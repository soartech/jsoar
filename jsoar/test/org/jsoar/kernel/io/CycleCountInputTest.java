/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.io;


import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class CycleCountInputTest
{
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testCycleCountInput() throws Exception
    {
        final List<Integer> matches = new ArrayList<Integer>();
        agent.getRhsFunctions().registerHandler(new AbstractRhsFunctionHandler("match") {

            @Override
            public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
            {
                matches.add(arguments.get(0).asInteger().getValue());
                return null;
            }});
        CycleCountInput input = new CycleCountInput(agent.getInputOutput(), agent.getEventManager());
        
        agent.decider.setWaitsnc(true);
        agent.loadProduction("testCycleCountInput " +
        		"(state <s> ^superstate nil ^io.input-link.cycle-count <cc>)" +
        		"-->" +
        		"(match <cc>)");
        
        final int n = 50;
        agent.runFor(n, RunType.DECISIONS);
        
        assertEquals(n - 1, matches.size());
        
        int expected = 1;
        for(Integer i : matches)
        {
            assertEquals(expected++, i.intValue());
        }
    }
}
