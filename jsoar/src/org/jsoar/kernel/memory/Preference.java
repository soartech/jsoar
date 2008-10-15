/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Formattable;
import java.util.Formatter;

import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.AsListItem;

/**
 * gdatastructs.h:191:preference_struct
 * 
 * @author ray
 */
public class Preference implements Formattable
{
    public final PreferenceType type;         /* acceptable, better, etc. */
    public boolean o_supported = false;  /* is the preference o-supported? */
    public boolean on_goal_list = false; /* is this pref on the list for its match goal */
    int reference_count = 0;
    public final Identifier id;
    public final SymbolImpl attr;
    public final SymbolImpl value;
    public final SymbolImpl referent;
    
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

    public final AsListItem<Preference> all_of_goal = new AsListItem<Preference>(this); // dll of all pref's from the same match goal
    
    /* dll (without header) of cloned preferences (created when chunking) */
    public Preference next_clone;
    public Preference prev_clone;
      
    public Instantiation inst;
    public final AsListItem<Preference> inst_next_prev = new AsListItem<Preference>(this);
    public Preference next_candidate;
    public Preference next_result;

    public int total_preferences_for_candidate = 0;
    public double numeric_value = 0.0;
    
    boolean deallocated = false;

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
    public Preference(PreferenceType type, Identifier id, SymbolImpl attr, SymbolImpl value, SymbolImpl referent)
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
        assert this.inst == null;
        this.inst = inst;
        this.inst_next_prev.insertAtHead(inst.preferences_generated);
    }
    
    /**
     * @return True if this preference is in temp memory, i.e. it's in a Slot.
     */
    public boolean isInTempMemory()
    {
        return slot != null;
    }
    
    /**
     * prefmem.h:68:preference_add_ref
     */
    public void preference_add_ref()
    {
        reference_count++;
    }
    
    public void preference_remove_ref(PreferenceMemory prefMem)
    {
        assert this.reference_count > 0;
        
        this.reference_count--;
        if (reference_count == 0)
        {
            prefMem.possibly_deallocate_preference_and_clones(this);
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

    

}
