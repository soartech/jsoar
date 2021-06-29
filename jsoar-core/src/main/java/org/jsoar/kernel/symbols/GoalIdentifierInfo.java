/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 1, 2010
 */
package org.jsoar.kernel.symbols;

import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.jsoar.kernel.Goal;
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.GoalDependencySetImpl;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.learning.rl.ReinforcementLearningInfo;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.rete.MatchSetChange;
import org.jsoar.util.ListHead;
import org.jsoar.util.adaptables.AbstractAdaptable;

/** @author ray */
public class GoalIdentifierInfo extends AbstractAdaptable implements Goal {
  private final IdentifierImpl id;
  public final ListHead<MatchSetChange> ms_o_assertions =
      ListHead.newInstance(); /* dll of o assertions at this level */
  public final ListHead<MatchSetChange> ms_i_assertions =
      ListHead.newInstance(); /* dll of i assertions at this level */
  public final ListHead<MatchSetChange> ms_retractions =
      ListHead.newInstance(); /* dll of retractions at this level */
  public Slot operator_slot;
  public Preference preferences_from_goal = null;
  public IdentifierImpl higher_goal;
  public IdentifierImpl lower_goal;
  public boolean allow_bottom_up_chunks;

  public GoalDependencySetImpl gds; // pointer to a goal's dependency set
  private final List<WmeImpl> impasseWMEs = new ArrayList<>();

  /**
   * FIRING_TYPE that must be restored if Waterfall processing returns to this level. See
   * consistency.cpp
   */
  public SavedFiringType saved_firing_type = SavedFiringType.NO_SAVED_PRODS;

  public GoalIdentifierInfo(IdentifierImpl id) {
    this.id = id;
  }

  // RL related structures
  public IdentifierImpl reward_header; // pointer to reward_link
  public ReinforcementLearningInfo rl_info; // various Soar-RL information

  @NonNull
  public List<WmeImpl> getImpasseWmes() {
    return List.copyOf(impasseWMEs);
  }

  public void addImpasseWme(WmeImpl w) {
    impasseWMEs.add(0, w);
  }

  public void removeAllImpasseWmes() {
    impasseWMEs.clear();
  }

  public void removeImpasseWme(WmeImpl w) {
    impasseWMEs.remove(w);
  }

  public void addGoalPreference(Preference pref) {
    pref.all_of_goal_next = preferences_from_goal;
    pref.all_of_goal_prev = null;

    if (preferences_from_goal != null) {
      preferences_from_goal.all_of_goal_prev = pref;
    }
    preferences_from_goal = pref;
  }

  public void removeGoalPreference(Preference pref) {
    if (preferences_from_goal == pref) {
      preferences_from_goal = pref.all_of_goal_next;
      if (preferences_from_goal != null) {
        preferences_from_goal.all_of_goal_prev = null;
      }
    } else {
      pref.all_of_goal_prev.all_of_goal_next = pref.all_of_goal_next;
      if (pref.all_of_goal_next != null) {
        pref.all_of_goal_next.all_of_goal_prev = pref.all_of_goal_prev;
      }
    }
    pref.all_of_goal_next = pref.all_of_goal_prev = null;
  }

  public Preference popGoalPreference() {
    final var head = preferences_from_goal;
    if (head != null) {
      removeGoalPreference(head);
    }
    return head;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Goal#getIdentifier()
   */
  @Override
  public Identifier getIdentifier() {
    return id;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Goal#getOperator()
   */
  @Override
  public IdentifierImpl getOperator() {
    final List<WmeImpl> WMEs = operator_slot != null ? operator_slot.getWmes() : null;
    return WMEs != null && !WMEs.isEmpty() ? WMEs.get(0).value.asIdentifier() : null;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Goal#getOperatorName()
   */
  @Override
  public Symbol getOperatorName() {
    final IdentifierImpl op = getOperator();
    final var slot = Slot.find_slot(op, id.factory.findString("name"));

    return slot != null && !id.slots.getWmes().isEmpty() ? slot.getWmes().get(0).getValue() : null;
  }

  /* (non-Javadoc)
   * @see org.jsoar.util.adaptables.AbstractAdaptable#getAdapter(java.lang.Class)
   */
  @Override
  public Object getAdapter(Class<?> klass) {
    if (Identifier.class.equals(klass)) {
      return getIdentifier();
    } else if (GoalDependencySet.class.equals(klass)) {
      return gds;
    }
    return super.getAdapter(klass);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getIdentifier().toString();
  }
}
