/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 4, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.LinkedList;

import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Wme;

/**
 * info on conditions used for backtracing (and by the rete)
 * 
 * gdatastructs.h:495:bt_info
 * 
 * @author ray
 */
public class BackTraceInfo
{
    public Wme wme_;               /* the actual wme that was matched */
    public int level;   /* level (at firing time) of the id of the wme */
    public Preference trace;        /* preference for BT, or NIL */

    /* mvp 5-17-94 */
    public final LinkedList<Preference> prohibits;  /* list of prohibit prefs to backtrace through */

    public BackTraceInfo()
    {
        prohibits = new LinkedList<Preference>();
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

}
