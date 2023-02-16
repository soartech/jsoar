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
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.kernel.tracing.Traceable;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * <p>intantiations.h:84
 * 
 * @author ray
 */
public class Instantiation implements Traceable
{
    public Production prod;
    public Instantiation nextInProdList, prevInProdList; // next/prev, dll of inst's from same prod
    public Token rete_token; // used by rete for retractions (TODO make final?)
    public WmeImpl rete_wme;     // used by rete for retractions (TODO make final?)
    public Condition top_of_instantiated_conditions;
    public Condition bottom_of_instantiated_conditions;
    
    public NotStruct nots = null;
    public Preference preferences_generated = null;    // header for dll of prefs
    public IdentifierImpl match_goal;                   // symbol, or NIL if none
    public int /* goal_stack_level */ match_goal_level;    // level, or ATTRIBUTE_IMPASSE_LEVEL
    
    /**
     * reliable: false iff instantiation is a justification whose
     * backtrace either:
     * 
     * - tests ^quiescence t, or
     * - contains a local negated condition and learn -N is set, or
     * - goes through an unreliable justification
     * 
     * Intuitively, a justification is unreliable if its creation is
     * not guaranteed by the state of production and working memory
     * 
     * <p>Initialized to true in recmem.cpp:673:create_instantiation
     */
    public boolean reliable = true;
    /**
     * TRUE iff this inst. is still in the match set
     * 
     * <p>Initialized to true in recmem.cpp:575:create_instantiation
     */
    public boolean in_ms = true;
    public int /* tc_number */ backtrace_number;
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
    
    /*
     * (non-Javadoc)
     * 
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
        formatter.format("%s", prod != null ? prod.getName() : "[dummy production]");
        
        if(wtt == WmeTraceType.NONE)
        {
            return;
        }
        
        // Note: replaced duplicate loop with call to getBacktraceWmes()
        formatter.format("\n");
        for(Wme wme : getBacktraceWmes())
        {
            switch(wtt)
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
            default:
                // do nothing
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
        final List<Wme> result = new ArrayList<>();
        for(Condition cond = top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if(pc != null)
            {
                result.add(pc.bt().wme_);
            }
        }
        return result;
    }
    
    public void insertGeneratedPreference(Preference pref)
    {
        pref.inst_next = preferences_generated;
        if(preferences_generated != null)
        {
            preferences_generated.inst_prev = pref;
        }
        preferences_generated = pref;
        pref.inst_prev = null;
    }
    
    void removeGeneratedPreferece(Preference pref)
    {
        if(pref == preferences_generated)
        {
            preferences_generated = pref.inst_next;
            if(preferences_generated != null)
            {
                preferences_generated.inst_prev = null;
            }
        }
        else
        {
            pref.inst_prev.inst_next = pref.inst_next;
            if(pref.inst_next != null)
            {
                pref.inst_next.inst_prev = pref.inst_prev;
            }
        }
        pref.inst_next = pref.inst_prev = null;
    }
    
    /**
     * Insert at the head of a list of instantiations
     * 
     * @param currentHead the current head
     * @return the new head (this)
     * @see RecognitionMemory#newly_created_instantiations
     * @see Production#instantiations
     */
    public Instantiation insertAtHeadOfProdList(Instantiation currentHead)
    {
        this.nextInProdList = currentHead;
        if(nextInProdList != null)
        {
            nextInProdList.prevInProdList = this;
        }
        prevInProdList = null;
        return this;
    }
    
    /**
     * Remove from a list of instantiations
     * 
     * @param currentHead the current head
     * @return the new head
     * @see RecognitionMemory#newly_created_instantiations
     * @see Production#instantiations
     */
    public Instantiation removeFromProdList(Instantiation currentHead)
    {
        if(this == currentHead)
        {
            currentHead = nextInProdList;
            if(currentHead != null)
            {
                currentHead.prevInProdList = null;
            }
        }
        else
        {
            prevInProdList.nextInProdList = nextInProdList;
            if(nextInProdList != null)
            {
                nextInProdList.prevInProdList = prevInProdList;
            }
        }
        nextInProdList = prevInProdList = null;
        return currentHead;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.Traceable#trace(org.jsoar.kernel.Trace, java.util.Formatter, int, int, int)
     */
    @Override
    public void trace(Trace trace, Formatter formatter, int flags, int width, int precision)
    {
        trace(formatter, trace.getWmeTraceType());
    }
    
}
