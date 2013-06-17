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
        runTest("testHamilton", 3);
    }
    
    @Test
    public void testKB() throws Exception
    {
        runTest("testKB", 246);
    }
    
    @Test
    public void testSingleStoreRetrieve() throws Exception
    {
        //The success rule in this test is an elaboration.  The decision phase counter used
        //in the framework actually an output phase counter, so this test runs one more
        //"decision phase" than the debugger shows.  If this breaks, make sure that elaborations
        //happen after epmem does retrieval.  -ACN
        runTest("testSingleStoreRetrieve", 2 + 1);
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
    	runTest("testAllNegQueriesEpmem", 12);
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
}
