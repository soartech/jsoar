/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 23, 2010
 */
package org.jsoar.kernel.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.memory.Wme;

import com.google.common.collect.Lists;

/**
 * Represents a change on the output link of an agent. It is either a WME
 * being added or removed. Instances of this class are immutable.
 * 
 * @see OutputEvent
 * @author ray
 */
public class OutputChange
{
    private final Wme wme;
    private final boolean added;
    
    /**
     * OutputChanges provided by {@link OutputEvent#getChanges()} are not sorted,
     * i.e. they are in arbitrary order. This method sorts the changes by timetag.
     * 
     * @param changeIt Iterator over a set of changes
     * @return Sorted list of changes
     */
    public static List<OutputChange> sortByTimeTag(Iterator<OutputChange> changeIt)
    {
        final ArrayList<OutputChange> changes = Lists.newArrayList(changeIt);
        Collections.sort(changes, new Comparator<OutputChange>() {

            @Override
            public int compare(OutputChange o1, OutputChange o2)
            {
                return o1.getWme().getTimetag() - o2.getWme().getTimetag();
            }
            
        });
        return changes;
    }
    
    public OutputChange(Wme wme, boolean added)
    {
        this.wme = wme;
        this.added = added;
    }

    /**
     * @return the relevant WME
     */
    public Wme getWme()
    {
        return wme;
    }

    /**
     * @return {@code true} if the wme was added, {@code false} otherwise
     */
    public boolean isAdded()
    {
        return added;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return String.format("(%d: %s ^%s %s) (%s)", wme.getTimetag(), 
                            wme.getIdentifier(), wme.getAttribute(), wme.getValue(), 
                            added ? "+" : "-");
    }
}
