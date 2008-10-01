/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.memory;

import java.util.EnumMap;

import org.jsoar.kernel.ImpasseType;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * Fields in a slot:
 *
 *    next, prev:  used for a doubly-linked list of all slots for a certain
 *      identifier.
 *
 *    id, attr:   identifier and attribute of the slot
 *
 *    wmes:  header of a doubly-linked list of all wmes in the slot
 *
 *    acceptable_preference_wmes:  header of doubly-linked list of all
 *      acceptable preference wmes in the slot.  (This is only used for
 *      context slots.)
 *
 *    all_preferences:  header of a doubly-linked list of all preferences
 *      currently in the slot
 *
 *    preferences[NUM_PREFERENCE_TYPES]: array of headers of doubly-linked
 *      lists, one for each possible type of preference.  These store
 *      all the preferences, sorted into lists according to their types.
 *      Within each list, the preferences are sorted according to their
 *      match goal, with the pref. supported by the highest goal at the
 *      head of the list.
 *
 *    impasse_id:  points to the identifier of the attribute impasse object
 *      for this slot.  (NIL if the slot isn't impassed.)
 *
 *    isa_context_slot:  TRUE iff this is a context slot
 *
 *    impasse_type:  indicates the type of the impasse for this slot.  This
 *      is one of NONE_IMPASSE_TYPE, CONSTRAINT_FAILURE_IMPASSE_TYPE, etc.
 *
 *    marked_for_possible_removal:  TRUE iff this slot is on the list of
 *      slots that might be deallocated at the end of the current top-level
 *      phases.
 *
 *    changed:  indicates whether the preferences for this slot have changed.
 *      For non-context slots, this is either NIL or a pointer to the
 *      corresponding dl_cons in changed_slots (see decide.c); for context
 *      slots, it's just a zero/nonzero flag.
 *
 *    acceptable_preference_changed:  for context slots only; this is zero
 *      if no acceptable or require preference in this slot has changed;
 *      if one has changed, it points to a dl_cons.
 *
 * 
 * <p>gdatastructs.h:288
 * 
 * @author ray
 */
public class Slot
{
    public final AsListItem<Slot> next_prev = new AsListItem<Slot>(this); // dll of slots for this id
    public final Identifier id; 
    public final Symbol attr;

    public final ListHead<Wme> wmes = ListHead.newInstance(); // dll of wmes in the slot
    public final ListHead<Wme> acceptable_preference_wmes = ListHead.newInstance();  // dll of acceptable pref. wmes
    public final ListHead<Preference> all_preferences = ListHead.newInstance(); // dll of all pref's in the slot
    private final EnumMap<PreferenceType, ListHead<Preference>> preferencesByType = new EnumMap<PreferenceType, ListHead<Preference>>(PreferenceType.class);

    public Identifier impasse_id = null;               // null if slot is not impassed
    public final boolean isa_context_slot;            
    public ImpasseType impasse_type = ImpasseType.NONE_IMPASSE_TYPE;
    boolean marked_for_possible_removal = false;
    
    /**
     * for non-context slots: points to the corresponding
     * dl_cons in changed_slots;  for context slots: just
     * zero/nonzero flag indicating slot changed
     * 
     * TODO Sub-class instead of using this for two things
     */
    public Object changed;
    
    /**
     * for context slots: either zero, or points to dl_cons if the slot has
     * changed + or ! pref's
     * 
     * TODO Sub-class instead of using this for two things
     */
    public Object acceptable_preference_changed;

    /**
     * <p>tempmem.cpp:64:make_slot
     * 
     * @param id
     * @param attr
     * @param operator_symbol
     * @return
     */
    public static Slot make_slot(Identifier id, Symbol attr, SymConstant operator_symbol)
    {
        // Search for a slot first.  If it exists for the given symbol, then just return it
        Slot s = find_slot(id, attr);
        if(s != null)
        {
            return s;
        }
        
        return new Slot(id, attr, operator_symbol);
    }
    
    /**
     * 
     * <p>tempmem.cpp:64:make_slot
     * 
     * @param id
     * @param attr
     * @param operator_symbol
     */
    private Slot(Identifier id, Symbol attr, SymConstant operator_symbol)
    {
        this.next_prev.insertAtHead(id.slots);

        /*
         * Context slots are goals and operators; operator slots get created
         * with a goal (see create_new_context).
         */
        if ((id.isa_goal) && (attr == operator_symbol))
        {
            this.isa_context_slot = true;
        }
        else
        {
            this.isa_context_slot = false;
        }

        // s->changed = NIL;
        // s->acceptable_preference_changed = NIL;
        this.id = id;
        this.attr = attr;
    }

    /**
     * Find_slot() looks for an existing slot for a given id/attr pair, and
     * returns it if found.  If no such slot exists, it returns NIL.
     * 
     * <p>tempmem.cpp:55:find_slot
     * 
     * @param id
     * @param attr
     * @return
     */
    public static Slot find_slot(Identifier id, Symbol attr)
    {
        if (id == null)
        {
            return null; // fixes bug #135 kjh
        } 
        for (AsListItem<Slot> s = id.slots.first; s != null; s = s.next)
        {
            if (s.get().attr == attr)
            {
                return s.get();
            }
        }
        return null;
    }
    
    public ListHead<Preference> getPreferenceList(Preference pref)
    {
        return getPreferenceList(pref.type);
    }
    
    public ListHead<Preference> getPreferenceList(PreferenceType type)
    {
        ListHead<Preference> list =  preferencesByType.get(type);
        if(list == null)
        {
            list = ListHead.newInstance();
            preferencesByType.put(type, list);
        }

        return list;
    }
}
