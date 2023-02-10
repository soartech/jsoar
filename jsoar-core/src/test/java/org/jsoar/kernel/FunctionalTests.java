/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author ray
 */
public class FunctionalTests extends FunctionalTestHarness
{
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testWaterJug() throws Exception
    {
        runTest("testWaterJug", -1);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWaterJugLookAhead() throws Exception
    {
        runTest("testWaterJugLookAhead", -1);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testWaterJugHierarchy() throws Exception
    {
        runTest("testWaterJugHierarchy", -1);
    }
    
    @Test
    public void testTowersOfHanoi() throws Exception
    {
        runTest("testTowersOfHanoi", 2048);
    }
    
    @Test
    public void testTowersOfHanoiFast() throws Exception
    {
        runTest("testTowersOfHanoiFast", 2047);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testEightPuzzle() throws Exception
    {
        runTest("testEightPuzzle", -1);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testBlocksWorld() throws Exception
    {
        runTest("testBlocksWorld", -1);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testBlocksWorldOperatorSubgoaling() throws Exception
    {
        runTest("testBlocksWorldOperatorSubgoaling", 5);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testBlocksWorldLookAhead() throws Exception
    {
        String testName = "testBlocksWorldLookAhead";
        runTestSetup(testName);
        agent.getRandom().setSeed(1);
        runTestExecute(testName, 27);
    }
    
    @Test
    public void testBlocksWorldLookAhead2() throws Exception
    {
        String testName = "testBlocksWorldLookAhead";
        runTestSetup(testName);
        agent.getRandom().setSeed(100000000002L);
        runTestExecute(testName, 29);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testBlocksWorldLookAheadRandom() throws Exception
    {
        runTest("testBlocksWorldLookAhead", -1);
    }
    
    @Test
    @Timeout(value = 80, unit = TimeUnit.SECONDS)
    public void testArithmetic() throws Exception
    {
        runTest("testArithmetic", -1);
        assertTrue(agent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue() > 40000);
    }
    
    @Test
    @Timeout(value = 80, unit = TimeUnit.SECONDS)
    public void testCountTest() throws Exception
    {
        runTest("testCountTest", 45047);
        assertEquals(42, agent.getProductions().getProductions(ProductionType.USER).size());
        assertEquals(15012, agent.getProductions().getProductions(ProductionType.CHUNK).size());
        assertEquals(115136, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        assertEquals(40039, agent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        assertEquals(120146, agent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
    }
}
