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
public class RLTests extends FunctionalTestHarness
{
    @Test
    public void testTemplateVariableNameBug1121() throws Exception
    {
        runTest("testTemplateVariableNameBug1121", 1);
        assertEquals(4, agent.getProperties().get(SoarProperties.E_CYCLE_COUNT).intValue());
    }
}
