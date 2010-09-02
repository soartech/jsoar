/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 1, 2010
 */
package org.jsoar.kernel.symbols;

import org.jsoar.kernel.GoalDependencySetImpl;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.learning.rl.ReinforcementLearningInfo;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.rete.MatchSetChange;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class GoalIdentifierInfo
{
    public final ListHead<MatchSetChange> ms_o_assertions = ListHead.newInstance(); /* dll of o assertions at this level */
    public final ListHead<MatchSetChange> ms_i_assertions = ListHead.newInstance(); /* dll of i assertions at this level */
    public final ListHead<MatchSetChange> ms_retractions = ListHead.newInstance();  /* dll of retractions at this level */
    public Slot operator_slot;
    public Preference preferences_from_goal = null;
    public IdentifierImpl higher_goal;
    public IdentifierImpl lower_goal;
    
    public GoalDependencySetImpl gds; // pointer to a goal's dependency set
    private WmeImpl impasse_wmes;
    
    /**
     * FIRING_TYPE that must be restored if Waterfall processing returns to this
     * level. See consistency.cpp
     */
    public SavedFiringType saved_firing_type = SavedFiringType.NO_SAVED_PRODS;
    
    
    // RL related structures
    public IdentifierImpl reward_header;        // pointer to reward_link
    public ReinforcementLearningInfo rl_info;   // various Soar-RL information

    public WmeImpl getImpasseWmes()
    {
        return impasse_wmes;
    }
    
    public void addImpasseWme(WmeImpl w)
    {
        this.impasse_wmes = w.addToList(this.impasse_wmes);
    }
    
    public void removeAllImpasseWmes()
    {
        this.impasse_wmes = null;
    }
    
    public void removeImpasseWme(WmeImpl w)
    {
        this.impasse_wmes = w.removeFromList(this.impasse_wmes);
    }
    
    public void addGoalPreference(Preference pref)
    {
        pref.all_of_goal_next = preferences_from_goal;
        pref.all_of_goal_prev = null;
        
        if(preferences_from_goal != null)
        {
            preferences_from_goal.all_of_goal_prev = pref;
        }
        preferences_from_goal = pref;
    }
    
    public void removeGoalPreference(Preference pref)
    {
        if(preferences_from_goal == pref)
        {
            preferences_from_goal = pref.all_of_goal_next;
            if(preferences_from_goal != null)
            {
                preferences_from_goal.all_of_goal_prev = null;
            }
        }
        else
        {
            pref.all_of_goal_prev.all_of_goal_next = pref.all_of_goal_next;
            if(pref.all_of_goal_next != null)
            {
                pref.all_of_goal_next.all_of_goal_prev = pref.all_of_goal_prev;
            }
        }
        pref.all_of_goal_next = pref.all_of_goal_prev = null;
    }

    public Preference popGoalPreference()
    {
        final Preference head = preferences_from_goal;
        if(head != null)
        {
            removeGoalPreference(head);
        }
        return head;
    }
}
