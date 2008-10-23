/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.events;

import java.util.Iterator;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WmeIterator;
import org.jsoar.util.events.SoarEvent;

/**
 * <p>callback.h:96:INPUT_WME_GARBAGE_COLLECTED_CALLBACK
 * 
 * @author ray
 */
public class InputWmeGarbageCollectedEvent implements SoarEvent
{
    private final WmeImpl headOfList;

    /**
     * @param headOfList Head of list of removed WMEs
     */
    public InputWmeGarbageCollectedEvent(WmeImpl headOfList)
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
