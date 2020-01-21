/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.jsoar.util.Arguments;

/**
 * Default implementation of {@link ExecutionTimer}. By default, uses ServiceLoader
 * to locate the implementation of {@link ExecutionTimeSource}. See API docs for
 * ServiceLoader.
 * 
 * @author ray
 */
public class DefaultExecutionTimer extends AbstractExecutionTimer
{
    private ExecutionTimeSource source;
    private long start;
    private long total;
    
    /**
     * @return A new instance of this timer using the first source implementation
     *    found by the ServiceLoader.
     */
    public static ExecutionTimer newInstance()
    {
        return new DefaultExecutionTimer();
    }
    
    /**
     * Create a new instance of an execution timer using the given time source
     * 
     * @param source The time source
     * @return New instance of an execution timer using the given time source
     * @throws IllegalArgumentException if source is <code>null</code>
     */
    public static ExecutionTimer newInstance(ExecutionTimeSource source)
    {
        return new DefaultExecutionTimer(source);
    }
    
    private DefaultExecutionTimer()
    {
        // http://commons.apache.org/discovery/ may be a nice alternative
        // is we starting doing more of this
        ServiceLoader<ExecutionTimeSource> loader = ServiceLoader.load(ExecutionTimeSource.class);
        Iterator<ExecutionTimeSource> it = loader.iterator();
        if(it.hasNext())
        {
            source = it.next();
        }
        else
        {
            throw new IllegalStateException("Could not locate an implementation of ExecutionTimeSource");
        }
    }
    
    /**
     * @param source
     */
    private DefaultExecutionTimer(ExecutionTimeSource source)
    {
        Arguments.checkNotNull(source, "source");
        this.source = source;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.timing.ExecutionTimer#getElapsedMicroseconds()
     */
    @Override
    public long getTotalMicroseconds()
    {
        return total;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.timing.ExecutionTimer#pause()
     */
    @Override
    public void pause()
    {
        this.total += (source.getMicroseconds() - this.start);
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.timing.ExecutionTimer#start()
     */
    @Override
    public void start()
    {
        this.start = source.getMicroseconds();
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.timing.ExecutionTimer#reset()
     */
    @Override
    public void reset()
    {
        this.total = 0;
    }

    ExecutionTimeSource __testGetSource()
    {
        return source;
    }
}
