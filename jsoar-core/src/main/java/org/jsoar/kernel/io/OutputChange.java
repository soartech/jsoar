/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 23, 2010
 */
package org.jsoar.kernel.io;

import org.jsoar.kernel.memory.Wme;

/**
 * @author ray
 */
public class OutputChange
{
    private final Wme wme;
    private final boolean added;
    
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
