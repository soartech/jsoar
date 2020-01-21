/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;

/**
 * Provides storage and retrieval for a particular property in a
 * {@link PropertyManager}.
 * 
 * <p>Although {@link PropertyManager} will create a default implementation
 * of this interface the first time a property is set, a property provide 
 * may provide custom storage along with expanded constraints on the value
 * of the property.
 * 
 * <p>It is expected that the implementation of the {@link #get()} and
 * {@link #set(Object)} methods will provide synchronization. That is,
 * the owning property manager's state is sufficiently guarded, but
 * the property provider is not protected by default.
 * 
 * @author ray
 */
public interface PropertyProvider <T>
{
    /**
     * @return the current value of the property
     */
    T get();
    
    /**
     * @param value the new value of the property
     * @return the old value of the property
     * @throws IllegalArgumentException if the value is not appropriate
     */
    T set(T value);
}
