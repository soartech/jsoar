/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 22, 2008
 */
package org.jsoar.util.properties;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fast, thread-safe property provider for an integer value. Uses an
 * {@link AtomicInteger} to provide safe access to the value. This
 * provider is intended for use with high-frequency counters.
 * 
 * @author ray
 */
public class IntegerPropertyProvider implements PropertyProvider<Integer>
{
    public final PropertyKey<Integer> key;
    
    /**
     * The current value. This value may be freely modified by owning
     * code as long as change events are not required.
     */
    public final AtomicInteger value;
    
    public IntegerPropertyProvider(PropertyKey<Integer> key)
    {
        this.key = key;
        this.value = new AtomicInteger(key.getDefaultValue());
    }
    
    public void reset()
    {
        this.value.set(key.getDefaultValue().intValue());
    }
    
    public int increment()
    {
        return value.incrementAndGet();
    }
    
    public int intValue()
    {
        return value.get();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.properties.PropertyProvider#get()
     */
    @Override
    public Integer get()
    {
        return value.get();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.properties.PropertyProvider#set(java.lang.Object)
     */
    @Override
    public Integer set(Integer value)
    {
        return this.value.getAndSet(value.intValue());
    }
    
    @Override
    public String toString()
    {
        return value.toString();
    }
}
