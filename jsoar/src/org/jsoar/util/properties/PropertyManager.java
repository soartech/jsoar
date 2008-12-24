/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages a set of properties. Properties are key/value pairs.
 * 
 * @author ray
 */
public class PropertyManager
{
    @SuppressWarnings("unchecked")
    private final Map<PropertyKey<?>, PropertyProvider> properties = Collections.synchronizedMap(new HashMap<PropertyKey<?>, PropertyProvider>());
    @SuppressWarnings("unchecked")
    private final Map<PropertyKey<?>, List> listeners = Collections.synchronizedMap(new HashMap<PropertyKey<?>, List>());
    
    /**
     * Construct a new property manager.
     */
    public PropertyManager()
    {
    }

    /**
     * Add a listener for a particular property.
     * 
     * <p>This method may be called from any thread.
     * 
     * @param <T> The value type of the property
     * @param key The property key
     * @param listener The listener
     * @see PropertyKey
     * @see PropertyListener
     */
    public <T> void addListener(PropertyKey<T> key, PropertyListener<T> listener)
    {
        getListenersForKey(key).add(listener);
    }
    
    /**
     * Remove a listener.
     * 
     * <p>This method may be called from any thread.
     * 
     * @param <T> The value type of the property
     * @param key The key the listener was registered for
     * @param listener The listener
     */
    public <T> void removeListener(PropertyKey<T> key, PropertyListener<T> listener)
    {
        getListenersForKey(key).remove(listener);
    }
    
    /**
     * Set the property provider for a particular property. A property
     * change event will be fired for the property.
     * 
     * <p>This method may be called from any thread
     * 
     * @param <T> The property type 
     * @param key The property key
     * @param provider The provider
     * @see PropertyProvider
     */
    public <T> void setProvider(PropertyKey<T> key, PropertyProvider<T> provider)
    {
        properties.put(key, provider);
        firePropertyChanged(key, provider.get(), provider.get());
    }
    
    /**
     * Get the current value of a particular property.
     * 
     * <p>This method may be called from any thread
     * 
     * @param <T> The property type
     * @param key The property key
     * @return The current value of the property
     */
    public <T> T get(PropertyKey<T> key)
    {
        return getProvider(key).get();
    }
    
    /**
     * Set the value of a property. A property changed event will be 
     * fired. A default property provider will be created if one has
     * not already been set.  If the property is readonly, an exception will be
     * thrown.
     * 
     * <p>This method may be called from any thread.
     * 
     * @param <T> The property type
     * @param <V> Value type, sub-class of T
     * @param key The key
     * @param value The new value
     * @return the old value
     * @throws IllegalArgumentException if the value is not compatible with the
     *      property (out of range, null, etc)
     * @throws UnsupportedOperationException if the property is readonly
     */
    public <T, V extends T> T set(PropertyKey<T> key, V value)
    {
        if(key.isReadonly())
        {
            throw new UnsupportedOperationException("property '" + key + "' is readonly");
        }
        final T oldValue = getProvider(key).set(value);
        
        firePropertyChanged(key, value, oldValue);
        
        return oldValue;
    }

    private <T, V extends T> void firePropertyChanged(PropertyKey<T> key, V value, final T oldValue)
    {
        final PropertyChangeEvent<T> event = new PropertyChangeEvent<T>(key, oldValue, value);
        for(PropertyListener<T> listener : getListenersForKey(key))
        {
            listener.propertyChanged(event);
        }
    }
    
    private <T> PropertyProvider<T> getProvider(PropertyKey<T> key)
    {
        synchronized(properties)
        {
            @SuppressWarnings("unchecked")
            PropertyProvider<T> provider = (PropertyProvider<T>) properties.get(key);
            if(provider == null)
            {
                provider = new DefaultPropertyProvider<T>(key);
                properties.put(key, provider);
            }
            return provider;
        }
    }
    
    private <T> List<PropertyListener<T>> getListenersForKey(PropertyKey<T> key)
    {
        synchronized(listeners)
        {
            @SuppressWarnings("unchecked")
            List<PropertyListener<T>> list = (List<PropertyListener<T>>) listeners.get(key);
            if(list == null)
            {
                list = new CopyOnWriteArrayList<PropertyListener<T>>();
                listeners.put(key, list);
            }
            return list;
        }
    }
}
