/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.StandaloneRhsFunctionHandler;
import org.jsoar.kernel.symbols.Symbol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class CycleCountInputTest
{
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        this.agent = new Agent();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception
    {
    }
    
    @Test
    public void testCycleCountInput() throws Exception
    {
        final List<Long> matches = new ArrayList<Long>();
        agent.getRhsFunctions().registerHandler(new StandaloneRhsFunctionHandler("match")
        {
            
            @Override
            public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
            {
                matches.add(arguments.get(0).asInteger().getValue());
                return null;
            }
        });
        CycleCountInput input = new CycleCountInput(agent.getInputOutput());
        
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.getProductions().loadProduction("testCycleCountInput " +
                "(state <s> ^superstate nil ^io.input-link.cycle-count <cc>)" +
                "-->" +
                "(match <cc>)");
        
        final long n = 50;
        agent.runFor(n, RunType.DECISIONS);
        
        assertEquals(n, matches.size());
        
        int expected = 1;
        for(Long i : matches)
        {
            assertEquals(expected++, i.intValue());
        }
        
        input.dispose();
        
        agent.runFor(1, RunType.DECISIONS);
        
        // make sure the production doesn't fire again, i.e. that the wme has been removed
        assertEquals(n, matches.size());
        assertNull(Wmes.matcher(agent.getSymbols()).attr("cycle-count").find(agent.getInputOutput().getInputLink()));
    }
}
