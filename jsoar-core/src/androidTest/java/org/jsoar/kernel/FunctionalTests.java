/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import junit.framework.Assert;

/**
 * @author ray
 */
public class FunctionalTests extends FunctionalTestHarness
{
    public void testWaterJug() throws Exception
    {
        runTest("testWaterJug", -1);
    }
    public void testWaterJugLookAhead() throws Exception
    {
        runTest("testWaterJugLookAhead", -1);
    }
    public void testWaterJugHierarchy() throws Exception
    {
        runTest("testWaterJugHierarchy", -1);
    }
    
    public void testTowersOfHanoi() throws Exception
    {
        runTest("testTowersOfHanoi", 2048);
    }
    
    public void testTowersOfHanoiFast() throws Exception
    {
        runTest("testTowersOfHanoiFast", 2047);
    }
    
    public void testEightPuzzle() throws Exception
    {
        runTest("testEightPuzzle", -1);
    }
    
    public void testBlocksWorld() throws Exception
    {
        runTest("testBlocksWorld", -1);
    }
    
    public void testBlocksWorldOperatorSubgoaling() throws Exception
    {
        runTest("testBlocksWorldOperatorSubgoaling", 5);
    }
    
    public void testBlocksWorldLookAhead() throws Exception
    {
        String testName = "testBlocksWorldLookAhead";
        runTestSetup(testName);
        agent.getRandom().setSeed(1);
        runTestExecute(testName, 27);
    }
    
    public void testBlocksWorldLookAhead2() throws Exception
    {
        String testName = "testBlocksWorldLookAhead";
        runTestSetup(testName);
        agent.getRandom().setSeed(100000000002L);
        runTestExecute(testName, 29);
    }
    
    public void testBlocksWorldLookAheadRandom() throws Exception
    {
        runTest("testBlocksWorldLookAhead", -1);
    }
    
    public void testArithmetic() throws Exception
    {
        runTest("testArithmetic", -1);
        Assert.assertTrue(agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue() > 40000);
    }
    
    public void testCountTest() throws Exception
    {
        runTest("testCountTest", 45047);
        Assert.assertEquals(42, agent.getProductions().getProductions(ProductionType.USER).size());
        Assert.assertEquals(15012, agent.getProductions().getProductions(ProductionType.CHUNK).size());
        Assert.assertEquals(115136, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        Assert.assertEquals(40039, agent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        Assert.assertEquals(120146, agent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
    }
}
