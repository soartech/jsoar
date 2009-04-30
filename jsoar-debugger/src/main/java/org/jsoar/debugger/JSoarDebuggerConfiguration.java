/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 7, 2009
 */
package org.jsoar.debugger;

/**
 * Interface to customize behavior of debugger, especially when
 * embedded in other systems. Call {@link JSoarDebugger#setConfiguration(JSoarDebuggerConfiguration)}.
 * 
 * @author ray
 */
public interface JSoarDebuggerConfiguration
{

    /**
     * Last method called when the debugger is exiting. The default 
     * implementation would just call System.exit() but this is not desirable
     * for many situations so it may be customized here.
     */
    void exit();
    
}
