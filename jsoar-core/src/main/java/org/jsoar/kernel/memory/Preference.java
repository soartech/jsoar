/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Formattable;
import java.util.Formatter;
import java.util.Set;

import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * gdatastructs.h:191:preference_struct
 * 
 * @author ray
 */
public class Preference implements Formattable
{
    public final PreferenceType type;         /* acceptable, better, etc. */
    public final IdentifierImpl id;
    public final SymbolImpl attr;
    public final SymbolImpl value;
    
    public SymbolImpl referent; // TODO: I'd like this to be final, but RL changes it.
    public boolean o_supported = false;  /* is the preference o-supported? */
    public boolean on_goal_list = false; /* is this pref on the list for its match goal */
    public int reference_count = 0; // TODO: this shouldn't be public if we can avoid it
    
    /**
     * The slot this preference is in. This is also a replacement for in_tm
     */
    public Slot slot = null;

    /**
     * next pointer for list of preferences in a slot by type. Use this when
     * iterating over the list head returned by
     * {@link Slot#getPreferencesByType(PreferenceType)}
     */
    public Preference next;
    Preference previous;

    /**
     * next pointer for list of all preference in a slot. Use this when 
     * iterating over the list head returned by
     * {@link Slot#getAllPreferences()}
     */
    public Preference nextOfSlot;
    Preference previousOfSlot;

    // dll of all pref's from the same match goal
    public Preference all_of_goal_next;
    public Preference all_of_goal_prev;
    
    // dll (without header) of cloned preferences (created when chunking)
    public Preference next_clone;
    public Preference prev_clone;
      
    public Instantiation inst;
    public Preference inst_next;
    public Preference inst_prev;
    
    public Preference next_candidate;
    public Preference next_result;

    public int total_preferences_for_candidate = 0;
    public double numeric_value = 0.0;
    
    boolean deallocated = false;
    
    public boolean rl_contribution = false; // RL-9.3.0 (false in make_preference)

    public Set<Wme> wma_o_set; // initialized by WorkingMemoryActivation

    /**
     * Make_preference() creates a new preference structure of the given type
     * with the given id/attribute/value/referent. (Referent is only used for
     * binary preferences.) The preference is not yet added to preference
     * memory, however.
     * 
     * prefmem.cpp:58:make_preference
     * 
     * @param type
     * @param id
     * @param attr
     * @param value
     * @param referent
     */
    public Preference(PreferenceType type, IdentifierImpl id, SymbolImpl attr, SymbolImpl value, SymbolImpl referent)
    {
        this.type = type;
        this.id = id;
        this.attr = attr;
        this.value = value;
        this.referent = referent;
        this.total_preferences_for_candidate = 0;
        this.numeric_value = 0;

        // #ifdef DEBUG_PREFS
        // print (thisAgent, "\nAllocating preference at 0x%8x: ", (unsigned
        // long)p);
        // print_preference (thisAgent, p);
        // #endif

        /*
         * BUGBUG check to make sure the pref doesn't have value or referent
         * .isa_goal or .isa_impasse;
         */
    }

    /**
     * Count the number of items in a candidates list
     * 
     * @param start The starting candidate, possibly null
     * @return Number of candidates in the list
     */
    public static int countCandidates(Preference start)
    {
        int count = 0;
        for(; start != null; start = start.next_candidate, ++count)
        {
            // nothing
        }
        return count;
    }
    
    public static Preference getCandidate(Preference start, int index)
    {
        for(int i = 0; start != null && i < index; start = start.next_candidate, ++i)
        {
            // nothing
        }
        
        return start;
    }
    
    public void setInstantiation(Instantiation inst)
    {
        assert !deallocated;
        assert this.inst == null;
        this.inst = inst;
        inst.insertGeneratedPreference(this);
    }
    
    /**
     * @return True if this preference is in temp memory, i.e. it's in a Slot.
     */
    public boolean isInTempMemory()
    {
        assert !deallocated;
        return slot != null;
    }
    
    /**
     * prefmem.h:68:preference_add_ref
     */
    public void preference_add_ref()
    {
        assert !deallocated;
        reference_count++;
    }
    
    public void preference_remove_ref(RecognitionMemory recMemory)
    {
        assert !deallocated;
        assert this.reference_count > 0;
        
        this.reference_count--;
        if (reference_count == 0)
        {
            possibly_deallocate_preference_and_clones(this, recMemory);
        }
    }

    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        formatter.format("(%s ^%s %s %c", id, attr, value, type.getIndicator());
        if (type.isBinary()) 
        {
            formatter.format(" %s", referent);
        }
        if (o_supported) formatter.format("  :O ");
        formatter.format(")\n");

        /* TODO preference XML output
        // <preference id="s1" attr="foo" value="123" pref_type=">"></preference>
        xml_begin_tag(thisAgent, kTagPreference);
        xml_att_val(thisAgent, kWME_Id, pref->id);
        xml_att_val(thisAgent, kWME_Attribute, pref->attr);
        xml_att_val(thisAgent, kWME_Value, pref->value);

        char buf[2];
        buf[0] = pref_type;
        buf[1] = 0;
        xml_att_val(thisAgent, kPreference_Type, (char*)buf);
        
        if (preference_is_binary(pref->type)) {
            xml_att_val(thisAgent, kReferent, pref->referent);
        }
        if (pref->o_supported) {
            xml_att_val(thisAgent, kOSupported, ":O");
        }
        xml_end_tag(thisAgent, kTagPreference);
        */
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        // For debugging only
        return String.format("%s", this);
    }

    /**
     * This routines take a given preference and finds the clone of it whose
     * match goal is at the given goal_stack_level. (This is used to find the
     * proper preference to backtrace through.) If the given preference itself
     * is at the right level, it is returned. If there is no clone at the right
     * level, NIL is returned.
     * 
     * <p>recmem.cpp:110:find_clone_for_level
     * 
     * @param p
     * @param level
     * @return the clone for the given leve, or null if not found
     */
    public static Preference find_clone_for_level(Preference p, int level)
    {
        if (p == null)
        {
            // if the wme doesn't even have a preference on it, we can't backtrace
            // at all (this happens with I/O and some architecture-created wmes
            return null;
        }
    
        // look at pref and all of its clones, find one at the right level
        if (p.inst.match_goal_level == level)
        {
            return p;
        }
    
        for (Preference clone = p.next_clone; clone != null; clone = clone.next_clone)
        {
            if (clone.inst.match_goal_level == level)
            {
                return clone;
            }
        }
    
        for (Preference clone = p.prev_clone; clone != null; clone = clone.prev_clone)
        {
            if (clone.inst.match_goal_level == level)
            {
                return clone;
            }
        }
    
        // if none was at the right level, we can't backtrace at all
        return null;
    }
    
    /**
     * prefmem.cpp:100:deallocate_preference
     * 
     * @param pref
     */
    static public void deallocate_preference (Preference pref, RecognitionMemory recMemory) 
    {
        assert !pref.deallocated;
        assert pref.reference_count == 0;
        
        // remove it from the list of pref's for its match goal
        if (pref.on_goal_list)
        {
            pref.inst.match_goal.goalInfo.removeGoalPreference(pref);
        }

        // remove it from the list of pref's from that instantiation
        pref.inst.removeGeneratedPreferece(pref);

        recMemory.possibly_deallocate_instantiation(pref.inst);

        pref.deallocated = true;
        pref.destroy();
    } 
    
    /**
     * Possibly_deallocate_preference_and_clones() checks whether a given
     * preference and all its clones have reference_count 0, and deallocates
     * them all if they do. It returns TRUE if they were actually deallocated,
     * FALSE otherwise.
     * 
     * prefmem.cpp:141:possibly_deallocate_preference_and_clones
     * 
     */
    boolean possibly_deallocate_preference_and_clones(Preference pref, RecognitionMemory recMemory)
    {
        assert !deallocated;
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
            final Preference next = clone.next_clone;
            deallocate_preference(clone, recMemory);
            clone = next;
        }
        clone = pref.prev_clone;
        while (clone != null)
        {
            final Preference next = clone.prev_clone;
            deallocate_preference(clone, recMemory);
            clone = next;
        }

        // deallocate pref
        deallocate_preference(pref, recMemory);

        return true;
    } 
    
    /**
     * Remove_preference_from_clones() splices a given preference out of the
     * list of clones. If the preference's reference_count is 0, it also
     * deallocates it and returns TRUE. Otherwise it returns FALSE.
     * 
     * prefmem.cpp:176:remove_preference_from_clones
     * 
     * @param recMemory
     * @return true if the preference is deallocated, false otherwise
     */
    public boolean remove_preference_from_clones(RecognitionMemory recMemory)
    {
        assert !deallocated;
        final Preference pref = this;
        Preference any_clone = null;
        if (this.next_clone != null)
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
            possibly_deallocate_preference_and_clones(any_clone, recMemory);
        if (pref.reference_count == 0)
        {
            deallocate_preference(pref, recMemory);
            return true;
        }
        else
        {
            return false;
        }
    }
    
    private void destroy(){
        referent = null;
        slot = null;

        next = null;
        previous = null;

        nextOfSlot = null;
        previousOfSlot = null;

        // dll of all pref's from the same match goal
        all_of_goal_next = null;
        all_of_goal_prev = null;
        
        // dll (without header) of cloned preferences (created when chunking)
        next_clone = null;
        prev_clone = null;
          
        inst = null;
        inst_next = null;
        inst_prev = null;
        
        next_candidate = null;
        next_result = null;

        wma_o_set = null; // initialized by WorkingMemoryActivation
    }
}
