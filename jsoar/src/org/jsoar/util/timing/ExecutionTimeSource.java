/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

/**
 * An interface representing a time source used by timers. 
 * 
 * @author ray
 */
public interface ExecutionTimeSource
{
    /**
     * @return A monotonically increasing timestamp with microsecond resolution
     */
    long getMicroseconds();
}
