/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;

import org.jsoar.kernel.learning.ReinforcementLearningInfo;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.MatchSetChange;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.TraceFormatRestriction;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;

/**
 * 
 * decide.cpp
 * @author ray
 */
public class Decider
{
    /**
     * agent.h:62
     * 
     * @author ray
     */
    public enum LinkUpdateType
    {
        UPDATE_LINKS_NORMALLY,
        UPDATE_DISCONNECTED_IDS_LIST,
        JUST_UPDATE_COUNT,
    }

    /**
     * A dll of instantiations that will be used to determine the gds through a
     * backtracing-style procedure, evaluate_gds in decide.cpp
     * 
     * instantiations.h:106:pi_struct
     * 
     * @author ray
     */
    private static class ParentInstantiation
    {
        ParentInstantiation next, prev;
        Instantiation inst;
    }

    private static final boolean DEBUG_GDS = false;
    private static final boolean DEBUG_GDS_HIGH = false;
    private static final boolean DEBUG_LINKS = false;
    
    private final Agent context;
    
    /**
     * agent.h:603:context_slots_with_changed_acceptable_preferences
     */
    private final ListHead<Slot> context_slots_with_changed_acceptable_preferences = ListHead.newInstance();
    /**
     * agent.h:615:promoted_ids
     */
    private final LinkedList<Identifier> promoted_ids = new LinkedList<Identifier>();

    /**
     * agent.h:616:link_update_mode
     */
    private LinkUpdateType link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;
    /**
     * agent.h:609:ids_with_unknown_level
     */
    private final ListHead<Identifier> ids_with_unknown_level = ListHead.newInstance();
    /**
     * agent.h:607:disconnected_ids
     */
    private final ListHead<Identifier> disconnected_ids = ListHead.newInstance();
    
    private int mark_tc_number;
    private int level_at_which_marking_started;
    private int highest_level_anything_could_fall_from;
    private int lowest_level_anything_could_fall_to;
    private int walk_tc_number;
    private int walk_level;
    
    public Identifier top_goal;
    public Identifier bottom_goal;
    public Identifier top_state;
    public Identifier prev_top_state;
    public Identifier active_goal;
    Identifier previous_active_goal;
    public int active_level;
    int previous_active_level;
    private boolean waitsnc;
    private boolean waitsnc_detect;
    
    /**
     * agent.h:384:parent_list_head
     */
    private ParentInstantiation parent_list_head;
    
    /**
     * @param context
     */
    public Decider(Agent context)
    {
        this.context = context;
    }
    
    /**
     * 
     * <p>chunk.cpp:753:find_goal_at_goal_stack_level
     * 
     * @param level
     * @return
     */
    public Identifier find_goal_at_goal_stack_level(int level)
    {
        for (Identifier g = top_goal; g != null; g = g.lower_goal)
            if (g.level == level)
                return (g);
        return null;
    }

    /**
     * Whenever some acceptable or require preference for a context slot
     * changes, we call mark_context_slot_as_acceptable_preference_changed().
     * 
     * decide.cpp:146:mark_context_slot_as_acceptable_preference_changed
     * 
     * @param s
     */
    public void mark_context_slot_as_acceptable_preference_changed(Slot s)
    {
        if (s.acceptable_preference_changed != null)
            return;

        AsListItem<Slot> dc = new AsListItem<Slot>(s);
        s.acceptable_preference_changed = dc;
        dc.insertAtHead(this.context_slots_with_changed_acceptable_preferences);
    } 

    /**
     * This updates the acceptable preference wmes for a single slot.
     * 
     * decide.cpp:158:do_acceptable_preference_wme_changes_for_slot
     * 
     * @param s
     */
    private void do_acceptable_preference_wme_changes_for_slot(Slot s)
    {
        // first, reset marks to "NOTHING"
        for (Wme w = s.getAcceptablePreferenceWmes(); w != null; w = w.next)
            w.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;

        // now mark values for which we WANT a wme as "CANDIDATE" values
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE).first; p != null; p = p.next)
            p.item.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE).first; p != null; p = p.next)
            p.item.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;

        // remove any existing wme's that aren't CANDIDATEs; mark the rest as
        // ALREADY_EXISTING

        Wme w = s.getAcceptablePreferenceWmes();
        while (w != null)
        {
            Wme next_w = w.next;
            if (w.value.decider_flag == DeciderFlag.CANDIDATE_DECIDER_FLAG)
            {
                w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG;
                w.value.decider_wme = w;
                w.preference = null; /* we'll update this later */
            }
            else
            {
                s.removeAcceptablePreferenceWme(w);
                
                /*
                 * IF we lose an acceptable preference for an operator, then
                 * that operator comes out of the slot immediately in OPERAND2.
                 * However, if the lost acceptable preference is not for item in
                 * the slot, then we don;t need to do anything special until
                 * mini-quiescence.
                 */
                if (context.operand2_mode)
                    context.consistency.remove_operator_if_necessary(s, w);

                context.workingMemory.remove_wme_from_wm(w);
            }
            w = next_w;
        }

        // add the necessary wme's that don't ALREADY_EXIST

        for (AsListItem<Preference> it = s.getFastPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE).first; it != null; it = it.next)
        {
            final Preference p = it.item;
            if (p.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG)
            {
                // found existing wme, so just update its trace
                Wme wme = p.value.decider_wme;
                if (wme.preference == null)
                    wme.preference = p;
            }
            else
            {
                Wme wme = context.workingMemory.make_wme(p.id, p.attr, p.value, true);
                s.addAcceptablePreferenceWme(wme);
                wme.preference = p;
                context.workingMemory.add_wme_to_wm(wme);
                p.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG;
                p.value.decider_wme = wme;
            }
        }

        for (AsListItem<Preference> it = s.getFastPreferenceList(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE).first; it != null; it = it.next)
        {
            final Preference p = it.item;
            if (p.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG)
            {
                // found existing wme, so just update its trace
                Wme wme = p.value.decider_wme;
                if (wme.preference == null)
                    wme.preference = p;
            }
            else
            {
                Wme wme = context.workingMemory.make_wme(p.id, p.attr, p.value, true);
                s.addAcceptablePreferenceWme(wme);
                wme.preference = p;
                context.workingMemory.add_wme_to_wm(wme);
                p.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG;
                p.value.decider_wme = wme;
            }
        }
    }
    

    /**
     * At the end of the phases, do_buffered_acceptable_preference_wme_changes()
     * is called to update the acceptable preference wmes. This should be called
     * *before* do_buffered_link_changes() and do_buffered_wm_changes().
     * 
     * decide.cpp:232:do_buffered_acceptable_preference_wme_changes
     */
    private void do_buffered_acceptable_preference_wme_changes()
    {
        while (!context_slots_with_changed_acceptable_preferences.isEmpty())
        {
            AsListItem<Slot> dc = context_slots_with_changed_acceptable_preferences.first;
            context_slots_with_changed_acceptable_preferences.first = dc.next;
            Slot s = dc.item;
            do_acceptable_preference_wme_changes_for_slot(s);
            s.acceptable_preference_changed = null;
        }
    }

    /**
     * Post a link addition for later processing.
     * 
     * decide.cpp:288:post_link_addition
     * 
     * @param from
     * @param to
     */
    public void post_link_addition(Identifier from, Identifier to)
    {
        // don't add links to goals/impasses, except the special one (NIL,goal)
        if ((to.isa_goal || to.isa_impasse) && from != null)
            return;

        to.link_count++;

        if (DEBUG_LINKS)
        {
            if (from != null)
                context.getPrinter().print("\nAdding link from %s to %s", from, to);
            else
                context.getPrinter().print("\nAdding special link to %s (count=%lu)", to, to.link_count);
        }
        
        if (from == null)
            return; /* if adding a special link, we're done */

        /* --- if adding link from same level, ignore it --- */
        if (from.promotion_level == to.promotion_level)
            return;

        /* --- if adding link from lower to higher, mark higher accordingly --- */
        if (from.promotion_level > to.promotion_level)
        {
            to.could_be_a_link_from_below = true;
            return;
        }

        /* --- otherwise buffer it for later --- */
        to.promotion_level = from.promotion_level;
        //symbol_add_ref (to);
        this.promoted_ids.push(to);
    }

    /**
     * decide.cpp:329:promote_if_needed
     * 
     * @param sym
     * @param new_level
     */
    private void promote_if_needed(Symbol sym, int new_level)
    {
        Identifier id = sym.asIdentifier();
        if (id != null)
            promote_id_and_tc(id, new_level);
    }

    /**
     * Promote an id and its transitive closure.
     * 
     * decide.cpp:333:promote_id_and_tc
     * 
     * @param id
     * @param new_level
     */
    private void promote_id_and_tc(Identifier id, /* goal_stack_level */int new_level)
    {

        // if it's already that high, or is going to be soon, don't bother
        if (id.level <= new_level)
            return;
        if (id.promotion_level < new_level)
            return;

        // update its level, etc.
        id.level = new_level;
        id.promotion_level = new_level;
        id.could_be_a_link_from_below = true;

        // sanity check
        if (id.isa_goal || id.isa_impasse)
        {
            throw new IllegalStateException("Internal error: tried to promote a goal or impasse id");
            /*
             * Note--since we can't promote a goal, we don't have to worry about
             * slot->acceptable_preference_wmes below
             */
        }

        // scan through all preferences and wmes for all slots for this id
        for (Wme w = id.getInputWmes(); w != null; w = w.next)
            promote_if_needed(w.value, new_level);
        
        for (AsListItem<Slot> s = id.slots.first; s != null; s = s.next)
        {
            for (Preference pref = s.item.getAllPreferences(); pref != null; pref = pref.next_of_slot)
            {
                promote_if_needed(pref.value, new_level);
                if (pref.type.isBinary())
                    promote_if_needed(pref.referent, new_level);
            }
            for (Wme w = s.item.getWmes(); w != null; w = w.next)
                promote_if_needed(w.value, new_level);
        }
    }

    /**
     * decide.cpp:375:do_promotion
     */
    private void do_promotion()
    {
        while (!promoted_ids.isEmpty())
        {
            Identifier to = promoted_ids.pop();
            promote_id_and_tc(to, to.promotion_level);
            // symbol_remove_ref (thisAgent, to);
        }
    }

    /**
     * Post a link removal for later processing
     * 
     * decide.cpp:424:post_link_removal
     * 
     * @param from
     * @param to
     */
    public void post_link_removal(Identifier from, Identifier to)
    {
        // don't remove links to goals/impasses, except the special one
        // (NIL,goal)
        if ((to.isa_goal || to.isa_impasse) && from != null)
            return;

        to.link_count--;

        if (DEBUG_LINKS)
        {
            if (from != null)
            {
                context.getPrinter().print("\nRemoving link from %s to %s (%d to %d)", from, to, from.level, to.level);
            }
            else
            {
                context.getPrinter().print("\nRemoving special link to %s  (%d)", to, to.level);
            }
            context.getPrinter().print(" (count=%lu)", to.link_count);
        }

        // if a gc is in progress, handle differently
        if (link_update_mode == LinkUpdateType.JUST_UPDATE_COUNT)
            return;

        if ((link_update_mode == LinkUpdateType.UPDATE_DISCONNECTED_IDS_LIST) && (to.link_count == 0))
        {
            if (to.unknown_level != null)
            {
                AsListItem<Identifier> dc = to.unknown_level;
                dc.remove(this.ids_with_unknown_level);
                dc.insertAtHead(this.disconnected_ids);
            }
            else
            {
                to.unknown_level = new AsListItem<Identifier>(to);
                to.unknown_level.insertAtHead(this.disconnected_ids);
            }
            return;
        }

        // if removing a link from a different level, there must be some
        // other link at the same level, so we can ignore this change
        if (from != null && (from.level != to.level))
            return;

        if (to.unknown_level == null)
        {
            to.unknown_level = new AsListItem<Identifier>(to);
            to.unknown_level.insertAtHead(this.ids_with_unknown_level);
        }
    }

    /**
     * Garbage collect an identifier. This removes all wmes, input wmes, and
     * preferences for that id from TM.
     * 
     * decide.cpp:483:garbage_collect_id
     * 
     * @param id
     */
    private void garbage_collect_id(Identifier id)
    {
        if(DEBUG_LINKS)
        {
            context.getPrinter().print("\n*** Garbage collecting id: %s",id);
        }

        /*
         * Note--for goal/impasse id's, this does not remove the impasse wme's.
         * This is handled by remove_existing_such-and-such...
         */

        // remove any input wmes from the id
        context.workingMemory.remove_wme_list_from_wm(id.getInputWmes(), true);
        id.removeAllInputWmes();

        for (AsListItem<Slot> sit = id.slots.first; sit != null; sit = sit.next)
        {
            final Slot s = sit.item;
            
            // remove any existing attribute impasse for the slot
            if (s.impasse_type != ImpasseType.NONE_IMPASSE_TYPE)
                remove_existing_attribute_impasse_for_slot(s);

            // remove all wme's from the slot
            context.workingMemory.remove_wme_list_from_wm(s.getWmes(), false);
            s.removeAllWmes();

            // remove all preferences for the slot
            Preference pref = s.getAllPreferences();
            while (pref != null)
            {
                final Preference next_pref = pref.next_of_slot;
                context.prefMemory.remove_preference_from_tm(pref);

                /*
                 * Note: the call to remove_preference_from_slot handles the
                 * removal of acceptable_preference_wmes
                 */
                pref = next_pref;
            }

            context.tempMemory.mark_slot_for_possible_removal(s);
        } /* end of for slots loop */
    }

    /**
     * decide.cpp:549:mark_unknown_level_if_needed
     * 
     * @param sym
     */
    private void mark_unknown_level_if_needed(Symbol sym)
    {
        Identifier id = sym.asIdentifier();
        if (id != null)
            mark_id_and_tc_as_unknown_level(id);
    }

    /**
     * Mark an id and its transitive closure as having an unknown level. Ids are
     * marked by setting id.tc_num to mark_tc_number. The starting id's goal
     * stack level is recorded in level_at_which_marking_started by the caller.
     * The marked ids are added to ids_with_unknown_level.
     * 
     * decide.cpp:555:mark_id_and_tc_as_unknown_level
     * 
     * @param id
     */
    private void mark_id_and_tc_as_unknown_level(Identifier id)
    {
        // if id is already marked, do nothing
        if (id.tc_number == this.mark_tc_number)
            return;

        // don't mark anything higher up as disconnected--in order to be higher
        // up, it must have a link to it up there
        if (id.level < this.level_at_which_marking_started)
            return;

        // mark id, so we won't do it again later
        id.tc_number = this.mark_tc_number;

        // update range of goal stack levels we'll need to walk
        if (id.level < this.highest_level_anything_could_fall_from)
            this.highest_level_anything_could_fall_from = id.level;
        if (id.level > this.lowest_level_anything_could_fall_to)
            this.lowest_level_anything_could_fall_to = id.level;
        if (id.could_be_a_link_from_below)
            this.lowest_level_anything_could_fall_to = SoarConstants.LOWEST_POSSIBLE_GOAL_LEVEL;

        // add id to the set of ids with unknown level
        if (id.unknown_level == null)
        {
            id.unknown_level = new AsListItem<Identifier>(id);
            id.unknown_level.insertAtHead(ids_with_unknown_level);
        }

        // scan through all preferences and wmes for all slots for this id
        for (Wme w = id.getInputWmes(); w != null; w = w.next)
            mark_unknown_level_if_needed(w.value);
        for (AsListItem<Slot> sit = id.slots.first; sit != null; sit = sit.next)
        {
            final Slot s = sit.item;
            for (Preference pref = s.getAllPreferences(); pref != null; pref = pref.next_of_slot)
            {
                mark_unknown_level_if_needed(pref.value);
                if (pref.type.isBinary())
                    mark_unknown_level_if_needed(pref.referent);
            }
            if (s.impasse_id != null)
                mark_unknown_level_if_needed(s.impasse_id);
            for (Wme w = s.getWmes(); w != null; w = w.next)
                mark_unknown_level_if_needed(w.value);
        }
    }

    /**
     * decide.cpp:617:update_levels_if_needed
     * 
     * @param sym
     */
    private void update_levels_if_needed(Symbol sym)
    {
        Identifier id = sym.asIdentifier();
        if (id != null)
            if (id.tc_number != this.walk_tc_number)
                walk_and_update_levels(id);
    }

    /**
     * After marking the ids with unknown level, we walk various levels of the
     * goal stack, higher level to lower level. If, while doing the walk, we
     * encounter an id marked as having an unknown level, we update its level
     * and remove it from ids_with_unknown_level.
     * 
     * decide.cpp:624:walk_and_update_levels
     * 
     * @param id
     */
    private void walk_and_update_levels(Identifier id)
    {
        // mark id so we don't walk it twice
        id.tc_number = this.walk_tc_number;

        // if we already know its level, and it's higher up, then exit
        if ((id.unknown_level == null) && (id.level < this.walk_level))
            return;

        // if we didn't know its level before, we do now
        if (id.unknown_level != null)
        {
            id.unknown_level.remove(this.ids_with_unknown_level);
            id.unknown_level = null;
            id.level = this.walk_level;
            id.promotion_level = this.walk_level;
        }

        // scan through all preferences and wmes for all slots for this id
        for (Wme w = id.getInputWmes(); w != null; w = w.next)
            update_levels_if_needed(w.value);
        for (AsListItem<Slot> sit = id.slots.first; sit != null; sit = sit.next)
        {
            final Slot s = sit.item;
            for (Preference pref = s.getAllPreferences(); pref != null; pref = pref.next_of_slot)
            {
                update_levels_if_needed(pref.value);
                if (pref.type.isBinary())
                    update_levels_if_needed(pref.referent);
            }
            if (s.impasse_id != null)
                update_levels_if_needed(s.impasse_id);
            for (Wme w = s.getWmes(); w != null; w = w.next)
                update_levels_if_needed(w.value);
        }
    }

    /**
     * Do all buffered demotions and gc's.
     * 
     * decide.cpp:666:do_demotion
     */
    private void do_demotion()
    {
        // scan through ids_with_unknown_level, move the ones with link_count==0
        // over to disconnected_ids
        AsListItem<Identifier> dc, next_dc;
        for (dc = ids_with_unknown_level.first; dc != null; dc = next_dc)
        {
            next_dc = dc.next;
            Identifier id = dc.item;
            if (id.link_count == 0)
            {
                dc.remove(this.ids_with_unknown_level);
                dc.insertAtHead(this.disconnected_ids);
            }
        }

        // keep garbage collecting ids until nothing left to gc
        this.link_update_mode = LinkUpdateType.UPDATE_DISCONNECTED_IDS_LIST;
        while (!this.disconnected_ids.isEmpty())
        {
            dc = disconnected_ids.first;
            this.disconnected_ids.first = dc.next;
            Identifier id = dc.item;
            garbage_collect_id(id);
        }
        this.link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;

        // if nothing's left with an unknown level, we're done
        if (this.ids_with_unknown_level.isEmpty())
            return;

        // do the mark
        this.highest_level_anything_could_fall_from = SoarConstants.LOWEST_POSSIBLE_GOAL_LEVEL;
        this.lowest_level_anything_could_fall_to = -1;
        this.mark_tc_number = context.syms.get_new_tc_number();
        for (dc = this.ids_with_unknown_level.first; dc != null; dc = dc.next)
        {
            Identifier id = dc.item;
            this.level_at_which_marking_started = id.level;
            mark_id_and_tc_as_unknown_level(id);
        }

        // do the walk
        Identifier g = this.top_goal;
        while (true)
        {
            if (g == null)
                break;
            if (g.level > this.lowest_level_anything_could_fall_to)
                break;
            if (g.level >= this.highest_level_anything_could_fall_from)
            {
                this.walk_level = g.level;
                this.walk_tc_number = context.syms.get_new_tc_number();
                walk_and_update_levels(g);
            }
            g = g.lower_goal;
        }

        // GC anything left with an unknown level after the walk
        this.link_update_mode = LinkUpdateType.JUST_UPDATE_COUNT;
        while (!ids_with_unknown_level.isEmpty())
        {
            dc = ids_with_unknown_level.first;
            this.ids_with_unknown_level.first = dc.next;
            Identifier id = dc.item;
            id.unknown_level = null; // AGR 640:  GAP set to NIL because symbol may still have pointers to it
            garbage_collect_id(id);
        }
        this.link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;
    }

    /**
     * This routine does all the buffered link (ownership) chages, updating the
     * goal stack level on all identifiers and garbage collecting disconnected
     * wmes.
     * 
     * decide.cpp:744:do_buffered_link_changes
     */
    private void do_buffered_link_changes()
    {
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // struct timeval saved_start_tv;
        // #endif
        // #endif

        /* --- if no promotions or demotions are buffered, do nothing --- */
        if (promoted_ids.isEmpty() && ids_with_unknown_level.isEmpty() && disconnected_ids.isEmpty())
            return;

        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // start_timer (thisAgent, &saved_start_tv);
        // #endif
        // #endif
        do_promotion();
        do_demotion();
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        //  stop_timer (thisAgent, &saved_start_tv, &thisAgent->ownership_cpu_time[thisAgent->current_phase]);
        //#endif
        //#endif
    }


    /**
     * Require_preference_semantics() is a helper function for
     * run_preference_semantics() that is used when there is at least one
     * require preference for the slot.
     * 
     * decide.cpp:803:require_preference_semantics
     * 
     * @return
     */
    private ImpasseType require_preference_semantics(Slot s, ByRef<Preference> result_candidates)
    {
        // collect set of required items into candidates list --- */
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE).first; p != null; p = p.next)
            p.item.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
        Preference candidates = null;
        for (AsListItem<Preference> it = s.getFastPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE).first; it != null; it = it.next)
        {
            final Preference p = it.item;
            if (p.value.decider_flag == DeciderFlag.NOTHING_DECIDER_FLAG)
            {
                p.next_candidate = candidates;
                candidates = p;
                // unmark it, in order to prevent it from being added twice
                p.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
            }
        }
        result_candidates.value = candidates;

        // if more than one required item, we have a constraint failure
        if (candidates.next_candidate != null)
            return ImpasseType.CONSTRAINT_FAILURE_IMPASSE_TYPE;

        // just one require, check for require-prohibit impasse
        Symbol value = candidates.value;
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.PROHIBIT_PREFERENCE_TYPE).first; p != null; p = p.next)
            if (p.item.value == value)
                return ImpasseType.CONSTRAINT_FAILURE_IMPASSE_TYPE;

        // the lone require is the winner
        if (candidates != null && context.rl.rl_enabled())
        {
            // TODO reinforcement learning
            // exploration_compute_value_of_candidate( thisAgent, candidates, s, 0 );
            // rl_perform_update( thisAgent, candidates->numeric_value, s->id );
        }

        return ImpasseType.NONE_IMPASSE_TYPE;
    }
    

    /**
     * Run_preference_semantics (slot *s, preference **result_candidates)
     * examines the preferences for a given slot, and returns an impasse type
     * for the slot. The argument "result_candidates" is set to a list of
     * candidate values for the slot--if the returned impasse type is
     * NONE_IMPASSE_TYPE, this is the set of winners; otherwise it is the set of
     * tied, conflicted, or constraint-failured values. This list of values is a
     * list of preferences for those values, linked via the "next_candidate"
     * field on each preference structure. If there is more than one preference
     * for a given value, only one is returned in the result_candidates, with
     * (first) require preferences being preferred over acceptable preferences,
     * and (second) preferences from higher match goals being preferred over
     * those from lower match goals.
     * 
     * BUGBUG There is a problem here: since the require/acceptable priority
     * takes precedence over the match goal level priority, it's possible that
     * we could return a require preference from lower in the goal stack than
     * some acceptable preference. If the goal stack gets popped soon afterwards
     * (i.e., before the next time the slot is re-decided, I think), we would be
     * left with a WME still in WM (not GC'd, because of the acceptable
     * preference higher up) but with a trace pointing to a deallocated require
     * preference. This case is very obsure and unlikely to come up, but it
     * could easily cause a core dump or worse.
     * 
     * decide.cpp:840:run_preference_semantics
     * 
     * @param s
     * @param result_candidates
     * @param consistency (defaulted to false in CSoar)
     * @param predict  (defaulted to false in CSoar)
     * @return
     */
    private ImpasseType run_preference_semantics(Slot s, ByRef<Preference> result_candidates,
            boolean consistency /* = false */, boolean predict /* = false */)
    {
        // if the slot has no preferences at all, things are trivial
        if (s.getAllPreferences() == null)
        {
            if (!s.isa_context_slot)
                context.tempMemory.mark_slot_for_possible_removal(s);
            result_candidates.value = null;
            return ImpasseType.NONE_IMPASSE_TYPE;
        }

        // if this is the true decision slot and selection has been made,
        // attempt force selection
        if (!(((context.attribute_preferences_mode == 2) || (context.operand2_mode == true)) && (!s.isa_context_slot)))
        {
            if (context.decisionManip.select_get_operator() != null)
            {
                Preference force_result = context.decisionManip.select_force(s.getAllPreferences(), !predict);

                if (force_result != null)
                {
                    result_candidates.value = force_result;

                    if (!predict && context.rl.rl_enabled())
                    {
                        // TODO reinforcement learning
                        // exploration_compute_value_of_candidate( thisAgent, force_result, s, 0 );
                        // rl_perform_update( thisAgent, force_result->numeric_value, s->id );
                    }

                    return ImpasseType.NONE_IMPASSE_TYPE;
                }
                else
                {
                    context.getPrinter().warn( "WARNING: Invalid forced selection operator id" );
                }
            }
        }

        /* === Requires === */
        if (!s.getFastPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE).isEmpty())
        {
            return require_preference_semantics(s, result_candidates);
        }

        /* === Acceptables, Prohibits, Rejects === */

        // mark everything that's acceptable, then unmark the prohibited and rejected items
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE).first; p != null; p = p.next)
            p.item.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.PROHIBIT_PREFERENCE_TYPE).first; p != null; p = p.next)
            p.item.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.REJECT_PREFERENCE_TYPE).first; p != null; p = p.next)
            p.item.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;

        /* --- now scan through acceptables and build the list of candidates --- */
        Preference candidates = null;
        for (AsListItem<Preference> it = s.getFastPreferenceList(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE).first; it != null; it = it.next)
        {
            final Preference p = it.item;
            if (p.value.decider_flag == DeciderFlag.CANDIDATE_DECIDER_FLAG)
            {
                p.next_candidate = candidates;
                candidates = p;
                // unmark it, in order to prevent it from being added twice
                p.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
            }
        }

        /* === Handling of attribute_preferences_mode 2 === */
        if (((context.attribute_preferences_mode == 2) || (context.operand2_mode == true)) && (!s.isa_context_slot))
        {
            result_candidates.value = candidates;
            return ImpasseType.NONE_IMPASSE_TYPE;
        }

        /* === If there are only 0 or 1 candidates, we're done === */
        if ((candidates == null) || (candidates.next_candidate == null))
        {
            result_candidates.value = candidates;

            if (!consistency && context.rl.rl_enabled() && candidates != null)
            {
                // perform update here for just one candidate
                // TODO reinforcement learning
                // exploration_compute_value_of_candidate( thisAgent, candidates, s, 0 );
                // rl_perform_update( thisAgent, candidates->numeric_value, s->id );
            }

            return ImpasseType.NONE_IMPASSE_TYPE;
        }

        /* === Better/Worse === */
        if (!s.getFastPreferenceList(PreferenceType.BETTER_PREFERENCE_TYPE).isEmpty()
                || !s.getFastPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE).isEmpty())
        {
            Symbol j, k;

            /* Algorithm to find conflicted set: 
            conflicted = {}
            for each (j > k):
              if j is (candidate or conflicted)
                 and k is (candidate or conflicted)
                 and at least one of j,k is a candidate
                then if (k > j) or (j < k) then
                  conflicted += j, if not already true
                  conflicted += k, if not already true
                  candidate -= j, if not already true
                  candidate -= k, if not already true
            for each (j < k):
              if j is (candidate or conflicted)
                 and k is (candidate or conflicted)
                 and at least one of j,k is a candidate
                 then if (k < j)
                   then
                      conflicted += j, if not already true
                      conflicted += k, if not already true
                      candidate -= j, if not already true
                      candidate -= k, if not already true
            ----------------------- */

            for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.BETTER_PREFERENCE_TYPE).first; p != null; p = p.next)
            {
                p.item.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
                p.item.referent.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
            }
            for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE).first; p != null; p = p.next)
            {
                p.item.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
                p.item.referent.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
            }
            for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
            {
                cand.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
            }
            for (AsListItem<Preference> pit = s.getFastPreferenceList(PreferenceType.BETTER_PREFERENCE_TYPE).first; pit != null; pit = pit.next)
            {
                final Preference p = pit.item;
                
                j = p.value;
                k = p.referent;
                if (j == k)
                    continue;
                if (j.decider_flag.isSomething() && k.decider_flag.isSomething())
                {
                    if (k.decider_flag != DeciderFlag.CONFLICTED_DECIDER_FLAG)
                        k.decider_flag = DeciderFlag.FORMER_CANDIDATE_DECIDER_FLAG;
                    if ((j.decider_flag != DeciderFlag.CONFLICTED_DECIDER_FLAG)
                            || (k.decider_flag != DeciderFlag.CONFLICTED_DECIDER_FLAG))
                    {
                        for (AsListItem<Preference> p2 = s.getFastPreferenceList(PreferenceType.BETTER_PREFERENCE_TYPE).first; p2 != null; p2 = p2.next)
                            if ((p2.item.value == k) && (p2.item.referent == j))
                            {
                                j.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                k.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                break;
                            }
                        for (AsListItem<Preference> p2 = s.getFastPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE).first; p2 != null; p2 = p2.next)
                            if ((p2.item.value == j) && (p2.item.referent == k))
                            {
                                j.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                k.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                break;
                            }
                    }
                }
            }
            for (AsListItem<Preference> it = s.getFastPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE).first; it != null; it = it.next)
            {
                final Preference p = it.item;
                j = p.value;
                k = p.referent;
                if (j == k)
                    continue;
                if (j.decider_flag.isSomething() && k.decider_flag.isSomething())
                {
                    if (j.decider_flag != DeciderFlag.CONFLICTED_DECIDER_FLAG)
                        j.decider_flag = DeciderFlag.FORMER_CANDIDATE_DECIDER_FLAG;
                    if ((j.decider_flag != DeciderFlag.CONFLICTED_DECIDER_FLAG)
                            || (k.decider_flag != DeciderFlag.CONFLICTED_DECIDER_FLAG))
                    {
                        for (AsListItem<Preference> p2 = s.getFastPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE).first; p2 != null; p2 = p2.next)
                            if ((p2.item.value == k) && (p2.item.referent == j))
                            {
                                j.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                k.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                break;
                            }
                    }
                }
            }

            // now scan through candidates list, look for conflicted stuff
            Preference cand = null, prev_cand = null;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                if (cand.value.decider_flag == DeciderFlag.CONFLICTED_DECIDER_FLAG)
                    break;
            if (cand != null)
            {
                // collect conflicted candidates into new candidates list
                prev_cand = null;
                cand = candidates;
                while (cand != null)
                {
                    if (cand.value.decider_flag != DeciderFlag.CONFLICTED_DECIDER_FLAG)
                    {
                        if (prev_cand != null)
                            prev_cand.next_candidate = cand.next_candidate;
                        else
                            candidates = cand.next_candidate;
                    }
                    else
                    {
                        prev_cand = cand;
                    }
                    cand = cand.next_candidate;
                }
                result_candidates.value = candidates;
                return ImpasseType.CONFLICT_IMPASSE_TYPE;
            }

            // no conflicts found, remove former_candidates from candidates
            prev_cand = null;
            cand = candidates;
            while (cand != null)
            {
                if (cand.value.decider_flag == DeciderFlag.FORMER_CANDIDATE_DECIDER_FLAG)
                {
                    if (prev_cand != null)
                        prev_cand.next_candidate = cand.next_candidate;
                    else
                        candidates = cand.next_candidate;
                }
                else
                {
                    prev_cand = cand;
                }
                cand = cand.next_candidate;
            }
        }

        /* === Bests === */
        if (!s.getFastPreferenceList(PreferenceType.BEST_PREFERENCE_TYPE).isEmpty())
        {
            Preference cand, prev_cand;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                cand.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
            for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.BEST_PREFERENCE_TYPE).first; p != null; p = p.next)
                p.item.value.decider_flag = DeciderFlag.BEST_DECIDER_FLAG;
            prev_cand = null;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                if (cand.value.decider_flag == DeciderFlag.BEST_DECIDER_FLAG)
                {
                    if (prev_cand != null)
                        prev_cand.next_candidate = cand;
                    else
                        candidates = cand;
                    prev_cand = cand;
                }
            if (prev_cand != null)
                prev_cand.next_candidate = null;
        }

        /* === Worsts === */
        if (!s.getFastPreferenceList(PreferenceType.WORST_PREFERENCE_TYPE).isEmpty())
        {
            Preference cand, prev_cand;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                cand.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
            for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.WORST_PREFERENCE_TYPE).first; p != null; p = p.next)
                p.item.value.decider_flag = DeciderFlag.WORST_DECIDER_FLAG;
            prev_cand = null;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                if (cand.value.decider_flag != DeciderFlag.WORST_DECIDER_FLAG)
                {
                    if (prev_cand != null)
                        prev_cand.next_candidate = cand;
                    else
                        candidates = cand;
                    prev_cand = cand;
                }
            if (prev_cand != null)
                prev_cand.next_candidate = null;
        }

        /* === If there are only 0 or 1 candidates, we're done === */
        if (candidates == null || candidates.next_candidate == null)
        {
            result_candidates.value = candidates;

            if (!consistency && context.rl.rl_enabled() && candidates != null)
            {
                // perform update here for just one candidate
                // TODO reinforcement learning
                // exploration_compute_value_of_candidate( thisAgent, candidates, s, 0 );
                // rl_perform_update( thisAgent, candidates->numeric_value, s->id );
            }

            return ImpasseType.NONE_IMPASSE_TYPE;
        }

        /* === Indifferents === */
        for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.UNARY_INDIFFERENT_PREFERENCE_TYPE).first; p != null; p = p.next)
            p.item.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT_DECIDER_FLAG;

        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.NUMERIC_INDIFFERENT_PREFERENCE_TYPE).first; p != null; p = p.next)
            p.item.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT_CONSTANT_DECIDER_FLAG;

        for (AsListItem<Preference> it = s.getFastPreferenceList(PreferenceType.BINARY_INDIFFERENT_PREFERENCE_TYPE).first; it != null; it = it.next)
        {
            final Preference p = it.item;
            if ((p.referent.asIntConstant() != null) || (p.referent.asFloatConstant() != null))
                p.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT_CONSTANT_DECIDER_FLAG;
        }

        boolean not_all_indifferent = false;
        for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
        {
            if (cand.value.decider_flag == DeciderFlag.UNARY_INDIFFERENT_DECIDER_FLAG)
                continue;
            else if (cand.value.decider_flag == DeciderFlag.UNARY_INDIFFERENT_CONSTANT_DECIDER_FLAG)
                continue;

            /* --- check whether cand is binary indifferent to each other one --- */
            for (Preference p = candidates; p != null; p = p.next_candidate)
            {
                if (p == cand)
                    continue;
                boolean match_found = false;
                for (AsListItem<Preference> it = s.getFastPreferenceList(PreferenceType.BINARY_INDIFFERENT_PREFERENCE_TYPE).first;
                         it != null; it = it.next)
                {
                    final Preference p2 = it.item;
                    if (((p2.value == cand.value) && (p2.referent == p.value))
                            || ((p2.value == p.value) && (p2.referent == cand.value)))
                    {
                        match_found = true;
                        break;
                    }
                }
                if (!match_found)
                {
                    not_all_indifferent = true;
                    break;
                }
            } /* end of for p loop */
            if (not_all_indifferent)
                break;
        } /* end of for cand loop */

        if (!not_all_indifferent)
        {
            if (!consistency)
            {
                result_candidates.value = context.exploration.exploration_choose_according_to_policy(s, candidates);
                result_candidates.value.next_candidate = null;
            }
            else
                result_candidates.value = candidates;

            return ImpasseType.NONE_IMPASSE_TYPE;
        }

        // items not all indifferent; for context slots this gives a tie
        if (s.isa_context_slot)
        {
            result_candidates.value = candidates;
            return ImpasseType.TIE_IMPASSE_TYPE;
        }

        /* === Parallels === */
        for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.UNARY_PARALLEL_PREFERENCE_TYPE).first; p != null; p = p.next)
            p.item.value.decider_flag = DeciderFlag.UNARY_PARALLEL_DECIDER_FLAG;
        boolean not_all_parallel = false;
        for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
        {
            /* --- if cand is unary parallel, it's fine --- */
            if (cand.value.decider_flag == DeciderFlag.UNARY_PARALLEL_DECIDER_FLAG)
                continue;
            /* --- check whether cand is binary parallel to each other candidate --- */
            for (Preference p = candidates; p != null; p = p.next_candidate)
            {
                if (p == cand)
                    continue;
                boolean match_found = false;
                for (AsListItem<Preference> it = s.getFastPreferenceList(PreferenceType.BINARY_PARALLEL_PREFERENCE_TYPE).first; it != null; it = it.next)
                {
                    final Preference p2 = it.item;
                    if (((p2.value == cand.value) && (p2.referent == p.value))
                            || ((p2.value == p.value) && (p2.referent == cand.value)))
                    {
                        match_found = true;
                        break;
                    }
                }
                if (!match_found)
                {
                    not_all_parallel = true;
                    break;
                }
            } /* end of for p loop */
            if (not_all_parallel)
                break;
        } /* end of for cand loop */

        result_candidates.value = candidates;

        if (!not_all_parallel)
        {
            /* --- items are all parallel, so return them all --- */
            return ImpasseType.NONE_IMPASSE_TYPE;
        }

        /* --- otherwise we have a tie --- */
        return ImpasseType.TIE_IMPASSE_TYPE;
    }
    
    /**
     * decide.cpp:1204:run_preference_semantics_for_consistency_check
     * 
     * @param s
     * @param result_candidates
     * @return
     */
    public ImpasseType run_preference_semantics_for_consistency_check (Slot s, ByRef<Preference> result_candidates) 
    {
        return run_preference_semantics(s, result_candidates, true, false );
    }
    
    /**
     * This creates a new wme and adds it to the given impasse object. "Id"
     * indicates the goal/impasse id; (id ^attr value) is the impasse wme to be
     * added. The "preference" argument indicates the preference (if non-NIL)
     * for backtracing.
     * 
     * decide.cpp:1224:add_impasse_wme
     * 
     * @param id
     * @param attr
     * @param value
     * @param p
     */
    private void add_impasse_wme(Identifier id, Symbol attr, Symbol value, Preference p)
    {
        Wme w = context.workingMemory.make_wme(id, attr, value, false);
        id.addImpasseWme(w);
        w.preference = p;
        context.workingMemory.add_wme_to_wm(w);
    }

    /**
     * This creates a new impasse, returning its identifier. The caller is
     * responsible for filling in either id->isa_impasse or id->isa_goal, and
     * all the extra stuff for goal identifiers.
     * 
     * decide.cpp:1241:create_new_impasse
     * 
     * @param isa_goal
     * @param object
     * @param attr
     * @param impasse_type
     * @param level
     *            Goal stack level
     * @return
     */
    private Identifier create_new_impasse(boolean isa_goal, Symbol object, Symbol attr, ImpasseType impasse_type,
            int level)
    {
        final PredefinedSymbols predefined = context.predefinedSyms; // reduce typing
        
        Identifier id = context.syms.make_new_identifier(isa_goal ? 'S' : 'I', level);
        post_link_addition(null, id); // add the special link

        add_impasse_wme(id, predefined.type_symbol, 
                        isa_goal ? predefined.state_symbol : predefined.impasse_symbol, null);

        if (isa_goal)
        {
            add_impasse_wme(id, predefined.superstate_symbol, object, null);

            id.reward_header = context.syms.make_new_identifier('R', level);
            context.io.add_input_wme(id, predefined.reward_link_symbol, id.reward_header);
        }
        else
            add_impasse_wme(id, predefined.object_symbol, object, null);

        if (attr != null)
            add_impasse_wme(id, predefined.attribute_symbol, attr, null);

        switch (impasse_type)
        {
        case NONE_IMPASSE_TYPE:
            break; /* this happens only when creating the top goal */
        case CONSTRAINT_FAILURE_IMPASSE_TYPE:
            add_impasse_wme(id, predefined.impasse_symbol, predefined.constraint_failure_symbol, null);
            add_impasse_wme(id, predefined.choices_symbol, predefined.none_symbol, null);
            break;
        case CONFLICT_IMPASSE_TYPE:
            add_impasse_wme(id, predefined.impasse_symbol, predefined.conflict_symbol, null);
            add_impasse_wme(id, predefined.choices_symbol, predefined.multiple_symbol, null);
            break;
        case TIE_IMPASSE_TYPE:
            add_impasse_wme(id, predefined.impasse_symbol, predefined.tie_symbol, null);
            add_impasse_wme(id, predefined.choices_symbol, predefined.multiple_symbol, null);
            break;
        case NO_CHANGE_IMPASSE_TYPE:
            add_impasse_wme(id, predefined.impasse_symbol, predefined.no_change_symbol, null);
            add_impasse_wme(id, predefined.choices_symbol, predefined.none_symbol, null);
            break;
        }
        return id;
    }
    
    /**
     * Create an attribute impasse for a given slot
     * 
     * decide.cpp:1293:create_new_attribute_impasse_for_slot
     * 
     * @param s
     * @param impasse_type
     */
    private void create_new_attribute_impasse_for_slot(Slot s, ImpasseType impasse_type)
    {
        s.impasse_type = impasse_type;
        Identifier id = create_new_impasse(false, s.id, s.attr, impasse_type, SoarConstants.ATTRIBUTE_IMPASSE_LEVEL);
        s.impasse_id = id;
        id.isa_impasse = true;

        // TODO callback CREATE_NEW_ATTRIBUTE_IMPASSE_CALLBACK
        // soar_invoke_callbacks(thisAgent, CREATE_NEW_ATTRIBUTE_IMPASSE_CALLBACK, (soar_call_data) s);
    }

    /**
     * Remove an attribute impasse from a given slot
     * 
     * decide.cpp:1307:remove_existing_attribute_impasse_for_slot
     * 
     * @param s
     * @param impasse_type
     */
    private void remove_existing_attribute_impasse_for_slot(Slot s)
    {
        // TODO callback REMOVE_ATTRIBUTE_IMPASSE_CALLBACK
        // soar_invoke_callbacks(thisAgent, REMOVE_ATTRIBUTE_IMPASSE_CALLBACK, (soar_call_data) s);

        Identifier id = s.impasse_id;
        s.impasse_id = null;
        s.impasse_type = ImpasseType.NONE_IMPASSE_TYPE;

        context.workingMemory.remove_wme_list_from_wm(id.getImpasseWmes(), false);
        id.removeAllImpasseWmes();
        post_link_removal(null, id); /* remove the special link */
    }

    /**
     * Fake Preferences for Goal ^Item Augmentations
     * 
     * When we backtrace through a (goal ^item) augmentation, we want to
     * backtrace to the acceptable preference wme in the supercontext
     * corresponding to that ^item. A slick way to do this automagically is to
     * set the backtracing preference pointer on the (goal ^item) wme to be a
     * "fake" preference for a "fake" instantiation. The instantiation has as
     * its LHS a list of one condition, which matched the acceptable preference
     * wme in the supercontext.
     * 
     * Make_fake_preference_for_goal_item() builds such a fake preference and
     * instantiation, given a pointer to the supergoal and the
     * acceptable/require preference for the value, and returns a pointer to the
     * fake preference. *** for Soar 8.3, we changed the fake preference to be
     * ACCEPTABLE instead of REQUIRE. This could potentially break some code,
     * but it avoids the BUGBUG condition that can occur when you have a REQUIRE
     * lower in the stack than an ACCEPTABLE but the goal stack gets popped
     * while the WME backtrace still points to the REQUIRE, instead of the
     * higher ACCEPTABLE. See the section above on Preference Semantics. It also
     * allows the GDS to backtrace through ^items properly.
     * 
     * decide.cpp:1350:make_fake_preference_for_goal_item
     * 
     * @param goal
     * @param cand
     * @return
     */
    private Preference make_fake_preference_for_goal_item(Identifier goal, Preference cand)
    {
        /* --- find the acceptable preference wme we want to backtrace to --- */
        Slot s = cand.slot;
        Wme ap_wme;
        for (ap_wme = s.getAcceptablePreferenceWmes(); ap_wme != null; ap_wme = ap_wme.next)
            if (ap_wme.value == cand.value)
                break;
        if (ap_wme == null)
        {
            throw new IllegalStateException("Internal error: couldn't find acceptable pref wme");
        }
        /* --- make the fake preference --- */
        /* kjc: here's where we changed REQUIRE to ACCEPTABLE */
        Preference pref = new Preference(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE, goal,
                context.predefinedSyms.item_symbol, cand.value, null);
        pref.all_of_goal.insertAtHead(goal.preferences_from_goal);
        pref.on_goal_list = true;
        pref.preference_add_ref();

        /* --- make the fake instantiation --- */
        Instantiation inst = new Instantiation(null, null, null);
        pref.setInstantiation(inst);
        inst.match_goal = goal;
        inst.match_goal_level = goal.level;
        inst.okay_to_variablize = true;
        inst.backtrace_number = 0;
        inst.in_ms = false;

        /* --- make the fake condition --- */
        PositiveCondition cond = new PositiveCondition();

        inst.top_of_instantiated_conditions = cond;
        inst.bottom_of_instantiated_conditions = cond;
        inst.nots = null;
        cond.id_test = Symbol.makeEqualityTest(ap_wme.id); // make_equality_test
                                                            // (ap_wme->id);
        cond.attr_test = Symbol.makeEqualityTest(ap_wme.attr);
        cond.value_test = Symbol.makeEqualityTest(ap_wme.value);
        cond.test_for_acceptable_preference = true;
        cond.bt.wme_ = ap_wme;
        if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
        {
            ap_wme.wme_add_ref();
        }
        else
        {
            if (inst.match_goal_level > SoarConstants.TOP_GOAL_LEVEL)
                ap_wme.wme_add_ref();
        }
        cond.bt.level = ap_wme.id.level;

        /* --- return the fake preference --- */
        return pref;
    }

    /**
     * Remove_fake_preference_for_goal_item() is called to clean up the fake
     * stuff once the (goal ^item) wme is no longer needed.
     * 
     * decide.cpp:1419:remove_fake_preference_for_goal_item
     * 
     * @param pref
     */
    private void remove_fake_preference_for_goal_item(Preference pref)
    {
        pref.preference_remove_ref(context.prefMemory); /* everything else happens automatically */
    }
    
    /**
     * This routine updates the set of ^item wmes on a goal or attribute
     * impasse. It takes the identifier of the goal/impasse, and a list of
     * preferences (linked via the "next_candidate" field) for the new set of
     * items that should be there.
     * 
     * decide.cpp:1432:update_impasse_items
     * 
     * @param id
     * @param items
     */
    private void update_impasse_items(Identifier id, Preference items)
    {
        final PredefinedSymbols predefined = context.predefinedSyms;
        
        int item_count = count_candidates(items); // SBW 5/07

        // reset flags on existing items to "NOTHING"
        for (Wme w = id.getImpasseWmes(); w != null; w = w.next)
            if (w.attr == predefined.item_symbol)
                w.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;

        // mark set of desired items as "CANDIDATEs"
        for (Preference cand = items; cand != null; cand = cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;

        // for each existing item: if it's supposed to be there still, then
        // mark it "ALREADY_EXISTING"; otherwise remove it
        Wme w = id.getImpasseWmes();
        while (w != null)
        {
            final Wme next_w = w.next;
            if (w.attr == predefined.item_symbol)
            {
                if (w.value.decider_flag == DeciderFlag.CANDIDATE_DECIDER_FLAG)
                {
                    w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG;
                    w.value.decider_wme = w; /*
                                                 * so we can update the pref
                                                 * later
                                                 */
                }
                else
                {
                    id.removeImpasseWme(w);
                    if (id.isa_goal)
                        remove_fake_preference_for_goal_item(w.preference);
                    context.workingMemory.remove_wme_from_wm(w);
                }
            }

            // SBW 5/07
            // remove item-count WME if it exists
            else if (w.attr == predefined.item_count_symbol)
            {
                id.removeImpasseWme(w);
                context.workingMemory.remove_wme_from_wm(w);
            }

            w = next_w;
        }

        /* --- for each desired item: if it doesn't ALREADY_EXIST, add it --- */
        for (Preference cand = items; cand != null; cand = cand.next_candidate)
        {
            Preference bt_pref;
            if (id.isa_goal)
                bt_pref = make_fake_preference_for_goal_item(id, cand);
            else
                bt_pref = cand;
            if (cand.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG)
            {
                if (id.isa_goal)
                    remove_fake_preference_for_goal_item(cand.value.decider_wme.preference);
                cand.value.decider_wme.preference = bt_pref;
            }
            else
            {
                add_impasse_wme(id, predefined.item_symbol, cand.value, bt_pref);
            }
        }

        // SBW 5/07
        // update the item-count WME
        // detect relevant impasses by having more than one item
        if (item_count > 0)
        {
            add_impasse_wme(id, predefined.item_count_symbol, context.syms.make_int_constant(item_count), null);
        }
    }

    
    /**
     * This routine decides a given slot, which must be a non-context slot. It
     * calls run_preference_semantics() on the slot, then updates the wmes
     * and/or impasse for the slot accordingly.
     * 
     * decide.cpp:1510:decide_non_context_slot
     * 
     * @param s
     */
    private void decide_non_context_slot (Slot s) 
    {
      ByRef<Preference> candidates = ByRef.create(null);
      
      ImpasseType impasse_type = run_preference_semantics (s, candidates, false, false);
      
      if (impasse_type==ImpasseType.NONE_IMPASSE_TYPE) 
      {
         // no impasse, so remove any existing one and update the wmes
         if (s.impasse_type != ImpasseType.NONE_IMPASSE_TYPE)
            remove_existing_attribute_impasse_for_slot (s);
         
         // reset marks on existing wme values to "NOTHING"
         for (Wme w = s.getWmes(); w != null; w = w.next)
            w.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
         
         // set marks on desired values to "CANDIDATES"
         for (Preference cand=candidates.value; cand!=null; cand=cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
         
            // for each existing wme, if we want it there, mark it as ALREADY_EXISTING; otherwise remove it
         Wme it = s.getWmes();
         while (it != null) 
         {
            final Wme w = it;
            it = w.next;
            
            if (w.value.decider_flag == DeciderFlag.CANDIDATE_DECIDER_FLAG) 
            {
               w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG;
               w.value.decider_wme = w; /* so we can set the pref later */
            } 
            else 
            {
               s.removeWme(w);
               if (context.operand2_mode)
               {
                  if (w.gds != null) 
                  {
                     if (w.gds.getGoal() != null)
                     {
                         /* If the goal pointer is non-NIL, then goal is in the stack */
                         // TODO or soar_verbose_flag
                         context.trace.print(Category.TRACE_WM_CHANGES_SYSPARAM, 
                                 "\nRemoving state S%d because element in GDS changed. WME: %s", w.gds.getGoal().level, w);
                         // TODO xml
    //                    if (thisAgent.soar_verbose_flag || thisAgent.sysparams[TRACE_WM_CHANGES_SYSPARAM]) 
    //                    {
    //                       print(thisAgent, "\nRemoving state S%d because element in GDS changed.", w.gds.goal.id.level);
    //                       print(thisAgent, " WME: "); 
    //
    //                       char buf[256];
    //                       SNPRINTF(buf, 254, "Removing state S%d because element in GDS changed.", w.gds.goal.id.level);
    //                       xml_begin_tag(thisAgent, kTagVerbose);
    //                       xml_att_val(thisAgent, kTypeString, buf);
    //                       print_wme(thisAgent, w);
    //                       xml_end_tag(thisAgent, kTagVerbose);
    //                    }
                        gds_invalid_so_remove_goal(w);
                     }
                  }
               }
               context.workingMemory.remove_wme_from_wm (w);
            }
         } 
         
         // for each desired value, if it's not already there, add it
         for (Preference cand=candidates.value; cand!=null; cand=cand.next_candidate) 
         {
            if (cand.value.decider_flag==DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG)
            {
               /* print(thisAgent, "\n This WME was marked as already existing...."); print_wme(cand->value->common.a.decider_wme); */
               cand.value.decider_wme.preference = cand;
            } 
            else 
            {
               Wme w = context.workingMemory.make_wme(cand.id, cand.attr, cand.value, false);
               s.addWme(w);
               w.preference = cand;
               
               if (context.operand2_mode)
               {
               /* Whenever we add a WME to WM, we also want to check and see if
               this new WME is o-supported.  If so, then we want to add the
               supergoal dependencies of the new, o-supported element to the
               goal in which the element was created (as long as the o_supported
               element was not created in the top state -- the top goal has
                  no gds).  */
                  
    //#ifndef NO_TIMING_STUFF
    //#ifdef DETAILED_TIMING_STATS
    //              start_timer(thisAgent, &thisAgent->start_gds_tv);
    //#endif 
    //#endif
                  
                  this.parent_list_head = null;
                  
                  /* If the working memory element being added is going to have
                  o_supported preferences and the instantion that created it
                  is not in the top_level_goal (where there is no GDS), then
                  loop over the preferences for this WME and determine which
                  WMEs should be added to the goal's GDS (the goal here being the
                  goal to which the added memory is attached). */
                  
                  if ((w.preference.o_supported == true) &&
                     (w.preference.inst.match_goal_level != 1)) {
                     
                     if (w.preference.inst.match_goal.gds == null) {
                     /* If there is no GDS yet for this goal,
                        * then we need to create one */
                        if (w.preference.inst.match_goal_level == w.preference.id.level) {
                           
                           create_gds_for_goal(w.preference.inst.match_goal );
                           
                           /* REW: BUG When chunks and result instantiations both create
                           * preferences for the same WME, then we only want to create
                           * the GDS for the highest goal.  Right now I ensure that we
                           * elaborate the correct GDS with the tests in the loop just
                           * below this code, but the GDS creation above assumes that
                           * the chunk will be first on the GDS list.  This order
                           * appears to be always true, although I am not 100% certain
                           * (I think it occurs this way because the chunk is
                           * necessarily added to the instantiaton list after the
                           * original instantiation and lists get built such older items
                           * appear further from the head of the list) . If not true,
                           * then we need to keep track of any GDS's that get created
                           * here to remove them later if we find a higher match goal
                           * for the WME. For now, the program just exits in this
                           * situation; otherwise, we would build a GDS for the wrong
                           * level and never elaborate it (resulting in a memory
                           * leak). 
                           */
                        } else {
                           throw new IllegalStateException("Wanted to create a GDS for a WME level different from the instantiation level.....Big problems....exiting....");
                        }
                     } /* end if no GDS yet for goal... */
                     
                       /* Loop over all the preferences for this WME:
                       *   If the instantiation that lead to the preference has not 
                       *         been already explored; OR
                       *   If the instantiation is not an subgoal instantiation
                       *          for a chunk instantiation we are already exploring
                       *   Then
                       *      Add the instantiation to a list of instantiations that
                       *          will be explored in elaborate_gds().
                     */
                     
                     for (Preference pref=w.preference; pref!=null; pref=pref.next_prev.getNextItem()) {
    if(DEBUG_GDS_HIGH){
                        context.getPrinter().print("\n\n   ");
                        context.getPrinter().print_preference(pref);
                        context.getPrinter().print("   Goal level of preference: %d\n", pref.id.level);
    }
                        
                        if (pref.inst.GDS_evaluated_already == false) {
    if(DEBUG_GDS_HIGH){
        context.getPrinter().print("   Match goal lev of instantiation %s is %d\n", pref.inst.prod.name, pref.inst.match_goal_level);
    }
                           if (pref.inst.match_goal_level > pref.id.level) {
    if(DEBUG_GDS_HIGH){
        context.getPrinter().print("        %s  is simply the instantiation that led to a chunk.\n        Not adding it the current instantiations.\n", pref.inst.prod.name);
    }
                              
                           } else {
    if(DEBUG_GDS_HIGH){
        context.getPrinter().print("\n   Adding %s to list of parent instantiations\n", pref.inst.prod.name); 
    }
                              uniquely_add_to_head_of_dll(pref.inst);
                              pref.inst.GDS_evaluated_already = true;
                           }
                        }  /* end if GDS_evaluated_already is FALSE */
                        else if(DEBUG_GDS_HIGH) {
                            context.getPrinter().print("\n    Instantiation %s was already explored; skipping it\n", pref.inst.prod.name);
                        }
                        
                     }  /* end of forloop over preferences for this wme */
                     
                     
    if(DEBUG_GDS_HIGH){
        context.getPrinter().print("\n    CALLING ELABORATE GDS....\n");
    }
                     elaborate_gds();
                     
                     /* technically, the list should be empty at this point ??? */
                     
                     free_parent_list(); 
    if(DEBUG_GDS_HIGH){
        context.getPrinter().print("    FINISHED ELABORATING GDS.\n\n");
    }
                  }  /* end if w->preference->o_supported == TRUE ... */
                  
                  
                  /* REW: begin 11.25.96 */ 
    //#ifndef NO_TIMING_STUFF
    //#ifdef DETAILED_TIMING_STATS
    //              stop_timer(thisAgent, &thisAgent->start_gds_tv, 
    //                 &thisAgent->gds_cpu_time[thisAgent->current_phase]);
    //#endif
    //#endif
                  /* REW: end   11.25.96 */ 
                  
                }  /* end if thisAgent->OPERAND2_MODE ... */
                   /* REW: end   09.15.96 */
       
                context.workingMemory.add_wme_to_wm (w);
             }
          }
          
          return;
       } /* end of if impasse type == NONE */
    
       // impasse type != NONE
       if (s.getWmes() != null) 
       {  
          // remove any existing wmes
          context.workingMemory.remove_wme_list_from_wm (s.getWmes(), false); 
          s.removeAllWmes();
       }
    
       /* --- create and/or update impasse structure --- */
       if (s.impasse_type != ImpasseType.NONE_IMPASSE_TYPE) 
       {
          if (s.impasse_type != impasse_type) 
          {
             remove_existing_attribute_impasse_for_slot (s);
             create_new_attribute_impasse_for_slot (s, impasse_type);
          }
          update_impasse_items (s.impasse_id, candidates.value);
       } 
       else 
       {
          create_new_attribute_impasse_for_slot (s, impasse_type);
          update_impasse_items (s.impasse_id, candidates.value);
       }
    }

    /**
     * This routine iterates through all changed non-context slots, and decides
     * each one.
     * 
     * decide.cpp:1766:decide_non_context_slots
     */
    private void decide_non_context_slots()
    {
        final ListHead<Slot> changed_slots = context.tempMemory.changed_slots;
        while (!changed_slots.isEmpty())
        {
            AsListItem<Slot> dc = changed_slots.first;
            changed_slots.first = changed_slots.first.next;
            Slot s = dc.item;
            decide_non_context_slot(s);
            s.changed = null;
        }
    }
    
    /**
     * This returns TRUE iff the given slot (which must be a context slot) is
     * decidable. A context slot is decidable if: 
     *   - it has an installed value in WM and there is a reconsider preference 
     *     for that value, or
     *   - it has no installed value but does have changed preferences
     * 
     * decide.cpp:1791:context_slot_is_decidable
     * 
     * @param s
     * @return
     */
    private boolean context_slot_is_decidable(Slot s)
    {
        if (s.getWmes() == null)
            return s.changed != null;

        Symbol v = s.getWmes().value;
        for (AsListItem<Preference> p = s.getFastPreferenceList(PreferenceType.RECONSIDER_PREFERENCE_TYPE).first; p != null; p = p.next)
        {
            if (v == p.item.value)
                return true;
        }

        return false;
    }

    /**
     * This removes the wmes (there can only be 0 or 1 of them) for the given
     * context slot.
     * 
     * decide.cpp:1816:remove_wmes_for_context_slot
     * 
     * @param s
     */
    void remove_wmes_for_context_slot(Slot s)
    {
        if (s.getWmes() == null)
            return;
        /*
         * Note that we only need to handle one wme--context slots never have
         * more than one wme in them
         */
        final Wme w = s.getWmes();
        w.preference.preference_remove_ref(context.prefMemory);
        context.workingMemory.remove_wme_from_wm(w);
        s.removeAllWmes();
    }    
    
    /**
     * This routine truncates the goal stack by removing the given goal and all
     * its subgoals. (If the given goal is the top goal, the entire context
     * stack is removed.)
     * 
     * decide.cpp:1836:remove_existing_context_and_descendents
     * 
     * @param goal
     */
    void remove_existing_context_and_descendents(Identifier goal)
    {
        // remove descendents of this goal
        if (goal.lower_goal != null)
            remove_existing_context_and_descendents(goal.lower_goal);

        // TODO callback POP_CONTEXT_STACK_CALLBACK
        // invoke callback routine
        // soar_invoke_callbacks(thisAgent,
        // POP_CONTEXT_STACK_CALLBACK,
        // (soar_call_data) goal);

        /* --- disconnect this goal from the goal stack --- */
        if (goal == top_goal)
        {
            top_goal = null;
            bottom_goal = null;
        }
        else
        {
            bottom_goal = goal.higher_goal;
            bottom_goal.lower_goal = null;
        }

        /* --- remove any preferences supported by this goal --- */
        if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
        {
            while (!goal.preferences_from_goal.isEmpty())
            {
                Preference p = goal.preferences_from_goal.getFirstItem();
                p.all_of_goal.remove(goal.preferences_from_goal);
                p.on_goal_list = false;
                if (!context.prefMemory.remove_preference_from_clones(p))
                    if (p.in_tm)
                        context.prefMemory.remove_preference_from_tm(p);
            }
        }
        else
        {
            /*
             * KJC Aug 05: this seems to cure a potential for exceeding
             * callstack when popping soar's goal stack and not doing
             * DO_TOP_LEVEL_REF_CTS Probably should make this change for all
             * cases, but needs testing.
             */
            /* Prefs are added to head of dll, so try removing from tail */
            if (!goal.preferences_from_goal.isEmpty())
            {
                AsListItem<Preference> p = goal.preferences_from_goal.first;
                while (p.next != null)
                    p = p.next; // TODO Replace with ListHead.getTail() or
                                // something
                while (p != null)
                {
                    AsListItem<Preference> p_next = p.previous; // RPM 10/06 we
                                                                // need to save
                                                                // this because
                                                                // p may be
                                                                // freed by the
                                                                // end of the
                                                                // loop
                    p.remove(goal.preferences_from_goal);
                    p.item.on_goal_list = false;
                    if (!context.prefMemory.remove_preference_from_clones(p.item))
                        if (p.item.in_tm)
                            context.prefMemory.remove_preference_from_tm(p.item);
                    p = p_next;
                }
            }
        }
        /* --- remove wmes for this goal, and garbage collect --- */
        remove_wmes_for_context_slot(goal.operator_slot);
        update_impasse_items(goal, null); /*
                                             * causes items & fake pref's to go
                                             * away
                                             */

        if (context.rl.rl_enabled())
        {
            // TODO reinforcement learning
            // rl_tabulate_reward_value_for_goal( thisAgent, goal );
            // rl_perform_update( thisAgent, 0, goal ); // this update only sees
            // reward - there is no next state
        }

        context.workingMemory.remove_wme_list_from_wm(goal.getImpasseWmes(), false);
        goal.removeAllImpasseWmes();
        
        /*
         * If there was a GDS for this goal, we want to set the pointer for the
         * goal to NIL to indicate it no longer exists. BUG: We probably also
         * need to make certain that the GDS doesn't need to be free'd here as
         * well.
         */
        if (goal.gds != null)
        {
            goal.gds.clearGoal();
        }

        /*
         * If we remove a goal WME, then we have to transfer any already
         * existing retractions to the nil-goal list on the current agent. We
         * should be able to do this more efficiently but the most obvious way
         * (below) still requires scanning over the whole list (to set the goal
         * pointer of each msc to NIL); therefore this solution should be
         * acceptably efficient.
         */

        if (!goal.ms_retractions.isEmpty())
        { /* There's something on the retraction list */

            MatchSetChange head = goal.ms_retractions.getFirstItem();
            MatchSetChange tail = head;

            /* find the tail of this list */
            while (tail.in_level.next != null)
            {
                tail.goal = null; /* force the goal to be NIL */
                tail = tail.in_level.getNextItem();
            }
            tail.goal = null;

            final ListHead<MatchSetChange> nil_goal_retractions = context.soarReteListener.nil_goal_retractions;
            if (!nil_goal_retractions.isEmpty())
            {
                /* There are already retractions on the list */

                /* Append this list to front of NIL goal list */
                // TODO replace this with a splice operation
                nil_goal_retractions.getFirstItem().in_level.previous = tail.in_level;
                tail.in_level.next = nil_goal_retractions.first;
                nil_goal_retractions.first = head.in_level;

            }
            else
            { /* If no retractions, make this list the NIL goal list */
                nil_goal_retractions.first = head.in_level;
            }
        }

        // TODO reinforcement learning
        //  delete goal->id.rl_info->eligibility_traces;
        //  free_list( thisAgent, goal->id.rl_info->prev_op_rl_rules );
        //  symbol_remove_ref( thisAgent, goal->id.reward_header );
        //  free_memory( thisAgent, goal->id.rl_info, MISCELLANEOUS_MEM_USAGE );

        /* REW: BUG
         * Tentative assertions can exist for removed goals.  However, it looks
         * like the removal forces a tentative retraction, which then leads to
         * the deletion of the tentative assertion.  However, I have not tested
         * such cases exhaustively -- I would guess that some processing may be
         * necessary for the assertions here at some point?
         */

        /* REW: end   08.20.97 */

        post_link_removal(null, goal); /* remove the special link */
    }

    /**
     * This routine creates a new goal context (becoming the new bottom goal)
     * below the current bottom goal. If there is no current bottom goal, this
     * routine creates a new goal and makes it both the top and bottom goal.
     * 
     * decide.cpp:1969:create_new_context
     * 
     * @param attr_of_impasse
     * @param impasse_type
     */
    private void create_new_context(Symbol attr_of_impasse, ImpasseType impasse_type)
    {
        Identifier id;

        if (bottom_goal != null)
        {
            /* Creating a sub-goal (or substate) */
            id = create_new_impasse(true, bottom_goal, attr_of_impasse, impasse_type, bottom_goal.level + 1);
            id.higher_goal = bottom_goal;
            bottom_goal.lower_goal = id;
            bottom_goal = id;
            add_impasse_wme(id, context.predefinedSyms.quiescence_symbol, context.predefinedSyms.t_symbol, null);
            if ((ImpasseType.NO_CHANGE_IMPASSE_TYPE == impasse_type) && (context.MAX_GOAL_DEPTH < bottom_goal.level))
            {
                // appear to be SNC'ing deep in goalstack, so interrupt and warn user
                // KJC note: we actually halt, because there is no interrupt function in SoarKernel
                // in the gSKI Agent code, if system_halted, MAX_GOAL_DEPTH is checked and if exceeded
                // then the interrupt is generated and system_halted is set to FALSE so the user can recover.

                context.getPrinter().warn("\nGoal stack depth exceeded %d on a no-change impasse.\n" +
                		"Soar appears to be in an infinite loop.  \n" +
                		"Continuing to subgoal may cause Soar to \n" +
                		"exceed the program stack of your system.\n",
                        context.MAX_GOAL_DEPTH);

                context.decisionCycle.stop_soar = true;
                context.decisionCycle.system_halted = true;
                context.decisionCycle.reason_for_stopping = "Max Goal Depth exceeded.";
            }
        }
        else
        {
            /* Creating the top state */
            id = create_new_impasse(true, context.predefinedSyms.nil_symbol, null, ImpasseType.NONE_IMPASSE_TYPE,
                    SoarConstants.TOP_GOAL_LEVEL);
            top_goal = id;
            bottom_goal = id;
            top_state = top_goal;
            id.higher_goal = null;
            id.lower_goal = null;
        }

        id.isa_goal = true;
        id.operator_slot = Slot.make_slot(id, context.predefinedSyms.operator_symbol,
                context.predefinedSyms.operator_symbol);
        id.allow_bottom_up_chunks = true;

        // TODO reinforcement learning
        id.rl_info = new ReinforcementLearningInfo();
        // id->id.rl_info->eligibility_traces = new rl_et_map(
        // std::less<production *>(), SoarMemoryAllocator<std::pair<production*
        // const, double> >( thisAgent, MISCELLANEOUS_MEM_USAGE ) );
        // id->id.rl_info->prev_op_rl_rules = NIL;
        // id->id.rl_info->previous_q = 0;
        // id->id.rl_info->reward = 0;
        //  id->id.rl_info->reward_age = 0;
        //  id->id.rl_info->num_prev_op_rl_rules = 0;
        //  id->id.rl_info->step = 0;  
        // id.rl_info.impasse_type = ImpasseType.NONE_IMPASSE_TYPE;

        /* --- invoke callback routine --- */
        // TODO callback CREATE_NEW_CONTEXT_CALLBACK
        //  soar_invoke_callbacks(thisAgent, 
        //                       CREATE_NEW_CONTEXT_CALLBACK, 
        //                       (soar_call_data) id);
    }
    
    /**
     * Given a goal, these routines return the type and attribute, respectively,
     * of the impasse just below that goal context. It does so by looking at the
     * impasse wmes for the next lower goal in the goal stack.
     * 
     * decide.cpp:2042:type_of_existing_impasse
     * 
     * @param goal
     * @return
     */
    public ImpasseType type_of_existing_impasse(Identifier goal)
    {
        if (goal.lower_goal == null)
            return ImpasseType.NONE_IMPASSE_TYPE;

        final PredefinedSymbols predefined = context.predefinedSyms;
        for (Wme w = goal.lower_goal.getImpasseWmes(); w != null; w = w.next)
        {
            if (w.attr == predefined.impasse_symbol)
            {
                if (w.value == predefined.no_change_symbol)
                    return ImpasseType.NO_CHANGE_IMPASSE_TYPE;
                if (w.value == predefined.tie_symbol)
                    return ImpasseType.TIE_IMPASSE_TYPE;
                if (w.value == predefined.constraint_failure_symbol)
                    return ImpasseType.CONSTRAINT_FAILURE_IMPASSE_TYPE;
                if (w.value == predefined.conflict_symbol)
                    return ImpasseType.CONFLICT_IMPASSE_TYPE;
                if (w.value == predefined.none_symbol)
                    return ImpasseType.NONE_IMPASSE_TYPE;

                throw new IllegalStateException("Internal error: bad type of existing impasse.");
            }
        }
        throw new IllegalStateException("Internal error: couldn't find type of existing impasse.");
    }
    
    /**
     * 
     * decide.cpp:2069:attribute_of_existing_impasse
     * @param goal
     * @return
     */
    public Symbol attribute_of_existing_impasse(Identifier goal)
    {
        if (goal.lower_goal == null)
            return null;
        
        for (Wme w = goal.lower_goal.getImpasseWmes(); w != null; w = w.next)
            if (w.attr == context.predefinedSyms.attribute_symbol)
                return w.value;

        throw new IllegalStateException("Internal error: couldn't find attribute of existing impasse.");
    }

    /**
     * This decides the given context slot. It normally returns TRUE, but
     * returns FALSE if the ONLY change as a result of the decision procedure
     * was a change in the set of ^item's on the impasse below the given slot.
     * 
     * decide.cpp:2092:decide_context_slot
     * 
     * @param goal
     * @param s
     * @param predict (defaulted to false in CSoar)
     * @return
     */
    private boolean decide_context_slot(Identifier goal, Slot s, boolean predict /*= false*/)
    {
        ImpasseType impasse_type;
        Symbol attribute_of_impasse;
        ByRef<Preference> candidates = ByRef.create(null);

        if (!context_slot_is_decidable(s))
        {
            // the only time we decide a slot that's not "decidable" is when
            // it's
            // the last slot in the entire context stack, in which case we have
            // a
            // no-change impasse there
            impasse_type = ImpasseType.NO_CHANGE_IMPASSE_TYPE;
            candidates.value = null; /*
                                         * we don't want any impasse ^item's
                                         * later
                                         */

            if (predict)
            {
                context.decisionManip.predict_set("none");
                return true;
            }
        }
        else
        {
            /* --- the slot is decidable, so run preference semantics on it --- */
            impasse_type = run_preference_semantics(s, candidates, false, false);

            if (predict)
            {
                switch (impasse_type)
                {
                case CONSTRAINT_FAILURE_IMPASSE_TYPE:
                    context.decisionManip.predict_set("constraint");
                    break;

                case CONFLICT_IMPASSE_TYPE:
                    context.decisionManip.predict_set("conflict");
                    break;

                case TIE_IMPASSE_TYPE:
                    context.decisionManip.predict_set("tie");
                    break;

                case NO_CHANGE_IMPASSE_TYPE:
                    context.decisionManip.predict_set("none");
                    break;

                default:
                    if (candidates.value == null || (candidates.value.value.asIdentifier() == null))
                        context.decisionManip.predict_set("none");
                    else
                    {
                        Identifier tempId = candidates.value.value.asIdentifier();
                        // TODO can this be null?
                        String temp = "" + tempId.name_letter + tempId.name_number;
                        context.decisionManip.predict_set(temp);
                    }
                    break;
                }

                return true;
            }

            remove_wmes_for_context_slot(s); // must remove old wme before
                                                // adding the new one (if any)
            if (impasse_type == ImpasseType.NONE_IMPASSE_TYPE)
            {
                if (candidates.value == null)
                {
                    /*
                     * --- no winner ==> no-change impasse on the previous slot
                     * ---
                     */
                    impasse_type = ImpasseType.NO_CHANGE_IMPASSE_TYPE;
                }
                else if (candidates.value.next_candidate != null)
                {
                    /* --- more than one winner ==> internal error --- */
                    throw new IllegalStateException("Internal error: more than one winner for context slot");
                }
            }
        } /* end if !context_slot_is_decidable */

        /* --- mark the slot as not changed --- */
        s.changed = null;

        // determine the attribute of the impasse (if there is no impasse, this
        // doesn't matter)
        if (impasse_type == ImpasseType.NO_CHANGE_IMPASSE_TYPE)
        {
            if (s.getWmes() != null)
            {
                attribute_of_impasse = s.attr;
            }
            else
            {
                attribute_of_impasse = context.predefinedSyms.state_symbol;
            }
        }
        else
        {
            // for all other kinds of impasses
            attribute_of_impasse = s.attr;
        }

        // remove wme's for lower slots of this context
        if (attribute_of_impasse == context.predefinedSyms.state_symbol)
        {
            remove_wmes_for_context_slot(goal.operator_slot);
        }

        // if we have a winner, remove any existing impasse and install the
        // new value for the current slot
        if (impasse_type == ImpasseType.NONE_IMPASSE_TYPE)
        {
            for (Preference temp = candidates.value; temp != null; temp = temp.next_candidate)
                temp.preference_add_ref();

            if (goal.lower_goal != null)
                remove_existing_context_and_descendents(goal.lower_goal);

            Wme w = context.workingMemory.make_wme(s.id, s.attr, candidates.value.value, false);
            s.addWme(w);
            w.preference = candidates.value;
            w.preference.preference_add_ref();

            /* JC Adding an operator to working memory in the current state */
            context.workingMemory.add_wme_to_wm(w);

            for (Preference temp = candidates.value; temp != null; temp = temp.next_candidate)
                temp.preference_remove_ref(context.prefMemory);

            if (context.rl.rl_enabled())
                context.rl.rl_store_data(goal, candidates.value);

            return true;
        }

        // TODO move to rl_info
        if (impasse_type != ImpasseType.NO_CHANGE_IMPASSE_TYPE)
            goal.rl_info.impasse_type = impasse_type;
        else if (s.getWmes() != null)
            goal.rl_info.impasse_type = ImpasseType.OP_NO_CHANGE_IMPASSE_TYPE;
        else
            goal.rl_info.impasse_type = ImpasseType.STATE_NO_CHANGE_IMPASSE_TYPE;

        // no winner; if an impasse of the right type already existed, just
        // update the ^item set on it
        if ((impasse_type == type_of_existing_impasse(goal))
                && (attribute_of_impasse == attribute_of_existing_impasse(goal)))
        {
            update_impasse_items(goal.lower_goal, candidates.value);
            return false;
        }

        // no impasse already existed, or an impasse of the wrong type
        // already existed
        for (Preference temp = candidates.value; temp != null; temp = temp.next_candidate)
            temp.preference_add_ref();

        if (goal.lower_goal != null)
            remove_existing_context_and_descendents(goal.lower_goal);

        /* REW: begin 10.24.97 */
        if (context.operand2_mode && this.waitsnc && (impasse_type == ImpasseType.NO_CHANGE_IMPASSE_TYPE)
                && (attribute_of_impasse == context.predefinedSyms.state_symbol))
        {
            this.waitsnc_detect = true;
        }
        else
        {
            /* REW: end     10.24.97 */
            create_new_context(attribute_of_impasse, impasse_type);
            update_impasse_items(goal.lower_goal, candidates.value);
        }

        for (Preference temp = candidates.value; temp != null; temp = temp.next_candidate)
            temp.preference_remove_ref(context.prefMemory);

        return true;
    }

    /**
     * This scans down the goal stack and runs the decision procedure on the
     * appropriate context slots.
     * 
     * decide.cpp:2289:decide_context_slots
     * 
     * @param predict (defaulted to false in CSoar)
     */
    private void decide_context_slots(boolean predict /* = false */)
    {
        Identifier goal;

        if (context.tempMemory.highest_goal_whose_context_changed != null)
        {
            goal = context.tempMemory.highest_goal_whose_context_changed;
        }
        else
            /* no context changed, so jump right to the bottom */
            goal = bottom_goal;

        Slot s = goal.operator_slot;

        /* --- loop down context stack --- */
        while (true)
        {
            /* --- find next slot to decide --- */
            while (true)
            {
                if (context_slot_is_decidable(s))
                    break;

                if ((s == goal.operator_slot) || (s.getWmes() == null))
                {
                    // no more slots to look at for this goal; have we reached
                    // the last slot in whole stack?
                    if (goal.lower_goal == null)
                        break;

                    // no, go down one level
                    goal = goal.lower_goal;
                    s = goal.operator_slot;
                }
            } /* end of while (TRUE) find next slot to decide */

            // now go and decide that slot
            if (decide_context_slot(goal, s, predict))
                break;

        } /* end of while (TRUE) loop down context stack */

        if (!predict)
            context.tempMemory.highest_goal_whose_context_changed = null;
    }
    
    /**
     * does the end-of-phases processing of WM changes, ownership calculations,
     * garbage collection, etc. 
     * 
     * decide.cpp::do_buffered_wm_and_ownership_changes
     */
    public void do_buffered_wm_and_ownership_changes()
    {
        do_buffered_acceptable_preference_wme_changes();
        do_buffered_link_changes();
        context.workingMemory.do_buffered_wm_changes(context.io);
        context.tempMemory.remove_garbage_slots();
    }
    
    /**
     * 
     * decide.cpp:2373:do_working_memory_phase
     */
    public void do_working_memory_phase()
    {
        if (context.trace.isEnabled() && context.trace.isEnabled(Category.TRACE_PHASES_SYSPARAM))
        {
            if (context.operand2_mode == true)
            {
                if (context.decisionCycle.current_phase == Phase.APPLY_PHASE)
                { // it's always IE for PROPOSE
                    // TODO xml
                    // xml_begin_tag(thisAgent, kTagSubphase);
                    // xml_att_val(thisAgent, kPhase_Name, kSubphaseName_ChangingWorkingMemory);
                    switch (context.recMemory.FIRING_TYPE)
                    {
                    case PE_PRODS:
                        context.getPrinter().print("\t--- Change Working Memory (PE) ---\n");
                        // TODO xml_att_val(thisAgent, kPhase_FiringType, kPhaseFiringType_PE);
                        break;
                    case IE_PRODS:
                        context.getPrinter().print("\t--- Change Working Memory (IE) ---\n");
                        // TODO xml_att_val(thisAgent, kPhase_FiringType, kPhaseFiringType_IE);
                        break;
                    }
                    // TODO xml_end_tag(thisAgent, kTagSubphase);
                }
            }
            else
            {
                // TODO the XML for this is generated in this function
                Phase.WM_PHASE.trace(context.trace, true);
            }
        }

        decide_non_context_slots();
        do_buffered_wm_and_ownership_changes();

        if(!context.operand2_mode)
        {
            Phase.WM_PHASE.trace(context.trace, false);
        }
    }

    /**
     * decide.cpp:2409:do_decision_phase
     * 
     * @param predict (defaulted to false in CSoar)
     */
    public void do_decision_phase(boolean predict /*=false*/)
    {
        if (!predict && context.rl.rl_enabled())
            context.rl.rl_tabulate_reward_values();

        context.decisionManip.predict_srand_restore_snapshot(!predict);

        /* phases printing moved to init_soar: do_one_top_level_phase */

        decide_context_slots(predict);

        if (!predict)
        {
            do_buffered_wm_and_ownership_changes();

            /*
             * Bob provided a solution to fix WME's hanging around unsupported
             * for an elaboration cycle.
             */
            decide_non_context_slots();
            do_buffered_wm_and_ownership_changes();

            context.exploration.exploration_update_parameters();
        }
    }

    /**
     * decide.cpp:2435:create_top_goal
     */
    public void create_top_goal()
    {
        create_new_context(null, ImpasseType.NONE_IMPASSE_TYPE);
        context.tempMemory.highest_goal_whose_context_changed = null; // nothing changed yet
        do_buffered_wm_and_ownership_changes();
    }

    /**
     * decide.cpp:2442:clear_goal_stack
     */
    public void clear_goal_stack()
    {
        if (top_goal == null)
            return;

        remove_existing_context_and_descendents(top_goal);
        context.tempMemory.highest_goal_whose_context_changed = null; // nothing changed yet
        do_buffered_wm_and_ownership_changes();
        top_state = null;
        active_goal = null;

        // TODO Do these really have any business benig here?
        context.io.do_input_cycle(); // tell input functions that the top state is gone
        context.io.do_output_cycle(); // tell output functions that output commands are gone
    }
    

    /**
     * decide.cpp:2522:uniquely_add_to_head_of_dll
     * 
     * @param inst
     */
    private void uniquely_add_to_head_of_dll(Instantiation inst)
    {
        /* print(thisAgent, "UNIQUE DLL:         scanning parent list...\n"); */

        for (ParentInstantiation curr_pi = parent_list_head; curr_pi != null; curr_pi = curr_pi.next)
        {
            if (curr_pi.inst == inst)
            {
                if (DEBUG_GDS)
                {
                    context.getPrinter().print("UNIQUE DLL:            %s is already in parent list\n",
                            curr_pi.inst.prod.name);
                }
                return;
            }
            if (DEBUG_GDS)
            {
                context.getPrinter().print("UNIQUE DLL:            %s\n", curr_pi.inst.prod.name);
            }
        } /* end for loop */

        ParentInstantiation new_pi = new ParentInstantiation();
        new_pi.next = null;
        new_pi.prev = null;
        new_pi.inst = inst;

        new_pi.next = parent_list_head;

        if (parent_list_head != null)
            parent_list_head.prev = new_pi;

        parent_list_head = new_pi;
        if (DEBUG_GDS)
        {
            context.getPrinter().print("UNIQUE DLL:         added: %s\n", inst.prod.name);
        }
    }

    /**
     * Added this function to make one place for wme's being added to the GDS. Callback for wme 
     * added to GDS is made here.
     * 
     * decide.cpp:2562:add_wme_to_gds
     * 
     * @param gds
     * @param wme_to_add
     */
    private void add_wme_to_gds(GoalDependencySet gds, Wme wme_to_add)
    {
        // Set the correct GDS for this wme (wme's point to their gds)
        wme_to_add.gds = gds;
        wme_to_add.gds_next_prev.insertAtHead(gds.wmes_in_gds);

        // TODO trace add wme to gds in verbose mode as well
        context.trace.print(Category.TRACE_WM_CHANGES_SYSPARAM, 
                "Adding to GDS for %s: WME: %s", wme_to_add.gds.getGoal(), wme_to_add);
    }
    

    /**
     * decide.cpp:2587:elaborate_gds
     */
    private void elaborate_gds()
    {
        ParentInstantiation temp_pi = null;
        for (ParentInstantiation curr_pi = parent_list_head; curr_pi != null; curr_pi = temp_pi)
        {

            Instantiation inst = curr_pi.inst;
            /*
            #ifdef DEBUG_GDS
                  print_with_symbols("\n      EXPLORING INSTANTIATION: %y\n",curr_pi->inst->prod->name);
                  print("      ");
                  print_instantiation_with_wmes( thisAgent, curr_pi->inst , TIMETAG_WME_TRACE, -1);
            #endif
            */
            for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next)
            {
                PositiveCondition pc = cond.asPositiveCondition();
                if (pc == null)
                    continue;

                // We'll deal with negative instantiations after we get the
                // positive ones figured out

                Wme wme_matching_this_cond = cond.bt.wme_;
                int wme_goal_level = cond.bt.level;
                Preference pref_for_this_wme = wme_matching_this_cond.preference;
                /*
                #ifdef DEBUG_GDS
                         context.getPrinter().print("\n       wme_matching_this_cond at goal_level = %d : ", wme_goal_level);
                         print_wme(thisAgent, wme_matching_this_cond); 

                         if (pref_for_this_wme) {
                            print(thisAgent, "       pref_for_this_wme                        : ");
                            print_preference(thisAgent, pref_for_this_wme);
                         } 
                #endif
                */

                // WME is in a supergoal or is arch-supported WME (except for fake instantiations,
                // which do have prefs, so they get handled under "wme is local and i-supported")
                if ((pref_for_this_wme == null) || (wme_goal_level < inst.match_goal_level))
                {

                    if (DEBUG_GDS)
                    {
                        if (pref_for_this_wme == null)
                        {
                            context.getPrinter().print(
                                    "         this wme has no preferences (it's an arch-created wme)\n");
                        }
                        else if (wme_goal_level < inst.match_goal_level)
                        {
                            context.getPrinter().print("         this wme is in the supergoal\n");
                        }
                        context.getPrinter().print("inst->match_goal [%s]\n", inst.match_goal);
                    }

                    if (wme_matching_this_cond.gds != null)
                    {
                        // Then we want to check and see if the old GDS value
                        // should be changed
                        if (wme_matching_this_cond.gds.getGoal() == null)
                        {
                            // The goal is NIL: meaning that the goal for the GDS
                            // is no longer around
                            wme_matching_this_cond.gds_next_prev.remove(wme_matching_this_cond.gds.wmes_in_gds);

                            // We have to check for GDS removal anytime we take a
                            // WME off the GDS wme list, not just when a WME is
                            // removed from memory.
                            if (wme_matching_this_cond.gds.wmes_in_gds.isEmpty())
                            {
                                wme_matching_this_cond.gds = null;
                                // free_memory(thisAgent, wme_matching_this_cond->gds, MISCELLANEOUS_MEM_USAGE);

                                if (DEBUG_GDS)
                                {
                                    context.getPrinter().print("\n  REMOVING GDS FROM MEMORY.");
                                }
                            }

                            /* JC ADDED: Separate adding wme to GDS as a function */
                            add_wme_to_gds(inst.match_goal.gds, wme_matching_this_cond);

                            if (DEBUG_GDS)
                            {
                                context.getPrinter().print(
                                        "\n       .....GDS' goal is NIL so switching from old to new GDS list....\n");
                            }

                        }
                        else if (wme_matching_this_cond.gds.getGoal().level > inst.match_goal_level)
                        {
                            /* if the WME currently belongs to the GDS of a goal below
                            * the current one */
                            /* 1. Take WME off old (current) GDS list 
                            * 2. Check to see if old GDS WME list is empty.  If so,
                            *         remove(free) it.
                            * 3. Add WME to new GDS list
                            * 4. Update WME pointer to new GDS list
                            */
                            if (inst.match_goal_level == 1)
                            {
                                // TODO uhhh is this necessary??
                                context.getPrinter().print("\n\n\n HELLO! HELLO! The inst->match_goal_level is 1");
                            }

                            wme_matching_this_cond.gds_next_prev.remove(wme_matching_this_cond.gds.wmes_in_gds);

                            if (wme_matching_this_cond.gds.wmes_in_gds.isEmpty())
                            {
                                wme_matching_this_cond.gds = null;
                                // free_memory(thisAgent, wme_matching_this_cond->gds, MISCELLANEOUS_MEM_USAGE);

                                if (DEBUG_GDS)
                                {
                                    context.getPrinter().print("\n  REMOVING GDS FROM MEMORY.");
                                }

                            }
                            /* JC ADDED: Separate adding wme to GDS as a function */
                            add_wme_to_gds(inst.match_goal.gds, wme_matching_this_cond);

                            if (DEBUG_GDS)
                            {
                                context.getPrinter().print("\n       ....switching from old to new GDS list....\n");
                            }

                            wme_matching_this_cond.gds = inst.match_goal.gds;
                        }
                    }
                    else
                    {
                        /* We know that the WME should be in the GDS of the current
                        * goal if the WME's GDS does not already exist.
                        * (i.e., if NIL GDS) */

                        /* JC ADDED: Separate adding wme to GDS as a function */
                        add_wme_to_gds(inst.match_goal.gds, wme_matching_this_cond);

                        if (wme_matching_this_cond.gds.wmes_in_gds.first.previous != null)
                        {
                            // TODO is this necessary??
                            context.getPrinter().print(
                                    "\nDEBUG DEBUG : The new header should never have a prev value.\n");
                        }

                        if (DEBUG_GDS)
                        {
                            context.getPrinter().print(
                                    "\n       ......WME did not have defined GDS.  Now adding to goal [%s].\n",
                                    wme_matching_this_cond.gds.getGoal());
                        }

                    } /* end else clause for "if wme_matching_this_cond->gds != NIL" */

                    if (DEBUG_GDS)
                    {
                        context.getPrinter().print("            Added WME to GDS for goal = %d [%s]\n",
                                wme_matching_this_cond.gds.getGoal().level, wme_matching_this_cond.gds.getGoal());
                    }
                } /* end "wme in supergoal or arch-supported" */
                else
                {
                    /* wme must be local */

                    /* if wme's pref is o-supported, then just ignore it and
                    * move to next condition */
                    if (pref_for_this_wme.o_supported == true)
                    {
                        if (DEBUG_GDS)
                        {
                            context.getPrinter().print("         this wme is local and o-supported\n");
                        }
                        continue;
                    }

                    else
                    {
                        /* wme's pref is i-supported, so remember it's instantiation
                        * for later examination */

                        /* this test avoids "backtracing" through the top state */
                        if (inst.match_goal_level == 1)
                        {
                            if (DEBUG_GDS)
                            {
                                context.getPrinter().print("         don't back up through top state\n");
                                if (inst.prod != null)
                                    if (inst.prod.name != null)
                                        context.getPrinter().print(
                                                "         don't back up through top state for instantiation %s\n",
                                                inst.prod.name);
                            }
                            continue;
                        }

                        else
                        { /* (inst->match_goal_level != 1) */
                            if (DEBUG_GDS)
                            {
                                context.getPrinter().print("         this wme is local and i-supported\n");
                            }
                            Slot s = Slot.find_slot(pref_for_this_wme.id, pref_for_this_wme.attr);
                            if (s == null)
                            {
                                /* this must be an arch-wme from a fake instantiation */

                                if (DEBUG_GDS)
                                {
                                    context.getPrinter().print("here's the wme with no slot:\t");
                                    // TODO print_wme(thisAgent,
                                    // pref_for_this_wme->inst->top_of_instantiated_conditions->bt.wme_);
                                }

                                /* this is the same code as above, just using the 
                                * differently-named pointer.  it probably should
                                * be a subroutine */
                                {
                                    Wme fake_inst_wme_cond = pref_for_this_wme.inst.top_of_instantiated_conditions.bt.wme_;
                                    if (fake_inst_wme_cond.gds != null)
                                    {
                                        /* Then we want to check and see if the old GDS
                                        * value should be changed */
                                        if (fake_inst_wme_cond.gds.getGoal() == null)
                                        {
                                            /* The goal is NIL: meaning that the goal for
                                            * the GDS is no longer around */
                                            fake_inst_wme_cond.gds_next_prev.remove(fake_inst_wme_cond.gds.wmes_in_gds);

                                            /* We have to check for GDS removal anytime we take
                                            * a WME off the GDS wme list, not just when a WME
                                            * is removed from memory. */
                                            if (fake_inst_wme_cond.gds.wmes_in_gds.isEmpty())
                                            {
                                                fake_inst_wme_cond.gds = null;
                                                // free_memory(thisAgent, fake_inst_wme_cond->gds,
                                                // MISCELLANEOUS_MEM_USAGE);
                                                if (DEBUG_GDS)
                                                {
                                                    context.getPrinter().print("\n  REMOVING GDS FROM MEMORY.");
                                                }
                                            }

                                            /* JC ADDED: Separate adding wme to GDS as a function */
                                            add_wme_to_gds(inst.match_goal.gds, fake_inst_wme_cond);

                                            if (DEBUG_GDS)
                                            {
                                                context
                                                        .getPrinter()
                                                        .print(
                                                                "\n       .....GDS' goal is NIL so switching from old to new GDS list....\n");
                                            }
                                        }
                                        else if (fake_inst_wme_cond.gds.getGoal().level > inst.match_goal_level)
                                        {
                                            /* if the WME currently belongs to the GDS of a
                                            *goal below the current one */
                                            /* 1. Take WME off old (current) GDS list 
                                            * 2. Check to see if old GDS WME list is empty.
                                            *    If so, remove(free) it.
                                            * 3. Add WME to new GDS list
                                            * 4. Update WME pointer to new GDS list
                                            */
                                            if (inst.match_goal_level == 1)
                                            {
                                                // TODO necessary???
                                                context.getPrinter().print(
                                                        "\n\n\n\n\n HELLO! HELLO! The inst->match_goal_level is 1");
                                            }

                                            fake_inst_wme_cond.gds_next_prev.remove(fake_inst_wme_cond.gds.wmes_in_gds);
                                            if (fake_inst_wme_cond.gds.wmes_in_gds.isEmpty())
                                            {
                                                fake_inst_wme_cond.gds = null;
                                                // free_memory(thisAgent, fake_inst_wme_cond->gds,
                                                // MISCELLANEOUS_MEM_USAGE);
                                                if (DEBUG_GDS)
                                                {
                                                    context.getPrinter().print("\n  REMOVING GDS FROM MEMORY.");
                                                }
                                            }

                                            /* JC ADDED: Separate adding wme to GDS as a function */
                                            add_wme_to_gds(inst.match_goal.gds, fake_inst_wme_cond);

                                            if (DEBUG_GDS)
                                            {
                                                context.getPrinter().print(
                                                        "\n       .....switching from old to new GDS list....\n");
                                            }
                                            fake_inst_wme_cond.gds = inst.match_goal.gds;
                                        }
                                    }
                                    else
                                    {
                                        /* We know that the WME should be in the GDS of
                                        * the current goal if the WME's GDS does not
                                        * already exist. (i.e., if NIL GDS) */

                                        /* JC ADDED: Separate adding wme to GDS as a function */
                                        add_wme_to_gds(inst.match_goal.gds, fake_inst_wme_cond);

                                        if (fake_inst_wme_cond.gds.wmes_in_gds.first.previous != null)
                                        {
                                            context.getPrinter().print(
                                                    "\nDEBUG DEBUG : The new header should never have a prev value.\n");
                                        }
                                        if (DEBUG_GDS)
                                        {
                                            context
                                                    .getPrinter()
                                                    .print(
                                                            "\n       ......WME did not have defined GDS.  Now adding to goal [%s].\n",
                                                            fake_inst_wme_cond.gds.getGoal());
                                        }
                                    }
                                    if (DEBUG_GDS)
                                    {
                                        context.getPrinter().print("            Added WME to GDS for goal = %d [%s]\n",
                                                fake_inst_wme_cond.gds.getGoal().level, fake_inst_wme_cond.gds.getGoal());
                                    }
                                } /* matches { wme *fake_inst_wme_cond  */
                            }
                            else
                            {
                                /* this was the original "local & i-supported" action */
                                for (AsListItem<Preference> it = s.getFastPreferenceList(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE).first; it != null; it = it.next)
                                {
                                    final Preference pref = it.item;
                                    /*
                                    #ifdef DEBUG_GDS
                                                            print(thisAgent, "           looking at pref for the wme: ");
                                                            print_preference(thisAgent, pref); 
                                    #endif
                                    */

                                    /* REW: 2004-05-27: Bug fix
                                       We must check that the value with acceptable pref for the slot
                                       is the same as the value for the wme in the condition, since
                                       operators can have acceptable preferences for values other than
                                       the WME value.  We dont want to backtrack thru acceptable prefs
                                       for other operators */

                                    if (pref.value == wme_matching_this_cond.value)
                                    {

                                        /* REW BUG: may have to go over all insts regardless
                                        * of this visited_already flag... */

                                        if (pref.inst.GDS_evaluated_already == false)
                                        {

                                            if (DEBUG_GDS)
                                            {
                                                context.getPrinter().print(
                                                        "\n           adding inst that produced the pref to GDS: %s\n",
                                                        pref.inst.prod.name);
                                            }
                                            ////////////////////////////////////////////////////// 
                                            /* REW: 2003-12-07 */
                                            /* If the preference comes from a lower level inst, then 
                                            ignore it. */
                                            /* Preferences from lower levels must come from result 
                                            instantiations;
                                            we just want to use the justification/chunk 
                                            instantiations at the match goal level*/
                                            if (pref.inst.match_goal_level <= inst.match_goal_level)
                                            {

                                                ////////////////////////////////////////////////////// 
                                                uniquely_add_to_head_of_dll(pref.inst);
                                                pref.inst.GDS_evaluated_already = true;
                                                ////////////////////////////////////////////////////// 
                                            }
                                            /*
                                            #ifdef DEBUG_GDS
                                                                       else 
                                                                       {
                                                                          print_with_symbols(thisAgent, "\n           ignoring inst %y because it is at a lower level than the GDS\n",pref->inst->prod->name);
                                                                          pref->inst->GDS_evaluated_already = TRUE;
                                                                       }
                                            #endif
                                            */
                                            /* REW: 2003-12-07 */

                                            //////////////////////////////////////////////////////
                                        }
                                        /*
                                        #ifdef DEBUG_GDS
                                                                else 
                                                                {
                                                                   print(thisAgent, "           the inst producing this pref was already explored; skipping it\n"); 
                                                                }
                                        #endif
                                        */

                                    }
                                    /*
                                    #ifdef DEBUG_GDS
                                                            else
                                                            {
                                                               print("        this inst is for a pref with a differnt value than the condition WME; skippint it\n");
                                                            }
                                    #endif
                                    */
                                } /* for pref = s->pref[ACCEPTABLE_PREF ...*/
                            }
                        }
                    }
                }
            } /* for (cond = inst->top_of_instantiated_cond ...  *;*/

            /* remove just used instantiation from list */

            if (DEBUG_GDS)
            {
                context.getPrinter().print("\n      removing instantiation: %s\n", curr_pi.inst.prod.name);
            }

            if (curr_pi.next != null)
                curr_pi.next.prev = curr_pi.prev;

            if (curr_pi.prev != null)
                curr_pi.prev.next = curr_pi.next;

            if (parent_list_head == curr_pi)
                parent_list_head = curr_pi.next;

            temp_pi = curr_pi.next;
            //free(curr_pi);

        } /* end of "for (curr_pi = thisAgent->parent_list_head ... */

        if (parent_list_head != null)
        {

            if (DEBUG_GDS)
            {
                context.getPrinter().print("\n    RECURSING using these parents:\n");
                for (ParentInstantiation curr_pi = parent_list_head; curr_pi != null; curr_pi = curr_pi.next)
                {
                    context.getPrinter().print("      %s\n", curr_pi.inst.prod.name);
                }
            }

            /* recursively explore the parents of all the instantiations */
            elaborate_gds();

            /* free the parent instantiation list.  technically, the list
            * should be empty at this point ??? */
            free_parent_list();
        }

    }

    /**
     * REW BUG: this needs to be smarter to deal with wmes that get support 
     * from multiple instantiations. for example ^enemy-out-there could be 
     * made by 50 instantiations. if one of those instantiations goes, should 
     * the goal be killed???? This routine says "yes" -- anytime a dependent 
     * item gets changed, we're gonna yank out the goal -- even when that 
     * i-supported element itself may not be removed (due to multiple 
     * preferences). So, we'll say that this is a "twitchy" version of OPERAND2, 
     * and leave open the possibility that other approaches may be better
     * 
     * decide.cpp::3040:gds_invalid_so_remove_goal
     * 
     * @param w
     */
    public void gds_invalid_so_remove_goal(Wme w)
    {
        /* REW: begin 11.25.96 */
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // start_timer(thisAgent, &thisAgent->start_gds_tv);
        // #endif
        // #endif
        /* REW: end   11.25.96 */

        /* REW: BUG.  I have no idea right now if this is a terrible hack or
         * actually what we want to do.  The idea here is that the context of
         * the immediately higher goal above a retraction should be marked as
         * having its context changed in order that the architecture doesn't
         * look below this level for context changes.  I think it's a hack b/c
         * it seems like there should aready be mechanisms for doing this in
         * the architecture but I couldn't find any.
         */
        /* Note: the inner 'if' is correct -- we only want to change
         * highest_goal_whose_context_changed if the pointer is currently at
         * or below (greater than) the goal which we are going to retract.
         * However, I'm not so sure about the outer 'else.'  If we don't set
         * this to the goal above the retraction, even if the current value
         * is NIL, we still seg fault in certain cases.  But setting it as we do 
         * in the inner 'if' seems to clear up the difficulty.
         */

        if (context.tempMemory.highest_goal_whose_context_changed != null)
        {
            if (context.tempMemory.highest_goal_whose_context_changed.level >= w.gds.getGoal().level)
            {
                context.tempMemory.highest_goal_whose_context_changed = w.gds.getGoal().higher_goal;
            }
        }
        else
        {
            // If nothing has yet changed (highest_ ... = NIL) then set the goal automatically
            context.tempMemory.highest_goal_whose_context_changed = w.gds.getGoal().higher_goal;
        }
        
        context.trace.print(Category.TRACE_OPERAND2_REMOVALS_SYSPARAM, 
                            "\n    REMOVING GOAL [%s] due to change in GDS WME %s",
                            w.gds.getGoal(), w);
        
        remove_existing_context_and_descendents(w.gds.getGoal());
        /* BUG: Need to reset highest_goal here ???*/

        /* usually, we'd call do_buffered_wm_and_ownership_changes() here, but
         * we don't need to because it will be done at the end of the working
         * memory phases; cf. the end of do_working_memory_phase().
         */

        /* REW: begin 11.25.96 */
        //  #ifndef NO_TIMING_STUFF
        //  #ifdef DETAILED_TIMING_STATS
        //  stop_timer(thisAgent, &thisAgent->start_gds_tv, 
        //             &thisAgent->gds_cpu_time[thisAgent->current_phase]);
        //  #endif
        //  #endif
        /* REW: end   11.25.96 */
    }

    /**
     * decide.cpp:3107:free_parent_list
     */
    private void free_parent_list()
    {
        // parent_inst *curr_pi;
        //
        // for (curr_pi = thisAgent->parent_list_head;
        // curr_pi;
        // curr_pi = curr_pi->next)
        // free(curr_pi);

        this.parent_list_head = null;
    }

    /**
     * decide.cpp:3119:create_gds_for_goal
     * 
     * TODO Make this a GoalDependencySet constructor?
     * 
     * @param goal
     */
    private void create_gds_for_goal(Identifier goal)
    {
        goal.gds = new GoalDependencySet(goal);
        if (DEBUG_GDS)
        {
            context.getPrinter().print("\nCreated GDS for goal [%s].\n", goal);
        }
    }

    /**
     * decide.cpp:3132:count_candidates
     * 
     * @param candidates
     * @return
     */
    private int count_candidates(Preference candidates)
    {
        /*
           Count up the number of candidates
           REW: 2003-01-06
           I'm assuming that all of the candidates have unary or 
           unary+value (binary) indifferent preferences at this point.
           So we loop over the candidates list and count the number of
           elements in the list.
         */

        int numCandidates = 0;
        for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
            numCandidates++;

        return numCandidates;
    }
    

    /**
     * TODO This should probably go somewhere else
     * 
     * decide.cpp:2456:print_lowest_slot_in_context_stack
     * 
     * @param writer
     * @throws IOException
     */
    public void print_lowest_slot_in_context_stack(Writer writer) throws IOException
    {

        /* REW: begin 10.24.97 */
        /* This doesn't work yet so for now just print the last selection */
        /*  if (thisAgent->operand2_mode && 
         *   thisAgent->waitsnc &&
         *   thisAgent->waitsnc_detect) {
         * thisAgent->waitsnc_detect = FALSE;
         * print_stack_trace (thisAgent->wait_symbol,
         *                    thisAgent->bottom_goal, FOR_OPERATORS_TF, TRUE);
         * print(thisAgent, "\n waiting"); 
         * return;
         *  }
         */
        /* REW: end   10.24.97 */

        if (bottom_goal.operator_slot.getWmes() != null)
        {
            context.traceFormats.print_stack_trace(writer, bottom_goal.operator_slot.getWmes().value,
                    bottom_goal, TraceFormatRestriction.FOR_OPERATORS_TF, true);
        }

        /*
        this coded is needed just so that when an ONC is created in OPERAND
        (i.e. if the previous goal's operator slot is not empty), it's stack
        trace line doesn't get a number.  this is done because in OPERAND,
        ONCs are detected for "free".
        */

        else
        {

            if (context.operand2_mode)
            {
                context.traceFormats.print_stack_trace(writer, bottom_goal, bottom_goal,
                        TraceFormatRestriction.FOR_STATES_TF, true);
            }
            else
            {
                if (context.decisionCycle.d_cycle_count == 0)
                    context.traceFormats.print_stack_trace(writer, bottom_goal, bottom_goal,
                            TraceFormatRestriction.FOR_STATES_TF, true);
                else
                {
                    if (bottom_goal.higher_goal != null && bottom_goal.higher_goal.operator_slot.getWmes() != null)
                    {
                        context.traceFormats.print_stack_trace(writer, bottom_goal, bottom_goal,
                                TraceFormatRestriction.FOR_STATES_TF, true);
                    }
                    else
                    {
                        context.traceFormats.print_stack_trace(writer, bottom_goal, bottom_goal,
                                TraceFormatRestriction.FOR_STATES_TF, true);
                    }
                }
            }
        }
    }
}
