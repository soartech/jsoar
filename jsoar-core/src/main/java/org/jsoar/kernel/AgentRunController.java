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
     * Set the agent's stop phase. When run by decision, the agent will run
     * N decisions and then run by phase until just <b>before</b> the stop
     * phase.
     * 
     * <p>See <a href="http://code.google.com/p/soar/wiki/CommandLineInterface#set-stop-phase">set-stop-phase command</a>
     * 
     * @param phase the phase to stop before
     * @see SoarProperties#STOP_PHASE
     */
    void setStopPhase(Phase phase);
    
    /**
     * Returns the current stop phase
     * 
     * <p>See <a href="http://code.google.com/p/soar/wiki/CommandLineInterface#set-stop-phase">set-stop-phase command</a>
     * 
     * @return the current stop phase
     * @see SoarProperties#STOP_PHASE
     */
    Phase getStopPhase();
    
    /**
     * Run this agent for the given number of steps with the given step type. 
     * The agent is run in the current thread.
     * 
     * @param n Number of steps. Ignored if runType is {@link RunType#FOREVER}.
     * @param runType The run type
     */
    void runFor(long n, RunType runType);
}
