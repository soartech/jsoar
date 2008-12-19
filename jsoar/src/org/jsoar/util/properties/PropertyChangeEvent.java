/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;


/**
 * Event fired when the value of a property is changed.
 * 
 * @author ray
 */
public class PropertyChangeEvent<T>
{
    private final PropertyKey<T> key;
    private final T oldValue;
    private final T newValue;
    
    /**
     * Construct a new property change event
     * 
     * @param key The property key
     * @param oldValue the old value of the property
     * @param newValue the new value of the property
     */
    PropertyChangeEvent(PropertyKey<T> key, T oldValue, T newValue)
    {
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * @return the key of the property that is changing
     */
    public PropertyKey<T> getKey()
    {
        return key;
    }

    /**
     * @return the new value of the property
     */
    public T getNewValue()
    {
        return newValue;
    }

    /**
     * @return the old value of the property
     */
    public T getOldValue()
    {
        return oldValue;
    }

}
