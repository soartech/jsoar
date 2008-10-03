/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 7, 2008
 */
package org.jsoar.kernel.memory;

import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.Identifier;

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
        assert !pref.deallocated;
        assert pref.reference_count == 0;
        
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
        
        pref.deallocated = true;
        
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
        s.addPreference(pref);

        // other miscellaneous stuff
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
                && (pref.type == PreferenceType.ACCEPTABLE_PREFERENCE_TYPE || 
                    pref.type == PreferenceType.REQUIRE_PREFERENCE_TYPE))
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
        s.removePreference(pref);

        // other miscellaneous stuff

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
     * phases (the list is linked via the "next" fields on the preference
     * structures). This routine removes all preferences for matching values
     * from TM, and deallocates the o-reject preferences when done.
     * 
     * prefmem.cpp:330:process_o_rejects_and_deallocate_them
     * 
     * @param o_rejects
     */
    public void process_o_rejects_and_deallocate_them(List<Preference> o_rejects)
    {
        for (Preference pref : o_rejects)
        {
            // prevents it from being deallocated if it's a clone of some other 
            // pref we're about to remove
            pref.preference_add_ref(); 
            // #ifdef DEBUG_PREFS
            // print (thisAgent, "\nO-reject posted at 0x%8x: ",(unsigned
            // long)pref);
            // print_preference (thisAgent, pref);
            // #endif
        }

        for(Preference pref : o_rejects)
        {
            Slot s = Slot.find_slot(pref.id, pref.attr);
            if (s != null)
            {
                // remove all pref's in the slot that have the same value
                Preference p = s.getAllPreferences();
                while (p != null)
                {
                    final Preference next_p = p.nextOfSlot;
                    if (p.value == pref.value)
                    {
                        remove_preference_from_tm(p);
                    }
                    p = next_p;
                }
            }
            pref.preference_remove_ref(this);
        }
    }

}
