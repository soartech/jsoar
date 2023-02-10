/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Goal;
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.GoalDependencySetImpl;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    @Override
    @BeforeEach
    public void setUp() throws Exception
    {
        super.setUp();
        
        this.agent = new Agent();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
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
        id.goalInfo = new GoalIdentifierInfo(id);
        assertNull(Adaptables.adapt(id, GoalDependencySet.class));
        id.goalInfo.gds = new GoalDependencySetImpl(id);
        assertSame(id.goalInfo.gds, Adaptables.adapt(id, GoalDependencySet.class));
    }
    
    @Test
    public void testFormatsLongTermIdentifiersCorrectly()
    {
        final IdentifierImpl id = syms.createIdentifier('S');
        id.smem_lti = 99;
        assertEquals("@S" + id.getNameNumber(), String.format("%s", id));
    }
    
    @Test
    public void testStateIsAdaptableToGoal()
    {
        final Identifier state = agent.getSymbols().findIdentifier('S', 1);
        assertNotNull(state);
        
        final Goal goal = Adaptables.adapt(state, Goal.class);
        assertNotNull(goal);
    }
    
    @Test
    public void testNonStatesAreNotAdaptableToGoal()
    {
        final Identifier id = agent.getSymbols().createIdentifier('Z');
        assertNotNull(id);
        assertFalse(id.isGoal());
        
        final Goal goal = Adaptables.adapt(id, Goal.class);
        assertNull(goal);
    }
    
    @Test
    public void testAdaptToGoalAndGetSelectedOperatorName() throws Exception
    {
        agent.getProductions().loadProduction("propose (state <s> ^superstate nil) --> (<s> ^operator.name test-operator)");
        agent.runFor(1, RunType.DECISIONS);
        
        final Goal state = agent.getGoalStack().get(0);
        assertNotNull(state);
        
        // just verify adaptability here..
        final Goal goal = Adaptables.adapt(state.getIdentifier(), Goal.class);
        assertNotNull(goal);
        
        // test the operator name
        assertEquals("test-operator", goal.getOperatorName().asString().getValue());
    }
}
