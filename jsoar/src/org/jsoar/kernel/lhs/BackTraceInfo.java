/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 4, 2008
 */
package org.jsoar.kernel.lhs;

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
    int level;   /* level (at firing time) of the id of the wme */
    Preference trace;        /* preference for BT, or NIL */

    /* mvp 5-17-94 */
    // TODO ::list *prohibits;          /* list of prohibit prefs to backtrace through */

}
