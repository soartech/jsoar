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
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.GoalDependencySetImpl;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.util.adaptables.Adaptables;
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
        agent.getProductions().loadProduction("testGetWmes " +
        		"(state <s> ^superstate nil)" +
        		"-->" +
        		"(<s> ^test <w>)" +
        		"(<w> ^a 1 ^b 2 ^c 3 ^d 4)");
        
        agent.runFor(1, RunType.DECISIONS);
        
        final Identifier id = agent.getSymbols().findIdentifier('S', 1);
        assertNotNull(id);
        
        // Find the test wme
        final MatcherBuilder m = Wmes.matcher(agent);
        final Wme test = m.attr("test").find(id);
        assertNotNull(test);
        
        // Now verify that all the sub-wmes are there.
        List<Wme> kids = new ArrayList<Wme>();
        final Identifier testId = test.getValue().asIdentifier();
        Iterators.addAll(kids, testId.getWmes());
        assertEquals(4, kids.size());
        assertNotNull(m.reset().attr("a").value(1).find(kids));
        assertNotNull(m.reset().attr("b").value(2).find(kids));
        assertNotNull(m.reset().attr("c").value(3).find(kids));
        assertNotNull(m.reset().attr("d").value(4).find(kids));
    }

    @Test
    public void testIsAdaptableToGoalDependencySet()
    {
        final IdentifierImpl id = syms.createIdentifier('S');
        assertNull(Adaptables.adapt(id, GoalDependencySet.class));
        id.gds = new GoalDependencySetImpl(id);
        assertSame(id.gds, Adaptables.adapt(id, GoalDependencySet.class));
        
    }
}
