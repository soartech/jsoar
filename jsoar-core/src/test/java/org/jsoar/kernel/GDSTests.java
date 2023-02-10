/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.SimpleMatcher;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.common.collect.Lists;

/**
 * @author ray
 */
public class GDSTests extends FunctionalTestHarness
{
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testGDSBug1144() throws Exception
    {
        runTest("testGDSBug1144", 7); // should halt not crash
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testGDSBug1011() throws Exception
    {
        runTest("testGDSBug1011", 8);
        assertEquals(19, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
    }
    
    @Test
    public void testSimple() throws Exception
    {
        runTest("testSimple", 5);
    }
    
    @Test
    public void testDoubleSupport() throws Exception
    {
        runTest("testDoubleSupport", 5);
        assertEquals(2, agent.getGoalStack().size());
        
        List<Goal> goals = agent.getGoalStack();
        GoalDependencySet gds = Adaptables.adapt(goals.get(0), GoalDependencySet.class);
        assertNull(gds, "Expected GDS for top state to be empty");
        
        gds = Adaptables.adapt(goals.get(1), GoalDependencySet.class);
        assertNotNull(gds, "Expected GDS for substate to be non-empty");
    }
    
    @Test
    public void testMultiLevel1() throws Exception
    {
        runTest("testMultiLevel1", 5);
        
        testMultiLevel();
    }
    
    @Test
    public void testMultiLevel2() throws Exception
    {
        runTest("testMultiLevel2", 5);
        
        testMultiLevel();
    }
    
    /**
     * 
     */
    private void testMultiLevel() throws Exception
    {
        final List<Goal> goals = agent.getGoalStack();
        assertEquals(3, goals.size(), "Unexpected number of states");
        
        // top state
        GoalDependencySet gds = Adaptables.adapt(goals.get(0), GoalDependencySet.class);
        assertNull(gds, "Expected first goal to have empty GDS");
        
        // first substate
        // (14: P1 ^name top)
        // (13: S1 ^problem-space P1)
        // (31: S3 ^attribute operator)
        // (32: S3 ^impasse no-change)
        // (16: S1 ^a a)
        // (17: S1 ^b b)
        // (18: S1 ^c c)
        // (23: S3 ^superstate S1)
        
        gds = Adaptables.adapt(goals.get(1), GoalDependencySet.class);
        assertNotNull(gds, "Expected second goal have non-empty GDS");
        
        Set<Wme> actual = new LinkedHashSet<Wme>(Lists.newArrayList(gds.getWmes()));
        
        final SimpleMatcher matcher = new SimpleMatcher();
        matcher.addProduction("expectedGDS \n" +
                "(<s3> ^attribute operator ^impasse no-change ^superstate <s1>) \n" +
                "(<s1> ^problem-space <p1> ^a a ^b b ^c c) \n" +
                "(<p1> ^name top) \n" +
                "--> \n" +
                "(write match)");
        
        for(Wme actualWme : actual)
        {
            matcher.addWme(actualWme);
        }
        
        // for debugging
        String s = matcher.getMatches("expectedGDS").toString();
        
        assertEquals(1, matcher.getNumberMatches("expectedGDS"), "expectedGDS didn't match: " + s);
        
        // reset matcher
        matcher.removeAllProductions();
        matcher.removeAllWmes();
        
        // second substate
        // (36: P2 ^name second)
        // (35: S3 ^problem-space P2)
        // (40: O2 ^name operator-2)
        // (42: S3 ^operator O2)
        // (53: S5 ^impasse no-change)
        // (38: S3 ^d d)
        // (39: S3 ^e e)
        // (44: S5 ^superstate S3)
        
        gds = Adaptables.adapt(goals.get(2), GoalDependencySet.class);
        assertNotNull(gds, "Expected third goal have non-empty GDS");
        actual = new LinkedHashSet<Wme>(Lists.newArrayList(gds.getWmes()));
        
        matcher.addProduction("expectedGDS \n" +
                "(<s5> ^impasse no-change ^superstate <s3>) \n" +
                "(<s3> ^problem-space <p2> ^operator <o2> ^d d ^e e) \n" +
                "(<p2> ^name second) \n" +
                "(<o2> ^name operator-2) \n" +
                "--> \n" +
                "(write match)");
        
        for(Wme actualWme : actual)
        {
            matcher.addWme(actualWme);
        }
        
        // for debugging
        s = matcher.getMatches("expectedGDS").toString();
        
        assertEquals(1, matcher.getNumberMatches("expectedGDS"), "expectedGDS didn't match: " + s);
    }
}
