/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * 
 * intantiations.h:84
 * 
 * @author ray
 */
public class Instantiation
{
    public final Production prod; 
    public final AsListItem<Instantiation> inProdList = new AsListItem<Instantiation>(this); // next/prev, dll of inst's from same prod
    public Token rete_token; // used by rete for retractions (TODO make final?)
    public Wme rete_wme;     // used by rete for retractions (TODO make final?)
    public Condition top_of_instantiated_conditions;
    public Condition bottom_of_instantiated_conditions;

    public NotStruct nots = null;
    public final ListHead<Preference>  preferences_generated = new ListHead<Preference>();    // header for dll of prefs
    public Identifier match_goal;                   // symbol, or NIL if none
    public int /*goal_stack_level*/ match_goal_level;    // level, or ATTRIBUTE_IMPASSE_LEVEL
    public boolean okay_to_variablize = true;
    public boolean in_ms= true;  // TRUE iff this inst. is still in the match set
    public int /*tc_number*/ backtrace_number;
    public boolean GDS_evaluated_already = false;
    
    /**
     * 
     * recmem.cpp:571:create_instantiation
     * 
     * @param prod
     * @param rete_token
     * @param rete_wme
     */
    public Instantiation(Production prod, Token rete_token, Wme rete_wme)
    {
        this.prod = prod;
        this.rete_token = rete_token;
        this.rete_wme = rete_wme;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        // For debugging only
        return prod.name.toString();
    }

    
    
}
