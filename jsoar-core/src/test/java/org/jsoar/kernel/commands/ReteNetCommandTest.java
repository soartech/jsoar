package org.jsoar.kernel.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.FunctionalTests;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for the <pre>load rete-net --load</pre> command.
 * 
 * @author charles.newton
 * @see org.jsoar.kernel.commands.LoadCommand.ReteNet
 */
public class ReteNetCommandTest
{
    private Agent revivedAgent;
    private Agent originalAgent;
    private FunctionalTests funTests;

    @BeforeEach
    public void setUp() throws Exception
    {
        funTests = new FunctionalTests();
        funTests.setUp();
        originalAgent = funTests.agent;
        originalAgent.getTrace().disableAll();
        revivedAgent = new Agent();
        revivedAgent.getTrace().disableAll();
    }

    @AfterEach
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
    
    @Test
    @Timeout(value = 2 * 5, unit = TimeUnit.SECONDS)
    public void testWaterJug() throws Exception
    {
        runTest("testWaterJug", 416, 0);
    }
    
    @Test
    @Timeout(value = 2 * 10, unit = TimeUnit.SECONDS)
    public void testWaterJugLookAhead() throws Exception
    {
        runTest("testWaterJugLookAhead", 27, 2000);
    }
    
    @Test
    @Timeout(value = 2 * 10, unit = TimeUnit.SECONDS)
    public void testWaterJugHierarchy() throws Exception
    {
        runTest("testWaterJugHierarchy", 1093, 0);
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
    
    @Test
    @Timeout(value = 2 * 10, unit = TimeUnit.SECONDS)
    public void testEightPuzzle() throws Exception
    {
        runTest("testEightPuzzle", 40, 0);
    }
    
    @Test
    @Timeout(value = 2 * 10, unit = TimeUnit.SECONDS)
    public void testBlocksWorld() throws Exception
    {
        runTest("testBlocksWorld", 12, 0);
    }
 
    @Test
    @Timeout(value = 2 * 10, unit = TimeUnit.SECONDS)
    public void testBlocksWorldOperatorSubgoaling() throws Exception
    {
        runTest("testBlocksWorldOperatorSubgoaling", 5, 0);
    }
    
    @Test
    @Timeout(value = 2 * 10, unit = TimeUnit.SECONDS)
    public void testBlocksWorldLookAhead() throws Exception
    {
        runTest("testBlocksWorldLookAhead", 27, 1);
    }
    
    @Test
    public void testBlocksWorldLookAhead2() throws Exception
    {
        runTest("testBlocksWorldLookAhead", 29, 100000000002L);
    }
    
    @Test
    @Timeout(value = 2 * 10, unit = TimeUnit.SECONDS)
    public void testBlocksWorldLookAheadRandom() throws Exception
    {
        runTest("testBlocksWorldLookAhead", 32, 0);
    }
    
    @Test
    @Timeout(value = 2 * 80, unit = TimeUnit.SECONDS)
    public void testArithmetic() throws Exception
    {
        runTest("testArithmetic", 41982, 0);
    } 
    
    @Test
    @Timeout(value = 2 * 80, unit = TimeUnit.SECONDS)
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
        funTests.agent.getInterpreter().eval("save rete-net -s test.jrete");
        funTests.installRHS(funTests.agent);
        funTests.runTestExecute(testName, expectedDecisions);
        revivedAgent.getInterpreter().eval("load rete-net -l test.jrete");
        funTests.installRHS(revivedAgent);
        revivedAgent.getRandom().setSeed(randSeed);
        funTests.agent = revivedAgent;
        funTests.runTestExecute(testName, expectedDecisions);
        funTests.agent = originalAgent;
    }
}
