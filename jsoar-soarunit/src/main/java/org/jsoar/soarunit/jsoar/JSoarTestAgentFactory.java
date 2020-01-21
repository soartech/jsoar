/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 27, 2010
 */
package org.jsoar.soarunit.jsoar;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgent;
import org.jsoar.soarunit.TestAgentFactory;

/**
 * @author ray
 */
public class JSoarTestAgentFactory implements TestAgentFactory
{

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgentFactory#createTestAgent(org.jsoar.soarunit.Test)
     */
    @Override
    public TestAgent createTestAgent()
    {
        return new JSoarTestAgent();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgentFactory#debugTest(org.jsoar.soarunit.Test)
     */
    @Override
    public void debugTest(Test test, boolean exitOnClose) throws SoarException, InterruptedException
    {
        new JSoarTestAgent().debug(test, exitOnClose);
    }

}
