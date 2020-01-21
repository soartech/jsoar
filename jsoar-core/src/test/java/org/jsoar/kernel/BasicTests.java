/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;


import org.junit.Test;

/**
 * @author ray
 */
public class BasicTests extends FunctionalTestHarness
{
    @Test
    public void testBasicElaborationAndMatch() throws Exception
    {
        runTest("testBasicElaborationAndMatch", 0);
    }
    
    @Test
    public void testInitialState() throws Exception
    {
        runTest("testInitialState", 0);
    }
}
