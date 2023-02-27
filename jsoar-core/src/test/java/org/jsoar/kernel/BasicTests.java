/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;

import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class BasicTests extends FunctionalTestHarness
{
    @Test
    void testBasicElaborationAndMatch() throws Exception
    {
        runTest("testBasicElaborationAndMatch", 0);
    }
    
    @Test
    void testInitialState() throws Exception
    {
        runTest("testInitialState", 0);
    }
}
