/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 22, 2009
 */
package org.jsoar.util.markers;

/**
 * Default implementation of {@link Marker}
 * 
 * @author ray
 */
public class DefaultMarker implements Marker
{
    /**
     * Get_new_tc_number() is called from lots of places. Any time we need
     * to mark a set of identifiers and/or variables, we get a new tc_number
     * by calling this routine, then proceed to mark various ids or vars
     * by setting the {@code sym->id.tc_num} or {@code sym->var.tc_num} fields.
     * 
     * <p>A global tc number counter is maintained and incremented by this
     * routine in order to generate a different tc_number each time. If
     * the counter ever wraps around back to 0, we bump it up to 1 and
     * reset the the tc_num fields on all existing identifiers and variables
     * to 0.
     * 
     * @return a new default marker
     */
    public static DefaultMarker create()
    {
        return new DefaultMarker();
    }
    
    private DefaultMarker()
    {
    }
}
