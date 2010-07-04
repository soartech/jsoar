/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2010
 */
package org.jsoar.kernel.smem;

import org.jsoar.kernel.FunctionalTestHarness;
import org.junit.Test;

/**
 * @author ray
 */
public class SMemFunctionalTests extends FunctionalTestHarness
{
    @Test
    public void testSimpleCueBasedRetrieval() throws Exception
    {
        runTest("testSimpleCueBasedRetrieval", 2);
    }
    
    @Test
    public void testSimpleNonCueBasedRetrieval() throws Exception
    {
        runTest("testSimpleNonCueBasedRetrieval", 3);
    }
    
    @Test
    public void testSimpleStore() throws Exception
    {
        runTest("testSimpleStore", 3);
    }
}
