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
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.AsListItem;

/**
 * gdatastructs.h:191:preference_struct
 * 
 * @author ray
 */
public class Preference implements Formattable
{
    // TODO implement formattable for print_preference
    
    public final PreferenceType type;         /* acceptable, better, etc. */
    public boolean o_supported = false;  /* is the preference o-supported? */
    public boolean in_tm = false;        /* is this currently in TM? */
    public boolean on_goal_list = false; /* is this pref on the list for its match goal */
    int reference_count = 0;
    public final Identifier id;
    public final Symbol attr;
    public final Symbol value;
    public final Symbol referent;
    public Slot slot = null;

    public final AsListItem<Preference> next_prev = new AsListItem<Preference>(this); // dll of pref's of same type in same slot */

    final AsListItem<Preference> all_of_slot = new AsListItem<Preference>(this); // dll of all pref's in same slot

    public final AsListItem<Preference> all_of_goal = new AsListItem<Preference>(this); // dll of all pref's from the same match goal
    
    /* dll (without header) of cloned preferences (created when chunking) */
    Preference next_clone;
    Preference prev_clone;
      
    public Instantiation inst;
    public final AsListItem<Preference> inst_next_prev = new AsListItem<Preference>(this);
    public Preference next_candidate;
    Preference next_result;

    int total_preferences_for_candidate = 0;
    double numeric_value = 0.0;

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
    public Preference(PreferenceType type, Identifier id, Symbol attr, Symbol value, Symbol referent)
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
    
    /**
     * prefmem.h:68:preference_add_ref
     */
    public void preference_add_ref()
    {
        reference_count++;
    }
    
    public void preference_remove_ref(PreferenceMemory prefMem)
    {
      this.reference_count--;
      if (reference_count == 0){
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

    

}
