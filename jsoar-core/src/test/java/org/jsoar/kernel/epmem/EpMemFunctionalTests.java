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
}
