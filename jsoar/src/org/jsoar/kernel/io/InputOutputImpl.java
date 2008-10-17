/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel.io;

import java.util.Iterator;
import java.util.LinkedList;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.io.OutputEvent.OutputMode;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.DoubleSymbolImpl;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.IntegerSymbolImpl;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.StringSymbolImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.Arguments;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

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
public class InputOutputImpl implements InputOutput, SymbolFactory
{
    private final Agent context;
    
    private boolean output_link_changed = false;

    private final LinkedList<OutputLink> existing_output_links = new LinkedList<OutputLink>();
    private final ListMultimap<IdentifierImpl, OutputLink> associated_output_links = Multimaps.newArrayListMultimap();
    
    private IdentifierImpl io_header;

    private IdentifierImpl io_header_input;

    private IdentifierImpl io_header_output;

    private WmeImpl io_header_link;
    private WmeImpl outputLinkWme;
    
    private OutputLink output_link_for_tc;
    private int output_link_tc_num;

    private final TopStateRemovedEvent topStateRemovedEvent = new TopStateRemovedEvent(this);

    private final InputCycleEvent inputCycleEvent = new InputCycleEvent(this);
    
    /**
     * @param context
     */
    public InputOutputImpl(Agent context)
    {
        this.context = context;
    }
    
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.io.InputOutput#getSymbolFactory()
     */
    @Override
    public SymbolFactory getSymbolFactory()
    {
        return this;
    }

    /**
     * I/O initialization originally in init_soar.cpp:init_agent_memory
     */
    public void init_agent_memory()
    {
        this.io_header = createIdentifier('I');
        this.io_header_input = createIdentifier ('I');
        this.io_header_output = createIdentifier ('I');

      /* The following code was taken from the do_input_cycle function of io.cpp */
        // Creating the io_header and adding the top state io header wme
        this.io_header_link = addInputWme(context.decider.top_state,
                                           context.predefinedSyms.io_symbol,
                                           this.io_header);
      // Creating the input and output header symbols and wmes
      // RPM 9/06 changed to use this.input/output_link_symbol
      // Note we don't have to save these wmes for later release since their parent
      //  is already being saved (above), and when we release it they will automatically be released
        addInputWme(this.io_header, context.predefinedSyms.input_link_symbol, this.io_header_input);
        outputLinkWme = addInputWme(this.io_header, context.predefinedSyms.output_link_symbol, this.io_header_output);
    }

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

    /**
     * io.cpp::get_new_io_identifier
     * 
     * @param first_letter
     * @return
     */
    public IdentifierImpl createIdentifier(char first_letter)
    {
        return context.syms.createIdentifier(first_letter);
    }

    /**
     * io.cpp::get_io_identifier
     * 
     * @param first_letter
     * @param number
     * @return
     */
    public IdentifierImpl findIdentifier(char first_letter, int number)
    {
        IdentifierImpl id = context.syms.findIdentifier(first_letter, number);

        if (id == null)
        {
            id = context.syms.createIdentifier(first_letter);
        }

        return id;
    }

    /**
     * io.cpp::get_io_sym_constant
     * 
     * @param name
     * @return
     */
    public StringSymbolImpl createString(String name)
    {
        return context.syms.createString(name);
    }

    /**
     * io.cpp::get_io_int_constant
     * 
     * @param value
     * @return
     */
    public IntegerSymbolImpl createInteger(int value)
    {
        return context.syms.createInteger(value);
    }

    /**
     * io.cpp::get_io_float_constant
     * 
     * @param value
     * @return
     */
    public DoubleSymbolImpl createDouble(double value)
    {
        return context.syms.createDouble(value);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#findDouble(double)
     */
    @Override
    public DoubleSymbol findDouble(double value)
    {
        return context.syms.findDouble(value);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#findInteger(int)
     */
    @Override
    public IntegerSymbol findInteger(int value)
    {
        return context.syms.findInteger(value);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#findString(java.lang.String)
     */
    @Override
    public StringSymbol findString(String value)
    {
        return context.syms.findString(value);
    }

    /**
     * io.cpp::add_input_wme
     * 
     * @param id
     * @param attr
     * @param value
     * @return
     */
    public WmeImpl addInputWme(Identifier id, Symbol attr, Symbol value)
    {
        Arguments.checkNotNull(id, "id");
        Arguments.checkNotNull(attr, "attr");
        Arguments.checkNotNull(value, "value");

        // go ahead and add the wme
        WmeImpl w = context.workingMemory.make_wme((IdentifierImpl) id, (SymbolImpl) attr, (SymbolImpl) value, false);
        ((IdentifierImpl) id).addInputWme(w);
        context.workingMemory.add_wme_to_wm(w);

        return w;
    }

    /**
     * io.cpp:243:remove_input_wme
     * 
     * @param w
     */
    public void removeInputWme(Wme wIn)
    {
        Arguments.checkNotNull(wIn, "w");
        Arguments.check(wIn instanceof WmeImpl, "Incompatible WME type");

        final WmeImpl w = (WmeImpl) wIn;
        
        if (!w.isMemberOfList(w.id.getInputWmes()))
        {
            throw new IllegalArgumentException("w is not currently in working memory");
        }

        /* TODO for efficiency, it might be better to use a hash table for the
        above test, rather than scanning the linked list.  We could have one
        global hash table for all the input wmes in the system. */
        // go ahead and remove the wme
        w.id.removeInputWme(w);

        if (context.operand2_mode)
        {
            if (w.gds != null)
            {
                if (w.gds.getGoal() != null)
                {
                    // TODO verbose trace wm changes in verbose as well
                    context.trace.print(Category.TRACE_WM_CHANGES_SYSPARAM, 
                            "remove_input_wme: Removing state S%d because element in GDS changed. WME: %s\n", 
                            w.gds.getGoal().level, w);

                    context.decider.gds_invalid_so_remove_goal(w);
                    
                    // NOTE: the call to remove_wme_from_wm will take care
                    // of checking if GDS should be removed
                }
            }
        }

        context.workingMemory.remove_wme_from_wm(w);
    }

    /**
     * io.cpp::do_input_cycle
     */
    public void do_input_cycle()
    {
        if (context.decider.prev_top_state != null && context.decider.top_state == null)
        {
            // top state was just removed

            context.getEventManager().fireEvent(topStateRemovedEvent);
            // soar_invoke_callbacks(thisAgent, INPUT_PHASE_CALLBACK, (soar_call_data) TOP_STATE_JUST_REMOVED);
            
            this.io_header = null;
            this.io_header_input = null;
            this.io_header_output = null;
            this.io_header_link = null;
        }

        // if there is a top state, do the normal input cycle
        if (context.decider.top_state != null)
        {
            context.getEventManager().fireEvent(inputCycleEvent);
            // soar_invoke_callbacks(thisAgent, INPUT_PHASE_CALLBACK, (soar_call_data) NORMAL_INPUT_CYCLE);
        }

        // do any WM resulting changes
        context.decider.do_buffered_wm_and_ownership_changes();

        // save current top state for next time
        context.decider.prev_top_state = context.decider.top_state;

        /* --- reset the output-link status flag to FALSE
         * --- when running til output, only want to stop if agent
         * --- does add-wme to output.  don't stop if add-wme done
         * --- during input cycle (eg simulator updates sensor status)
         *     KJC 11/23/98
         */
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
        // TODO get output link callback
        // symbol_to_string(thisAgent, w->attr, FALSE, link_name, LINK_NAME_SIZE);
        // cb = soar_exists_callback_id(thisAgent, OUTPUT_PHASE_CALLBACK, link_name);
        // if (!cb) return;
        
        if(w != outputLinkWme)
        {
            return;
        }
        
        // create new output link structure
        OutputLink ol = new OutputLink(w);
        existing_output_links.push(ol);

        // TODO ol->cb = cb;

        /* SW 07 10 2003
           previously, this wouldn't be done until the first OUTPUT phases.
           However, if we add an output command in the 1st decision cycle,
           Soar seems to ignore it.

           There may be two things going on, the first having to do with the tc 
           calculation, which may get done too late, in such a way that the
           initial calculation includes the command.  The other thing appears
           to be that some data structures are not initialized until the first 
           output phases.  Namely, id->associated_output_links does not seem
           reflect the current output links until the first output-phases.

           To get past these issues, we fake a transitive closure calculation
           with the knowledge that the only thing on the output link at this
           point is the output-link identifier itself.  This way, we capture
           a snapshot of the empty output link, so Soar can detect any changes
           that might occur before the first output_phase. */

        /* KJC & RPM 10/06 commenting out SW's change.
           See near end of init_agent_memory for details  */
        // thisAgent->output_link_tc_num = get_new_tc_number(thisAgent);
        // ol->link_wme->value->id.tc_num = thisAgent->output_link_tc_num;
        // thisAgent->output_link_for_tc = ol;
        // /* --- add output_link to id's list --- */
        // push(thisAgent, thisAgent->output_link_for_tc, ol->link_wme->value->id.associated_output_links);
    }

    /**
     * io.cpp:473:update_for_top_state_wme_removal
     * 
     * @param w
     */
    private void update_for_top_state_wme_removal(WmeImpl w)
    {
        if (w.output_link == null)
            return;
        w.output_link.status = OutputLinkStatus.REMOVED_OL_STATUS;
    }

    /**
     * io.cpp:478:update_for_io_wme_change
     * 
     * @param w
     */
    private void update_for_io_wme_change(WmeImpl w)
    {
        assert associated_output_links.containsKey(w.id);
        
        for (OutputLink ol : associated_output_links.get(w.id))
        {
            if (w.value.asIdentifier() != null)
            {
                // mark ol "modified"
                if ((ol.status == OutputLinkStatus.UNCHANGED_OL_STATUS)
                        || (ol.status == OutputLinkStatus.MODIFIED_BUT_SAME_TC_OL_STATUS))
                    ol.status = OutputLinkStatus.MODIFIED_OL_STATUS;
            }
            else
            {
                // mark ol "modified but same tc"
                if (ol.status == OutputLinkStatus.UNCHANGED_OL_STATUS)
                    ol.status = OutputLinkStatus.MODIFIED_BUT_SAME_TC_OL_STATUS;
            }
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
        for (AsListItem<WmeImpl> it = wmes_being_added.first; it != null; it = it.next)
        {
            final WmeImpl w = it.item;
            if (w.id == io_header)
            {
                update_for_top_state_wme_addition(w);
                setOutputLinkChanged(true);
            }
            if (associated_output_links.containsKey(w.id))
            {
                update_for_io_wme_change(w);
                setOutputLinkChanged(true);
            }
        }
        for (AsListItem<WmeImpl> it = wmes_being_removed.first; it != null; it = it.next)
        {
            final WmeImpl w = it.item;
            if (w.id == io_header)
                update_for_top_state_wme_removal(w);
            if (associated_output_links.containsKey(w.id))
                update_for_io_wme_change(w);
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
    private void remove_output_link_tc_info(OutputLink ol)
    {
        while (!ol.ids_in_tc.isEmpty())
        { /* for each id in the old TC... */
            final IdentifierImpl id = ol.ids_in_tc.pop();

            // remove "ol" from the list of associated_output_links(id)
            if (null == associated_output_links.removeAll(id))
            {
                throw new IllegalStateException("Internal error: can't find output link in id's list");
            }
        }
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
        output_link_for_tc.ids_in_tc.push(id);

        // add output_link to id's list
        // TODO: Make this a method on IdentifierImpl?
        associated_output_links.put(id, output_link_for_tc);

        // do TC through working memory
        // scan through all wmes for all slots for this id
        for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
        {
            IdentifierImpl valueAsId = w.value.asIdentifier();
            if (valueAsId != null)
                add_id_to_output_link_tc(valueAsId);
        }
        for (AsListItem<Slot> s = id.slots.first; s != null; s = s.next)
            for (WmeImpl w = s.item.getWmes(); w != null; w = w.next)
            {
                IdentifierImpl valueAsId = w.value.asIdentifier();
                if (valueAsId != null)
                    add_id_to_output_link_tc(valueAsId);
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
    private void calculate_output_link_tc_info(OutputLink ol)
    {
        // if link doesn't have any substructure, there's no TC
        IdentifierImpl valueAsId = ol.link_wme.value.asIdentifier();
        if (valueAsId == null)
            return;

        // do TC starting with the link wme's value
        output_link_for_tc = ol;
        output_link_tc_num = context.syms.get_new_tc_number();
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
    private LinkedList<Wme> get_io_wmes_for_output_link(OutputLink ol)
    {
        LinkedList<Wme> io_wmes = new LinkedList<Wme>();
        io_wmes.push(ol.link_wme);
        for (IdentifierImpl id : ol.ids_in_tc)
        {
            for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
                io_wmes.push(w);
            for (AsListItem<Slot> s = id.slots.first; s != null; s = s.next)
                for (WmeImpl w = s.item.getWmes(); w != null; w = w.next)
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
        // output_call_info output_call_data;

        Iterator<OutputLink> it = existing_output_links.iterator();
        while(it.hasNext())
        {
            final OutputLink ol = it.next();

            LinkedList<Wme> iw_list = null;

            switch (ol.status)
            {
            case UNCHANGED_OL_STATUS:
                // output link is unchanged, so do nothing
                break;

            case NEW_OL_STATUS:
                // calculate tc, and call the output function
                calculate_output_link_tc_info(ol);
                iw_list = get_io_wmes_for_output_link(ol);
                // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
                // stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // #endif

                context.getEventManager().fireEvent(new OutputEvent(this, OutputMode.ADDED_OUTPUT_COMMAND, iw_list));

                // #ifndef NO_TIMING_STUFF
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // start_timer (thisAgent, &thisAgent->start_phase_tv);
                // #endif
                ol.status = OutputLinkStatus.UNCHANGED_OL_STATUS;
                break;

            case MODIFIED_BUT_SAME_TC_OL_STATUS:
                // don't have to redo the TC, but do call the output function
                iw_list = get_io_wmes_for_output_link(ol);

                // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
                // stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // #endif

                context.getEventManager().fireEvent(new OutputEvent(this, OutputMode.MODIFIED_OUTPUT_COMMAND, iw_list));

                // #ifndef NO_TIMING_STUFF
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // start_timer (thisAgent, &thisAgent->start_phase_tv);
                // #endif
                ol.status = OutputLinkStatus.UNCHANGED_OL_STATUS;
                break;

            case MODIFIED_OL_STATUS:
                // redo the TC, and call the output function
                remove_output_link_tc_info(ol);
                calculate_output_link_tc_info(ol);
                iw_list = get_io_wmes_for_output_link(ol);

                // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
                // stop_timer (thisAgent, &thisAgent->start_phase_tv,
                // &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // #endif

                context.getEventManager().fireEvent(new OutputEvent(this, OutputMode.MODIFIED_OUTPUT_COMMAND, iw_list));

                // #ifndef NO_TIMING_STUFF
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // start_timer (thisAgent, &thisAgent->start_phase_tv);
                // #endif
                ol.status = OutputLinkStatus.UNCHANGED_OL_STATUS;
                break;

            case REMOVED_OL_STATUS:
                // call the output function, and free output_link structure
                remove_output_link_tc_info(ol); /* sets ids_in_tc to NIL */
                iw_list = get_io_wmes_for_output_link(ol); /* gives just the link wme */

                // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
                //      stop_timer (thisAgent, &thisAgent->start_phase_tv, &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
                //      stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
                //      start_timer (thisAgent, &thisAgent->start_kernel_tv);
                //      #endif

                context.getEventManager().fireEvent(new OutputEvent(this, OutputMode.REMOVED_OUTPUT_COMMAND, iw_list));

                //      #ifndef NO_TIMING_STUFF      
                //      stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
                //      start_timer (thisAgent, &thisAgent->start_kernel_tv);
                //      start_timer (thisAgent, &thisAgent->start_phase_tv);
                //      #endif
                ol.link_wme.wme_remove_ref(context.workingMemory);
                it.remove();
                break;
            }
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
