/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 28, 2010
 */
package org.jsoar.soarunit.sml;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgent;
import org.jsoar.soarunit.TestAgentFactory;

/**
 * @author ray
 */
public class SmlTestAgentFactory implements TestAgentFactory
{
    static
    {
        preloadSmlLibraries();
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgentFactory#createTestAgent()
     */
    @Override
    public TestAgent createTestAgent()
    {
        return new SmlTestAgent();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgentFactory#debugTest(org.jsoar.soarunit.Test, boolean)
     */
    @Override
    public void debugTest(Test test, boolean exitOnClose) throws SoarException,
            InterruptedException
    {
        // TODO Auto-generated method stub

    }

    private static void preloadSmlLibraries()
    {
        System.loadLibrary("ElementXML");
        System.loadLibrary("SoarKernelSML");
        System.loadLibrary("Java_sml_ClientInterface");        
    }
}
