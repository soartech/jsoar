/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.Collection;
import java.util.LinkedList;

import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.io.OutputLink;
import org.jsoar.kernel.learning.ReinforcementLearningInfo;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.MatchSetChange;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class Identifier extends Symbol
{
    public final int name_number;
    public final char name_letter;
    
    public boolean isa_goal;
    public boolean isa_impasse;
    public boolean did_PE;
    public short isa_operator;
    public boolean allow_bottom_up_chunks;
    
    public boolean could_be_a_link_from_below;
    public int level;
    public int promotion_level;
    public int link_count;
    public AsListItem<Identifier> unknown_level;
    public final ListHead<Slot> slots = ListHead.newInstance(); // dll of slots for this identifier
    public int tc_number; /* used for transitive closures, marking, etc. */
    public Symbol variablization; /* used by the chunker */

    // fields used only on goals and impasse identifiers
    private Wme impasse_wmes;
    
    /* --- fields used only on goals --- */
    public Identifier higher_goal, lower_goal;
    public Slot operator_slot;
    public final ListHead<Preference> preferences_from_goal = ListHead.newInstance();

    public Symbol reward_header;        // pointer to reward_link
    public ReinforcementLearningInfo rl_info;           // various Soar-RL information

    /* REW: begin 09.15.96 */
    public GoalDependencySet gds; // pointer to a goal's dependency set
    /* REW: begin 09.15.96 */

    /* REW: begin 08.20.97 */
    public SavedFiringType saved_firing_type = SavedFiringType.NO_SAVED_PRODS;     /* FIRING_TYPE that must be restored if Waterfall
                  processing returns to this level.
                  See consistency.cpp */
    public final ListHead<MatchSetChange> ms_o_assertions = ListHead.newInstance(); /* dll of o assertions at this level */
    public final ListHead<MatchSetChange> ms_i_assertions = ListHead.newInstance(); /* dll of i assertions at this level */
    public final ListHead<MatchSetChange> ms_retractions = ListHead.newInstance();  /* dll of retractions at this level */
    /* REW: end   08.20.97 */

    /* --- fields used for Soar I/O stuff --- */
    public LinkedList<OutputLink> associated_output_links = null;
    private Wme input_wmes;

    public int depth; /* used to track depth of print (bug 988) RPM 4/07 */

    
    /**
     * @param hash_id
     */
    /*package*/ Identifier(int hash_id, char name_letter, int name_number)
    {
        super(hash_id);
        
        this.name_letter = name_letter;
        this.name_number = name_number;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asIdentifier()
     */
    @Override
    public Identifier asIdentifier()
    {
        return this;
    }

    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#isSameTypeAs(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean isSameTypeAs(Symbol other)
    {
        return other.asIdentifier() != null;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#getFirstLetter()
     */
    @Override
    public char getFirstLetter()
    {
        return name_letter;
    }

    /**
     * <p>production.cpp:1043:mark_identifier_if_unmarked
     * 
     * @param tc
     * @param id_list
     */
    private void mark_identifier_if_unmarked(int tc, LinkedList<Identifier> id_list)
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
    
    public Wme getInputWmes()
    {
        return input_wmes;
    }
    
    public void addInputWme(Wme w)
    {
        this.input_wmes = w.addToList(this.input_wmes);
    }
    
    public void removeInputWme(Wme w)
    {
        this.input_wmes = w.removeFromList(this.input_wmes);
    }
    
    public void removeAllInputWmes()
    {
        this.input_wmes = null;
    }
    
    public Wme getImpasseWmes()
    {
        return impasse_wmes;
    }
    
    public void addImpasseWme(Wme w)
    {
        this.impasse_wmes = w.addToList(this.impasse_wmes);
    }
    
    public void removeAllImpasseWmes()
    {
        this.impasse_wmes = null;
    }
    
    public void removeImpasseWme(Wme w)
    {
        this.impasse_wmes = w.removeFromList(this.impasse_wmes);
    }
    /**
     * <p>production.cpp:1068:unmark_identifiers_and_free_list
     * 
     * @param ids
     */
    public static void unmark(Collection<Identifier> ids)
    {
        for(Identifier id : ids)
        {
            id.tc_number = 0;
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#add_symbol_to_tc(int, java.util.LinkedList, java.util.LinkedList)
     */
    @Override
    public void add_symbol_to_tc(int tc, LinkedList<Identifier> id_list, LinkedList<Variable> var_list)
    {
        mark_identifier_if_unmarked (tc, id_list);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#symbol_is_in_tc(int)
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
    
    
}
