/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 4, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.Iterator;
import java.util.LinkedList;

import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.WmeImpl;

/**
 * info on conditions used for backtracing (and by the rete)
 * 
 * gdatastructs.h:495:bt_info
 * 
 * @author ray
 */
public class BackTraceInfo implements Iterable<Preference>
{
    public WmeImpl wme_;               /* the actual wme that was matched */
    public int level;   /* level (at firing time) of the id of the wme */
    public Preference trace;        /* preference for BT, or NIL */

    private LinkedList<Preference> prohibits;  /* list of prohibit prefs to backtrace through */

    public BackTraceInfo()
    {
    }
    
    public BackTraceInfo(BackTraceInfo other)
    {
        this.wme_ = other.wme_;
        this.level = other.level;
        this.trace = other.trace;
        this.prohibits = other.prohibits;
    }
    
    /**
     * @return A shallow copy of this backtrace into
     */
    public BackTraceInfo copy()
    {
        return new BackTraceInfo(this);
    }

    public void addProhibit(Preference pref)
    {
        if(prohibits == null)
        {
            prohibits = new LinkedList<Preference>();
        }
        prohibits.push(pref);
        pref.preference_add_ref();
    }
    
    public boolean hasProhibits()
    {
        return prohibits != null && !prohibits.isEmpty();
    }
    
    public void clearProhibits()
    {
        if(prohibits != null)
        {
            prohibits.clear();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Preference> iterator()
    {
        return prohibits.iterator();
    }
}
