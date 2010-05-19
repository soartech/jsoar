/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2010
 */
package org.jsoar.kernel;

/**
 * @author ray
 */
public interface AgentRunController
{
    /**
     * Run this agent for the given number of steps with the given step type. 
     * The agent is run in the current thread.
     * 
     * @param n Number of steps. Ignored if runType is {@link RunType#FOREVER}.
     * @param runType The run type
     */
    public void runFor(long n, RunType runType);
}
