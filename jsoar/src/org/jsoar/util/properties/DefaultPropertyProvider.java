/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;

/**
 * Default implementation of {@link PropertyProvider}.
 * 
 * @author ray
 */
public class DefaultPropertyProvider <T> implements PropertyProvider <T>
{
    private T value;
    
    public DefaultPropertyProvider(PropertyKey<T> key)
    {
        this.value = key.getDefaultValue();
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.properties.PropertyProvider#get()
     */
    @Override
    public T get()
    {
        return value;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.properties.PropertyProvider#set(java.lang.Object)
     */
    @Override
    public T set(T value)
    {
        T oldValue = this.value;
        this.value = value;
        return oldValue;
    }

}
