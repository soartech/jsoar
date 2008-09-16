/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.LinkedList;

import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.MatchSetChange;
import org.jsoar.kernel.io.OutputLink;
import org.jsoar.kernel.learning.ReinforcementLearningInfo;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
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
    public final ListHead<Slot> slots = new ListHead<Slot>(); // dll of slots for this identifier
    public int tc_number; /* used for transitive closures, marking, etc. */
    public Symbol variablization; /* used by the chunker */

    /* --- fields used only on goals and impasse identifiers --- */
    public final ListHead<Wme> impasse_wmes = new ListHead<Wme>();
    
    /* --- fields used only on goals --- */
    public Identifier higher_goal, lower_goal;
    public Slot operator_slot;
    public final ListHead<Preference> preferences_from_goal = new ListHead<Preference>();

    public Symbol reward_header;        // pointer to reward_link
    public ReinforcementLearningInfo rl_info;           // various Soar-RL information

    /* REW: begin 09.15.96 */
    public GoalDependencySet gds; // pointer to a goal's dependency set
    /* REW: begin 09.15.96 */

    /* REW: begin 08.20.97 */
    public SavedFiringType saved_firing_type = SavedFiringType.NO_SAVED_PRODS;     /* FIRING_TYPE that must be restored if Waterfall
                  processing returns to this level.
                  See consistency.cpp */
    public final ListHead<MatchSetChange> ms_o_assertions = new ListHead<MatchSetChange>(); /* dll of o assertions at this level */
    public final ListHead<MatchSetChange> ms_i_assertions = new ListHead<MatchSetChange>(); /* dll of i assertions at this level */
    public final ListHead<MatchSetChange> ms_retractions = new ListHead<MatchSetChange>();  /* dll of retractions at this level */
    /* REW: end   08.20.97 */

    /* --- fields used for Soar I/O stuff --- */
    public LinkedList<OutputLink> associated_output_links = null;
    public final ListHead<Wme> input_wmes = new ListHead<Wme>();

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


    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#add_symbol_to_tc(int, java.util.LinkedList, java.util.LinkedList)
     */
    @Override
    public void add_symbol_to_tc(int tc, LinkedList<Identifier> id_list, LinkedList<Variable> var_list)
    {
        // TODO add_symbol_to_tc: implement for Identifier
        throw new UnsupportedOperationException("Not implemented");
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
