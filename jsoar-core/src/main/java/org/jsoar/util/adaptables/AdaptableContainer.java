/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 24, 2010
 */
package org.jsoar.util.adaptables;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ray
 */
public class AdaptableContainer extends AbstractAdaptable
{
    private final List<Object> adaptables = new ArrayList<Object>();
    
    public static AdaptableContainer from(Object ... objects)
    {
        final AdaptableContainer result = new AdaptableContainer();
        for(Object o : objects)
        {
            result.adaptables.add(o);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.adaptables.AbstractAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        return Adaptables.findAdapter(adaptables, klass);
    }
}
