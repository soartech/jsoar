/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 21, 2008
 */
package org.jsoar.kernel;

/**
 * The various ways of running an agent.
 * 
 * @author ray
 * @see AgentRunController#runFor(long, RunType)
 */
public enum RunType
{
    /**
     * Run the agent by decisions.
     */
    DECISIONS,
    /**
     * Run the agent by elaboration
     */
    ELABORATIONS,
    /**
     * Run the agent forever. That is, run the agent until it halts or is
     * interrupted.
     */
    FOREVER,
    /**
     * Run the agent by individual phases
     */
    PHASES,
    /**
     * Run the agent by modifications of output.
     */
    MODIFICATIONS_OF_OUTPUT,
}
