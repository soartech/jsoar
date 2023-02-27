/*
 * Copyright (c) 2013  SoarTech, Inc.
 *
 * Created on Apr 25, 2013
 */
package org.jsoar.util.properties;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A fast, thread-safe property provider for a double value. Uses an
 * {@link AtomicLong} to provide safe access to the value.
 */
public class DoublePropertyProvider implements PropertyProvider<Double>
{
    public final PropertyKey<Double> key;
    
    /**
     * The current value (storage is done using bit conversion functions)
     */
    public final AtomicLong value;
    
    public DoublePropertyProvider(PropertyKey<Double> key)
    {
        this.key = key;
        this.value = new AtomicLong(Double.doubleToLongBits(key.getDefaultValue()));
    }
    
    public void reset()
    {
        this.value.set(Double.doubleToLongBits(key.getDefaultValue()));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.properties.PropertyProvider#get()
     */
    @Override
    public Double get()
    {
        return Double.longBitsToDouble(value.get());
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.util.properties.PropertyProvider#set(java.lang.Object)
     */
    @Override
    public Double set(Double value)
    {
        return Double.longBitsToDouble(this.value.getAndSet(Double.doubleToLongBits(value)));
    }
    
    @Override
    public String toString()
    {
        return Double.toString(Double.longBitsToDouble(value.get()));
    }
}
