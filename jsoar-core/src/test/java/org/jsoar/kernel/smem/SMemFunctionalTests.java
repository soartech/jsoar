/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 4, 2010
 */
package org.jsoar.kernel.smem;

import org.jsoar.kernel.FunctionalTestHarness;
import org.jsoar.kernel.Phase;
import org.junit.Test;

/**
 * @author ray
 */
public class SMemFunctionalTests extends FunctionalTestHarness
{
    @Test
    public void testSimpleCueBasedRetrieval() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleCueBasedRetrieval", 1);
    }
    
    @Test
    public void testSimpleNonCueBasedRetrieval() throws Exception
    {
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleNonCueBasedRetrieval", 2);
    }
    
    @Test
    public void testSimpleStore() throws Exception
    {
        
        agent.setStopPhase(Phase.OUTPUT);
        runTest("testSimpleStore", 2);
    }
}
