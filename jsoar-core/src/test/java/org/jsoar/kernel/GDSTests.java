/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author ray
 */
public class GDSTests extends FunctionalTestHarness
{
    @Test(timeout=10000)
    public void testGDSBug1144() throws Exception
    {
        runTest("testGDSBug1144", 7); // should halt not crash
    }
    
    @Test(timeout=10000)
    public void testGDSBug1011() throws Exception
    {
        runTest("testGDSBug1011", 8);
        assertEquals(19, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
    }
    
    @Test
    public void testSimple() throws Exception
    {
        runTest("testSimple", 5);
    }
}
