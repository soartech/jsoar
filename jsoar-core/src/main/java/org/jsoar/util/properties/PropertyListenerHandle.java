/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 5, 2009
 */
package org.jsoar.util.properties;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A handle for a registered property listener. The {@link #removeListener()}
 * method can be used to easily deregister the listener without having to
 * remember the key it was associated with, etc.
 * 
 * @see PropertyManager#addListener(PropertyKey, PropertyListener)
 * @author ray
 */
public class PropertyListenerHandle<T>
{
    private final PropertyManager manager;
    private final PropertyKey<T> key;
    private final PropertyListener<T> listener;
    private final AtomicBoolean added = new AtomicBoolean(true);
    
    /**
     * Construct a new handle. Only for use by PropertyManager. Assumes that
     * the listener is already added to the manager
     * 
     * @param manager the manager
     * @param key the key
     * @param listener the listener
     */
    PropertyListenerHandle(PropertyManager manager, PropertyKey<T> key,
            PropertyListener<T> listener)
    {
        this.manager = manager;
        this.key = key;
        this.listener = listener;
    }
    
    /**
     * Add the listener to the manager. Does nothing if the listener is
     * already added.
     * 
     * <p>This method may be called from any thread.
     */
    public void addListener()
    {
        if(!added.getAndSet(true))
        {
            manager.addListener(key, listener);
        }
    }
    
    /**
     * Remove the listener from the manager. Does nothing if the listener is
     * already removed.
     * 
     * <p>This method may be called from any thread.
     */
    public void removeListener()
    {
        if(added.getAndSet(false))
        {
            manager.removeListener(key, listener);
        }
    }
    
}
