/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel.io;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.events.AsynchronousInputReadyEvent;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.events.TopStateRemovedEvent;
import org.jsoar.kernel.events.OutputEvent.OutputMode;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeFactory;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.Arguments;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.events.SoarEventManager;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;

/**
 * User-defined Soar I/O routines should be added at system startup time
 * via calls to add_input_function() and add_output_function().  These 
 * calls add things to the system's list of (1) functions to be called 
 * every input cycle, and (2) symbol-to-function mappings for output
 * commands.  File io.cpp contains the system I/O mechanism itself (i.e.,
 * the stuff that calls the input and output functions), plus the text
 * I/O routines.
 *
 * <p>Init_soar_io() does what it says.  Do_input_cycle() and do_output_cycle()
 * perform the entire input and output cycles -- these routines are called 
 * once per elaboration cycle.  (once per Decision cycle in Soar 8).
 * The output module is notified about WM changes via a call to
 * inform_output_module_of_wm_changes().
 * 
 * <h3>Output Routines</h3>
 *
 * <p>Inform_output_module_of_wm_changes() and do_output_cycle() are the
 * two top-level entry points to the output routines.  The former is
 * called by the working memory manager, and the latter from the top-level
 * phases sequencer.
 *
 * <p>This module maintains information about all the existing output links
 * and the identifiers and wmes that are in the transitive closure of them.
 * On each output link wme, we put a pointer to an output_link structure.
 * Whenever inform_output_module_of_wm_changes() is called, we look for
 * new output links and modifications/removals of old ones, and update
 * the output_link structures accordingly.
 *
 * <p>Transitive closure information is kept as follows:  each output_link
 * structure has a list of all the ids in the link's TC.  Each id in
 * the system has a list of all the output_link structures that it's
 * in the TC of.
 *
 * <p>After some number of calls to inform_output_module_of_wm_changes(),
 * eventually do_output_cycle() gets called.  It scans through the list
 * of output links and calls the necessary output function for each
 * link that has changed in some way (add/modify/remove).
 * 
 * <p>The following fields and functions were omitted:
 * <ul>
 * <li>agent.h:368:d_cycle_last_output
 * <li>io.cpp:626:add_wme_to_collected_io_wmes
 * <li>io.cpp:216:find_input_wme_by_timetag_from_id
 * </ul>
 * <p>io.cpp
 * 
 * @author ray
 */
public class InputOutputImpl implements InputOutput, WmeFactory<InputWme>
{
    private static final Log logger = LogFactory.getLog(InputOutputImpl.class);
    
    /**
     * io.cpp:387
     */
    private static enum OutputLinkStatus
    {
        UNINITIALIZED_OL_STATUS,
        NEW_OL_STATUS,                     /* just created it */
        UNCHANGED_OL_STATUS,             /* normal status */
        MODIFIED_BUT_SAME_TC_OL_STATUS, /* some value in its TC has been
                                                      modified, but the ids in its TC
                                                      are the same */
        MODIFIED_OL_STATUS,               /* the set of ids in its TC has
                                                      changed */
        REMOVED_OL_STATUS,                /* link has just been removed */

    }
    
    private final Agent context;
    private Decider decider;
    private WorkingMemory workingMemory;
    
    /**
     * agent.h:679:prev_top_state
     */
    private IdentifierImpl prev_top_state = null;
    
    private IdentifierImpl io_header;

    private IdentifierImpl io_header_input;

    private IdentifierImpl io_header_output;

    @SuppressWarnings("unused")
    private WmeImpl io_header_link;
    
    private WmeImpl outputLinkWme;
    private OutputLinkStatus outputLinkStatus = OutputLinkStatus.UNINITIALIZED_OL_STATUS;  /* current xxx_OL_STATUS */
    private LinkedList<IdentifierImpl> ids_in_tc = new LinkedList<IdentifierImpl>(); /* ids in TC(link) */
    private boolean output_link_changed = false;
    private final Set<Wme> pendingCommands = new HashSet<Wme>();
    private Marker output_link_tc_num;

    private final TopStateRemovedEvent topStateRemovedEvent = new TopStateRemovedEvent(this);

    private final InputEvent inputEvent = new InputEvent(this);
    private final AsynchronousInputReadyEvent asyncInputReadyEvent = new AsynchronousInputReadyEvent(this);
    
    private final ConcurrentLinkedQueue<InputWmeImpl> wmesToRemove = new ConcurrentLinkedQueue<InputWmeImpl>();
    
    //private final Set<InputWmeImpl> allInputWmes = new LinkedHashSet<InputWmeImpl>();
    
    /**
     * @param context
     */
    public InputOutputImpl(Agent context)
    {
        this.context = context;
    }
    
    public void initialize()
    {
        this.decider = Adaptables.adapt(context, Decider.class);
        this.workingMemory = Adaptables.adapt(context, WorkingMemory.class);
    }
    
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputOutput#asWmeFactory()
     */
    @Override
    public WmeFactory<InputWme> asWmeFactory()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.memory.WmeFactory#addWme(org.jsoar.kernel.symbols.Identifier, org.jsoar.kernel.symbols.Symbol, org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public InputWme addWme(Identifier id, Symbol attr, Symbol value)
    {
        return addInputWme(id, attr, value);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputOutput#getSymbolFactory()
     */
    @Override
    public SymbolFactory getSymbols()
    {
        return context.getSymbols();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputOutput#getEvents()
     */
    @Override
    public SoarEventManager getEvents()
    {
        return context.getEvents();
    }

    /**
     * I/O initialization originally in init_soar.cpp:init_agent_memory
     */
    public void init_agent_memory()
    {
        final PredefinedSymbols predefinedSyms = Adaptables.adapt(context, PredefinedSymbols.class);
        final SymbolFactoryImpl syms = predefinedSyms.getSyms();
        
        // Creating the input and output header symbols and wmes
        this.io_header = syms.createIdentifier('I');
        this.io_header_input = syms.createIdentifier ('I');
        this.io_header_output = syms.createIdentifier ('I');

        /* The following code was taken from the do_input_cycle function of io.cpp */
        // Creating the io_header and adding the top state io header wme

        this.io_header_link = addInputWmeInternal(decider.top_state, predefinedSyms.io_symbol, this.io_header);
        
        // RPM 9/06 changed to use this.input/output_link_symbol
        // Note we don't have to save these wmes for later release since their parent
        //  is already being saved (above), and when we release it they will automatically be released
        addInputWmeInternal(this.io_header, predefinedSyms.input_link_symbol, this.io_header_input);
        outputLinkWme = addInputWmeInternal(this.io_header, predefinedSyms.output_link_symbol, this.io_header_output);
        
        //restorePreviousInput();
    }
    
    /*
    private Symbol findRestoredSymbol(Symbol oldSym, Map<Identifier, Identifier> idMap)
    {
        final Identifier oldId = oldSym.asIdentifier();
        if(oldId != null)
        {
            Identifier newId = idMap.get(oldId);
            if(newId == null)
            {
                newId = context.syms.findOrCreateIdentifier(oldId.getNameLetter(), oldId.getNameNumber());
                idMap.put(oldId, newId);
            }
            return newId;
        }
        else
        {
            return oldSym;
        }
    }

    private void restorePreviousInput()
    {
        final Map<Identifier, Identifier> idMap = new HashMap<Identifier, Identifier>();
        for(InputWmeImpl iw : allInputWmes)
        {
            final Identifier newId = (Identifier) findRestoredSymbol(iw.getIdentifier(), idMap);
            final Symbol newAttr = findRestoredSymbol(iw.getAttribute(), idMap);
            final Symbol newValue = findRestoredSymbol(iw.getValue(), idMap);
            iw.setInner(addInputWmeInternal(newId, newAttr, newValue));
        }
    }
    */
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputOutput#getInputLink()
     */
    @Override
    public Identifier getInputLink()
    {
        return io_header_input;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputOutput#getOutputLink()
     */
    @Override
    public Identifier getOutputLink()
    {
        return io_header_output;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputOutput#addInputWme(org.jsoar.kernel.symbols.Identifier, org.jsoar.kernel.symbols.Symbol, org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public InputWme addInputWme(Identifier id, Symbol attr, Symbol value)
    {
        final InputWmeImpl iw = new InputWmeImpl(this, addInputWmeInternal(id, attr, value));
        //allInputWmes.add(iw);
        return iw;
    }

    public WmeImpl addInputWmeInternal(Identifier id, Symbol attr, Symbol value)
    {
        Arguments.checkNotNull(id, "id");
        Arguments.checkNotNull(attr, "attr");
        Arguments.checkNotNull(value, "value");

        // go ahead and add the wme
        WmeImpl w = this.workingMemory.make_wme((IdentifierImpl) id, (SymbolImpl) attr, (SymbolImpl) value, false);
        ((IdentifierImpl) id).addInputWme(w);
        this.workingMemory.add_wme_to_wm(w);

        return w;
    }
    
    void removeInputWme(InputWme w)
    {
        Arguments.check(w instanceof InputWmeImpl, "Incompatible WME type: " + w + ", " + w.getClass());
        //allInputWmes.remove(w);
        wmesToRemove.offer((InputWmeImpl)w);
    }
    
    private void processPendingWmeRemovals()
    {
        InputWmeImpl w = wmesToRemove.poll();
        while(w != null)
        {
            removeInputWmeInternal(w.getInner());
            w = wmesToRemove.poll();
        }
    }
    
    public void removeInputWmeInternal(WmeImpl w)
    {
        Arguments.checkNotNull(w, "w");

        if (!w.isMemberOfList(w.id.getInputWmes()))
        {
            logger.warn(String.format("removeInputWmeInternal: %s is not currently in working memory. Ignoring.", w));
            return;
        }

        /* TODO for efficiency, it might be better to use a hash table for the
        above test, rather than scanning the linked list.  We could have one
        global hash table for all the input wmes in the system. */
        // go ahead and remove the wme
        w.id.removeInputWme(w);

        if (w.gds != null)
        {
            if (w.gds.getGoal() != null)
            {
                // TODO verbose trace wm changes in verbose as well
                context.getTrace().print(Category.WM_CHANGES, 
                        "remove_input_wme: Removing state S%d because element in GDS changed. WME: %s\n", 
                        w.gds.getGoal().level, w);

                decider.gds_invalid_so_remove_goal(w);
                
                // NOTE: the call to remove_wme_from_wm will take care
                // of checking if GDS should be removed
            }
        }

        this.workingMemory.remove_wme_from_wm(w);
    }

    InputWme updateInputWme(InputWme w, Symbol newValue)
    {
        Arguments.checkNotNull(w, "w");
        Arguments.check(w instanceof InputWmeImpl, "Incompatible WME type: " + w + ", " + w.getClass());

        if(newValue == w.getValue())
        {
            return w;
        }
        
        final InputWmeImpl iw = (InputWmeImpl) w;
        final WmeImpl inner = iw.getInner();
        
        removeInputWmeInternal(inner);
        iw.setInner(addInputWmeInternal(w.getIdentifier(), w.getAttribute(), newValue));
        return w;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputOutput#getPendingCommands()
     */
    @Override
    public List<Wme> getPendingCommands()
    {
        return new ArrayList<Wme>(pendingCommands);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputOutput#asynchronousInputReady()
     */
    @Override
    public void asynchronousInputReady()
    {
        context.getEvents().fireEvent(asyncInputReadyEvent);
    }

    /**
     * io.cpp::do_input_cycle
     */
    public void do_input_cycle()
    {
        this.pendingCommands.clear();
        
        if (prev_top_state != null && decider.top_state == null)
        {
            // top state was just removed

            context.getEvents().fireEvent(topStateRemovedEvent);
            // soar_invoke_callbacks(thisAgent, INPUT_PHASE_CALLBACK, (soar_call_data) TOP_STATE_JUST_REMOVED);
            
            this.io_header = null;
            this.io_header_input = null;
            this.io_header_output = null;
            this.io_header_link = null;
        }

        // if there is a top state, do the normal input cycle
        if (decider.top_state != null)
        {
            context.getEvents().fireEvent(inputEvent);
            // soar_invoke_callbacks(thisAgent, INPUT_PHASE_CALLBACK, (soar_call_data) NORMAL_INPUT_CYCLE);
        }

        processPendingWmeRemovals();
        
        // do any WM resulting changes
        decider.do_buffered_wm_and_ownership_changes();

        // save current top state for next time
        prev_top_state = decider.top_state;

        // reset the output-link status flag to FALSE when running til output, 
        // only want to stop if agent does add-wme to output.  don't stop if 
        // add-wme done during input cycle (eg simulator updates sensor status)
        // KJC 11/23/98
        this.setOutputLinkChanged(false);

    }

    /**
     * Output Link Status Updates on WM Changes
     * 
     * <h3>Top-state link changes:</h3>
     * 
     * <pre>
     * For wme addition: (top-state ^link-attr anything) 
     *  create new output_link structure; mark it "new" For wme
     * removal: (top-state ^link-attr anything) 
     *  mark the output_link "removed"
     * </pre>
     * 
     * <h3>TC of existing link changes:</h3>
     * 
     * <pre>
     * For wme addition or removal: (<id> ^att constant): 
     *  for each link in associated_output_links(id), 
     *  mark link "modified but same tc" (unless it's already marked some other more serious way)
     * </pre>
     * <pre>
     * For wme addition or removal: (<id> ^att <id2>): 
     *  for each link in associated_output_links(id), 
     *  mark link "modified" (unless it's already marked some other more serious way)
     * </pre>
     * 
     * <p>Note that we don't update all the TC information after every WM change. 
     * The TC info doesn't get updated until do_output_cycle() is called.
     * 
     * <p>io.cpp:424:update_for_top_state_wme_addition
     * 
     * @param w
     */
    private void update_for_top_state_wme_addition(WmeImpl w)
    {
        // check whether the attribute is an output function
        if(w == outputLinkWme)
        {
            // create new output link structure
            this.outputLinkStatus = OutputLinkStatus.NEW_OL_STATUS;
            // (removed in jsoar) outputLinkWme.wme_add_ref();
        }
    }

    /**
     * io.cpp:473:update_for_top_state_wme_removal
     * 
     * @param w
     */
    private void update_for_top_state_wme_removal(WmeImpl w)
    {
        if (w == outputLinkWme)
        {
            outputLinkStatus = OutputLinkStatus.REMOVED_OL_STATUS;
        }
    }

    /**
     * io.cpp:478:update_for_io_wme_change
     * 
     * @param w
     * @param added 
     */
    private void update_for_io_wme_change(WmeImpl w, boolean added)
    {
        if(outputLinkStatus != OutputLinkStatus.UNINITIALIZED_OL_STATUS && outputLinkWme.value == w.id)
        {
            if (w.value.asIdentifier() != null)
            {
                // mark ol "modified"
                if ((outputLinkStatus == OutputLinkStatus.UNCHANGED_OL_STATUS)
                        || (outputLinkStatus == OutputLinkStatus.MODIFIED_BUT_SAME_TC_OL_STATUS))
                    outputLinkStatus = OutputLinkStatus.MODIFIED_OL_STATUS;
                
                if(added)
                {
                    pendingCommands.add(w);
                }
                else
                {
                    pendingCommands.remove(w);
                }
            }
            else
            {
                // mark ol "modified but same tc"
                if (outputLinkStatus == OutputLinkStatus.UNCHANGED_OL_STATUS)
                    outputLinkStatus = OutputLinkStatus.MODIFIED_BUT_SAME_TC_OL_STATUS;
            }
            setOutputLinkChanged(true);
        }
    }

    /**
     * <p>TODO: This should be a WM listener or something rather than a direct call. To
     * decouple IO from working memory.
     * 
     * <p>io.cpp:497:inform_output_module_of_wm_changes
     * 
     * @param wmes_being_added
     * @param wmes_being_removed
     */
    public void inform_output_module_of_wm_changes(ListHead<WmeImpl> wmes_being_added, ListHead<WmeImpl> wmes_being_removed)
    {
        // if wmes are added, set flag so can stop when running til output
        for (ListItem<WmeImpl> it = wmes_being_added.first; it != null; it = it.next)
        {
            final WmeImpl w = it.item;
            if (w.id == io_header)
            {
                update_for_top_state_wme_addition(w);
                setOutputLinkChanged(true);
            }
            update_for_io_wme_change(w, true);
        }
        for (ListItem<WmeImpl> it = wmes_being_removed.first; it != null; it = it.next)
        {
            final WmeImpl w = it.item;
            if (w.id == io_header)
                update_for_top_state_wme_removal(w);
            update_for_io_wme_change(w, false);
        }
    }

    /**
     * Updating Link TC Information
     * 
     * <p>We make no attempt to do the TC updating intelligently. Whenever the TC changes, we throw away all the old TC
     * info and recalculate the new TC from scratch. I figure that this part of the system won't get used very
     * frequently and I hope it won't be a time hog.
     * 
     * <p>Remove_output_link_tc_info() and calculate_output_link_tc_info() are the main routines here.
     * 
     * <p>io.cpp:548:remove_output_link_tc_info
     * 
     * @param ol
     */
    private void remove_output_link_tc_info()
    {
        ids_in_tc.clear();
    }

    /**
     * 
     * <p>io.cpp:576:add_id_to_output_link_tc
     * 
     * @param id
     */
    private void add_id_to_output_link_tc(IdentifierImpl id)
    {
        // if id is already in the TC, exit
        if (id.tc_number == output_link_tc_num)
            return;
        id.tc_number = output_link_tc_num;

        // add id to output_link's list
        ids_in_tc.push(id);

        // do TC through working memory scan through all wmes for all slots for this id
        for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
        {
            IdentifierImpl valueAsId = w.value.asIdentifier();
            if (valueAsId != null)
                add_id_to_output_link_tc(valueAsId);
        }
        for (Slot s = id.slots; s != null; s = s.next)
        {
            for (WmeImpl w = s.getWmes(); w != null; w = w.next)
            {
                IdentifierImpl valueAsId = w.value.asIdentifier();
                if (valueAsId != null)
                    add_id_to_output_link_tc(valueAsId);
            }
        }
        // don't need to check impasse_wmes, because we couldn't have a pointer
        // to a goal or impasse identifier
    }

    /**
     * 
     * io.cpp:606:calculate_output_link_tc_info
     * 
     * @param ol
     */
    private void calculate_output_link_tc_info()
    {
        // if link doesn't have any substructure, there's no TC
        IdentifierImpl valueAsId = outputLinkWme.value.asIdentifier();
        if (valueAsId == null)
            return;

        // do TC starting with the link wme's value
        output_link_tc_num = DefaultMarker.create();
        add_id_to_output_link_tc(valueAsId);
    }

    /* --------------------------------------------------------------------
                        Building the list of IO_Wme's

       These routines create and destroy the list of io_wme's in the TC
       of a given output_link.  Get_io_wmes_for_output_link() and
       deallocate_io_wme_list() are the main entry points.  The TC info
       must have already been calculated for the given output link before
       get_io_wmes_for_output_link() is called.
    -------------------------------------------------------------------- */

    /**
     * io.cpp:638:get_io_wmes_for_output_link
     * 
     * @param ol
     * @return
     */
    private LinkedList<Wme> get_io_wmes_for_output_link()
    {
        LinkedList<Wme> io_wmes = new LinkedList<Wme>();
        io_wmes.push(outputLinkWme);
        for (IdentifierImpl id : ids_in_tc)
        {
            for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
                io_wmes.push(w);
            for (Slot s = id.slots; s != null; s = s.next)
                for (WmeImpl w = s.getWmes(); w != null; w = w.next)
                    io_wmes.push(w);
        }
        return io_wmes;
    }

    /**
     * This routine is called from the top-level sequencer, and it performs the 
     * whole output phases. It scans through the list of existing output links,
     * and takes the appropriate action on each one that's changed.
     * 
     * <p>io.cpp:677:do_output_cycle
     */
    public void do_output_cycle()
    {
        LinkedList<Wme> iw_list = null;

        switch (outputLinkStatus)
        {
        case UNCHANGED_OL_STATUS:
            // output link is unchanged, so do nothing
            break;

        case NEW_OL_STATUS:
            // calculate tc, and call the output function
            calculate_output_link_tc_info();
            iw_list = get_io_wmes_for_output_link();
            // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
            // stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
            // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
            // start_timer (thisAgent, &thisAgent->start_kernel_tv);
            // #endif

            context.getEvents().fireEvent(new OutputEvent(this, OutputMode.ADDED_OUTPUT_COMMAND, iw_list));

            // #ifndef NO_TIMING_STUFF
            // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
            // start_timer (thisAgent, &thisAgent->start_kernel_tv);
            // start_timer (thisAgent, &thisAgent->start_phase_tv);
            // #endif
            outputLinkStatus = OutputLinkStatus.UNCHANGED_OL_STATUS;
            break;

        case MODIFIED_BUT_SAME_TC_OL_STATUS:
            // don't have to redo the TC, but do call the output function
            iw_list = get_io_wmes_for_output_link();

            // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
            // stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
            // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
            // start_timer (thisAgent, &thisAgent->start_kernel_tv);
            // #endif

            context.getEvents().fireEvent(new OutputEvent(this, OutputMode.MODIFIED_OUTPUT_COMMAND, iw_list));

            // #ifndef NO_TIMING_STUFF
            // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
            // start_timer (thisAgent, &thisAgent->start_kernel_tv);
            // start_timer (thisAgent, &thisAgent->start_phase_tv);
            // #endif
            outputLinkStatus = OutputLinkStatus.UNCHANGED_OL_STATUS;
            break;

        case MODIFIED_OL_STATUS:
            // redo the TC, and call the output function
            remove_output_link_tc_info();
            calculate_output_link_tc_info();
            iw_list = get_io_wmes_for_output_link();

            // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
            // stop_timer (thisAgent, &thisAgent->start_phase_tv,
            // &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
            // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
            // start_timer (thisAgent, &thisAgent->start_kernel_tv);
            // #endif

            context.getEvents().fireEvent(new OutputEvent(this, OutputMode.MODIFIED_OUTPUT_COMMAND, iw_list));

            // #ifndef NO_TIMING_STUFF
            // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
            // start_timer (thisAgent, &thisAgent->start_kernel_tv);
            // start_timer (thisAgent, &thisAgent->start_phase_tv);
            // #endif
            outputLinkStatus = OutputLinkStatus.UNCHANGED_OL_STATUS;
            break;

        case REMOVED_OL_STATUS:
            // call the output function, and free output_link structure
            remove_output_link_tc_info(); /* sets ids_in_tc to NIL */
            iw_list = get_io_wmes_for_output_link(); /* gives just the link wme */

            // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
            //      stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
            //      stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
            //      start_timer (thisAgent, &thisAgent->start_kernel_tv);
            //      #endif

            context.getEvents().fireEvent(new OutputEvent(this, OutputMode.REMOVED_OUTPUT_COMMAND, iw_list));

            //      #ifndef NO_TIMING_STUFF      
            //      stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
            //      start_timer (thisAgent, &thisAgent->start_kernel_tv);
            //      start_timer (thisAgent, &thisAgent->start_phase_tv);
            //      #endif
            // (removed in jsoar) outputLinkWme.wme_remove_ref(context.workingMemory);
            outputLinkStatus = OutputLinkStatus.UNINITIALIZED_OL_STATUS;
            break;
        }
    }

    /**
     * @param output_link_changed the output_link_changed to set
     */
    private void setOutputLinkChanged(boolean output_link_changed)
    {
        this.output_link_changed = output_link_changed;
    }

    /**
     * @return the output_link_changed
     */
    public boolean isOutputLinkChanged()
    {
        return output_link_changed;
    }
}
