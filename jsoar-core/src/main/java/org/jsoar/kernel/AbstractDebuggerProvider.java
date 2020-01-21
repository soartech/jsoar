/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 27, 2010
 */
package org.jsoar.kernel;

import java.util.HashMap;
import java.util.Map;

/**
 * Base implementation of {@link DebuggerProvider}
 * 
 * @author ray
 */
public abstract class AbstractDebuggerProvider implements DebuggerProvider
{
    private Map<String, Object> properties = new HashMap<String, Object>();

    /* (non-Javadoc)
     * @see org.jsoar.kernel.DebuggerProvider#getProperties()
     */
    @Override
    public synchronized Map<String, Object> getProperties()
    {
        return new HashMap<String, Object>(properties);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.DebuggerProvider#setProperties(java.util.Map)
     */
    @Override
    public synchronized void setProperties(Map<String, Object> props)
    {
        properties.putAll(props);
    }
}
