/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jsoar.kernel.memory.DummyWme;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.adaptables.Adaptables;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author ray
 */
public class GDSTests extends FunctionalTestHarness
{
    @Test(timeout=10000)
    public void testGDSBug1144() throws Exception
    {
        runTest("testGDSBug1144", 7); // should halt not crash
    }
    
    @Test(timeout=10000)
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
        
        List<Identifier> goals = agent.getGoalStack();
        GoalDependencySet gds = Adaptables.adapt(goals.get(0), GoalDependencySet.class);
        assertTrue("Expected GDS for top state to be empty", gds == null);
        
        gds = Adaptables.adapt(goals.get(1), GoalDependencySet.class);
        assertTrue("Expected GDS for substate to be non-empty", gds != null);
    }
    
    @Test
    public void testMultilevel1() throws Exception
    {
        runTest("testMultilevel1", 5);
        
        testMultilevel();
    }

    @Test
    public void testMultilevel2() throws Exception
    {
        runTest("testMultilevel2", 5);
        
        testMultilevel();
    }
    
    /**
     * 
     */
    private void testMultilevel()
    {
        List<Identifier> goals = agent.getGoalStack();
        assertTrue("Unexpected number of states", goals.size() == 3);
        
        // top state
        GoalDependencySet gds = Adaptables.adapt(goals.get(0), GoalDependencySet.class);
        assertTrue("Expected first goal to have empty GDS", gds == null);

        // first substate
        gds = Adaptables.adapt(goals.get(1), GoalDependencySet.class);
        assertTrue("Expected second goal have non-empty GDS", gds != null);

        SymbolFactory sf = agent.getSymbols();
        
//      (14: P1 ^name top)
//      (13: S1 ^problem-space P1)
//      (31: S3 ^attribute operator)
//      (32: S3 ^impasse no-change)
//      (16: S1 ^a a)
//      (17: S1 ^b b)
//      (18: S1 ^c c)
//      (23: S3 ^superstate S1)
        
        Set<Wme> expected = DummyWme.create(sf, 
                Symbols.NEW_ID, "name", "top",
                Symbols.NEW_ID, "problem-space", Symbols.NEW_ID,
                Symbols.NEW_ID, "attribute", "operator",
                Symbols.NEW_ID, "impasse", "no-change",
                Symbols.NEW_ID, "a", "a",
                Symbols.NEW_ID, "b", "b",
                Symbols.NEW_ID, "c", "c",
                Symbols.NEW_ID, "superstate", Symbols.NEW_ID);
        
        // TODO: would be nice if I didn't have to construct the collection
        final Set<Wme> actual = new LinkedHashSet<Wme>(Lists.newArrayList(gds.getWmes()));
        
        // TODO: would be nice if this actually reported which wme didn't match
        assertTrue("Actual wmes don't match expected wmes", Wmes.equal(actual, expected, true));
        
        // second substate
        gds = Adaptables.adapt(goals.get(2), GoalDependencySet.class);
        assertTrue("Expected third goal have non-empty GDS", gds != null);
        
//        (36: P2 ^name second)
//        (35: S3 ^problem-space P2)
//        (40: O2 ^name operator-2)
//        (42: S3 ^operator O2)
//        (53: S5 ^impasse no-change)
//        (38: S3 ^d d)
//        (39: S3 ^e e)
//        (44: S5 ^superstate S3)
        
        Set<Wme> expected2 = new LinkedHashSet<Wme>();
        expected2.add(new DummyWme(sf.createIdentifier('P'), sf.createString("name"), sf.createString("second")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("problem-space"), sf.createIdentifier('P')));
        expected2.add(new DummyWme(sf.createIdentifier('O'), sf.createString("name"), sf.createString("operator-2")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("operator"), sf.createIdentifier('O')));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("impasse"), sf.createString("no-change")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("d"), sf.createString("d")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("e"), sf.createString("e")));
        expected2.add(new DummyWme(sf.createIdentifier('S'), sf.createString("superstate"), sf.createIdentifier('S')));
        
        // TODO: would be nice if didn't have to construct the collection
        final Set<Wme> actual2 = new LinkedHashSet<Wme>(Lists.newArrayList(gds.getWmes()));
        
        // TODO: would be nice if this actually reported which wme didn't match
        assertTrue("Actual wmes don't match expected wmes", Wmes.equal(actual2, expected2, true));
    }
}
