/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Iterator;

import org.jsoar.kernel.events.SoarEvent;

/**
 * callback.h:96:INPUT_WME_GARBAGE_COLLECTED_CALLBACK
 * 
 * @author ray
 */
public class InputWmeGarbageCollectedEvent implements SoarEvent
{
    private final WmeImpl headOfList;

    /**
     * @param headOfList
     */
    InputWmeGarbageCollectedEvent(WmeImpl headOfList)
    {
        this.headOfList = headOfList;
    }
    
    /**
     * @return Iterator over the list of removed WMEs
     */
    public Iterator<Wme> getRemovedWmes()
    {
        return new WmeIterator(headOfList);
    }
}
