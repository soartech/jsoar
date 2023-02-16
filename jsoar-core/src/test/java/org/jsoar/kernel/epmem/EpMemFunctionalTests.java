/*
 * Copyright (c) 2012 Soar Technology, Inc.
 *
 * Created on Jan 18, 2013
 */
package org.jsoar.kernel.epmem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.runtime.ThreadedAgent;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bob.marinier
 */
class EpMemFunctionalTests extends FunctionalTestHarness
{
    private static final Logger LOG = LoggerFactory.getLogger(EpMemFunctionalTests.class);
    
    @Test
    void testCountEpMem() throws Exception
    {
        runTest("testCountEpMem", 1693);
    }
    
    @Test
    void testHamilton() throws Exception
    {
        runTest("testHamilton", 2);
    }
    
    @Test
    void testFilterEpMem() throws Exception
    {
        runTest("testFilterEpMem", 103);
    }
    
    @Test
    void testAddCommand() throws Exception
    {
        runTest("testAddCommand", 27);
    }
    
    @Test
    void testInclusions() throws Exception
    {
        runTest("testInclusions", 5);
    }
    
    @Test
    void testDeliberateStorage() throws Exception
    {
        runTest("testDeliberateStorage", 7);
    }
    
    @Test
    void testKB() throws Exception
    {
        runTest("testKB", 246);
    }
    
    @Test
    void testSingleStoreRetrieve() throws Exception
    {
        runTest("testSingleStoreRetrieve", 2);
    }
    
    @Test
    void testOddEven() throws Exception
    {
        runTest("testOddEven", 12);
    }
    
    @Test
    void testBeforeEpMem() throws Exception
    {
        runTest("testBeforeEpMem", 12);
    }
    
    @Test
    void testAfterEpMem() throws Exception
    {
        runTest("testAfterEpMem", 12);
    }
    
    @Test
    void testAllNegQueriesEpMem() throws Exception
    {
        runTest("testAllNegQueriesEpMem", 12);
    }
    
    @Test
    void testBeforeAfterProhibitEpMem() throws Exception
    {
        runTest("testBeforeAfterProhibitEpMem", 12);
    }
    
    @Test
    void testMaxDoublePrecision_Irrational() throws Exception
    {
        runTest("testMaxDoublePrecision-Irrational", 4);
    }
    
    @Test
    void testMaxDoublePrecisionEpMem() throws Exception
    {
        runTest("testMaxDoublePrecisionEpMem", 4);
    }
    
    @Test
    void testNegativeEpisode() throws Exception
    {
        runTest("testNegativeEpisode", 12);
    }
    
    @Test
    void testNonExistingEpisode() throws Exception
    {
        runTest("testNonExistingEpisode", 12);
    }
    
    @Test
    void testSimpleFloatEpMem() throws Exception
    {
        runTest("testSimpleFloatEpMem", 4);
    }
    
    @Test
    void testCyclicQuery() throws Exception
    {
        runTest("testCyclicQuery", 4);
    }
    
    @Test
    void testWMELength_OneCycle() throws Exception
    {
        runTest("testWMELength_OneCycle", 4);
    }
    
    @Test
    void testWMELength_FiveCycle() throws Exception
    {
        runTest("testWMELength_FiveCycle", 7);
    }
    
    @Test
    void testWMELength_InfiniteCycle() throws Exception
    {
        runTest("testWMELength_InfiniteCycle", 12);
    }
    
    @Test
    void testWMELength_MultiCycle() throws Exception
    {
        runTest("testWMELength_MultiCycle", 12);
    }
    
    @Test
    void testWMActivation_Balance0() throws Exception
    {
        runTest("testWMActivation_Balance0", 5);
    }
    
    @Test
    void testEpMemEncodeOutput_NoWMA() throws Exception
    {
        runTest("testEpMemEncodeOutput_NoWMA", 4);
    }
    
    @Test
    void testEpMemEncodeOutput_WMA() throws Exception
    {
        runTest("testEpMemEncodeOutput_WMA", 4);
    }
    
    @Test
    void testEpMemEncodeSelection_NoWMA() throws Exception
    {
        runTest("testEpMemEncodeSelection_NoWMA", 5);
    }
    
    @Test
    void testEpMemEncodeSelection_WMA() throws Exception
    {
        runTest("testEpMemEncodeSelection_WMA", 5);
    }
    
    @Test
    void testEpMemYRemoval() throws Exception
    {
        runTest("testYRemoval", 9);
    }
    
    public void testEpMemSoarGroupTests() throws Exception
    {
        runTest("testEpMemSoarGroupTests", 140);
    }
    
    @Test
    void readCSoarDB() throws Exception
    {
        agent.initialize();
        
        URL db = getClass().getResource("epmem-csoar-db.sqlite");
        assertNotNull(db, "No CSoar db!");
        agent.getInterpreter().eval("epmem --set path " + db.getPath());
        agent.getInterpreter().eval("epmem --set append-database on");
        agent.getInterpreter().eval("epmem --reinit");
        
        StringWriter sw = new StringWriter();
        agent.getPrinter().pushWriter(sw);
        agent.getInterpreter().eval("epmem --print 4");
        agent.getPrinter().popWriter();
        String actualResult = sw.toString();
        
        String expectedResult = "(<id0> ^counter 2 ^io <id1> ^name Factorization ^needs-factorization true ^number-to-factor 2 ^number-to-factor-int 2 ^operator <id2> ^operator* <id2> ^reward-link <id3> ^superstate nil ^type state ^using-epmem true)\n"
                +
                "(<id1> ^input-link <id5> ^output-link <id4>)\n" +
                "(<id2> ^name factor-number ^number-to-factor 2)\n";
        
        LOG.info("Epmem test actual result: " + actualResult);
        assertEquals(expectedResult, actualResult, "Unexpected output from CSoar database! ");
    }
    
    @Test
    void testMultiAgent() throws Exception
    {
        List<ThreadedAgent> agents = new ArrayList<>();
        
        for(int i = 1; i <= 250; i++)
        {
            ThreadedAgent t = ThreadedAgent.create("Agent " + i);
            t.getAgent().getTrace().setEnabled(true);
            String sourceName = getClass().getSimpleName() + "_testMultiAgent.soar";
            URL sourceUrl = getClass().getResource(sourceName);
            assertNotNull(sourceUrl, "Could not find test file " + sourceName);
            t.getAgent().getInterpreter().source(sourceUrl);
            
            agents.add(t);
        }
        
        for(ThreadedAgent a : agents)
        {
            a.runFor(3 + 1, RunType.DECISIONS);
        }
        
        boolean allStopped = false;
        while(!allStopped)
        {
            allStopped = true;
            
            for(ThreadedAgent a : agents)
            {
                if(a.isRunning())
                {
                    allStopped = false;
                    break;
                }
            }
        }
        
        for(ThreadedAgent a : agents)
        {
            if(a.getAgent().getProperties().get(SoarProperties.DECISION_PHASES_COUNT).intValue() != 3)
            {
                throw new AssertionError("Agent did not stop correctly! Ran too many cycles!");
            }
            
            StringWriter sw = new StringWriter();
            a.getPrinter().pushWriter(sw);
            a.getAgent().getInterpreter().eval("epmem");
            a.getPrinter().popWriter();
            String result = sw.toString();
            
            if(!result.contains("Native"))
            {
                throw new AssertionError("Non Native Driver!");
            }
        }
    }
}
