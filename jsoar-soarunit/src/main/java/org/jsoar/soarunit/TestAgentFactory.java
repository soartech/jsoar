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
    TestAgent createTestAgent();
    
    void debugTest(Test test) throws SoarException, InterruptedException;
}
