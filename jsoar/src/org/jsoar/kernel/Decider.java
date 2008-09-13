/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel;

import java.util.LinkedList;

import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * 
 * decide.cpp
 * @author ray
 */
public class Decider
{
    private final SoarContext context;
    /**
     * agent.h:603:context_slots_with_changed_acceptable_preferences
     */
    private final ListHead<Slot> context_slots_with_changed_acceptable_preferences = new ListHead<Slot>();
    /**
     * agent.h:615:promoted_ids
     */
    private final LinkedList<Identifier> promoted_ids = new LinkedList<Identifier>();

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
}
