/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

/**
 * Default implementation of {@link ExecutionTimeSource} that returns timestamps
 * based on wall clock time, i.e. System.currentTimeMillis(). Timers that use
 * this source will not accurately measure the actual CPU time used, but only
 * the total wall clock time used.
 * 
 * @author ray
 */
public class WallclockExecutionTimeSource implements ExecutionTimeSource
{
    private long startMillis = System.currentTimeMillis();
    
    /* (non-Javadoc)
     * @see org.jsoar.util.timing.ExecutionTimeSource#getMicroseconds()
     */
    @Override
    public long getMicroseconds()
    {
        return (System.currentTimeMillis() - startMillis) * 1000;
    }
}
