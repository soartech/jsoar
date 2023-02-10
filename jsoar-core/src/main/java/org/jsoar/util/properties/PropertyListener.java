/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;

/**
 * Interface implemented by objects interested in changes to properties in
 * a property manager.
 * 
 * @author ray
 */
public interface PropertyListener<T>
{
    /**
     * Called when the value of a property changes.
     * 
     * @param event The event
     */
    void propertyChanged(PropertyChangeEvent<T> event);
}
