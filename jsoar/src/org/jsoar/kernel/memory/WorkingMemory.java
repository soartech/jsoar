/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 7, 2008
 */
package org.jsoar.kernel.memory;

import java.util.LinkedList;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.Category;

/**
 * @author ray
 */
public class WorkingMemory
{
    private final Agent context;
    
    private int num_existing_wmes;
    private int current_wme_timetag;
    private final LinkedList<Wme> wmes_to_add = new LinkedList<Wme>();
    private final LinkedList<Wme> wmes_to_remove = new LinkedList<Wme>();
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
     * wmem.cpp:71:reset_wme_timetags
     */
    public void reset_wme_timetags()
    {
        if (num_existing_wmes != 0)
        {
            final Printer printer = context.trace.getPrinter();
            printer.warn("Internal warning: wanted to reset wme timetag generator, but\n" +
            		"there are still some wmes allocated. (Probably a memory leak.)\n" +
            		"(Leaving timetag numbers alone.)\n");
            return;
        }
        current_wme_timetag = 1;
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
    public Wme make_wme(Identifier id, Symbol attr, Symbol value, boolean acceptable)
    {
        Wme w = new Wme(id, attr, value, acceptable, current_wme_timetag++);

        return w;
    }
    

    /**
     * wmem.cpp:122:add_wme_to_wm
     * 
     * @param w
     */
    public void add_wme_to_wm(Wme w)
    {
        wmes_to_add.push(w);
        Identifier valueId = w.value.asIdentifier();
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
    public void remove_wme_from_wm(Wme w)
    {
        wmes_to_remove.push(w);

        Identifier valueId = w.value.asIdentifier();

        if (valueId != null)
        {
            context.decider.post_link_removal(w.id, valueId);
            if (w.attr == context.predefinedSyms.operator_symbol)
            {
                /*
                 * Do this afterward so that gSKI can know that this is an
                 * operator
                 */
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
            w.gds_next_prev.remove(w.gds.wmes_in_gds);

            if (w.gds.wmes_in_gds.isEmpty())
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
    public void remove_wme_list_from_wm(Wme w, boolean updateWmeMap /*=false*/)
    {
        Wme next_w = null;

        while (w != null)
        {
            next_w = w.next_prev.getNextItem();

            if (updateWmeMap)
            {
                // TODO soar_invoke_callbacks( thisAgent, INPUT_WME_GARBAGE_COLLECTED_CALLBACK, reinterpret_cast<soar_call_data >( w ) );
            }
            remove_wme_from_wm(w);

            w = next_w;
        }
    }
    

    /**
     * wmem.cpp:186:do_buffered_wm_changes
     */
    public void do_buffered_wm_changes(InputOutput io)
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
        
        /* --- invoke callback routine. wmes_to_add and wmes_to_remove can --- */
        /* --- be fetched from the agent structure. --- */
        // TODO WM_CHANGES_CALLBACK
        // soar_invoke_callbacks(thisAgent, WM_CHANGES_CALLBACK, (soar_call_data) NULL);
        
        /* --- stuff wme changes through the rete net --- */
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // start_timer (thisAgent, &start_tv);
        // #endif
        // #endif
        for (Wme w : wmes_to_add)
        {
            context.rete.add_wme_to_rete(w);
        }
        for (Wme w : wmes_to_remove)
        {
            context.rete.remove_wme_from_rete(w);
        }
        // #ifndef NO_TIMING_STUFF
        // #ifdef DETAILED_TIMING_STATS
        // stop_timer (thisAgent, &start_tv,
        // &thisAgent->match_cpu_time[thisAgent->current_phase]);
        // #endif
        // #endif

        warnIfSameWmeAddedAndRemoved();
        
        /* --- do tracing and cleanup stuff --- */
        for (Wme w : wmes_to_add)
        {
            // TODO Originally "filtered_print_wme_add", but filtering seems disabled in CSoar...
            context.trace.print(Category.TRACE_WM_CHANGES_SYSPARAM, "=>WM: %s", w);
            w.wme_add_ref();
            wme_addition_count++;
        }
        for (Wme w : wmes_to_remove)
        {
            // TODO Originally "filtered_print_wme_remove", but filtering seems disabled in CSoar...
            context.trace.print(Category.TRACE_WM_CHANGES_SYSPARAM, "<=WM: %s", w);

            w.wme_remove_ref(this);
            wme_removal_count++;
        }
        wmes_to_add.clear();
        wmes_to_remove.clear();
    }

    /**
     * wmem.cpp:283:deallocate_wme
     * 
     * @param w
     */
    public void deallocate_wme(Wme w)
    {
        //#ifdef DEBUG_WMES  
        //  print_with_symbols (thisAgent, "\nDeallocate wme: ");
        //  print_wme (thisAgent, w);
        //#endif
        //  symbol_remove_ref (thisAgent, w->id);
        //  symbol_remove_ref (thisAgent, w->attr);
        //  symbol_remove_ref (thisAgent, w->value);
        //  free_with_pool (&thisAgent->wme_pool, w);
        num_existing_wmes--;
    }
    
    /**
     * a utility function for finding the value of the ^name attribute on a 
     * given object (symbol).  It returns the name, or NIL if the object has 
     * no name.
     * 
     * TODO: This seems kind of out of place here.
     * 
     * wmem.cpp:295:find_name_of_object
     * 
     * @param object
     * @return
     */
    public static Symbol find_name_of_object(Symbol object, SymConstant name_symbol)
    {
        Identifier id = object.asIdentifier();
        if (id == null)
        {
            return null;
        }
        Slot s = Slot.find_slot(id, name_symbol);
        if (s == null)
        {
            return null;
        }
        if (s.wmes.isEmpty())
        {
            return null;
        }
        return s.wmes.first.get().value;
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
