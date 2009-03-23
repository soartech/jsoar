/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.EnumSet;
import java.util.Formatter;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.learning.rl.ReinforcementLearningInfo;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WmeType;
import org.jsoar.kernel.rete.MatchSetChange;
import org.jsoar.util.ListItem;
import org.jsoar.util.ListHead;

import com.google.common.collect.Iterators;

/**
 * This is the internal implementation class for identifier symbols. It should
 * only be used in the kernel. External code (I/O and RHS functions) should use
 * {@link Identifier}
 * 
 * <p>Field omitted because they were unused or unnecessary:
 * <ul>
 * <li>did_PE
 * </ul>
 * @author ray
 */
public class IdentifierImpl extends SymbolImpl implements Identifier
{
    private final int name_number;
    private final char name_letter;
    
    public boolean isa_goal;
    public boolean isa_impasse;
    public short isa_operator;
    public boolean allow_bottom_up_chunks;
    
    public boolean could_be_a_link_from_below;
    public int level;
    public int promotion_level;
    public int link_count;
    public ListItem<IdentifierImpl> unknown_level;
    public final ListHead<Slot> slots = ListHead.newInstance(); // dll of slots for this identifier
    public int tc_number; /* used for transitive closures, marking, etc. */
    public SymbolImpl variablization; /* used by the chunker */

    // TODO I think, in the long run, all of these fields should be pushed into a Goal object.
    // If anything, they're sitting around taking up memory on all non-goal identifiers. Also,
    // it will probably make some of the code clearer to have an actual Goal class in the 
    // system.  I experimented with this (only about 30 minutes of refactoring), but I didn't
    // note any significant speedup or memory usage improvement, so I'll leave it out until we
    // get really serious about refactoring the kernel.
    
    // Fields used only on goals and impasse identifiers
    private WmeImpl impasse_wmes;
    public IdentifierImpl higher_goal, lower_goal;
    public Slot operator_slot;
    public final ListHead<Preference> preferences_from_goal = ListHead.newInstance();

    public SymbolImpl reward_header;        // pointer to reward_link
    public ReinforcementLearningInfo rl_info;           // various Soar-RL information

    public GoalDependencySet gds; // pointer to a goal's dependency set

    /**
     * FIRING_TYPE that must be restored if Waterfall processing returns to this
     * level. See consistency.cpp
     */
    public SavedFiringType saved_firing_type = SavedFiringType.NO_SAVED_PRODS;
    
    public final ListHead<MatchSetChange> ms_o_assertions = ListHead.newInstance(); /* dll of o assertions at this level */
    public final ListHead<MatchSetChange> ms_i_assertions = ListHead.newInstance(); /* dll of i assertions at this level */
    public final ListHead<MatchSetChange> ms_retractions = ListHead.newInstance();  /* dll of retractions at this level */

    /* --- fields used for Soar I/O stuff --- */
    private WmeImpl input_wmes;

    public int depth; /* used to track depth of print (bug 988) RPM 4/07 */

    
    /**
     * @param hash_id
     */
    IdentifierImpl(int hash_id, char name_letter, int name_number)
    {
        super(hash_id);
        
        this.name_letter = name_letter;
        this.name_number = name_number;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.IdSymbol#getNameLetter()
     */
    @Override
    public char getNameLetter()
    {
        return name_letter;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.IdSymbol#getNameNumber()
     */
    @Override
    public int getNameNumber()
    {
        return name_number;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.IdSymbol#getWmes()
     */
    @Override
    public Iterator<Wme> getWmes()
    {
        // Return an iterator that is a concatenation of iterators over input WMEs
        // and the WME in each slot
        return Iterators.concat(new WmeIteratorSet(this));
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Identifier#getWmes(java.util.EnumSet)
     */
    @Override
    public Iterator<Wme> getWmes(EnumSet<WmeType> desired)
    {
        // Return an iterator that is a concatenation of iterators over input WMEs
        // and the WME in each slot
        return Iterators.concat(new WmeIteratorSet(this, desired));
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asIdentifier()
     */
    @Override
    public IdentifierImpl asIdentifier()
    {
        return this;
    }

    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#isSameTypeAs(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean isSameTypeAs(SymbolImpl other)
    {
        return other.asIdentifier() != null;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#getFirstLetter()
     */
    @Override
    public char getFirstLetter()
    {
        return name_letter;
    }
    
    

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Identifier#isGoal()
     */
    @Override
    public boolean isGoal()
    {
        return isa_goal;
    }

    /**
     * <p>production.cpp:1043:mark_identifier_if_unmarked
     * 
     * @param tc
     * @param id_list
     */
    private void mark_identifier_if_unmarked(int tc, ListHead<IdentifierImpl> id_list)
    {
        if (tc_number != (tc))
        {
            tc_number = (tc);
            if (id_list != null)
            {
                id_list.push(this);
            }
        }
    }
    
    public WmeImpl getInputWmes()
    {
        return input_wmes;
    }
    
    public void addInputWme(WmeImpl w)
    {
        this.input_wmes = w.addToList(this.input_wmes);
    }
    
    public void removeInputWme(WmeImpl w)
    {
        this.input_wmes = w.removeFromList(this.input_wmes);
    }
    
    public void removeAllInputWmes()
    {
        this.input_wmes = null;
    }
    
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
    /**
     * <p>production.cpp:1068:unmark_identifiers_and_free_list
     * 
     * @param ids
     */
    public static void unmark(ListHead<IdentifierImpl> ids)
    {
        for(ListItem<IdentifierImpl> id = ids.first; id != null; id = id.next)
        {
            id.item.tc_number = 0;
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#add_symbol_to_tc(int, java.util.LinkedList, java.util.LinkedList)
     */
    @Override
    public void add_symbol_to_tc(int tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list)
    {
        mark_identifier_if_unmarked (tc, id_list);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#symbol_is_in_tc(int)
     */
    @Override
    public boolean symbol_is_in_tc(int tc)
    {
        return tc_number == tc;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name_letter + Integer.toString(name_number);
    }

    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        formatter.format(name_letter + Integer.toString(name_number));
    }
    
    private static class WmeIteratorSet implements Iterator<Iterator<Wme>>
    {
        private final IdentifierImpl id;
        private boolean didImpasseWmes = false;
        private boolean didInputs = false;
        private ListItem<Slot> slot;
        
        public WmeIteratorSet(IdentifierImpl id, EnumSet<WmeType> desired)
        {
            this.id = id;
            this.didImpasseWmes = !desired.contains(WmeType.IMPASSE);
            this.didInputs = !desired.contains(WmeType.INPUT);
            this.slot = desired.contains(WmeType.NORMAL) ? id.slots.first : null;
        }
        public WmeIteratorSet(IdentifierImpl id)
        {
            this.id = id;
            this.slot = id.slots.first;
        }
        
        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext()
        {
            return (!didImpasseWmes &&  id.getImpasseWmes() != null) || (!didInputs && id.getInputWmes() != null) || slot != null;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Override
        public Iterator<Wme> next()
        {
            // First try to return an iterator over the impasse wmes
            if(!didImpasseWmes)
            {
                didImpasseWmes = true;
                if(id.getImpasseWmes() != null)
                {
                    return id.getImpasseWmes().iterator();
                }
            }
            // Next try to return an iterator over the input wmes
            if(!didInputs)
            {
                didInputs = true;
                if(id.getInputWmes() != null)
                {
                    return id.getInputWmes().iterator();
                }
            }
            // Next return an iterator over the wmes in the current slot and
            // advance to the next slots
            if(slot == null)
            {
                throw new NoSuchElementException();
            }
            Iterator<Wme> r = slot.item.getWmeIterator();
            slot = slot.next;
            return r;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
        
    }
}
