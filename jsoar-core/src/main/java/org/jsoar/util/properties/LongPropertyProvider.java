/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on June 25, 2009
 */
package org.jsoar.util.properties;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A fast, thread-safe property provider for a long value. Uses an
 * {@link AtomicLong} to provide safe access to the value. This
 * provider is intended for use with high-frequency counters.
 * 
 * @author ray
 */
public class LongPropertyProvider implements PropertyProvider<Long>
{
    public final PropertyKey<Long> key;
    
    /**
     * The current value. This value may be freely modified by owning
     * code as long as change events are not required.
     */
    public final AtomicLong value;
    
    public LongPropertyProvider(PropertyKey<Long> key)
    {
        this.key = key;
        this.value = new AtomicLong(key.getDefaultValue());
    }
    
    public void reset()
    {
        this.value.set(key.getDefaultValue().intValue());
    }
    
    public long increment()
    {
        return value.incrementAndGet();
    }
    
    public long longValue()
    {
        return value.get();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.properties.PropertyProvider#get()
     */
    @Override
    public Long get()
    {
        return value.get();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.properties.PropertyProvider#set(java.lang.Object)
     */
    @Override
    public Long set(Long value)
    {
        return this.value.getAndSet(value.intValue());
    }
    
    @Override
    public String toString()
    {
        return value.toString();
    }
}
