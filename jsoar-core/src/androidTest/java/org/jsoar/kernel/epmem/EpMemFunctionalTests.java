/*
 * Copyright (c) 2012 Soar Technology, Inc.
 *
 * Created on Jan 18, 2013
 */
package org.jsoar.kernel.epmem;

import android.util.Log;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.runtime.ThreadedAgent;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author bob.marinier
 */
public class EpMemFunctionalTests extends FunctionalTestHarness
{
    private static final Logger logger = LoggerFactory.getLogger(EpMemFunctionalTests.class);
	
	@Test
    public void testCountEpMem() throws Exception
    {
        runTest("testCountEpMem", 1693);
    }
    
    public void testHamilton() throws Exception
    {
        runTest("testHamilton", 2);
    }
    
    public void testFilterEpMem() throws Exception
    {
        runTest("testFilterEpMem", 103);
    }
    
    public void testAddCommand() throws Exception
    {
        runTest("testAddCommand", 27);
    }
    
    public void testInclusions() throws Exception
    {
        runTest("testInclusions", 5);
    }
    
    public void testDeliberateStorage() throws Exception
    {
        runTest("testDeliberateStorage", 7);
    }
    
    public void testKB() throws Exception
    {
        runTest("testKB", 246);
    }
    
    public void testSingleStoreRetrieve() throws Exception
    {
        runTest("testSingleStoreRetrieve", 2);
    }
    
    public void testOddEven() throws Exception
    {
        runTest("testOddEven", 12);
    }
    
    public void testBeforeEpMem() throws Exception
    {
    	runTest("testBeforeEpMem", 12);
    }
    
    public void testAfterEpMem() throws Exception
    {
    	runTest("testAfterEpMem", 12);
    }
    
    public void testAllNegQueriesEpMem() throws Exception
    {
    	runTest("testAllNegQueriesEpMem", 12);
    }
    
    public void testBeforeAfterProhibitEpMem() throws Exception
    {
    	runTest("testBeforeAfterProhibitEpMem", 12);
    }
    
    public void testMaxDoublePrecision_Irrational() throws Exception
    {
    	runTest("testMaxDoublePrecision-Irrational", 4);
    }
    
    public void testMaxDoublePrecisionEpMem() throws Exception
    {
    	runTest("testMaxDoublePrecisionEpMem", 4);
    }
    
    public void testNegativeEpisode() throws Exception
    {
    	runTest("testNegativeEpisode", 12);
    }
    
    public void testNonExistingEpisode() throws Exception
    {
    	runTest("testNonExistingEpisode", 12);
    }
    
    public void testSimpleFloatEpMem() throws Exception
    {
    	runTest("testSimpleFloatEpMem", 4);
    }
    
    public void testCyclicQuery() throws Exception
    {
    	runTest("testCyclicQuery", 4);
    }
    
    public void testWMELength_OneCycle() throws Exception
    {
    	runTest("testWMELength_OneCycle", 4);
    }
    
    public void testWMELength_FiveCycle() throws Exception
    {
    	runTest("testWMELength_FiveCycle", 7);
    }
    
    public void testWMELength_InfiniteCycle() throws Exception
    {
    	runTest("testWMELength_InfiniteCycle", 12);
    }
    
    public void testWMELength_MultiCycle() throws Exception
    {
    	runTest("testWMELength_MultiCycle", 12);
    }
    
    public void testWMActivation_Balance0() throws Exception
    {
    	runTest("testWMActivation_Balance0", 5);
    }
    
    public void testEpMemEncodeOutput_NoWMA() throws Exception
    {
    	runTest("testEpMemEncodeOutput_NoWMA", 4);
    }
    
    public void testEpMemEncodeOutput_WMA() throws Exception
    {
    	runTest("testEpMemEncodeOutput_WMA", 4);
    }
    
    public void testEpMemEncodeSelection_NoWMA() throws Exception
    {
    	runTest("testEpMemEncodeSelection_NoWMA", 5);
    }
    
    public void testEpMemEncodeSelection_WMA() throws Exception
    {
    	runTest("testEpMemEncodeSelection_WMA", 5);
    }
    
    public void testEpMemYRemoval() throws Exception{
        runTest("testYRemoval", 9);
    }
    
    public void testEpMemSoarGroupTests() throws Exception
    {
        runTest("testEpMemSoarGroupTests", 140);
    }
    
    @Ignore("Not currently compatible with CSoar db's. TODO: check whether it's just a sqlite version difference issue.")
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
                
        logger.info("Epmem test actual result: " + actualResult);
        assertTrue("Unexpected output from CSoar database! ", actualResult.equals(expectedResult));
    }
    
    @Ignore("db driver is now always native, so no longer specifies 'native' in version number, so the test fails when it shouldn't")
    @Test
    public void testMultiAgent() throws Exception
    {
        List<ThreadedAgent> agents = new ArrayList<>();

        for (int i = 1;i <= 1;i++)
        {
            final ThreadedAgent t = ThreadedAgent.create("Agent " + i, getContext());
            t.getAgent().getTrace().setEnabled(true);
            String sourceName = getClass().getSimpleName() + "_testMultiAgent.soar";
            URL sourceUrl = getClass().getResource(sourceName);
            assertNotNull("Could not find test file " + sourceName, sourceUrl);
            t.getAgent().getInterpreter().source(sourceUrl);
            t.getAgent().getPrinter().addPersistentWriter(new Writer(agent) {

                String agent = t.getName();

                @Override
                public void close() throws IOException {

                }

                @Override
                public void flush() throws IOException {

                }

                @Override
                public void write(char[] buf, int offset, int count) throws IOException {
                    Log.i(agent, String.valueOf(buf, offset, count));
                }
            });
            agents.add(t);
        }

        for (ThreadedAgent a : agents)
        {
            a.runFor(3+1, RunType.DECISIONS);
        }

        boolean allStopped = false;
        while (!agents.isEmpty())
        {
            allStopped = true;

            Iterator<ThreadedAgent> iter = agents.iterator();
            while(iter.hasNext()){
                ThreadedAgent agent = iter.next();
                if(!agent.isRunning()){
                    agent.dispose();
                    iter.remove();
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
