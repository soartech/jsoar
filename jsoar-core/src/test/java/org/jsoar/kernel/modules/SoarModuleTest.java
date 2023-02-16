/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 26, 2010
 */
package org.jsoar.kernel.modules;

import org.jsoar.kernel.Agent;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class SoarModuleTest
{
    
    @Test
    void testCanInitializeWithAgentAsContext()
    {
        final Agent a = new Agent();
        final SoarModule m = new SoarModule();
        m.initialize(a);
        // If we get here without exceptions, everything's good
    }
}
