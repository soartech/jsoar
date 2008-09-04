/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import org.jsoar.kernel.Preference;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.Slot;
import org.jsoar.kernel.Wme;
import org.jsoar.kernel.MatchSetChange;
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
    public short level;
    public short promotion_level;
    public int link_count;
    // TODO: dl_cons unknown_level
    public Slot slots; /* dll of slots for this identifier */
    public int tc_number; /* used for transitive closures, marking, etc. */
    public Symbol variablization; /* used by the chunker */

    /* --- fields used only on goals and impasse identifiers --- */
    public Wme impasse_wmes;
    
    /* --- fields used only on goals --- */
    public Symbol higher_goal, lower_goal;
    public Slot operator_slot;
    public Preference preferences_from_goal;

    public Symbol reward_header;        // pointer to reward_link
    // TODO struct rl_data_struct *rl_info;           // various Soar-RL information

    /* REW: begin 09.15.96 */
    // TODO struct gds_struct *gds;    /* Pointer to a goal's dependency set */
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
    // TODO ::list *associated_output_links;
    public Wme input_wmes;

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
     * @see org.jsoar.kernel.symbols.Symbol#getFirstLetter()
     */
    @Override
    public char getFirstLetter()
    {
        return name_letter;
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
