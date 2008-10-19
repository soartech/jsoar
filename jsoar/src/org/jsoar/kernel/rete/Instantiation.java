/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import java.util.Formatter;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Traceable;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.AsListItem;
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
    public final AsListItem<Instantiation> inProdList = new AsListItem<Instantiation>(this); // next/prev, dll of inst's from same prod
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
    public boolean in_ms= true;  // TRUE iff this inst. is still in the match set
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
        return prod.name.toString();
    }

    /**
     * print.cpp:1011:print_instantiation_with_wmes
     * 
     * @param formatter
     * @param wtt
     */
    public void trace(Formatter formatter, WmeTraceType wtt)
    {
        formatter.format("%s\n", prod != null ? prod.name : "[dummy production]");

        if (wtt == WmeTraceType.NONE_WME_TRACE)
        {
            return;
        }

        for (Condition cond = top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                switch (wtt)
                {
                case TIMETAG_WME_TRACE:
                    formatter.format(" %d", pc.bt.wme_.timetag);
                    break;
                case FULL_WME_TRACE:
                    // TODO: In CSoar, at this point in/ print_instantiation_with_wmes() there's
                    // some stuff about DO_TOP_LEVEL_REF_CTS and avoiding/ printing WMEs because
                    // they may have been deleted already during a retraction. I don't think
                    // this should be a problem in jsoar, so I'm just printing the WME. Yay.
                    formatter.format(" %s", pc.bt.wme_);
                    break;
                }
            }
        }
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
