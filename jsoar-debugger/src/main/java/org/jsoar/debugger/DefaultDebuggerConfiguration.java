/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 24, 2009
 */
package org.jsoar.debugger;

/**
 * Default implementation of {@link JSoarDebuggerConfiguration}. This class 
 * performs a System.exit() when the debugger exits.
 * 
 * @author ray
 */
public class DefaultDebuggerConfiguration implements JSoarDebuggerConfiguration
{

    /* (non-Javadoc)
     * @see org.jsoar.debugger.JSoarDebuggerConfiguration#exit()
     */
    @Override
    public void exit()
    {
        System.exit(0);
    }

}
