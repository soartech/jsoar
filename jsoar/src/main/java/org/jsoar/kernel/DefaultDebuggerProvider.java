/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel;

/**
 * @author ray
 */
public class DefaultDebuggerProvider implements DebuggerProvider
{
    /* (non-Javadoc)
     * @see org.jsoar.kernel.DebuggerProvider#openDebugger()
     */
    @Override
    public void openDebugger(Agent agent)
    {
        throw new UnsupportedOperationException("No debugger provider registered on agent '" + agent + "'");
    }

}
