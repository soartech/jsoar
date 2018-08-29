/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import com.google.common.collect.Lists;

import junit.framework.Assert;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.SimpleMatcher;
import org.jsoar.util.adaptables.Adaptables;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ray
 */
public class GDSTests extends FunctionalTestHarness
{
    public void testGDSBug1144() throws Exception
    {
        runTest("testGDSBug1144", 7); // should halt not crash
    }
    
    public void testGDSBug1011() throws Exception
    {
        runTest("testGDSBug1011", 8);
        Assert.assertEquals(19, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
    }
    
    public void testSimple() throws Exception
    {
        runTest("testSimple", 5);
    }
    
    public void testDoubleSupport() throws Exception
    {
        runTest("testDoubleSupport", 5);
        Assert.assertEquals(2, agent.getGoalStack().size());
        
        List<Goal> goals = agent.getGoalStack();
        GoalDependencySet gds = Adaptables.adapt(goals.get(0), GoalDependencySet.class);
        Assert.assertNull("Expected GDS for top state to be empty", gds);
        
        gds = Adaptables.adapt(goals.get(1), GoalDependencySet.class);
        Assert.assertNotNull("Expected GDS for substate to be non-empty", gds);
    }
    
    public void testMultiLevel1() throws Exception
    {
        runTest("testMultiLevel1", 5);
        
        internalTestMultiLevel();
    }

    public void testMultiLevel2() throws Exception
    {
        runTest("testMultiLevel2", 5);
        
        internalTestMultiLevel();
    }
    
    /**
     *
     */
    private void internalTestMultiLevel() throws Exception
    {
        final List<Goal> goals = agent.getGoalStack();
        Assert.assertEquals("Unexpected number of states", goals.size(), 3);
        
        // top state
        GoalDependencySet gds = Adaptables.adapt(goals.get(0), GoalDependencySet.class);
        Assert.assertNull("Expected first goal to have empty GDS", gds);

        
        // first substate        
        //      (14: P1 ^name top)
        //      (13: S1 ^problem-space P1)
        //      (31: S3 ^attribute operator)
        //      (32: S3 ^impasse no-change)
        //      (16: S1 ^a a)
        //      (17: S1 ^b b)
        //      (18: S1 ^c c)
        //      (23: S3 ^superstate S1)

        
        gds = Adaptables.adapt(goals.get(1), GoalDependencySet.class);
        Assert.assertNotNull("Expected second goal have non-empty GDS", gds);

        Set<Wme> actual = new LinkedHashSet<Wme>(Lists.newArrayList(gds.getWmes()));
                
        final SimpleMatcher matcher = new SimpleMatcher();
        matcher.addProduction("expectedGDS \n" +
                "(<s3> ^attribute operator ^impasse no-change ^superstate <s1>) \n" +
                "(<s1> ^problem-space <p1> ^a a ^b b ^c c) \n" +
                "(<p1> ^name top) \n" +
                "--> \n" +
                "(write match)");
        
        for (Wme actualWme : actual)
        {
            matcher.addWme(actualWme);
        }
        
        // for debugging
        String s = matcher.getMatches("expectedGDS").toString();

        Assert.assertEquals("expectedGDS didn't match: " + s, matcher.getNumberMatches("expectedGDS"), 1);
        
        // reset matcher
        matcher.removeAllProductions();
        matcher.removeAllWmes();

        // second substate
        //        (36: P2 ^name second)
        //        (35: S3 ^problem-space P2)
        //        (40: O2 ^name operator-2)
        //        (42: S3 ^operator O2)
        //        (53: S5 ^impasse no-change)
        //        (38: S3 ^d d)
        //        (39: S3 ^e e)
        //        (44: S5 ^superstate S3)
        
        gds = Adaptables.adapt(goals.get(2), GoalDependencySet.class);
        Assert.assertNotNull("Expected third goal have non-empty GDS", gds);
        actual = new LinkedHashSet<Wme>(Lists.newArrayList(gds.getWmes()));
        
        matcher.addProduction("expectedGDS \n" +
                "(<s5> ^impasse no-change ^superstate <s3>) \n" +
                "(<s3> ^problem-space <p2> ^operator <o2> ^d d ^e e) \n" +
                "(<p2> ^name second) \n" +
                "(<o2> ^name operator-2) \n" +
                "--> \n" +
                "(write match)");
        
        for (Wme actualWme : actual)
        {
            matcher.addWme(actualWme);
        }
        
        // for debugging
        s = matcher.getMatches("expectedGDS").toString();

        Assert.assertEquals("expectedGDS didn't match: " + s, matcher.getNumberMatches("expectedGDS"), 1);
    }
}
