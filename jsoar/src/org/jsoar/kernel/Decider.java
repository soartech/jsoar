/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel;

import java.util.Dictionary;
import java.util.LinkedList;

import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
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

    
    private final SoarContext context;
    /**
     * agent.h:603:context_slots_with_changed_acceptable_preferences
     */
    private final ListHead<Slot> context_slots_with_changed_acceptable_preferences = new ListHead<Slot>();
    /**
     * agent.h:602:changed_slots
     */
    private final ListHead<Slot> changed_slots = new ListHead<Slot>();
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
    private final ListHead<Identifier> ids_with_unknown_level = new ListHead<Identifier>();
    /**
     * agent.h:607:disconnected_ids
     */
    private final ListHead<Identifier> disconnected_ids = new ListHead<Identifier>();
    
    
    
    private int mark_tc_number;
    private int level_at_which_marking_started;
    private int highest_level_anything_could_fall_from;
    private int lowest_level_anything_could_fall_to;
    private int walk_tc_number;
    private int walk_level;
    private Identifier top_goal;
    
    /**
     * agent.h:384:parent_list_head
     */
    private ParentInstantiation parent_list_head;
    
    /**
     * @param context
     */
    public Decider(SoarContext context)
    {
        this.context = context;
    }

    /**
     * Whenever some acceptable or require preference for a context slot
     * changes, we call mark_context_slot_as_acceptable_preference_changed().
     * 
     * decide.cpp:146:mark_context_slot_as_acceptable_preference_changed
     * 
     * @param s
     */
    private void mark_context_slot_as_acceptable_preference_changed(Slot s)
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
        for (Wme w : s.acceptable_preference_wmes)
            w.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;

        // now mark values for which we WANT a wme as "CANDIDATE" values
        for (Preference p : s.getPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE))
            p.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
        for (Preference p : s.getPreferenceList(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE))
            p.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;

        // remove any existing wme's that aren't CANDIDATEs; mark the rest as
        // ALREADY_EXISTING

        AsListItem<Wme> wmeItem = s.acceptable_preference_wmes.first;
        while (wmeItem != null)
        {
            AsListItem<Wme> next_w = wmeItem.next;
            Wme w = wmeItem.get();
            if (w.value.decider_flag == DeciderFlag.CANDIDATE_DECIDER_FLAG)
            {
                w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG;
                w.value.decider_wme = w;
                w.preference = null; /* we'll update this later */
            }
            else
            {
                w.next_prev.remove(s.acceptable_preference_wmes);
                /* REW: begin 09.15.96 */
                /*
                 * IF we lose an acceptable preference for an operator, then
                 * that operator comes out of the slot immediately in OPERAND2.
                 * However, if the lost acceptable preference is not for item in
                 * the slot, then we don;t need to do anything special until
                 * mini-quiescence.
                 */
                if (context.operand2_mode)
                    context.consistency.remove_operator_if_necessary(s, w);
                /* REW: end 09.15.96 */
                context.workingMemory.remove_wme_from_wm(w);
            }
            wmeItem = next_w;
        }

        // add the necessary wme's that don't ALREADY_EXIST

        for (Preference p : s.getPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE))
        {
            if (p.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG)
            {
                // found existing wme, so just update its trace
                Wme w = p.value.decider_wme;
                if (w.preference == null)
                    w.preference = p;
            }
            else
            {
                Wme w = new Wme(p.id, p.attr, p.value, true, 0);
                w.next_prev.insertAtHead(s.acceptable_preference_wmes);
                w.preference = p;
                context.workingMemory.add_wme_to_wm(w);
                p.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG;
                p.value.decider_wme = w;
            }
        }

        for (Preference p : s.getPreferenceList(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE))
        {
            if (p.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG)
            {
                // found existing wme, so just update its trace
                Wme w = p.value.decider_wme;
                if (w.preference == null)
                    w.preference = p;
            }
            else
            {
                Wme w = new Wme(p.id, p.attr, p.value, true, 0);
                w.next_prev.insertAtHead(s.acceptable_preference_wmes);
                w.preference = p;
                context.workingMemory.add_wme_to_wm(w);
                p.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG;
                p.value.decider_wme = w;
            }
        }
    }
    

    /**
     * At the end of the phase, do_buffered_acceptable_preference_wme_changes()
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
            Slot s = dc.get();
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

        // #ifdef DEBUG_LINKS
        // if (from)
        // print_with_symbols (thisAgent, "\nAdding link from %y to %y", from,
        // to);
        // else
        // print_with_symbols (thisAgent, "\nAdding special link to %y", to);
        // print (" (count=%lu)", to->id.link_count);
        // #endif

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
        for (Wme w : id.input_wmes)
            promote_if_needed(w.value, new_level);
        
        for (Slot s : id.slots)
        {
            for (Preference pref : s.all_preferences)
            {
                promote_if_needed(pref.value, new_level);
                if (pref.type.isBinary())
                    promote_if_needed(pref.referent, new_level);
            }
            for (Wme w : s.wmes)
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

        // #ifdef DEBUG_LINKS
        // if (from) {
        // print_with_symbols (thisAgent, "\nRemoving link from %y to %y", from,
        // to);
        // print (" (%d to %d)", from->id.level, to->id.level);
        // } else {
        // print_with_symbols (thisAgent, S"\nRemoving special link to %y ",
        // to);
        // print (" (%d)", to->id.level);
        // }
        // print (" (count=%lu)", to->id.link_count);
        // #endif

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
        // #ifdef DEBUG_LINKS
        // print_with_symbols (thisAgent, "\n*** Garbage collecting id: %y",id);
        // #endif

        /*
         * Note--for goal/impasse id's, this does not remove the impasse wme's.
         * This is handled by remove_existing_such-and-such...
         */

        // remove any input wmes from the id
        context.workingMemory.remove_wme_list_from_wm(id.input_wmes.getFirstItem(), true);
        id.input_wmes.first = null;

        for (Slot s : id.slots)
        {
            /* --- remove any existing attribute impasse for the slot --- */
            if (s.impasse_type != ImpasseType.NONE_IMPASSE_TYPE)
                remove_existing_attribute_impasse_for_slot(s);

            /* --- remove all wme's from the slot --- */
            context.workingMemory.remove_wme_list_from_wm(s.wmes.getFirstItem(), false);
            s.wmes.first = null;

            /* --- remove all preferences for the slot --- */
            AsListItem<Preference> pref = s.all_preferences.first;
            while (pref != null)
            {
                AsListItem<Preference> next_pref = pref.next;
                context.prefMemory.remove_preference_from_tm(pref.get());

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
        for (Wme w : id.input_wmes)
            mark_unknown_level_if_needed(w.value);
        for (Slot s : id.slots)
        {
            for (Preference pref : s.all_preferences)
            {
                mark_unknown_level_if_needed(pref.value);
                if (pref.type.isBinary())
                    mark_unknown_level_if_needed(pref.referent);
            }
            if (s.impasse_id != null)
                mark_unknown_level_if_needed(s.impasse_id);
            for (Wme w : s.wmes)
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
            AsListItem<Identifier> dc = id.unknown_level;
            dc.remove(this.ids_with_unknown_level);
            id.unknown_level = null;
            id.level = this.walk_level;
            id.promotion_level = this.walk_level;
        }

        // scan through all preferences and wmes for all slots for this id
        for (Wme w : id.input_wmes)
            update_levels_if_needed(w.value);
        for (Slot s : id.slots)
        {
            for (Preference pref : s.all_preferences)
            {
                update_levels_if_needed(pref.value);
                if (pref.type.isBinary())
                    update_levels_if_needed(pref.referent);
            }
            if (s.impasse_id != null)
                update_levels_if_needed(s.impasse_id);
            for (Wme w : s.wmes)
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
            Identifier id = dc.get();
            if (id.link_count == 0)
            {
                dc.remove(this.ids_with_unknown_level);
                dc.insertAtHead(this.disconnected_ids);
            }
        }

        /* --- keep garbage collecting ids until nothing left to gc --- */
        this.link_update_mode = LinkUpdateType.UPDATE_DISCONNECTED_IDS_LIST;
        while (!this.disconnected_ids.isEmpty())
        {
            dc = disconnected_ids.first;
            this.disconnected_ids.first = dc.next;
            Identifier id = dc.get();
            garbage_collect_id(id);
        }
        this.link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;

        /* --- if nothing's left with an unknown level, we're done --- */
        if (this.ids_with_unknown_level.isEmpty())
            return;

        /* --- do the mark --- */
        this.highest_level_anything_could_fall_from = SoarConstants.LOWEST_POSSIBLE_GOAL_LEVEL;
        this.lowest_level_anything_could_fall_to = -1;
        this.mark_tc_number = context.syms.get_new_tc_number();
        for (dc = this.ids_with_unknown_level.first; dc != null; dc = dc.next)
        {
            Identifier id = dc.get();
            this.level_at_which_marking_started = id.level;
            mark_id_and_tc_as_unknown_level(id);
        }

        /* --- do the walk --- */
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

        /* --- GC anything left with an unknown level after the walk --- */
        this.link_update_mode = LinkUpdateType.JUST_UPDATE_COUNT;
        while (!ids_with_unknown_level.isEmpty())
        {
            dc = ids_with_unknown_level.first;
            this.ids_with_unknown_level.first = dc.next;
            Identifier id = dc.get();
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

        /* --- collect set of required items into candidates list --- */
        for (Preference p : s.getPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE))
            p.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
        Preference candidates = null;
        for (Preference p : s.getPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE))
        {
            if (p.value.decider_flag == DeciderFlag.NOTHING_DECIDER_FLAG)
            {
                p.next_candidate = candidates;
                candidates = p;
                /*
                 * --- unmark it, in order to prevent it from being added twice
                 * ---
                 */
                p.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
            }
        }
        result_candidates.value = candidates;

        /* --- if more than one required item, we have a constraint failure --- */
        if (candidates.next_candidate != null)
            return ImpasseType.CONSTRAINT_FAILURE_IMPASSE_TYPE;

        /* --- just one require, check for require-prohibit impasse --- */
        Symbol value = candidates.value;
        for (Preference p : s.getPreferenceList(PreferenceType.PROHIBIT_PREFERENCE_TYPE))
            if (p.value == value)
                return ImpasseType.CONSTRAINT_FAILURE_IMPASSE_TYPE;

        /* --- the lone require is the winner --- */
        if (candidates != null && context.rl.rl_enabled())
        {
            // TODO reinforcement learning
            // exploration_compute_value_of_candidate( thisAgent, candidates, s,
            // 0 );
            //       rl_perform_update( thisAgent, candidates->numeric_value, s->id );
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
     * @param consistency
     * @param predict
     * @return
     */
    private ImpasseType run_preference_semantics(Slot s, ByRef<Preference> result_candidates,
            boolean consistency /* = false */, boolean predict /* = false */)
    {
        // preference *p, *p2, *cand, *prev_cand;

        /* --- if the slot has no preferences at all, things are trivial --- */
        if (s.all_preferences.isEmpty())
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
                Preference force_result = context.decisionManip.select_force(s.all_preferences.first.get(), !predict);

                if (force_result != null)
                {
                    result_candidates.value = force_result;

                    if (!predict && context.rl.rl_enabled())
                    {
                        // TODO reinforcement learning
                        // exploration_compute_value_of_candidate( thisAgent,
                        // force_result, s, 0 );
                        // rl_perform_update( thisAgent,
                        // force_result->numeric_value, s->id );
                    }

                    return ImpasseType.NONE_IMPASSE_TYPE;
                }
                else
                {
                    // TODO warning
                    // print( thisAgent, "WARNING: Invalid forced selection
                    // operator id" );
                    // xml_generate_warning( thisAgent, "WARNING: Invalid forced
                    // selection operator id" );
                }
            }
        }

        /* === Requires === */
        if (!s.getPreferenceList(PreferenceType.REQUIRE_PREFERENCE_TYPE).isEmpty())
        {
            return require_preference_semantics(s, result_candidates);
        }

        /* === Acceptables, Prohibits, Rejects === */

        /*
         * --- mark everything that's acceptable, then unmark the prohibited and
         * rejected items ---
         */
        for (Preference p : s.getPreferenceList(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE))
            p.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
        for (Preference p : s.getPreferenceList(PreferenceType.PROHIBIT_PREFERENCE_TYPE))
            p.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
        for (Preference p : s.getPreferenceList(PreferenceType.REJECT_PREFERENCE_TYPE))
            p.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;

        /* --- now scan through acceptables and build the list of candidates --- */
        Preference candidates = null;
        for (Preference p : s.getPreferenceList(PreferenceType.ACCEPTABLE_PREFERENCE_TYPE))
        {
            if (p.value.decider_flag == DeciderFlag.CANDIDATE_DECIDER_FLAG)
            {
                p.next_candidate = candidates;
                candidates = p;
                /*
                 * --- unmark it, in order to prevent it from being added twice
                 * ---
                 */
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
                // exploration_compute_value_of_candidate( thisAgent,
                // candidates, s, 0 );
                // rl_perform_update( thisAgent, candidates->numeric_value,
                // s->id );
            }

            return ImpasseType.NONE_IMPASSE_TYPE;
        }

        /* === Better/Worse === */
        if (!s.getPreferenceList(PreferenceType.BETTER_PREFERENCE_TYPE).isEmpty()
                || !s.getPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE).isEmpty())
        {
            Symbol j, k;

            /* -------------------- Algorithm to find conflicted set: 
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

            for (Preference p : s.getPreferenceList(PreferenceType.BETTER_PREFERENCE_TYPE))
            {
                p.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
                p.referent.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
            }
            for (Preference p : s.getPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE))
            {
                p.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
                p.referent.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
            }
            for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
            {
                cand.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
            }
            for (Preference p : s.getPreferenceList(PreferenceType.BETTER_PREFERENCE_TYPE))
            {
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
                        for (Preference p2 : s.getPreferenceList(PreferenceType.BETTER_PREFERENCE_TYPE))
                            if ((p2.value == k) && (p2.referent == j))
                            {
                                j.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                k.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                break;
                            }
                        for (Preference p2 : s.getPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE))
                            if ((p2.value == j) && (p2.referent == k))
                            {
                                j.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                k.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                break;
                            }
                    }
                }
            }
            for (Preference p : s.getPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE))
            {
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
                        for (Preference p2 : s.getPreferenceList(PreferenceType.WORSE_PREFERENCE_TYPE))
                            if ((p2.value == k) && (p2.referent == j))
                            {
                                j.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                k.decider_flag = DeciderFlag.CONFLICTED_DECIDER_FLAG;
                                break;
                            }
                    }
                }
            }

            /*
             * --- now scan through candidates list, look for conflicted stuff
             * ---
             */
            Preference cand = null, prev_cand = null;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                if (cand.value.decider_flag == DeciderFlag.CONFLICTED_DECIDER_FLAG)
                    break;
            if (cand != null)
            {
                /*
                 * --- collect conflicted candidates into new candidates list
                 * ---
                 */
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

            /*
             * --- no conflicts found, remove former_candidates from candidates
             * ---
             */
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
        if (!s.getPreferenceList(PreferenceType.BEST_PREFERENCE_TYPE).isEmpty())
        {
            Preference cand, prev_cand;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                cand.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
            for (Preference p : s.getPreferenceList(PreferenceType.BEST_PREFERENCE_TYPE))
                p.value.decider_flag = DeciderFlag.BEST_DECIDER_FLAG;
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
        if (!s.getPreferenceList(PreferenceType.WORST_PREFERENCE_TYPE).isEmpty())
        {
            Preference cand, prev_cand;
            for (cand = candidates; cand != null; cand = cand.next_candidate)
                cand.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
            for (Preference p : s.getPreferenceList(PreferenceType.WORST_PREFERENCE_TYPE))
                p.value.decider_flag = DeciderFlag.WORST_DECIDER_FLAG;
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
                // exploration_compute_value_of_candidate( thisAgent,
                // candidates, s, 0 );
                // rl_perform_update( thisAgent, candidates->numeric_value,
                // s->id );
            }

            return ImpasseType.NONE_IMPASSE_TYPE;
        }

        /* === Indifferents === */
        for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
        for (Preference p : s.getPreferenceList(PreferenceType.UNARY_INDIFFERENT_PREFERENCE_TYPE))
            p.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT_DECIDER_FLAG;

        for (Preference p : s.getPreferenceList(PreferenceType.NUMERIC_INDIFFERENT_PREFERENCE_TYPE))
            p.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT_CONSTANT_DECIDER_FLAG;

        for (Preference p : s.getPreferenceList(PreferenceType.BINARY_INDIFFERENT_PREFERENCE_TYPE))
            if ((p.referent.asIntConstant() != null) || (p.referent.asFloatConstant() != null))
                p.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT_CONSTANT_DECIDER_FLAG;

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
                for (Preference p2 : s.getPreferenceList(PreferenceType.BINARY_INDIFFERENT_PREFERENCE_TYPE))
                    if (((p2.value == cand.value) && (p2.referent == p.value))
                            || ((p2.value == p.value) && (p2.referent == cand.value)))
                    {
                        match_found = true;
                        break;
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

        /* --- items not all indifferent; for context slots this gives a tie --- */
        if (s.isa_context_slot)
        {
            result_candidates.value = candidates;
            return ImpasseType.TIE_IMPASSE_TYPE;
        }

        /* === Parallels === */
        for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
        for (Preference p : s.getPreferenceList(PreferenceType.UNARY_PARALLEL_PREFERENCE_TYPE))
            p.value.decider_flag = DeciderFlag.UNARY_PARALLEL_DECIDER_FLAG;
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
                for (Preference p2 : s.getPreferenceList(PreferenceType.BINARY_PARALLEL_PREFERENCE_TYPE))
                    if (((p2.value == cand.value) && (p2.referent == p.value))
                            || ((p2.value == p.value) && (p2.referent == cand.value)))
                    {
                        match_found = true;
                        break;
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
        Wme w = new Wme(id, attr, value, false, 0);
        w.next_prev.insertAtHead(id.impasse_wmes);
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
    private Identifier create_new_impasse(boolean isa_goal, Identifier object, Symbol attr, ImpasseType impasse_type,
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
        // soar_invoke_callbacks(thisAgent,
        // CREATE_NEW_ATTRIBUTE_IMPASSE_CALLBACK,
        // (soar_call_data) s);
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
        // soar_invoke_callbacks(thisAgent,
        // REMOVE_ATTRIBUTE_IMPASSE_CALLBACK,
        // (soar_call_data) s);

        Identifier id = s.impasse_id;
        s.impasse_id = null;
        s.impasse_type = ImpasseType.NONE_IMPASSE_TYPE;

        context.workingMemory.remove_wme_list_from_wm(id.impasse_wmes.getFirstItem(), false);
        id.impasse_wmes.first = null;
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
        AsListItem<Wme> ap_wme;
        for (ap_wme = s.acceptable_preference_wmes.first; ap_wme != null; ap_wme = ap_wme.next)
            if (ap_wme.get().value == cand.value)
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
        pref.inst = inst;
        inst.preferences_generated.first = pref.inst_next_prev;
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
        cond.id_test = new EqualityTest(ap_wme.get().id); // make_equality_test
                                                            // (ap_wme->id);
        cond.attr_test = new EqualityTest(ap_wme.get().attr);
        cond.value_test = new EqualityTest(ap_wme.get().value);
        cond.test_for_acceptable_preference = true;
        cond.bt.wme_ = ap_wme.get();
        if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
        {
            ap_wme.get().wme_add_ref();
        }
        else
        {
            if (inst.match_goal_level > SoarConstants.TOP_GOAL_LEVEL)
                ap_wme.get().wme_add_ref();
        }
        cond.bt.level = ap_wme.get().id.level;

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

        /* --- reset flags on existing items to "NOTHING" --- */
        for (Wme w : id.impasse_wmes)
            if (w.attr == predefined.item_symbol)
                w.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;

        /* --- mark set of desired items as "CANDIDATEs" --- */
        for (Preference cand = items; cand != null; cand = cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;

        /*
         * --- for each existing item: if it's supposed to be there still, then
         * mark it "ALREADY_EXISTING"; otherwise remove it ---
         */
        AsListItem<Wme> wmeItem = id.impasse_wmes.first;
        while (wmeItem != null)
        {
            final Wme w = wmeItem.get();
            final AsListItem<Wme> next_w = wmeItem.next;
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
                    w.next_prev.remove(id.impasse_wmes);
                    if (id.isa_goal)
                        remove_fake_preference_for_goal_item(w.preference);
                    context.workingMemory.remove_wme_from_wm(w);
                }
            }

            // SBW 5/07
            // remove item-count WME if it exists
            else if (w.attr == predefined.item_count_symbol)
            {
                w.next_prev.remove(id.impasse_wmes);
                context.workingMemory.remove_wme_from_wm(w);
            }

            wmeItem = next_w;
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
        // TODO does the int constant get its reference removed when the impasse goes
        // away?
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
         for (Wme w : s.wmes)
            w.value.decider_flag = DeciderFlag.NOTHING_DECIDER_FLAG;
         
         // set marks on desired values to "CANDIDATES"
         for (Preference cand=candidates.value; cand!=null; cand=cand.next_candidate)
            cand.value.decider_flag = DeciderFlag.CANDIDATE_DECIDER_FLAG;
         
            // for each existing wme, if we want it there, mark it as ALREADY_EXISTING; otherwise remove it
         AsListItem<Wme> wmeItem = s.wmes.first;
         while (wmeItem != null) 
         {
            final Wme w = wmeItem.get();
            final AsListItem<Wme> next_w = wmeItem.next;
            if (w.value.decider_flag == DeciderFlag.CANDIDATE_DECIDER_FLAG) 
            {
               w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG;
               w.value.decider_wme = w; /* so we can set the pref later */
            } 
            else 
            {
               w.next_prev.remove(s.wmes);
               /* REW: begin 09.15.96 */
               if (context.operand2_mode)
               {
                  if (w.gds != null) 
                  {
                     if (w.gds.goal != null)
                     {
                         // TODO verbose trace
    //                    /* If the goal pointer is non-NIL, then goal is in the stack */
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
               /* REW: end   09.15.96 */
               context.workingMemory.remove_wme_from_wm (w);
            }
            wmeItem = next_w;
         } 
         
         /* --- for each desired value, if it's not already there, add it --- */
         for (Preference cand=candidates.value; cand!=null; cand=cand.next_candidate) 
         {
            if (cand.value.decider_flag==DeciderFlag.ALREADY_EXISTING_WME_DECIDER_FLAG)
            {
               /* REW: begin 11.22.97 */ 
               /* print(thisAgent, "\n This WME was marked as already existing...."); print_wme(cand->value->common.a.decider_wme); */
               
               /* REW: end   11.22.97 */ 
               cand.value.decider_wme.preference = cand;
            } 
            else 
            {
               Wme w = new Wme(cand.id, cand.attr, cand.value, false, 0);
               w.next_prev.insertAtHead(s.wmes);
               w.preference = cand;
               
               /* REW: begin 09.15.96 */
               if (context.operand2_mode)
               {
               /* Whenever we add a WME to WM, we also want to check and see if
               this new WME is o-supported.  If so, then we want to add the
               supergoal dependencies of the new, o-supported element to the
               goal in which the element was created (as long as the o_supported
               element was not created in the top state -- the top goal has
                  no gds).  */
                  
                  /* REW: begin 11.25.96 */ 
    //#ifndef NO_TIMING_STUFF
    //#ifdef DETAILED_TIMING_STATS
    //              start_timer(thisAgent, &thisAgent->start_gds_tv);
    //#endif 
    //#endif
                  /* REW: end   11.25.96 */ 
                  
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
                           
                           create_gds_for_goal( thisAgent, w.preference.inst.match_goal );
                           
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
    //#ifdef DEBUG_GDS_HIGH
    //                    print(thisAgent, thisAgent, "\n\n   "); print_preference(pref);
    //                    print(thisAgent, "   Goal level of preference: %d\n",
    //                       pref->id->id.level);
    //#endif
                        
                        if (pref.inst.GDS_evaluated_already == false) {
    //#ifdef DEBUG_GDS_HIGH
    //                       print_with_symbols(thisAgent, "   Match goal lev of instantiation %y ",
    //                          pref->inst->prod->name);
    //                       print(thisAgent, "is %d\n", pref->inst->match_goal_level);
    //#endif
                           if (pref.inst.match_goal_level > pref.id.level) {
    //#ifdef DEBUG_GDS_HIGH
    //                          print_with_symbols(thisAgent, "        %y  is simply the instantiation that led to a chunk.\n        Not adding it the current instantiations.\n", pref->inst->prod->name);
    //#endif
                              
                           } else {
    //#ifdef DEBUG_GDS_HIGH
    //                          print_with_symbols(thisAgent, "\n   Adding %y to list of parent instantiations\n", pref->inst->prod->name); 
    //#endif
                              uniquely_add_to_head_of_dll(pref.inst);
                              pref.inst.GDS_evaluated_already = true;
                           }
                        }  /* end if GDS_evaluated_already is FALSE */
    //#ifdef DEBUG_GDS_HIGH
    //                    else
    //                       print_with_symbols(thisAgent, "\n    Instantiation %y was already explored; skipping it\n", pref->inst->prod->name);
    //#endif
                        
                     }  /* end of forloop over preferences for this wme */
                     
                     
    //#ifdef DEBUG_GDS_HIGH
    //                 print(thisAgent, "\n    CALLING ELABORATE GDS....\n");
    //#endif 
                     elaborate_gds();
                     
                     /* technically, the list should be empty at this point ??? */
                     
                     free_parent_list(); 
    //#ifdef DEBUG_GDS_HIGH
    //                 print(thisAgent, "    FINISHED ELABORATING GDS.\n\n");
    //#endif
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
       if (!s.wmes.isEmpty()) 
       {  
          // remove any existing wmes
          context.workingMemory.remove_wme_list_from_wm (s.wmes.getFirstItem(), false); 
          s.wmes.first = null;
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
        while (!changed_slots.isEmpty())
        {
            AsListItem<Slot> dc = changed_slots.first;
            changed_slots.first = changed_slots.first.next;
            Slot s = dc.get();
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
        if (s.wmes.isEmpty())
            return s.changed != null;

        Symbol v = s.wmes.getFirstItem().value;
        for (Preference p : s.getPreferenceList(PreferenceType.RECONSIDER_PREFERENCE_TYPE))
        {
            if (v == p.value)
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
    private void remove_wmes_for_context_slot(Slot s)
    {
        if (s.wmes.isEmpty())
            return;
        /*
         * Note that we only need to handle one wme--context slots never have
         * more than one wme in them
         */
        Wme w = s.wmes.getFirstItem();
        w.preference.preference_remove_ref(context.prefMemory);
        context.workingMemory.remove_wme_from_wm(w);
        s.wmes.first = null;
    }    
}
