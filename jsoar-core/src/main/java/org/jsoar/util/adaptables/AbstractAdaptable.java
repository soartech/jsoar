/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.util.adaptables;

/**
 * Basic implementation of Adaptable interface. Sub-classes should first
 * check for any particular adapter types and then call the super
 * implementation of {@link #getAdapter(Class)} which
 * will handle all "this instanceof" tests as well as forwarding the
 * request to the context.
 * 
 * @author ray
 */
public class AbstractAdaptable implements Adaptable
{
    /*
     * (non-Javadoc)
     * 
     * @see com.soartech.simjr.Adaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class<?> klass)
    {
        return Adaptables.adapt(this, klass, false);
    }
    
}
