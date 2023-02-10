/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 26, 2008
 */
package org.jsoar.kernel.commands;

import java.io.IOException;
import java.util.Iterator;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.ImpasseType;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.kernel.tracing.TraceFormats;
import org.jsoar.util.ByRef;
import org.jsoar.util.adaptables.Adaptables;

/**
 * 
 * <p>sml_KernelHelpers.cpp:906:soar_ecPrintPreferences
 * 
 * @author ray
 */
public class PrintPreferencesCommand
{
    private IdentifierImpl id;
    private Symbol attr;
    private boolean object = false;
    private boolean print_prod = false;
    private WmeTraceType wtt = WmeTraceType.NONE;
    
    /**
     * @return the id
     */
    public Identifier getId()
    {
        return id;
    }
    
    /**
     * @param id the id to set
     */
    public void setId(Identifier id)
    {
        this.id = (IdentifierImpl) id;
    }
    
    /**
     * @return the attr
     */
    public Symbol getAttr()
    {
        return attr;
    }
    
    /**
     * @param attr the attr to set
     */
    public void setAttr(Symbol attr)
    {
        this.attr = attr;
    }
    
    /**
     * @return the object
     */
    public boolean isObject()
    {
        return object;
    }
    
    /**
     * @param object the object to set
     */
    public void setObject(boolean object)
    {
        this.object = object;
    }
    
    /**
     * @return the print_prod
     */
    public boolean getPrintProduction()
    {
        return print_prod;
    }
    
    /**
     * @param print_prod the print_prod to set
     */
    public void setPrintProduction(boolean print_prod)
    {
        this.print_prod = print_prod;
    }
    
    /**
     * @return the wtt
     */
    public WmeTraceType getWmeTraceType()
    {
        return wtt;
    }
    
    /**
     * @param wtt the wtt to set
     */
    public void setWmeTraceType(WmeTraceType wtt)
    {
        this.wtt = wtt;
    }
    
    /**
     * <p>sml_KernelHelpers.cpp:906:soar_ecPrintPreferences
     * <p>Parameters in original implementation are member properties here
     * 
     * @param agent The agent
     * @param printer The printer to print to
     * @throws IOException
     */
    public void print(Agent agent, Printer printer) throws IOException
    {
        final PredefinedSymbols predefinedSyms = Adaptables.adapt(agent, PredefinedSymbols.class);
        // We have one of three cases now, as of v8.6.3
        // 1. --object is specified: return prefs for all wmes comprising object
        // ID
        // (--depth not yet implemented...)
        // 2. non-state ID is given: return prefs for wmes whose <val> is ID
        // 3. default (no args): return prefs of slot (id, attr) <s> ^operator
        
        if(object)
        {
            // step thru dll of slots for ID, printing prefs for each one
            for(Slot s = id.slots; s != null; s = s.next)
            {
                if(s.attr == predefinedSyms.operator_symbol)
                    printer.print("Preferences for %s ^%s:", s.id, s.attr);
                else
                    printer.print("Support for %s ^%s:\n", s.id, s.attr);
                for(PreferenceType pt : PreferenceType.values())
                {
                    if(s.getPreferencesByType(pt) != null)
                    {
                        if(s.isa_context_slot)
                            printer.print("\n%ss:\n", pt.getDisplayName());
                        for(Preference p = s.getPreferencesByType(pt); p != null; p = p.next)
                        {
                            print_preference_and_source(agent, printer, p);
                        }
                    }
                }
            }
            if(id.goalInfo != null && id.goalInfo.getImpasseWmes() != null)
                printer.print("Arch-created wmes for %s :\n", id);
            for(WmeImpl w = id.goalInfo != null ? id.goalInfo.getImpasseWmes() : null; w != null; w = w.next)
            {
                printer.print("%s", w);
            }
            if(id.getInputWmes() != null)
                printer.print("Input (IO) wmes for %s :\n", id);
            for(WmeImpl w = id.getInputWmes(); w != null; w = w.next)
            {
                printer.print("%s", w);
            }
            
            return;
        }
        else if(!id.isGoal() && attr == null)
        {
            // find wme(s?) whose value is <ID> and print prefs if they exist
            // ??? should write print_prefs_for_id(soarAgent, id, print_prod,
            // wtt);
            // return;
            for(Wme w : agent.getAllWmesInRete())
            {
                if(w.getValue() == id)
                {
                    if(w.getValue() == predefinedSyms.operator_symbol)
                        printer.print("Preferences for ");
                    else
                        printer.print("Support for ");
                    printer.print("(%d: %s ^%s %s)\n", w.getTimetag(), w.getIdentifier(), w.getAttribute(), w.getValue());
                    Iterator<Preference> it = w.getPreferences();
                    if(!it.hasNext())
                    {
                        printer.print("    This is an architecture or input wme and has no prefs.\n");
                    }
                    else
                    {
                        while(it.hasNext())
                        {
                            print_preference_and_source(agent, printer, it.next());
                        }
                    }
                }
            }
            return;
        }
        
        // print prefs for specified slot
        Slot s = Slot.find_slot(id, attr);
        if(s == null)
        {
            printer.print("Could not find prefs for id,attribute pair: %s %s\n", id, attr);
            return;
        }
        printer.print("\nPreferences for %s ^%s:\n", id, attr);
        
        for(PreferenceType pt : PreferenceType.values())
        {
            if(s.getPreferencesByType(pt) != null)
            {
                printer.print("\n%ss:\n", pt.getDisplayName());
                for(Preference p = s.getPreferencesByType(pt); p != null; p = p.next)
                {
                    print_preference_and_source(agent, printer, p);
                }
            }
        }
        
        if(id.isGoal() && attr.toString().equals("operator"))
        {
            // voigtjr march 2010
            // print selection probabilities re: issue 18
            // run preference semantics "read only" via _consistency_check
            // returns a list of candidates without deciding which one in the event of indifference
            final ByRef<Preference> cand = ByRef.create(null);
            final Decider decider = Adaptables.adapt(agent, Decider.class);
            ImpasseType impasse_type = decider.run_preference_semantics(s, cand, true);
            
            // if the impasse isn't NONE_IMPASSE_TYPE, there's an impasse and we don't want to print anything
            // if we have no candidates, we don't want to print anything
            if((impasse_type == ImpasseType.NONE) && cand.value != null)
            {
                printer.print("\nselection probabilities:\n");
                
                // some of this following code is redundant with code in exploration.cpp
                // see exploration_choose_according_to_policy
                // see exploration_compute_value_of_candidate
                // see exploration_probabilistically_select
                int count = 0;
                double total_probability = 0;
                final Exploration exploration = Adaptables.adapt(agent, Exploration.class);
                // add up positive numeric values, count candidates
                for(Preference p = cand.value; p != null; p = p.next_candidate)
                {
                    exploration.exploration_compute_value_of_candidate(p, s, 0);
                    ++count;
                    if(p.numeric_value > 0)
                        total_probability += p.numeric_value;
                }
                assert (count != 0);
                for(Preference p = cand.value; p != null; p = p.next_candidate)
                {
                    // if total probability is zero, fall back to random
                    double prob = total_probability > 0.0 ? p.numeric_value / total_probability : 1.0 / count;
                    print_preference_and_source(agent, printer, p, prob);
                }
            }
        }
    }
    
    /**
     * This procedure prints a preference and the production which is the source
     * of the preference.
     * 
     * <p>NOTE: The called of this routine should be stepping thru slots only, (not
     * stepping thru WMEs) and therefore input wmes and arch-wmes are already
     * excluded and we can print :I when o_support is FALSE.
     * 
     * <p>sml_KernelHelpers.cpp:794:print_preference_and_source
     * 
     * @throws IOException
     */
    private void print_preference_and_source(Agent agnt, Printer printer, Preference pref) throws IOException
    {
        print_preference_and_source(agnt, printer, pref, null);
    }
    
    private void print_preference_and_source(Agent agnt, Printer printer, Preference pref, Double selection_probability) throws IOException
    {
        final TraceFormats traceFormats = Adaptables.adapt(agnt, TraceFormats.class);
        final PredefinedSymbols predefinedSyms = Adaptables.adapt(agnt, PredefinedSymbols.class);
        printer.print("  ");
        if(pref.attr == predefinedSyms.operator_symbol)
        {
            traceFormats.print_object_trace(printer.getWriter(), pref.value);
        }
        else
        {
            printer.print("(%s ^%s %s) ", pref.id, pref.attr, pref.value);
        }
        if(pref.attr == predefinedSyms.operator_symbol)
        {
            printer.print(" %c", pref.type.getIndicator());
        }
        if(pref.type.isBinary())
            traceFormats.print_object_trace(printer.getWriter(), pref.referent);
        if(pref.o_supported)
            printer.print(" :O ");
        else
            printer.print(" :I ");
        if(selection_probability != null)
            printer.print("(%.1f%%)", selection_probability * 100.0);
        printer.print("\n");
        if(print_prod)
        {
            printer.print("    From ");
            pref.inst.trace(printer.asFormatter(), wtt);
            printer.print("\n");
        }
    }
}
