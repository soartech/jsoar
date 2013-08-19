/*
 * Copyright (c) 2012 Soar Technology, Inc.
 *
 * Created on Jan 18, 2013
 */
package org.jsoar.kernel.epmem;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.jsoar.kernel.FunctionalTestHarness;
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
}
