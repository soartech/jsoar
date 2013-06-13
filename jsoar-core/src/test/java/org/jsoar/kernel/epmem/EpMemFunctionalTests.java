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
}
