/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 26, 2010
 */
package org.jsoar.kernel.modules;

import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;


/**
 * @author ray
 */
public class SoarModuleTest extends AndroidTestCase
{

    public void testCanInitializeWithAgentAsContext()
    {
        final Agent a = new Agent(getContext());
        final SoarModule m = new SoarModule();
        m.initialize(a);
        // If we get here without exceptions, everything's good
    }
}
