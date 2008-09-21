/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 7, 2008
 */
package org.jsoar.kernel.memory;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * prefmem.cpp
 * 
 * @author ray
 */
public class PreferenceMemory
{
    private final Agent context;

    
    /**
     * @param tempMem
     * @param operator_symbol
     */
    public PreferenceMemory(Agent context)
    {
        this.context = context;
    }

    /**
     * prefmem.cpp:100:deallocate_preference
     * 
     * @param pref
     */
    void deallocate_preference (Preference pref) 
    {
//        #ifdef DEBUG_PREFS  
//          print (thisAgent, "\nDeallocating preference at 0x%8x: ",(unsigned long)pref);
//          print_preference (thisAgent, pref);
//          if (pref->reference_count != 0) {   /* --- sanity check --- */
//            char msg[BUFFER_MSG_SIZE];
//            strncpy (msg, "prefmem.c: Internal Error: Deallocating preference with ref. count != 0\n", BUFFER_MSG_SIZE);
//            msg[BUFFER_MSG_SIZE - 1] = 0; /* ensure null termination */
//            abort_with_fatal_error(thisAgent, msg);
//          }
//        #endif

        // remove it from the list of pref's for its match goal
        if (pref.on_goal_list)
        {
            pref.all_of_goal.remove(pref.inst.match_goal.preferences_from_goal);
        }

        // remove it from the list of pref's from that instantiation
        pref.inst_next_prev.remove(pref.inst.preferences_generated);

        context.recMemory.possibly_deallocate_instantiation(pref.inst);

        if (pref.type.isBinary())
        {
            //symbol_remove_ref (thisAgent, pref->referent);
        }
    } 
    
    /**
     * Possibly_deallocate_preference_and_clones() checks whether a given
     * preference and all its clones have reference_count 0, and deallocates
     * them all if they do. It returns TRUE if they were actually deallocated,
     * FALSE otherwise.
     * 
     * prefmem.cpp:141:possibly_deallocate_preference_and_clones
     * 
     * @param pref
     * @return
     */
    boolean possibly_deallocate_preference_and_clones(Preference pref)
    {
        if (pref.reference_count > 0)
        {
            return false;
        }
        for (Preference clone = pref.next_clone; clone != null; clone = clone.next_clone)
        {
            if (clone.reference_count > 0)
            {
                return false;
            }
        }
        for (Preference clone = pref.prev_clone; clone != null; clone = clone.prev_clone)
        {
            if (clone.reference_count > 0)
            {
                return false;
            }
        }

        // deallocate all the clones
        Preference clone = pref.next_clone;
        while (clone != null)
        {
            Preference next = clone.next_clone;
            deallocate_preference(clone);
            clone = next;
        }
        clone = pref.prev_clone;
        while (clone != null)
        {
            Preference next = clone.prev_clone;
            deallocate_preference(clone);
            clone = next;
        }

        /* --- deallocate pref --- */
        deallocate_preference(pref);

        return true;
    } 
    
    /**
     * Remove_preference_from_clones() splices a given preference out of the
     * list of clones. If the preference's reference_count is 0, it also
     * deallocates it and returns TRUE. Otherwise it returns FALSE.
     * 
     * prefmem.cpp:176:remove_preference_from_clones
     * 
     * @param pref
     * @return
     */
    public boolean remove_preference_from_clones(Preference pref)
    {
        Preference any_clone = null;
        if (pref.next_clone != null)
        {
            any_clone = pref.next_clone;
            pref.next_clone.prev_clone = pref.prev_clone;
        }
        if (pref.prev_clone != null)
        {
            any_clone = pref.prev_clone;
            pref.prev_clone.next_clone = pref.next_clone;
        }
        pref.next_clone = pref.prev_clone = null;
        if (any_clone != null)
            possibly_deallocate_preference_and_clones(any_clone);
        if (pref.reference_count == 0)
        {
            deallocate_preference(pref);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Add_preference_to_tm() adds a given preference to preference memory (and
     * hence temporary memory). 
     * 
     * prefmem.cpp:203:add_preference_to_tm
     * 
     * @param pref
     */
    public void add_preference_to_tm(Preference pref)
    {
        // #ifdef DEBUG_PREFS
        // print (thisAgent, "\nAdd preference at 0x%8x: ",(unsigned long)pref);
        // print_preference (thisAgent, pref);
        // #endif

        // JC: This will retrieve the slot for pref->id if it already exists
        Slot s = Slot.make_slot(pref.id, pref.attr, context.predefinedSyms.operator_symbol);
        pref.slot = s;
        pref.all_of_slot.insertAtHead(s.all_preferences);

        // add preference to the list (in the right place, according to match
        // goal level of the instantiations) for the slot
        ListHead<Preference> s_prefs = s.getPreferenceList(pref);
        if (s_prefs.isEmpty())
        {
            // this is the only pref. of its type, just put it at the head
            pref.next_prev.insertAtHead(s_prefs);
        }
        else if (s_prefs.getFirstItem().inst.match_goal_level >= pref.inst.match_goal_level)
        {
            // it belongs at the head of the list, so put it there
            pref.next_prev.insertAtHead(s_prefs);
        }
        else
        {
            // scan through the pref. list, find the one to insert after
            AsListItem<Preference> it = s_prefs.first;
            for (; it.next != null; it = it.next)
            {
                Preference p2 = it.get();
                Preference next = it.getNextItem();

                if (p2.inst.match_goal_level >= pref.inst.match_goal_level)
                {
                    break;
                }
            }

            // insert pref after it
            pref.next_prev.insertAfter(s_prefs, it);
            // pref.next_prev.next = it.next;
            // pref.next_prev.previous = it;
            // it.next = pref.next_prev;
            // if (pref.next_prev.next != null)
            // {
            // pref.next_prev.next.previous = pref.next_prev;
            // }
        }

        // other miscellaneous stuff
        pref.in_tm = true;
        pref.preference_add_ref();

        context.tempMemory.mark_slot_as_changed(s);

        // update identifier levels
        Identifier valueId = pref.value.asIdentifier();
        if (valueId != null)
        {
            context.decider.post_link_addition (pref.id, valueId);
        }

        if (pref.type.isBinary())
        {
            Identifier refId = pref.referent.asIdentifier();
            if (refId != null)
            {
                context.decider.post_link_addition (pref.id, refId);
            }
        }

        // if acceptable/require pref for context slot, we may need to add a wme
        // later
        if (s.isa_context_slot
                && (pref.type == PreferenceType.ACCEPTABLE_PREFERENCE_TYPE || pref.type == PreferenceType.REQUIRE_PREFERENCE_TYPE))
        {
            context.decider.mark_context_slot_as_acceptable_preference_changed (s);
        }
    }

    /**
     * removes a given preference from PM and TM.
     * 
     * prefmem.cpp:282:remove_preference_from_tm
     * 
     * @param pref
     */
    public void remove_preference_from_tm(Preference pref)
    {
        Slot s = pref.slot;

        // #ifdef DEBUG_PREFS
        // print (thisAgent, "\nRemove preference at 0x%8x: ",(unsigned
        // long)pref);
        // print_preference (thisAgent, pref);
        // #endif

        // remove preference from the list for the slot
        pref.all_of_slot.remove(s.all_preferences);
        pref.next_prev.remove(s.getPreferenceList(pref));

        // other miscellaneous stuff
        pref.in_tm = false;
        pref.slot = null; // BUG shouldn't we use pref->slot in place of pref->in_tm?

        context.tempMemory.mark_slot_as_changed(s);

        /// if acceptable/require pref for context slot, we may need to remove a wme later
        if ((s.isa_context_slot)
                && ((pref.type == PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) || (pref.type == PreferenceType.REQUIRE_PREFERENCE_TYPE)))
        {
            context.decider.mark_context_slot_as_acceptable_preference_changed(s);
        }

        // update identifier levels
        Identifier valueId = pref.value.asIdentifier();
        if (valueId != null)
        {
            context.decider.post_link_removal (pref.id, valueId);
        }
        if (pref.type.isBinary())
        {
            Identifier refId = pref.referent.asIdentifier();
            if (refId != null)
            {
                context.decider.post_link_removal (pref.id, refId);
            }
        }

        // deallocate it and clones if possible
        pref.preference_remove_ref(this);
    }

    /**
     * Process_o_rejects_and_deallocate_them() handles the processing of
     * o-supported reject preferences. This routine is called from the firer and
     * passed a list of all the o-rejects generated in the current preference
     * phase (the list is linked via the "next" fields on the preference
     * structures). This routine removes all preferences for matching values
     * from TM, and deallocates the o-reject preferences when done.
     * 
     * prefmem.cpp:330:process_o_rejects_and_deallocate_them
     * 
     * @param o_rejects
     */
    public void process_o_rejects_and_deallocate_them(AsListItem<Preference> o_rejects)
    {
        // preference *pref, *next_pref, *p, *next_p;

        for (AsListItem<Preference> pref = o_rejects; pref != null; pref = pref.next)
        {
            pref.get().preference_add_ref(); /*
                                                 * prevents it from being
                                                 * deallocated if it's a clone
                                                 * of some other pref we're
                                                 * about to remove
                                                 */
            // #ifdef DEBUG_PREFS
            // print (thisAgent, "\nO-reject posted at 0x%8x: ",(unsigned
            // long)pref);
            // print_preference (thisAgent, pref);
            // #endif
        }

        AsListItem<Preference> prefIt = o_rejects;
        while (prefIt != null)
        {
            AsListItem<Preference> next_pref = prefIt.next;
            Preference pref = prefIt.get();
            Slot s = Slot.find_slot(pref.id, pref.attr);
            if (s != null)
            {
                // remove all pref's in the slot that have the same value
                AsListItem<Preference> p = s.all_preferences.first;
                while (p != null)
                {
                    AsListItem<Preference> next_p = p.next;
                    if (p.get().value == pref.value)
                    {
                        remove_preference_from_tm(p.get());
                    }
                    p = next_p;
                }
            }
            pref.preference_remove_ref(this);
            prefIt = next_pref;
        }
    }

}
