/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 4, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.Iterator;
import java.util.LinkedList;
import org.jsoar.kernel.SoarConstants;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;

/**
 * info on conditions used for backtracing (and by the rete)
 *
 * <p>gdatastructs.h:495:bt_info
 *
 * @author ray
 */
public class BackTraceInfo implements Iterable<Preference> {
  public WmeImpl wme_; /* the actual wme that was matched */
  public int level; /* level (at firing time) of the id of the wme */
  public Preference trace; /* preference for BT, or NIL */

  private LinkedList<Preference> cdps; /* list of substate evaluation prefs to backtrace through,
                                             i.e. the context dependent preference set. */

  public BackTraceInfo() {}

  private BackTraceInfo(BackTraceInfo other) {
    this.wme_ = other.wme_;
    this.level = other.level;
    this.trace = other.trace;
    this.cdps = other.cdps;
  }

  /** @return A shallow copy of this backtrace into */
  public BackTraceInfo copy() {
    return new BackTraceInfo(this);
  }

  public void addContextDependentPreference(Preference pref) {
    if (cdps == null) {
      cdps = new LinkedList<Preference>();
    }
    cdps.push(pref);
    pref.preference_add_ref();
  }

  public boolean hasContextDependentPreferences() {
    return cdps != null && !cdps.isEmpty();
  }

  public void clearContextDependentPreferenceSet(final Adaptable context) {
    if (!hasContextDependentPreferences()) {
      return;
    }

    final RecognitionMemory recMemory = Adaptables.adapt(context, RecognitionMemory.class);

    Iterator<Preference> it = cdps.iterator();
    while (it.hasNext()) {
      Preference p = it.next();
      if (SoarConstants.DO_TOP_LEVEL_REF_CTS) {
        if (level > SoarConstants.TOP_GOAL_LEVEL) {
          p.preference_remove_ref(recMemory);
        }
      } else {
        p.preference_remove_ref(recMemory);
      }
      it.remove();
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<Preference> iterator() {
    return cdps.iterator();
  }
}
