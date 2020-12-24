/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 7, 2008
 */
package org.jsoar.debugger;

/**
 * @author ray
 */
public interface JSoarDebuggerPlugin
{

    void initialize(JSoarDebugger debugger, String[] args);
    
    default void shutdown() {};
}
