/*
 * Copyright (c) 2012 Soar Technology, Inc.
 *
 * Created on Jan 18, 2013
 */
package org.jsoar.kernel.epmem;

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
}
