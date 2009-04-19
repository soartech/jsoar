/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.memory;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.rete.NotStruct;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Traceable;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.ListItem;
import org.jsoar.util.ListHead;

/**
 * 
 * intantiations.h:84
 * 
 * @author ray
 */
public class Instantiation implements Traceable
{
    public final Production prod; 
    public final ListItem<Instantiation> inProdList = new ListItem<Instantiation>(this); // next/prev, dll of inst's from same prod
    public Token rete_token; // used by rete for retractions (TODO make final?)
    public WmeImpl rete_wme;     // used by rete for retractions (TODO make final?)
    public Condition top_of_instantiated_conditions;
    public Condition bottom_of_instantiated_conditions;

    public NotStruct nots = null;
    public final ListHead<Preference>  preferences_generated = ListHead.newInstance();    // header for dll of prefs
    public IdentifierImpl match_goal;                   // symbol, or NIL if none
    public int /*goal_stack_level*/ match_goal_level;    // level, or ATTRIBUTE_IMPASSE_LEVEL
    /**
     * <p>Initialized to true in recmem.cpp:574:create_instantiation
     */
    public boolean okay_to_variablize = true;
    /**
     * TRUE iff this inst. is still in the match set
     * 
     * <p>Initialized to true in recmem.cpp:575:create_instantiation
     */
    public boolean in_ms = true;
    public int /*tc_number*/ backtrace_number;
    /**
     * <p>Initialized to false in recmem.cpp:582:create_instantiation
     */
    public boolean GDS_evaluated_already = false;
    
    /**
     * 
     * <p>recmem.cpp:571:create_instantiation
     * 
     * @param prod
     * @param rete_token
     * @param rete_wme
     */
    public Instantiation(Production prod, Token rete_token, WmeImpl rete_wme)
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
        return prod.getName().toString();
    }

    /**
     * print.cpp:1011:print_instantiation_with_wmes
     * 
     * @param formatter
     * @param wtt
     */
    public void trace(Formatter formatter, WmeTraceType wtt)
    {
        formatter.format("%s\n", prod != null ? prod.getName() : "[dummy production]");

        if (wtt == WmeTraceType.NONE)
        {
            return;
        }

        // Note: replaced duplicate loop with call to getBacktraceWmes()
        for (Wme wme : getBacktraceWmes())
        {
            switch (wtt)
            {
            case TIMETAG:
                formatter.format(" %d", wme.getTimetag());
                break;
            case FULL:
                // Note: In CSoar, at this point in/ print_instantiation_with_wmes() there's
                // some stuff about DO_TOP_LEVEL_REF_CTS and avoiding/ printing WMEs because
                // they may have been deleted already during a retraction. I don't think
                // this should be a problem in jsoar, so I'm just printing the WME. Yay.
                formatter.format(" %s", wme);
                break;
            }
        }
    }
    
    /**
     * Get list of backtrace wmes in this instantiation
     * 
     * @return List of backtrace WMEs in this instantiation
     */
    public List<Wme> getBacktraceWmes()
    {
        final List<Wme> result = new ArrayList<Wme>();
        for (Condition cond = top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                result.add(pc.bt.wme_);
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Traceable#trace(org.jsoar.kernel.Trace, java.util.Formatter, int, int, int)
     */
    @Override
    public void trace(Trace trace, Formatter formatter, int flags, int width, int precision)
    {
        trace(formatter, trace.getWmeTraceType());
    }
    
}
