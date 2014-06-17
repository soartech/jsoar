/*
 * Copyright (c) 2012 Soar Technology, Inc.
 *
 * Created on Jan 18, 2013
 */
package org.jsoar.kernel.epmem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.runtime.ThreadedAgent;
import org.junit.Test;

/**
 * @author bob.marinier
 */
public class EpMemFunctionalTests extends FunctionalTestHarness
{
    @Test
    public void testCountEpMem() throws Exception
    {
        runTest("testCountEpMem", 1693);
    }
    
    @Test
    public void testHamilton() throws Exception
    {
        runTest("testHamilton", 2);
    }
    
    @Test
    public void testFilterEpMem() throws Exception
    {
        runTest("testFilterEpMem", 27);
    }
    
    @Test
    public void testAddCommand() throws Exception
    {
        runTest("testAddCommand", 27);
    }
    
    @Test
    public void testInclusions() throws Exception
    {
        runTest("testInclusions", 5);
    }
    
    @Test
    public void testDeliberateStorage() throws Exception
    {
        runTest("testDeliberateStorage", 7);
    }
    
    @Test
    public void testKB() throws Exception
    {
        runTest("testKB", 246);
    }
    
    @Test
    public void testSingleStoreRetrieve() throws Exception
    {
        runTest("testSingleStoreRetrieve", 2);
    }
    
    @Test
    public void testOddEven() throws Exception
    {
        runTest("testOddEven", 12);
    }
    
    @Test
    public void testBeforeEpMem() throws Exception
    {
    	runTest("testBeforeEpMem", 12);
    }
    
    @Test
    public void testAfterEpMem() throws Exception
    {
    	runTest("testAfterEpMem", 12);
    }
    
    @Test
    public void testAllNegQueriesEpMem() throws Exception
    {
    	runTest("testAllNegQueriesEpMem", 12);
    }
    
    @Test
    public void testBeforeAfterProhibitEpMem() throws Exception
    {
    	runTest("testBeforeAfterProhibitEpMem", 12);
    }
    
    @Test
    public void testMaxDoublePrecision_Irrational() throws Exception
    {
    	runTest("testMaxDoublePrecision-Irrational", 4);
    }
    
    @Test
    public void testMaxDoublePrecisionEpMem() throws Exception
    {
    	runTest("testMaxDoublePrecisionEpMem", 4);
    }
    
    @Test
    public void testNegativeEpisode() throws Exception
    {
    	runTest("testNegativeEpisode", 12);
    }
    
    @Test
    public void testNonExistingEpisode() throws Exception
    {
    	runTest("testNonExistingEpisode", 12);
    }
    
    @Test
    public void testSimpleFloatEpMem() throws Exception
    {
    	runTest("testSimpleFloatEpMem", 4);
    }
    
    @Test
    public void testCyclicQuery() throws Exception
    {
    	runTest("testCyclicQuery", 4);
    }
    
    @Test
    public void testWMELength_OneCycle() throws Exception
    {
    	runTest("testWMELength_OneCycle", 4);
    }
    
    @Test
    public void testWMELength_FiveCycle() throws Exception
    {
    	runTest("testWMELength_FiveCycle", 7);
    }
    
    @Test
    public void testWMELength_InfiniteCycle() throws Exception
    {
    	runTest("testWMELength_InfiniteCycle", 12);
    }
    
    @Test
    public void testWMELength_MultiCycle() throws Exception
    {
    	runTest("testWMELength_MultiCycle", 12);
    }
    
    @Test
    public void testWMActivation_Balance0() throws Exception
    {
    	runTest("testWMActivation_Balance0", 5);
    }
    
    @Test
    public void testEpMemEncodeOutput_NoWMA() throws Exception
    {
    	runTest("testEpMemEncodeOutput_NoWMA", 4);
    }
    
    @Test
    public void testEpMemEncodeOutput_WMA() throws Exception
    {
    	runTest("testEpMemEncodeOutput_WMA", 4);
    }
    
    @Test
    public void testEpMemEncodeSelection_NoWMA() throws Exception
    {
    	runTest("testEpMemEncodeSelection_NoWMA", 5);
    }
    
    @Test
    public void testEpMemEncodeSelection_WMA() throws Exception
    {
    	runTest("testEpMemEncodeSelection_WMA", 5);
    }
    
    @Test
    public void testEpMemYRemoval() throws Exception{
        runTest("testYRemoval", 9);
    }
    
    public void testEpMemSoarGroupTests() throws Exception
    {
        runTest("testEpMemSoarGroupTests", 140);
    }
    
    @Test
    public void readCSoarDB() throws Exception
    {
        agent.initialize();
        
        URL db = getClass().getResource("epmem-csoar-db.sqlite");
        assertNotNull("No CSoar db!", db);
        agent.getInterpreter().eval("epmem --set path " + db.getPath());
        agent.getInterpreter().eval("epmem --set append-database on");
        agent.getInterpreter().eval("epmem --reinit");
        
        String actualResult = agent.getInterpreter().eval("epmem --print 4");
        
        String expectedResult = "(<id0> ^counter 2 ^io <id1> ^name Factorization ^needs-factorization true ^number-to-factor 2 ^number-to-factor-int 2 ^operator <id2> ^operator* <id2> ^reward-link <id3> ^superstate nil ^type state ^using-epmem true)\n" +
                                "(<id1> ^input-link <id5> ^output-link <id4>)\n" +
                                "(<id2> ^name factor-number ^number-to-factor 2)\n";
                
        assertTrue("Unexpected output from CSoar database!", actualResult.equals(expectedResult));
    }
    
    @Test
    public void testMultiAgent() throws Exception
    {
        List<ThreadedAgent> agents = new ArrayList<ThreadedAgent>();
        
        for (int i = 1;i <= 250;i++)
        {
            ThreadedAgent t = ThreadedAgent.create("Agent " + i);
            t.getAgent().getTrace().setEnabled(true);
            String sourceName = getClass().getSimpleName() + "_testMultiAgent.soar";
            URL sourceUrl = getClass().getResource(sourceName);
            assertNotNull("Could not find test file " + sourceName, sourceUrl);
            t.getAgent().getInterpreter().source(sourceUrl);
            
            agents.add(t);
        }
        
        for (ThreadedAgent a : agents)
        {
            a.runFor(3+1, RunType.DECISIONS);
        }
        
        boolean allStopped = false;
        while (!allStopped)
        {
            allStopped = true;
            
            for (ThreadedAgent a : agents)
            {
                if (a.isRunning())
                {
                    allStopped = false;
                    break;
                }
            }
        }
        
        for (ThreadedAgent a : agents)
        {
            if (a.getAgent().getProperties().get(SoarProperties.DECISION_PHASES_COUNT).intValue() != 3)
            {
                throw new AssertionError("Agent did not stop correctly! Ran too many cycles!");
            }
            
            String result = a.getAgent().getInterpreter().eval("epmem");
            
            if (!result.contains("native"))
            {
                throw new AssertionError("Non Native Driver!");
            }
        }
    }
}
