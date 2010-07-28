/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 27, 2010
 */
package org.jsoar.soarunit;

import org.jsoar.kernel.SoarException;

/**
 * @author ray
 */
public interface TestAgentFactory
{
    /**
     * @return a new, uninitialized test agent
     */
    TestAgent createTestAgent();
    
    /**
     * Open the debugger, pre-loaded with the given test for debugging.
     * 
     * @param test the test to load
     * @param exitOnClose if true, the debugger should cause the VM to exit when it's
     *      closed. Otherwise, the debugger should just clean up and hide itself
     * @throws SoarException
     * @throws InterruptedException
     */
    void debugTest(Test test, boolean exitOnClose) throws SoarException, InterruptedException;
}
