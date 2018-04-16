package org.jsoar.kernel.commands;

import android.support.test.InstrumentationRegistry;
import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.FunctionalTests;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarProperties;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;

/**
 * Tests for the <pre>rete-net</pre> command.
 * 
 * @author charles.newton
 * @see org.jsoar.kernel.commands.ReteNetCommand
 */
public class ReteNetCommandTest extends AndroidTestCase
{
    private Agent revivedAgent;
    private Agent originalAgent;
    private FunctionalTests funTests;
    public void setUp() throws Exception
    {
        funTests = new FunctionalTests();
        funTests.setUp();
        originalAgent = funTests.agent;
        originalAgent.getTrace().disableAll();
        revivedAgent = new Agent(InstrumentationRegistry.getTargetContext());
        revivedAgent.getTrace().disableAll();
    }

    @Override
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
    
    public void testWaterJug() throws Exception
    {
        runTest("testWaterJug", 416, 0);
    }
    
    public void testWaterJugLookAhead() throws Exception
    {
        runTest("testWaterJugLookAhead", 27, 2000);
    }
    
    public void testWaterJugHierarchy() throws Exception
    {
        runTest("testWaterJugHierarchy", 1093, 0);
    }
    
    public void testTowersOfHanoi() throws Exception
    {
        runTest("testTowersOfHanoi", 2048, 0);
    }
    
    public void testTowersOfHanoiFast() throws Exception
    {
        runTest("testTowersOfHanoiFast", 2047, 0);
    }
    
    public void testEightPuzzle() throws Exception
    {
        runTest("testEightPuzzle", 40, 0);
    }
    
    public void testBlocksWorld() throws Exception
    {
        runTest("testBlocksWorld", 12, 0);
    }
 
    public void testBlocksWorldOperatorSubgoaling() throws Exception
    {
        runTest("testBlocksWorldOperatorSubgoaling", 5, 0);
    }
    
    public void testBlocksWorldLookAhead() throws Exception
    {
        runTest("testBlocksWorldLookAhead", 27, 1);
    }
    
    public void testBlocksWorldLookAhead2() throws Exception
    {
        runTest("testBlocksWorldLookAhead", 29, 100000000002L);
    }
    
    public void testBlocksWorldLookAheadRandom() throws Exception
    {
        runTest("testBlocksWorldLookAhead", 32, 0);
    }
    
    public void testArithmetic() throws Exception
    {
        runTest("testArithmetic", 41982, 0);
    } 
    
    public void testCountTest() throws Exception
    {
        runTest("testCountTest", 45047, 0);
        
        Assert.assertEquals(42, originalAgent.getProductions().getProductions(ProductionType.USER).size());
        Assert.assertEquals(15012, originalAgent.getProductions().getProductions(ProductionType.CHUNK).size());
        Assert.assertEquals(115136, originalAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        Assert.assertEquals(40039, originalAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        Assert.assertEquals(42, originalAgent.getProductions().getProductions(ProductionType.USER).size());
        Assert.assertEquals(15012, originalAgent.getProductions().getProductions(ProductionType.CHUNK).size());
        Assert.assertEquals(115136, originalAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        Assert.assertEquals(40039, originalAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        Assert.assertEquals(120146, originalAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());

        Assert.assertEquals(revivedAgent.getProductions().getProductions(ProductionType.USER).size(), originalAgent.getProductions().getProductions(ProductionType.USER).size());
        Assert.assertEquals(revivedAgent.getProductions().getProductions(ProductionType.CHUNK).size(), originalAgent.getProductions().getProductions(ProductionType.CHUNK).size());
        Assert.assertEquals(revivedAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        Assert.assertEquals(revivedAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        Assert.assertEquals(revivedAgent.getProductions().getProductions(ProductionType.USER).size(), originalAgent.getProductions().getProductions(ProductionType.USER).size());
        Assert.assertEquals(revivedAgent.getProductions().getProductions(ProductionType.CHUNK).size(), originalAgent.getProductions().getProductions(ProductionType.CHUNK).size());
        Assert.assertEquals(revivedAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
        Assert.assertEquals(revivedAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.PE_CYCLE_COUNT).intValue());
        Assert.assertEquals(revivedAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
        Assert.assertEquals(revivedAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
        Assert.assertEquals(revivedAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue(), originalAgent.getProperties().get(SoarProperties.INNER_E_CYCLE_COUNT).intValue());
    }
    
    public void runTest(String testName, int expectedDecisions, long randSeed) throws Exception
    {
        File filesDir = InstrumentationRegistry.getTargetContext().getFilesDir();

        funTests.runTestSetup(testName);
        funTests.agent.getRandom().setSeed(randSeed);
        funTests.agent.getInterpreter().eval("rete-net -s "+filesDir.getPath()+"test.jrete");
        funTests.installRHS(funTests.agent);
        funTests.runTestExecute(testName, expectedDecisions);
        revivedAgent.getInterpreter().eval("rete-net -l "+filesDir.getPath()+"test.jrete");
        funTests.installRHS(revivedAgent);
        revivedAgent.getRandom().setSeed(randSeed);
        funTests.agent = revivedAgent;
        funTests.runTestExecute(testName, expectedDecisions);
        funTests.agent = originalAgent;
    }
}
