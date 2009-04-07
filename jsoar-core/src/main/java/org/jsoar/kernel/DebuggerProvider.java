/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel;

/**
 * @author ray
 */
public interface DebuggerProvider
{
    /**
     * Opens a debugger and attaches it to the given agent
     * 
     * @param agent the agent
     * @throws SoarException 
     */
    void openDebugger(Agent agent) throws SoarException;
}
