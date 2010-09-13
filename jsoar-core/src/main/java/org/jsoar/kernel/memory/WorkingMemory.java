/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 7, 2008
 */
package org.jsoar.kernel.memory;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.events.InputWmeGarbageCollectedEvent;
import org.jsoar.kernel.events.WorkingMemoryChangedEvent;
import org.jsoar.kernel.io.InputOutputImpl;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.events.SoarEventManager;
import org.jsoar.util.properties.LongPropertyProvider;
import org.jsoar.util.properties.PropertyManager;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * <p>wmem.cpp
 * <p>The following fields or functions were removed because they were unused or
 * unnecessary in Java:
 * <ul>
 * <li>wmem.cpp:283:deallocate_wme
 * <li>num_existing_wmes
 * </ul>
 * 
 * @author ray
 */
public class WorkingMemory
{
    private Rete rete;
    private PredefinedSymbols predefinedSyms;
    private Trace trace;
    private Decider decider;
    private SoarEventManager eventManager;
    
    private int current_wme_timetag = 1;
    
    // Stats stuff
    private final ListHead<WmeImpl> wmes_to_add = ListHead.newInstance();
    private final ListHead<WmeImpl> wmes_to_remove = ListHead.newInstance();
    private final LongPropertyProvider wme_addition_count = new LongPropertyProvider(SoarProperties.WME_ADDITION_COUNT);
    private final LongPropertyProvider wme_removal_count = new LongPropertyProvider(SoarProperties.WME_REMOVAL_COUNT);
    private final LongPropertyProvider max_wm_size = new LongPropertyProvider(SoarProperties.MAX_WM_SIZE);
    private final LongPropertyProvider cumulative_wm_size = new LongPropertyProvider(SoarProperties.CUMULATIVE_WM_SIZE);
    private final LongPropertyProvider num_wm_sizes_accumulated = new LongPropertyProvider(SoarProperties.NUM_WM_SIZES_ACCUMULATED);
    
    public WorkingMemory()
    {
    }
    
    public void initialize(Agent context)
    {
        final PropertyManager pm = context.getProperties();
        pm.setProvider(wme_addition_count.key, wme_addition_count);
        pm.setProvider(wme_removal_count.key, wme_removal_count);
        pm.setProvider(max_wm_size.key, max_wm_size);
        pm.setProvider(cumulative_wm_size.key, cumulative_wm_size);
        pm.setProvider(num_wm_sizes_accumulated.key, num_wm_sizes_accumulated);
        
        this.rete = Adaptables.adapt(context, Rete.class);
        this.predefinedSyms = Adaptables.adapt(context, PredefinedSymbols.class);
        this.trace = context.getTrace();
        this.decider = Adaptables.adapt(context, Decider.class);
        this.eventManager = context.getEvents();
    }

    /**
     * <p>wmem.cpp:71:reset_wme_timetags
     * <p>init_soar.cpp:297:reset_statistics 
     */
    public void reset()
    {
        // reset_statistics
        wme_addition_count.reset();
        wme_removal_count.reset();
        max_wm_size.reset();
        cumulative_wm_size.reset();
        num_wm_sizes_accumulated.reset();
        
        // Note: Originally num_existing_wmes was checked here and a warning was printed
        // to catch memory leaks. Removed in jsoar.

        current_wme_timetag = 1;
    }
    
    /**
     * Updates WM size stats. Extracted from do_one_top_level_phase().
     * 
     * <p>init_soar.cpp:1060:do_one_top_level_phase
     * 
     * @param num_wmes_in_rete
     */
    public void updateStats(int num_wmes_in_rete)
    {
        if (num_wmes_in_rete > max_wm_size.longValue())
            max_wm_size.value.set(num_wmes_in_rete);
        cumulative_wm_size.value.addAndGet(num_wmes_in_rete);
        num_wm_sizes_accumulated.increment();
    }
    
    /**
     * Create a new WME object. The WME's timetag is automatically assigned.
     * 
     * <p>wmem.cpp:82:make_wme
     * 
     * @param id the id of the WME
     * @param attr the attribute of the WME
     * @param value the value of the WME
     * @param acceptable true if the WME is acceptable
     * @return the new WME object
     */
    public WmeImpl make_wme(IdentifierImpl id, SymbolImpl attr, SymbolImpl value, boolean acceptable)
    {
        WmeImpl w = new WmeImpl(id, attr, value, acceptable, current_wme_timetag++);
        return w;
    }
    

    /**
     * wmem.cpp:122:add_wme_to_wm
     * 
     * @param w
     */
    public void add_wme_to_wm(WmeImpl w)
    {
        wmes_to_add.push(w);
        IdentifierImpl valueId = w.value.asIdentifier();
        if (valueId != null)
        {
            this.decider.post_link_addition(w.id, valueId);
            if (w.attr == this.predefinedSyms.operator_symbol)
            {
                valueId.isa_operator++;
            }
        }
    }

    /**
     * wmem.cpp:135:remove_wme_from_wm
     * 
     * @param w
     */
    public void remove_wme_from_wm(WmeImpl w)
    {
        wmes_to_remove.push(w);

        IdentifierImpl valueId = w.value.asIdentifier();

        if (valueId != null)
        {
            if(valueId.decider_wme == w)
            {
                valueId.decider_wme = null; // This is essential to avoid memory leaks!
            }
            
            this.decider.post_link_removal(w.id, valueId);
            if (w.attr == this.predefinedSyms.operator_symbol)
            {
                // Do this afterward so that gSKI can know that this is an operator
                valueId.isa_operator--;
            }
        }

        // Avoid memory leaks!
        w.preference = null;
        w.chunker_bt_pref = null;

        // When we remove a WME, we always have to determine if it's on a GDS,
        // and, if so, after removing the WME, if there are no longer any WMEs
        // on the GDS, then we can free the GDS memory
        if (w.gds != null)
        {
            w.gds.removeWme(w);
        }
    }

    /**
     * wmem.cpp:167:remove_wme_list_from_wm
     * 
     * @param w
     * @param updateWmeMap (defaults to false in CSoar)
     */
    public void remove_wme_list_from_wm(WmeImpl w, boolean updateWmeMap /*=false*/)
    {
        if(updateWmeMap)
        {
            // Note: For jsoar, moved this out of the loop below into a single callback
            // soar_invoke_callbacks( thisAgent, INPUT_WME_GARBAGE_COLLECTED_CALLBACK, reinterpret_cast<soar_call_data >( w ) );
            eventManager.fireEvent(new InputWmeGarbageCollectedEvent(w));
        }
        
        while (w != null)
        {
            final WmeImpl next_w = w.next;
            remove_wme_from_wm(w);
            w = next_w;
        }
    }
    

    /**
     * wmem.cpp:186:do_buffered_wm_changes
     */
    public void do_buffered_wm_changes(InputOutputImpl io)
    {
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // struct timeval start_tv;
        // #endif
        // #endif

        // if no wme changes are buffered, do nothing
        if (wmes_to_add.isEmpty() && wmes_to_remove.isEmpty())
        {
            return;
        }

        // call output module in case any changes are output link changes
        io.inform_output_module_of_wm_changes (wmes_to_add, wmes_to_remove);
        
        eventManager.fireEvent(new WorkingMemoryChangedEvent(wmes_to_add, wmes_to_remove));
        
        // stuff wme changes through the rete net
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // start_timer (thisAgent, &start_tv);
        // #endif
        // #endif
        for (ListItem<WmeImpl> w = wmes_to_add.first; w != null; w = w.next)
        {
            this.rete.add_wme_to_rete(w.item);
        }
        
        for (ListItem<WmeImpl> w = wmes_to_remove.first; w != null; w = w.next)
        {
            this.rete.remove_wme_from_rete(w.item);
        }
        
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // stop_timer (thisAgent, &start_tv, &thisAgent->match_cpu_time[thisAgent->current_phase]);
        // #endif
        // #endif

        warnIfSameWmeAddedAndRemoved();
        
        final boolean traceChanges = trace.isEnabled(Category.WM_CHANGES);
        // do tracing and cleanup stuff
        for (ListItem<WmeImpl> w = wmes_to_add.first; w != null; w = w.next)
        {
            // TODO Originally "filtered_print_wme_add", but filtering seems disabled in CSoar...
            if(traceChanges)
            {
                trace.startNewLine().print("=>WM: %s", w.item);
            }
            wme_addition_count.increment();
        }
        
        for (ListItem<WmeImpl> w = wmes_to_remove.first; w != null; w = w.next)
        {
            // TODO Originally "filtered_print_wme_remove", but filtering seems disabled in CSoar...
            if(traceChanges)
            {
                trace.startNewLine().print("<=WM: %s", w.item);
            }
            wme_removal_count.increment();
        }
        
        wmes_to_add.clear();
        wmes_to_remove.clear();
    }

    /**
     * Extracted from do_buffered_wm_changes
     */
    private void warnIfSameWmeAddedAndRemoved()
    {
        // TODO warn if watching wmes and same wme was added and removed
        // if (thisAgent->sysparams[TRACE_WM_CHANGES_SYSPARAM]) {
        // for (c=thisAgent->wmes_to_add; c!=NIL; c=next_c) {
        // next_c = c->rest;
        // w = static_cast<wme_struct *>(c->first);
        // for (cr=thisAgent->wmes_to_remove; cr!=NIL; cr=next_c) {
        // next_c = cr->rest;
        // if (w == cr->first) {
        // const char * const kWarningMessage = "WARNING: WME added and removed in same phases : ";
        // print (thisAgent, const_cast< char* >( kWarningMessage) );
        // xml_begin_tag( thisAgent, kTagWarning );
        // xml_att_val( thisAgent, kTypeString, kWarningMessage );
        // print_wme(thisAgent, w);
        // xml_end_tag( thisAgent, kTagWarning );
        // }
        // }
        // }
        // }
    }
}
