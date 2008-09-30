/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel.io;

import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarConstants;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.FloatConstant;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IntConstant;
import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.Arguments;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * User-defined Soar I/O routines should be added at system startup time
 * via calls to add_input_function() and add_output_function().  These 
 * calls add things to the system's list of (1) functions to be called 
 * every input cycle, and (2) symbol-to-function mappings for output
 * commands.  File io.cpp contains the system I/O mechanism itself (i.e.,
 * the stuff that calls the input and output functions), plus the text
 * I/O routines.
 *
 * Init_soar_io() does what it says.  Do_input_cycle() and do_output_cycle()
 * perform the entire input and output cycles -- these routines are called 
 * once per elaboration cycle.  (once per Decision cycle in Soar 8).
 * The output module is notified about WM changes via a call to
 * inform_output_module_of_wm_changes().
 * 
 * Output Routines
 *
 * Inform_output_module_of_wm_changes() and do_output_cycle() are the
 * two top-level entry points to the output routines.  The former is
 * called by the working memory manager, and the latter from the top-level
 * phases sequencer.
 *
 * This module maintains information about all the existing output links
 * and the identifiers and wmes that are in the transitive closure of them.
 * On each output link wme, we put a pointer to an output_link structure.
 * Whenever inform_output_module_of_wm_changes() is called, we look for
 * new output links and modifications/removals of old ones, and update
 * the output_link structures accordingly.
 *
 * Transitive closure information is kept as follows:  each output_link
 * structure has a list of all the ids in the link's TC.  Each id in
 * the system has a list of all the output_link structures that it's
 * in the TC of.
 *
 * After some number of calls to inform_output_module_of_wm_changes(),
 * eventually do_output_cycle() gets called.  It scans through the list
 * of output links and calls the necessary output function for each
 * link that has changed in some way (add/modify/remove).
 * 
 * io.cpp
 * 
 * @author ray
 */
public class InputOutput
{
    private final Agent context;
    
    public boolean output_link_changed = false;

    public final ListHead<OutputLink> existing_output_links = ListHead.newInstance();
    
    private Identifier io_header;

    private Identifier io_header_input;

    private Identifier io_header_output;

    private Wme io_header_link;
    
    private OutputLink output_link_for_tc;
    private InputOutputWme collected_io_wmes;

    private int output_link_tc_num;

    private int d_cycle_last_output;
    
    /**
     * @param context
     */
    public InputOutput(Agent context)
    {
        this.context = context;
    }
    
    /**
     * I/O initialization originally in init_soar.cpp:init_agent_memory
     */
    public void init_agent_memory()
    {
        this.io_header = get_new_io_identifier ('I');
        this.io_header_input = get_new_io_identifier ('I');
        this.io_header_output = get_new_io_identifier ('I');

      /* The following code was taken from the do_input_cycle function of io.cpp */
      // Creating the io_header and adding the top state io header wme
      this.io_header_link = add_input_wme (context.decider.top_state,
                                           context.predefinedSyms.io_symbol,
                                           this.io_header);
      // Creating the input and output header symbols and wmes
      // RPM 9/06 changed to use this.input/output_link_symbol
      // Note we don't have to save these wmes for later release since their parent
      //  is already being saved (above), and when we release it they will automatically be released
      add_input_wme (this.io_header,
              context.predefinedSyms.input_link_symbol,
                     this.io_header_input);
      add_input_wme (this.io_header,
              context.predefinedSyms.output_link_symbol,
                     this.io_header_output);
        
    }

    /**
     * io.cpp::get_new_io_identifier
     * 
     * @param first_letter
     * @return
     */
    public Identifier get_new_io_identifier(char first_letter)
    {
        return context.syms.make_new_identifier(first_letter, SoarConstants.TOP_GOAL_LEVEL);
    }

    /**
     * io.cpp::get_io_identifier
     * 
     * @param first_letter
     * @param number
     * @return
     */
    public Identifier get_io_identifier(char first_letter, int number)
    {
        Identifier id = context.syms.find_identifier(first_letter, number);

        if (id == null)
        {
            id = context.syms.make_new_identifier(first_letter, SoarConstants.TOP_GOAL_LEVEL);
        }

        return id;
    }

    /**
     * io.cpp::get_io_sym_constant
     * 
     * @param name
     * @return
     */
    public SymConstant get_io_sym_constant(String name)
    {
        return context.syms.make_sym_constant(name);
    }

    /**
     * io.cpp::get_io_int_constant
     * 
     * @param value
     * @return
     */
    public IntConstant get_io_int_constant(int value)
    {
        return context.syms.make_int_constant(value);
    }

    /**
     * io.cpp::get_io_float_constant
     * 
     * @param value
     * @return
     */
    public FloatConstant get_io_float_constant(double value)
    {
        return context.syms.make_float_constant(value);
    }

    /**
     * io.cpp::add_input_wme
     * 
     * @param id
     * @param attr
     * @param value
     * @return
     */
    public Wme add_input_wme(Identifier id, Symbol attr, Symbol value)
    {
        Arguments.checkNotNull(id, "id");
        Arguments.checkNotNull(attr, "attr");
        Arguments.checkNotNull(value, "value");

        /* --- go ahead and add the wme --- */
        Wme w = context.workingMemory.make_wme(id, attr, value, false);
        w.next_prev.insertAtHead(id.input_wmes);
        context.workingMemory.add_wme_to_wm(w);

        return w;
    }

    /**
     * io.cpp::find_input_wme_by_timetag_from_id
     * 
     * @param idSym
     * @param timetag
     * @param tc
     * @return
     */
    public Wme find_input_wme_by_timetag_from_id(Identifier idSym, int timetag, int tc)
    {
        // Mark this id as having been visited (the key here is that tc numbers always increase so tc_num must be < tc
        // until it's marked)
        idSym.tc_number = tc;

        // This is inefficient. Using a hash table could save a lot here.
        for (Wme pWME : idSym.input_wmes)
        {
            // PrintDebugFormat("Timetag %ld", pWME->timetag) ;
            if (pWME.timetag == timetag)
                return pWME;

            // NOTE: The test for the tc_num keeps us from getting stuck in loops within graphs
            Identifier valueAsId = pWME.value.asIdentifier();
            if (valueAsId != null && valueAsId.tc_number != tc)
            {
                Wme w = find_input_wme_by_timetag_from_id(valueAsId, timetag, tc);
                if (w != null)
                    return w;
            }
        }

        return null;
    }

    /**
     * io.cpp::remove_input_wme
     * 
     * @param w
     */
    public void remove_input_wme(Wme w)
    {
        Arguments.checkNotNull(w, "w");

        if (!w.id.input_wmes.contains(w))
        {
            throw new IllegalArgumentException("w is not currently in working memory");
        }

        /* Note: for efficiency, it might be better to use a hash table for the
        above test, rather than scanning the linked list.  We could have one
        global hash table for all the input wmes in the system. */
        /* --- go ahead and remove the wme --- */
        w.next_prev.remove(w.id.input_wmes);
        /* REW: begin 09.15.96 */
        if (context.operand2_mode)
        {
            if (w.gds != null)
            {
                if (w.gds.getGoal() != null)
                {
                    // TODO verbose trace wm changes
                    /*
                    if (thisAgent->soar_verbose_flag || thisAgent->sysparams[TRACE_WM_CHANGES_SYSPARAM]) 
                    {
                        char buf[256];
                        SNPRINTF(buf, 254, "remove_input_wme: Removing state S%d because element in GDS changed.", w->gds->goal->id.level);

                        print(thisAgent, buf );
                        print(thisAgent, " WME: "); 

                        xml_begin_tag( thisAgent, kTagVerbose );
                        xml_att_val( thisAgent, kTypeString, buf );
                        print_wme(thisAgent, w);
                        xml_end_tag( thisAgent, kTagVerbose );
                    }
                    */

                    context.decider.gds_invalid_so_remove_goal(w);
                    /* NOTE: the call to remove_wme_from_wm will take care
                    of checking if GDS should be removed */
                }
            }
        }

        /* REW: end   09.15.96 */

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

            // TODO callback INPUT_PHASE_CALLBACK/TOP_STATE_JUST_REMOVED
            // soar_invoke_callbacks(thisAgent, INPUT_PHASE_CALLBACK,
            // (soar_call_data) TOP_STATE_JUST_REMOVED);
            this.io_header = null; /* RBD added 3/25/95 */
            this.io_header_input = null; /* RBD added 3/25/95 */
            this.io_header_output = null; /* KJC added 3/3/99 */
            this.io_header_link = null; /* KJC added 3/3/99 */
        }

        /* --- if there is a top state, do the normal input cycle --- */

        if (context.decider.top_state != null)
        {
            // TODO callback INPUT_PHASE_CALLBACK/NORMAL_INPUT_CYCLE
            // soar_invoke_callbacks(thisAgent, INPUT_PHASE_CALLBACK,
            //             (soar_call_data) NORMAL_INPUT_CYCLE);
        }

        // do any WM resulting changes
        context.decider.do_buffered_wm_and_ownership_changes();

        /* --- save current top state for next time --- */
        context.decider.prev_top_state = context.decider.top_state;

        /* --- reset the output-link status flag to FALSE
         * --- when running til output, only want to stop if agent
         * --- does add-wme to output.  don't stop if add-wme done
         * --- during input cycle (eg simulator updates sensor status)
         *     KJC 11/23/98
         */
        this.output_link_changed = false;

    }

    /**
     * Output Link Status Updates on WM Changes
     * 
     * Top-state link changes:
     * 
     * For wme addition: (top-state ^link-attr anything) create new output_link structure; mark it "new" For wme
     * removal: (top-state ^link-attr anything) mark the output_link "removed"
     * 
     * TC of existing link changes:
     * 
     * For wme addition or removal: (<id> ^att constant): for each link in associated_output_links(id), mark link
     * "modified but same tc" (unless it's already marked some other more serious way)
     * 
     * For wme addition or removal: (<id> ^att <id2>): for each link in associated_output_links(id), mark link
     * "modified" (unless it's already marked some other more serious way)
     * 
     * Note that we don't update all the TC information after every WM change. The TC info doesn't get updated until
     * do_output_cycle() is called.
     * 
     * io.cpp:424:update_for_top_state_wme_addition
     * 
     * @param w
     */
    private void update_for_top_state_wme_addition(Wme w)
    {
        /* --- check whether the attribute is an output function --- */
        // TODO get output link callback
        // symbol_to_string(thisAgent, w->attr, FALSE, link_name, LINK_NAME_SIZE);
        // cb = soar_exists_callback_id(thisAgent, OUTPUT_PHASE_CALLBACK, link_name);
        // if (!cb) return;
        /* --- create new output link structure --- */
        OutputLink ol = new OutputLink(w);
        ol.next_prev.insertAtHead(existing_output_links);

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
    private void update_for_top_state_wme_removal(Wme w)
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
    private void update_for_io_wme_change(Wme w)
    {
        if (w.id.associated_output_links == null)
            return;

        for (OutputLink ol : w.id.associated_output_links)
        {
            if (w.value.asIdentifier() != null)
            {
                /* --- mark ol "modified" --- */
                if ((ol.status == OutputLinkStatus.UNCHANGED_OL_STATUS)
                        || (ol.status == OutputLinkStatus.MODIFIED_BUT_SAME_TC_OL_STATUS))
                    ol.status = OutputLinkStatus.MODIFIED_OL_STATUS;
            }
            else
            {
                /* --- mark ol "modified but same tc" --- */
                if (ol.status == OutputLinkStatus.UNCHANGED_OL_STATUS)
                    ol.status = OutputLinkStatus.MODIFIED_BUT_SAME_TC_OL_STATUS;
            }
        }
    }

    /**
     * io.cpp:497:inform_output_module_of_wm_changes
     * 
     * @param wmes_being_added
     * @param wmes_being_removed
     */
    public void inform_output_module_of_wm_changes(List<Wme> wmes_being_added, List<Wme> wmes_being_removed)
    {
        /* if wmes are added, set flag so can stop when running til output */
        for (Wme w : wmes_being_added)
        {
            if (w.id == io_header)
            {
                update_for_top_state_wme_addition(w);
                output_link_changed = true; /* KJC 11/23/98 */
                this.d_cycle_last_output = context.decisionCycle.d_cycle_count; /* KJC 11/17/05 */
            }
            if (w.id.associated_output_links != null && !w.id.associated_output_links.isEmpty())
            {
                update_for_io_wme_change(w);
                output_link_changed = true; /* KJC 11/23/98 */
                this.d_cycle_last_output = context.decisionCycle.d_cycle_count; /* KJC 11/17/05 */
            }
        }
        for (Wme w : wmes_being_removed)
        {
            if (w.id == io_header)
                update_for_top_state_wme_removal(w);
            if (w.id.associated_output_links != null && !w.id.associated_output_links.isEmpty())
                update_for_io_wme_change(w);
        }
    }

    /**
     * Updating Link TC Information
     * 
     * We make no attempt to do the TC updating intelligently. Whenever the TC changes, we throw away all the old TC
     * info and recalculate the new TC from scratch. I figure that this part of the system won't get used very
     * frequently and I hope it won't be a time hog.
     * 
     * Remove_output_link_tc_info() and calculate_output_link_tc_info() are the main routines here.
     * 
     * io.cpp:548:remove_output_link_tc_info
     * 
     * @param ol
     */
    private void remove_output_link_tc_info(OutputLink ol)
    {
        while (!ol.ids_in_tc.isEmpty())
        { /* for each id in the old TC... */
            Identifier id = ol.ids_in_tc.pop();

            // remove "ol" from the list of associated_output_links(id)
            if (id.associated_output_links == null || !id.associated_output_links.remove(ol))
            {
                throw new IllegalStateException("Internal error: can't find output link in id's list");
            }
        }
    }

    /**
     * 
     * io.cpp:576:add_id_to_output_link_tc
     * 
     * @param id
     */
    private void add_id_to_output_link_tc(Identifier id)
    {
        // if id is already in the TC, exit
        if (id.tc_number == output_link_tc_num)
            return;
        id.tc_number = output_link_tc_num;

        // add id to output_link's list
        output_link_for_tc.ids_in_tc.push(id);

        // add output_link to id's list
        // TODO: Make this a method on Identifier?
        if (id.associated_output_links == null)
        {
            id.associated_output_links = new LinkedList<OutputLink>();
        }
        id.associated_output_links.push(output_link_for_tc);

        // do TC through working memory
        // scan through all wmes for all slots for this id
        for (Wme w : id.input_wmes)
        {
            Identifier valueAsId = w.value.asIdentifier();
            if (valueAsId != null)
                add_id_to_output_link_tc(valueAsId);
        }
        for (Slot s : id.slots)
            for (Wme w : s.wmes)
            {
                Identifier valueAsId = w.value.asIdentifier();
                if (valueAsId != null)
                    add_id_to_output_link_tc(valueAsId);
            }
        /* don't need to check impasse_wmes, because we couldn't have a pointer
           to a goal or impasse identifier */
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
        Identifier valueAsId = ol.link_wme.value.asIdentifier();
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
     * io.cpp:626:add_wme_to_collected_io_wmes
     * 
     * @param w
     */
    private void add_wme_to_collected_io_wmes(Wme w)
    {
        InputOutputWme New = new InputOutputWme(w.id, w.attr, w.value, w.timetag);
        New.next = collected_io_wmes;
        collected_io_wmes = New;
    }

    /**
     * io.cpp:638:get_io_wmes_for_output_link
     * 
     * @param ol
     * @return
     */
    private InputOutputWme get_io_wmes_for_output_link(OutputLink ol)
    {
        this.collected_io_wmes = null;
        add_wme_to_collected_io_wmes(ol.link_wme);
        for (Identifier id : ol.ids_in_tc)
        {
            for (Wme w : id.input_wmes)
                add_wme_to_collected_io_wmes(w);
            for (Slot s : id.slots)
                for (Wme w : s.wmes)
                    add_wme_to_collected_io_wmes(w);
        }
        return this.collected_io_wmes;
    }

    /**
     * This routine is called from the top-level sequencer, and it performs the whole output phases. It scans through the
     * list of existing output links, and takes the appropriate action on each one that's changed.
     * 
     * io.cpp:677:do_output_cycle
     */
    public void do_output_cycle()
    {
        // output_call_info output_call_data;

        AsListItem<OutputLink> olItem, next_ol;
        for (olItem = existing_output_links.first; olItem != null; olItem = next_ol)
        {
            next_ol = olItem.next;
            OutputLink ol = olItem.get();

            InputOutputWme iw_list = null;

            switch (ol.status)
            {
            case UNCHANGED_OL_STATUS:
                /* --- output link is unchanged, so do nothing --- */
                break;

            case NEW_OL_STATUS:
                /* --- calculate tc, and call the output function --- */
                calculate_output_link_tc_info(ol);
                iw_list = get_io_wmes_for_output_link(ol);
                // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
                // stop_timer (thisAgent, &thisAgent->start_phase_tv,
                // &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // #endif

                // TODO Set up and call output callback
                /*
                output_call_data.mode = ADDED_OUTPUT_COMMAND;
                output_call_data.outputs = iw_list;
                if (ol->cb) (ol->cb->function)(thisAgent, ol->cb->eventid, ol->cb->data, &output_call_data);
                */

                // #ifndef NO_TIMING_STUFF
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // start_timer (thisAgent, &thisAgent->start_phase_tv);
                // #endif
                ol.status = OutputLinkStatus.UNCHANGED_OL_STATUS;
                break;

            case MODIFIED_BUT_SAME_TC_OL_STATUS:
                /* --- don't have to redo the TC, but do call the output function --- */
                iw_list = get_io_wmes_for_output_link(ol);

                // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
                // stop_timer (thisAgent, &thisAgent->start_phase_tv,
                // &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // #endif

                // TODO set up and call output link callback
                /*
                output_call_data.mode = MODIFIED_OUTPUT_COMMAND;
                output_call_data.outputs = iw_list;
                if (ol->cb) (ol->cb->function)(thisAgent, ol->cb->eventid, ol->cb->data, &output_call_data);
                */

                // #ifndef NO_TIMING_STUFF
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // start_timer (thisAgent, &thisAgent->start_phase_tv);
                // #endif
                ol.status = OutputLinkStatus.UNCHANGED_OL_STATUS;
                break;

            case MODIFIED_OL_STATUS:
                /* --- redo the TC, and call the output function */
                remove_output_link_tc_info(ol);
                calculate_output_link_tc_info(ol);
                iw_list = get_io_wmes_for_output_link(ol);

                // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
                // stop_timer (thisAgent, &thisAgent->start_phase_tv,
                // &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // #endif

                // TODO set up and call output link callback
                /*
                output_call_data.mode = MODIFIED_OUTPUT_COMMAND;
                output_call_data.outputs = iw_list;
                if (ol->cb) (ol->cb->function)(thisAgent, ol->cb->eventid, ol->cb->data, &output_call_data);
                */

                // #ifndef NO_TIMING_STUFF
                // stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
                // start_timer (thisAgent, &thisAgent->start_kernel_tv);
                // start_timer (thisAgent, &thisAgent->start_phase_tv);
                // #endif
                ol.status = OutputLinkStatus.UNCHANGED_OL_STATUS;
                break;

            case REMOVED_OL_STATUS:
                /* --- call the output function, and free output_link structure --- */
                remove_output_link_tc_info(ol); /* sets ids_in_tc to NIL */
                iw_list = get_io_wmes_for_output_link(ol); /* gives just the link wme */

                // #ifndef NO_TIMING_STUFF /* moved here from do_one_top_level_phase June 05. KJC */
                //      stop_timer (thisAgent, &thisAgent->start_phase_tv, 
                //                   &thisAgent->decision_cycle_phase_timers[thisAgent->current_phase]);
                //      stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->total_kernel_time);
                //      start_timer (thisAgent, &thisAgent->start_kernel_tv);
                //      #endif

                // TODO set up and call output link callback
                /*
                output_call_data.mode = REMOVED_OUTPUT_COMMAND;
                output_call_data.outputs = iw_list;
                if (ol->cb) (ol->cb->function)(thisAgent, ol->cb->eventid, ol->cb->data, &output_call_data);
                */

                //      #ifndef NO_TIMING_STUFF      
                //      stop_timer (thisAgent, &thisAgent->start_kernel_tv, &thisAgent->output_function_cpu_time);
                //      start_timer (thisAgent, &thisAgent->start_kernel_tv);
                //      start_timer (thisAgent, &thisAgent->start_phase_tv);
                //      #endif
                ol.link_wme.wme_remove_ref(context.workingMemory);
                ol.next_prev.remove(existing_output_links);
                break;
            }
        } /* end of for ol */

    }

    /**
     * This is a simple utility function for use in users' output functions. It finds things in an io_wme chain. It
     * takes "outputs" (the io_wme chain), and "id" and "attr" (symbols to match against the wmes), and returns the
     * value from the first wme in the chain with a matching id and attribute. Either "id" or "attr" (or both) can be
     * specified as "don't care" by giving NULL (0) pointers for them instead of pointers to symbols. If no matching wme
     * is found, the function returns a NULL pointer.
     * 
     * @param outputs
     * @param id
     * @param attr
     * @return
     */
    public Symbol get_output_value(InputOutputWme outputs, Identifier id, Symbol attr)
    {
        for (InputOutputWme iw = outputs; iw != null; iw = iw.next)
            if (((id == null) || (id == iw.id)) && ((attr == null) || (attr == iw.attr)))
                return iw.value;
        return null;
    }
}
