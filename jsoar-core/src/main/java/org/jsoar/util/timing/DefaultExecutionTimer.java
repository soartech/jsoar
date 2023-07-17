/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.jsoar.util.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ExecutionTimer}. By default, uses ServiceLoader
 * to locate the implementation of {@link ExecutionTimeSource}. See API docs for
 * ServiceLoader.
 * 
 * @author ray
 */
public class DefaultExecutionTimer extends AbstractExecutionTimer
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultExecutionTimer.class);
    
    private ExecutionTimeSource source;
    private long start;
    private long total;
    
    /**
     * @return A new instance of this timer using the first source implementation
     * found by the ServiceLoader.
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
        // if we starting doing more of this
        ServiceLoader<ExecutionTimeSource> loader = ServiceLoader.load(ExecutionTimeSource.class);
        Iterator<ExecutionTimeSource> it = loader.iterator();
        if(it.hasNext())
        {
            source = it.next();
            LOG.info("Found ExecutionTimeSource implementation: {}", source.getClass().getCanonicalName());
        }
        else
        {
            LOG.warn("Could not dynmaically locate an implementation of ExecutionTimeSource. Using default WallclockExecutionTimeSource.");
            source = new WallclockExecutionTimeSource();
        }
    }
    
    private DefaultExecutionTimer(ExecutionTimeSource source)
    {
        Arguments.checkNotNull(source, "source");
        this.source = source;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.timing.ExecutionTimer#getElapsedMicroseconds()
     */
    @Override
    public long getTotalMicroseconds()
    {
        return total;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.timing.ExecutionTimer#pause()
     */
    @Override
    public void pause()
    {
        this.total += (source.getMicroseconds() - this.start);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.timing.ExecutionTimer#start()
     */
    @Override
    public void start()
    {
        this.start = source.getMicroseconds();
    }
    
    /*
     * (non-Javadoc)
     * 
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
