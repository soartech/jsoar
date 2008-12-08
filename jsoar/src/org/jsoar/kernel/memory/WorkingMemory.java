/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 7, 2008
 */
package org.jsoar.kernel.memory;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.events.InputWmeGarbageCollectedEvent;
import org.jsoar.kernel.events.WorkingMemoryChangedEvent;
import org.jsoar.kernel.io.InputOutputImpl;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
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
    private final Agent context;
    
    private int current_wme_timetag = 1;
    private final ListHead<WmeImpl> wmes_to_add = ListHead.newInstance();
    private final ListHead<WmeImpl> wmes_to_remove = ListHead.newInstance();
    private int wme_addition_count;
    private int wme_removal_count;
    
    // Stats stuff
    public int max_wm_size = 0;
    public int cumulative_wm_size = 0;
    public int num_wm_sizes_accumulated;
    
    /**
     * @param operator_symbol
     */
    public WorkingMemory(Agent context)
    {
        this.context = context;
    }

    /**
     * <p>wmem.cpp:71:reset_wme_timetags
     * <p>init_soar.cpp:297:reset_statistics 
     */
    public void reset()
    {
        // reset_statistics
        wme_addition_count = 0;
        wme_removal_count = 0;
        max_wm_size = 0;
        cumulative_wm_size = 0;
        num_wm_sizes_accumulated = 0;
        
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
        if (num_wmes_in_rete > max_wm_size)
            max_wm_size = num_wmes_in_rete;
        cumulative_wm_size += num_wmes_in_rete;
        num_wm_sizes_accumulated++;
    }
    
    /**
     * wmem.cpp:82:make_wme
     * 
     * @param id
     * @param attr
     * @param value
     * @param acceptable
     * @return
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
            context.decider.post_link_addition(w.id, valueId);
            if (w.attr == context.predefinedSyms.operator_symbol)
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
            context.decider.post_link_removal(w.id, valueId);
            if (w.attr == context.predefinedSyms.operator_symbol)
            {
                // Do this afterward so that gSKI can know that this is an operator
                valueId.isa_operator--;
            }
        }

        /*
         * When we remove a WME, we always have to determine if it's on a GDS,
         * and, if so, after removing the WME, if there are no longer any WMEs
         * on the GDS, then we can free the GDS memory
         */
        if (w.gds != null)
        {
            w.gds.removeWme(w);

            if (w.gds.getWmes() == null)
            {
                w.gds = null;
            }
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
            context.getEventManager().fireEvent(new InputWmeGarbageCollectedEvent(w));
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
        
        context.getEventManager().fireEvent(new WorkingMemoryChangedEvent(wmes_to_add, wmes_to_remove));
        
        // stuff wme changes through the rete net
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // start_timer (thisAgent, &start_tv);
        // #endif
        // #endif
        for (AsListItem<WmeImpl> w = wmes_to_add.first; w != null; w = w.next)
        {
            context.rete.add_wme_to_rete(w.item);
        }
        for (AsListItem<WmeImpl> w = wmes_to_remove.first; w != null; w = w.next)
        {
            context.rete.remove_wme_from_rete(w.item);
        }
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // stop_timer (thisAgent, &start_tv, &thisAgent->match_cpu_time[thisAgent->current_phase]);
        // #endif
        // #endif

        warnIfSameWmeAddedAndRemoved();
        
        // do tracing and cleanup stuff
        for (AsListItem<WmeImpl> w = wmes_to_add.first; w != null; w = w.next)
        {
            // TODO Originally "filtered_print_wme_add", but filtering seems disabled in CSoar...
            context.trace.print(Category.WM_CHANGES, "=>WM: %s", w.item);
            wme_addition_count++;
        }
        
        for (AsListItem<WmeImpl> w = wmes_to_remove.first; w != null; w = w.next)
        {
            // TODO Originally "filtered_print_wme_remove", but filtering seems disabled in CSoar...
            context.trace.print(Category.WM_CHANGES, "<=WM: %s", w.item);
            wme_removal_count++;
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
