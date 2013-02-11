package org.jsoar.kernel.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.FunctionalTests;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the <pre>rete-net</pre> command.
 * 
 * @author charles.newton
 * @see org.jsoar.kernel.commands.ReteNetCommand
 */
public class ReteNetCommandTest
{
    private Agent revivedAgent;
    private Agent originalAgent;
    private FunctionalTests funTests;

    @Before
    public void setUp() throws Exception
    {
        funTests = new FunctionalTests();
        funTests.setUp();
        originalAgent = funTests.agent;
        originalAgent.getTrace().disableAll();
        revivedAgent = new Agent();
        revivedAgent.getTrace().disableAll();
        revivedAgent.initialize();
    }

    @After
    public void tearDown() throws Exception
    {
        funTests.tearDown();
        revivedAgent.dispose();
        File file = new File("test.jrete");
        if (file.exists())
        {
            file.delete();
        }
    }
    
    @Test(timeout=2*5000)
    public void testWaterJug() throws Exception
    {
        runTest("testWaterJug", -1, 0);
    }
    @Test(timeout=4*10000)
    public void testWaterJugLookAhead() throws Exception
    {
        runTest("testWaterJugLookAhead", -1, 0);
    }
    @Test(timeout=2*10000)
    public void testWaterJugHierarchy() throws Exception
    {
        runTest("testWaterJugHierarchy", -1, 0);
    }
    
    @Test
    public void testTowersOfHanoi() throws Exception
    {
        runTest("testTowersOfHanoi", 2048, 0);
    }
    
    @Test
    public void testTowersOfHanoiFast() throws Exception
    {
        runTest("testTowersOfHanoiFast", 2047, 0);
    }
    
    @Test(timeout=2*10000)
    public void testEightPuzzle() throws Exception
    {
        runTest("testEightPuzzle", -1, 0);
    }
    
    @Test(timeout=2*10000)
    public void testBlocksWorld() throws Exception
    {
        runTest("testBlocksWorld", -1, 0);
    }
 
    @Test(timeout=2*10000)
    public void testBlocksWorldOperatorSubgoaling() throws Exception
    {
        runTest("testBlocksWorldOperatorSubgoaling", 5, 0);
    }
    
    @Test(timeout=2*10000)
    public void testBlocksWorldLookAhead() throws Exception
    {
        runTest("testBlocksWorldLookAhead", 27, 1);
    }
    
    @Test
    public void testBlocksWorldLookAhead2() throws Exception
    {
        runTest("testBlocksWorldLookAhead", 29, 100000000002L);
    }
    
    @Test(timeout=2*10000)
    public void testBlocksWorldLookAheadRandom() throws Exception
    {
        runTest("testBlocksWorldLookAhead", -1, 0);
    }
    
    @Test(timeout=2*80000)
    public void testArithmetic() throws Exception
    {
        runTest("testArithmetic", -1, 0);
        assertTrue(originalAgent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue() > 40000);
        assertTrue(originalAgent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue() == originalAgent.getProperties().get(SoarProperties.D_CYCLE_COUNT).intValue());
    } 
    
    @Test(timeout=2*80000)
    public void testCountTest() throws Exception
    {
        runTest("testCountTest", 45047, 0);
        
        assertEquals(42, originalAgent.getProductions().getProductions(ProductionType.USER).size());
        assertEquals(15012, originalAgent.getProductions().getProductions(ProductionType.CHUNK).size());
        assertEquals(115136, originalAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        assertEquals(40039, originalAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        assertEquals(42, originalAgent.getProductions().getProductions(ProductionType.USER).size());
        assertEquals(15012, originalAgent.getProductions().getProductions(ProductionType.CHUNK).size());
        assertEquals(115136, originalAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        assertEquals(40039, originalAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        assertEquals(120146, originalAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
        
        assertEquals(revivedAgent.getProductions().getProductions(ProductionType.USER).size(), originalAgent.getProductions().getProductions(ProductionType.USER).size());
        assertEquals(revivedAgent.getProductions().getProductions(ProductionType.CHUNK).size(), originalAgent.getProductions().getProductions(ProductionType.CHUNK).size());
        assertEquals(revivedAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        assertEquals(revivedAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        assertEquals(revivedAgent.getProductions().getProductions(ProductionType.USER).size(), originalAgent.getProductions().getProductions(ProductionType.USER).size());
        assertEquals(revivedAgent.getProductions().getProductions(ProductionType.CHUNK).size(), originalAgent.getProductions().getProductions(ProductionType.CHUNK).size());
        assertEquals(revivedAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        assertEquals(revivedAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        assertEquals(revivedAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
        assertEquals(revivedAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
        assertEquals(revivedAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
    }
    
    public void runTest(String testName, int expectedDecisions, long randSeed) throws Exception
    {
        funTests.runTestSetup(testName);
        funTests.agent.getRandom().setSeed(randSeed);
        funTests.agent.getInterpreter().eval("rete-net -s test.jrete");
        funTests.installRHS(funTests.agent);
        funTests.runTestExecute(testName, expectedDecisions);
        revivedAgent.getInterpreter().eval("rete-net -l test.jrete");
        funTests.installRHS(revivedAgent);
        revivedAgent.getRandom().setSeed(randSeed);
        funTests.agent = revivedAgent;
        funTests.runTestExecute(testName, expectedDecisions);
        funTests.agent = originalAgent;
    }
}
