/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.symbols;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterators;

/**
 * @author ray
 */
public class IdentifierImplTest extends JSoarTest
{
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        
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

    /**
     * Test method for {@link org.jsoar.kernel.symbols.IdentifierImpl#getWmes()}.
     */
    @Test
    public void testGetWmes() throws Exception
    {
        // Load a production that creates some WMEs off of S1, then
        // test iterating over them.
        agent.loadProduction("testGetWmes " +
        		"(state <s> ^superstate nil)" +
        		"-->" +
        		"(<s> ^test <w>)" +
        		"(<w> ^a 1 ^b 2 ^c 3 ^d 4)");
        
        agent.decisionCycle.run_for_n_decision_cycles(1);
        
        Identifier id = agent.getSymbols().findIdentifier('S', 1);
        assertNotNull(id);
        
        // Find the test wme
        Wme test = Wmes.find(id.getWmes(), Wmes.newMatcher(agent.getSymbols(), null, "test"));
        assertNotNull(test);
        
        // Now verify that all the sub-wmes are there.
        List<Wme> kids = new ArrayList<Wme>();
        final Identifier testId = test.getValue().asIdentifier();
        Iterators.addAll(kids, testId.getWmes());
        assertEquals(4, kids.size());
        assertNotNull(Wmes.find(kids.iterator(), Wmes.newMatcher(agent.getSymbols(), testId, "a", 1)));
        assertNotNull(Wmes.find(kids.iterator(), Wmes.newMatcher(agent.getSymbols(), testId, "b", 2)));
        assertNotNull(Wmes.find(kids.iterator(), Wmes.newMatcher(agent.getSymbols(), testId, "c", 3)));
        assertNotNull(Wmes.find(kids.iterator(), Wmes.newMatcher(agent.getSymbols(), testId, "d", 4)));
    }

}
