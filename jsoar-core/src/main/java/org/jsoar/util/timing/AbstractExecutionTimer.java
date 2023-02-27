/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

/**
 * Basic implementation of {@link ExecutionTimer}. The name property defaults
 * to {@link Object#toString()}. toString() is overloaded to return name.
 * 
 * @author ray
 */
public abstract class AbstractExecutionTimer implements ExecutionTimer
{
    private String name = super.toString();
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.timing.ExecutionTimer#getName()
     */
    @Override
    public String getName()
    {
        return name;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.timing.ExecutionTimer#getTotalSeconds()
     */
    @Override
    public double getTotalSeconds()
    {
        return getTotalMicroseconds() / 1000000.0;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.timing.ExecutionTimer#setName(java.lang.String)
     */
    @Override
    public ExecutionTimer setName(String name)
    {
        this.name = name;
        return this;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.timing.ExecutionTimer#update()
     */
    @Override
    public void update()
    {
        pause();
        start();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name;
    }
    
}
