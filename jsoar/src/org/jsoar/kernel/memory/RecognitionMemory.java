/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 10, 2008
 */
package org.jsoar.kernel.memory;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionSupport;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.SoarConstants;
import org.jsoar.kernel.SoarContext;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.learning.ReinforcementLearning;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.rete.ConditionsAndNots;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.SoarReteAssertion;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.ActionSupport;
import org.jsoar.kernel.rhs.FunctionAction;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsFunctionCall;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.rhs.UnboundVariable;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.AsListItem;
import org.jsoar.util.ListHead;

/**
 * Recognition Memory (Firer and Chunker) Routines (Does not include the Rete
 * net)
 * 
 * Init_firer() and init_chunker() should be called at startup time, to do
 * initialization.
 * 
 * Do_preference_phase() runs the entire preference phase. This is called from
 * the top-level control in main.c.
 * 
 * Possibly_deallocate_instantiation() checks whether an instantiation can be
 * deallocated yet, and does so if possible. This is used whenever the
 * (implicit) reference count on the instantiation decreases.
 * 
 * recmem.cpp
 * 
 * @author ray
 */
public class RecognitionMemory
{
    private final SoarContext context;
    
    /**
     * agent.h:174:firer_highest_rhs_unboundvar_index
     */
    private int firer_highest_rhs_unboundvar_index;
    /**
     * agent.h:571:newly_created_instantiations
     */
    private final ListHead<Instantiation> newly_created_instantiations = new ListHead<Instantiation>();
    
    /**
     * during firing, points to the prod. being fired 
     * 
     * agent.h:574:production_being_fired
     */
    private Production production_being_fired;
    
    /**
     * agent.h:367:production_firing_count
     */
    private int production_firing_count = 0;
    /**
     * agent.h:720:FIRING_TYPE
     */
    private SavedFiringType FIRING_TYPE;
    
    /**
     * @param context
     */
    public RecognitionMemory(SoarContext context)
    {
        this.context = context;
    }

    /**
     * Build Prohibit Preference List for Backtracing
     * 
     * recmem.cpp:70:build_prohibits_list
     * 
     * @param inst
     */
    private void build_prohibits_list(Instantiation inst)
    {
        for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            cond.bt.prohibits.clear();
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null && cond.bt.trace != null)
            {
                if (cond.bt.trace.slot != null)
                {
                    Preference pref = cond.bt.trace.slot.getPreferenceList(PreferenceType.PROHIBIT_PREFERENCE_TYPE)
                            .getFirstItem();
                    while (pref != null)
                    {
                        Preference new_pref = null;
                        if (pref.inst.match_goal_level == inst.match_goal_level && pref.in_tm)
                        {
                            cond.bt.prohibits.push(pref);
                            pref.preference_add_ref();
                        }
                        else
                        {
                            new_pref = find_clone_for_level(pref, inst.match_goal_level);
                            if (new_pref != null)
                            {
                                if (new_pref.in_tm)
                                {
                                    cond.bt.prohibits.push(new_pref);
                                    new_pref.preference_add_ref();
                                }
                            }
                        }
                        pref = pref.next_prev.getNextItem();
                    }
                }
            }
        }
    }

    /**
     * This routines take a given preference and finds the clone of it whose
     * match goal is at the given goal_stack_level. (This is used to find the
     * proper preference to backtrace through.) If the given preference itself
     * is at the right level, it is returned. If there is no clone at the right
     * level, NIL is returned.
     * 
     * TODO: Make a method of Preference
     * 
     * recmem.cpp:110:find_clone_for_level
     * 
     * @param p
     * @param level
     * @return
     */
    public static Preference find_clone_for_level(Preference p, int level)
    {
        if (p == null)
        {
            // if the wme doesn't even have a preference on it, we can't backtrace
            // at all (this happens with I/O and some architecture-created wmes
            return null;
        }

        // look at pref and all of its clones, find one at the right level
        if (p.inst.match_goal_level == level)
        {
            return p;
        }

        for (Preference clone = p.next_clone; clone != null; clone = clone.next_clone)
        {
            if (clone.inst.match_goal_level == level)
            {
                return clone;
            }
        }

        for (Preference clone = p.prev_clone; clone != null; clone = clone.prev_clone)
        {
            if (clone.inst.match_goal_level == level)
            {
                return clone;
            }
        }

        // if none was at the right level, we can't backtrace at all
        return null;
    }
    
    /**
     * Given an instantiation, this routines looks at the instantiated
     * conditions to find its match goal. It fills in inst->match_goal and
     * inst->match_goal_level. If there is a match goal, match_goal is set to
     * point to the goal identifier. If no goal was matched, match_goal is set
     * to NIL and match_goal_level is set to ATTRIBUTE_IMPASSE_LEVEL.
     * 
     * TODO Make a method of Instantiation?
     * 
     * recmem.cpp:149:find_match_goal
     * 
     * @param inst
     */
    private static void find_match_goal(Instantiation inst)
    {
        Identifier lowest_goal_so_far = null;
        int lowest_level_so_far = -1;
        for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                Identifier id = cond.bt.wme_.id;
                if (id.isa_goal)
                {
                    if (cond.bt.level > lowest_level_so_far)
                    {
                        lowest_goal_so_far = id;
                        lowest_level_so_far = cond.bt.level;
                    }
                }
            }
        }

        inst.match_goal = lowest_goal_so_far;
        if (lowest_goal_so_far != null)
        {
            inst.match_goal_level = lowest_level_so_far;
        }
        else
        {
            inst.match_goal_level = SoarConstants.ATTRIBUTE_IMPASSE_LEVEL;
        }
    }

    /**
     * Execute_action() executes a given RHS action. For MAKE_ACTION's, it
     * returns the created preference structure, or NIL if an error occurs. For
     * FUNCALL_ACTION's, it returns NIL.
     * 
     * Instantiate_rhs_value() returns the (symbol) instantiation of an
     * rhs_value, or NIL if an error occurs. It takes a new_id_level argument
     * indicating what goal_stack_level a new id is to be created at, in case a
     * gensym is needed for the instantiation of a variable. (although I'm not
     * sure this is really needed.)
     * 
     * As rhs unbound variables are encountered, they are instantiated with new
     * gensyms. These gensyms are then stored in the rhs_variable_bindings
     * array, so if the same unbound variable is encountered a second time it
     * will be instantiated with the same gensym.
     * 
     * recmem.cpp:195:instantiate_rhs_value
     * 
     * @param rv
     * @param new_id_level
     * @param new_id_letter
     * @param tok
     * @param w
     * @return
     */
    public Symbol instantiate_rhs_value(RhsValue rv, int new_id_level, char new_id_letter, Token tok, Wme w)
    {
        RhsSymbolValue rsv = rv.asSymbolValue();
        if (rv != null)
        {
            return rsv.getSym();
        }

        UnboundVariable uv = rv.asUnboundVariable();
        if (uv != null)
        {

            int index = uv.getIndex();
            if (this.firer_highest_rhs_unboundvar_index < index)
            {
                this.firer_highest_rhs_unboundvar_index = index;
            }
            Symbol sym = context.rete.getRhsVariableBinding(index);

            if (sym == null)
            {
                sym = context.syms.make_new_identifier(new_id_letter, new_id_level);
                context.rete.setRhsVariableBinding(index, sym);
                return sym;
            }
            else if (sym.asVariable() != null)
            {
                Variable v = sym.asVariable();
                new_id_letter = v.getFirstLetter();
                sym = context.syms.make_new_identifier(new_id_letter, new_id_level);
                context.rete.setRhsVariableBinding(index, sym);
                return sym;
            }
            else
            {
                return sym;
            }
        }

        ReteLocation rl = rv.asReteLocation();
        if (rl != null)
        {
            return Rete.get_symbol_from_rete_loc(rl.getLevelsUp(), rl.getFieldNum(), tok, w);
        }

        RhsFunctionCall fc = rv.asFunctionCall();
        if (fc != null)
        {
            throw new IllegalStateException("Unknow RhsValue type: " + rv);
        }

        // build up list of argument values
        List<Symbol> arguments = new ArrayList<Symbol>(fc.getArguments().size());
        boolean nil_arg_found = false;
        for (RhsValue arg : fc.getArguments())
        {
            Symbol instArg = instantiate_rhs_value(arg, new_id_level, new_id_letter, tok, w);
            if (instArg == null)
            {
                nil_arg_found = true;
            }
            arguments.add(instArg);
        }

        // if all args were ok, call the function

        if (!nil_arg_found)
        {
            // stop the kernel timer while doing RHS funcalls KJC 11/04
            // the total_cpu timer needs to be updated in case RHS fun is
            // statsCmd
            // #ifndef NO_TIMING_STUFF
            // stop_timer (thisAgent, &thisAgent->start_kernel_tv,
            // &thisAgent->total_kernel_time);
            // stop_timer (thisAgent, &thisAgent->start_total_tv,
            //            &thisAgent->total_cpu_time);
            //    start_timer (thisAgent, &thisAgent->start_total_tv);
            //    #endif

            // TODO: Lookup RHS function and call it
            throw new UnsupportedOperationException("RHS function call not implemented");

            //    #ifndef NO_TIMING_STUFF  // restart the kernel timer
            //    start_timer (thisAgent, &thisAgent->start_kernel_tv);
            //    #endif

        }

        return null;
    }

    /**
     * recmem.cpp:292:execute_action
     * 
     * @param a
     * @param tok
     * @param w
     * @return
     */
    private Preference execute_action(Action a, Token tok, Wme w)
    {
        FunctionAction fa = a.asFunctionAction();
        if (fa != null)
        {
            instantiate_rhs_value(fa.getCall(), -1, 'v', tok, w);
            return null;
        }

        MakeAction ma = a.asMakeAction();

        Symbol idSym = instantiate_rhs_value(ma.id, -1, 's', tok, w);
        if (idSym == null)
        {
            return null; // goto abort_execute_action;
        }
        Identifier id = idSym.asIdentifier();
        if (id == null)
        {
            // TODO: print error
            // print_with_symbols (thisAgent, "Error: RHS makes a preference for
            // %y (not an identifier)\n", id);
            return null; // goto abort_execute_action;
        }

        Symbol attr = instantiate_rhs_value(ma.attr, id.level, 'a', tok, w);
        if (attr == null)
        {
            return null;
        }

        char first_letter = attr.getFirstLetter();

        Symbol value = instantiate_rhs_value(ma.value, id.level, first_letter, tok, w);
        if (value == null)
        {
            return null; // goto abort_execute_action;
        }

        Symbol referent = null;
        if (a.preference_type.isBinary())
        {
            referent = instantiate_rhs_value(ma.referent, id.level, first_letter, tok, w);
            if (referent == null)
            {
                return null; // goto abort_execute_action;
            }
        }

        /* --- RBD 4/17/95 added stuff to handle attribute_preferences_mode --- */
        if (((a.preference_type != PreferenceType.ACCEPTABLE_PREFERENCE_TYPE) && (a.preference_type != PreferenceType.REJECT_PREFERENCE_TYPE))
                && (!(id.isa_goal && (attr == context.predefinedSyms.operator_symbol))))
        {
            if ((context.attribute_preferences_mode == 2) || (context.operand2_mode == true))
            {
                // TODO Print error
                // print_with_symbols (thisAgent, "\nError: attribute preference
                // other than +/- for %y ^%y -- ignoring it.", id, attr);
                return null; // goto abort_execute_action;
            }
            else if (context.attribute_preferences_mode == 1)
            {
                // TODO Print warning
                // print_with_symbols (thisAgent, "\nWarning: attribute
                // preference other than +/- for %y ^%y.", id, attr);
                //
                // growable_string gs = make_blank_growable_string(thisAgent);
                // add_to_growable_string(thisAgent, &gs, "Warning: attribute
                // preference other than +/- for ");
                // add_to_growable_string(thisAgent, &gs,
                // symbol_to_string(thisAgent, id, true, 0, 0));
                //        add_to_growable_string(thisAgent, &gs, " ^");
                //        add_to_growable_string(thisAgent, &gs, symbol_to_string(thisAgent, attr, true, 0, 0));
                //        add_to_growable_string(thisAgent, &gs, ".");
                //        xml_generate_warning(thisAgent, text_of_growable_string(gs));
                //        free_growable_string(thisAgent, gs);

            }
        }

        return new Preference(a.preference_type, id, attr, value, referent);
    }

    /**
     * This routine fills in a newly created instantiation structure with
     * various information.    * At input, the instantiation should have:
     *   - preferences_generated filled in; 
     *   - instantiated conditions filled in;
     *   - top-level positive conditions should have bt.wme_, bt.level, and
     *      bt.trace filled in, but bt.wme_ and bt.trace shouldn't have their
     *      reference counts incremented yet.
     *
     * This routine does the following:
     *   - increments reference count on production;
     *   - fills in match_goal and match_goal_level;
     *   - for each top-level positive cond:
     *        replaces bt.trace with the preference for the correct level,
     *        updates reference counts on bt.pref and bt.wmetraces and wmes
     *   - for each preference_generated, adds that pref to the list of all
     *       pref's for the match goal
     *   - fills in backtrace_number;
     *   - if "need_to_do_support_calculations" is TRUE, calculates o-support
     *       for preferences_generated;
     *       
     * recmem.cpp:385:fill_in_new_instantiation_stuff
     * 
     * @param inst
     * @param need_to_do_support_calculations
     * @param top_goal
     * @param o_support_calculation_type
     */
    void fill_in_new_instantiation_stuff(Instantiation inst, boolean need_to_do_support_calculations,
            final Identifier top_goal)
    {

        // TODO ?? production_add_ref (inst->prod);

        find_match_goal(inst);

        int level = inst.match_goal_level;

        /*
         * Note: since we'll never backtrace through instantiations at the top
         * level, it might make sense to not increment the reference counts on
         * the wmes and preferences here if the instantiation is at the top
         * level. As it stands now, we could gradually accumulate garbage at the
         * top level if we have a never-ending sequence of production firings at
         * the top level that chain on each other's results. (E.g., incrementing
         * a counter on every decision cycle.) I'm leaving it this way for now,
         * because if we go to S-Support, we'll (I think) need to save these
         * around (maybe).
         */

        /*
         * KJC 6/00: maintaining all the top level ref cts does have a big
         * impact on memory pool usage and also performance (due to malloc).
         * (See tests done by Scott Wallace Fall 99.) Therefore added
         * preprocessor macro so that by unsetting macro the top level ref cts
         * are not incremented. It's possible that in some systems, these ref
         * cts may be desireable: they can be added by defining
         * DO_TOP_LEVEL_REF_CTS
         */

        for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            final PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
                {
                    cond.bt.wme_.wme_add_ref();
                }
                else
                {
                    if (level > SoarConstants.TOP_GOAL_LEVEL)
                        cond.bt.wme_.wme_add_ref();
                }
                /*
                 * --- if trace is for a lower level, find one for this level
                 * ---
                 */
                if (pc.bt.trace != null)
                {
                    if (cond.bt.trace.inst.match_goal_level > level)
                    {
                        cond.bt.trace = find_clone_for_level(cond.bt.trace, level);
                    }
                    if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
                    {
                        if (cond.bt.trace != null)
                            cond.bt.trace.preference_add_ref();
                    }
                    else
                    {
                        if ((cond.bt.trace != null) && (level > SoarConstants.TOP_GOAL_LEVEL))
                            cond.bt.trace.preference_add_ref();
                    }
                }
            }
        }

        if (inst.match_goal != null)
        {
            for (Preference p : inst.preferences_generated)
            {
                p.all_of_goal.insertAtHead(inst.match_goal.preferences_from_goal);
                p.on_goal_list = true;
            }
        }
        inst.backtrace_number = 0;

        if ((context.osupport.o_support_calculation_type == 0) || 
            (context.osupport.o_support_calculation_type == 3) || 
            (context.osupport.o_support_calculation_type == 4))
        {
            /* --- do calc's the normal Soar 6 way --- */
            if (need_to_do_support_calculations)
                context.osupport.calculate_support_for_instantiation_preferences(inst, top_goal, context.operand2_mode);
        }
        else if (context.osupport.o_support_calculation_type == 1)
        {
            if (need_to_do_support_calculations)
                context.osupport.calculate_support_for_instantiation_preferences(inst, top_goal, context.operand2_mode);
            /* --- do calc's both ways, warn on differences --- */
            if ((inst.prod.declared_support != ProductionSupport.DECLARED_O_SUPPORT)
                    && (inst.prod.declared_support != ProductionSupport.DECLARED_I_SUPPORT))
            {
                /*
                 * --- At this point, we've done them the normal way. To look
                 * for differences, save o-support flags on a list, then do
                 * Doug's calculations, then compare and restore saved flags.
                 * ---
                 */
                List<Preference> saved_flags = new ArrayList<Preference>();
                for (Preference pref : inst.preferences_generated)
                {
                    saved_flags.add(pref.o_supported ? pref : null);
                }
                // Note: I just used add() above, so the list isn't backwards in
                // Java
                // saved_flags = destructively_reverse_list (saved_flags);
                context.osupport.dougs_calculate_support_for_instantiation_preferences(inst);
                boolean difference_found = false;
                int savedFlagsIndex = 0;
                for (Preference pref : inst.preferences_generated)
                {
                    Preference saved = saved_flags.get(savedFlagsIndex++);
                    boolean b = (saved != null ? true : false);
                    if (pref.o_supported != b)
                        difference_found = true;
                    pref.o_supported = b;
                }
                if (difference_found)
                {
                    // TODO: warning
                    //        print_with_symbols(thisAgent, "\n*** O-support difference found in production %y",
                    //                           inst.prod.name);
                }
            }
        }
        else
        {
            /* --- do calc's Doug's way --- */
            if ((inst.prod.declared_support != ProductionSupport.DECLARED_O_SUPPORT)
                    && (inst.prod.declared_support != ProductionSupport.DECLARED_I_SUPPORT))
            {
                context.osupport.dougs_calculate_support_for_instantiation_preferences(inst);
            }
        }
    }
    
    /**
     * Macro returning TRUE iff we're supposed to trace firings for the
     * given instantiation, which should have the "prod" field filled in.
     * 
     * recmem.cpp:532:trace_firings_of_inst
     * 
     * @param inst
     * @return
     */
    private boolean trace_firings_of_inst(Instantiation inst)
    {
        // TODO: Implement trace_firings_of_inst
        return false;
//      return ((inst)->prod &&
//        (thisAgent->sysparams[TRACE_FIRINGS_OF_USER_PRODS_SYSPARAM+(inst)->prod->type] ||
//        ((inst)->prod->trace_firings)));
    }

    /**
     * This builds the instantiation for a new match, and adds it to
     * newly_created_instantiations. It also calls chunk_instantiation() to do
     * any necessary chunk or justification building.
     * 
     * recmem.cpp:548:create_instantiation
     * 
     * @param prod
     * @param tok
     * @param w
     * @param top_goal
     * @param o_support_calculation_type
     */
    public void create_instantiation(Production prod, Token tok, Wme w, Identifier top_goal)
    {
        // Symbol **cell;

        // #ifdef BUG_139_WORKAROUND
        /* RPM workaround for bug #139: don't fire justifications */
        if (prod.type == ProductionType.JUSTIFICATION_PRODUCTION_TYPE)
        {
            return;
        }
        // #endif

        Instantiation inst = new Instantiation(prod, tok, w);
        inst.inProdList.insertAtHead(this.newly_created_instantiations);

        if (context.operand2_mode)
        {
            // TODO verbose
            // if (thisAgent->soar_verbose_flag == TRUE) {
            // print_with_symbols(thisAgent, "\n in create_instantiation: %y",
            // inst->prod->name);
            // char buf[256];
            // SNPRINTF(buf, 254, "in create_instantiation: %s",
            // symbol_to_string(thisAgent, inst->prod->name, true, 0, 0));
            // xml_generate_verbose(thisAgent, buf);
            // }
        }
        /* REW: end 09.15.96 */

        this.production_being_fired = inst.prod;
        prod.firing_count++;
        this.production_firing_count++;

        // build the instantiated conditions, and bind LHS variables
        ConditionsAndNots cans = context.rete.p_node_to_conditions_and_nots(prod.p_node, tok, w, false);
        inst.top_of_instantiated_conditions = cans.dest_top_cond;
        inst.bottom_of_instantiated_conditions = cans.dest_bottom_cond;
        inst.nots = cans.dest_nots;

        // record the level of each of the wmes that was positively tested
        for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            if (cond.asPositiveCondition() != null)
            {
                cond.bt.level = cond.bt.wme_.id.level;
                cond.bt.trace = cond.bt.wme_.preference;
            }
        }

        /* --- print trace info --- */
        boolean trace_it = trace_firings_of_inst(inst);
        if (trace_it)
        {
            // TODO trace firings
            // if (get_printer_output_column(thisAgent)!=1) print (thisAgent,
            // "\n"); /* AGR 617/634 */
            // print (thisAgent, "Firing ");
            // print_instantiation_with_wmes
            // (thisAgent, inst,
            // (wme_trace_type)(thisAgent->sysparams[TRACE_FIRINGS_WME_TRACE_TYPE_SYSPARAM]),
            // 0);
        }

        /*
         * --- initialize rhs_variable_bindings array with names of variables
         * (if there are any stored on the production -- for chunks there won't
         * be any) ---
         */
        int index = 0;
        for (Variable c : prod.rhs_unbound_variables)
        {
            context.rete.setRhsVariableBinding(index, c);
            index++;
        }
        this.firer_highest_rhs_unboundvar_index = index - 1;

        /*
         * 7.1/8 merge: Not sure about this. This code in 704, but not in either
         * 7.1 or 703/soar8
         */
        /* --- Before executing the RHS actions, tell the user that the -- */
        /* --- phase has changed to output by printing the arrow --- */
        // TODO trace firings
        // if (trace_it &&
        // thisAgent->sysparams[TRACE_FIRINGS_PREFERENCES_SYSPARAM]) {
        // print (thisAgent, " -->\n");
        // xml_object( thisAgent, kTagActionSideMarker );
        // }
        /* --- execute the RHS actions, collect the results --- */
        inst.preferences_generated.first = null;
        boolean need_to_do_support_calculations = false;
        for (Action a = prod.action_list; a != null; a = a.next)
        {

            Preference pref = null;
            if (prod.type != ProductionType.TEMPLATE_PRODUCTION_TYPE)
            {
                pref = execute_action(a, tok, w);
            }
            else
            {
                pref = null;
                /* Symbol *result = */ReinforcementLearning.rl_build_template_instantiation(inst, tok, w);
            }

            /*
             * SoarTech changed from an IF stmt to a WHILE loop to support
             * GlobalDeepCpy
             */
            while (pref != null)
            {
                pref.inst = inst;
                pref.inst_next_prev.insertAtHead(inst.preferences_generated);
                if (inst.prod.declared_support == ProductionSupport.DECLARED_O_SUPPORT)
                    pref.o_supported = true;
                else if (inst.prod.declared_support == ProductionSupport.DECLARED_I_SUPPORT)
                {
                    pref.o_supported = false;
                }
                else
                {

                    if (context.operand2_mode)
                    {
                        pref.o_supported = (this.FIRING_TYPE == SavedFiringType.PE_PRODS) ? true : false;
                    }
                    /* REW: end 09.15.96 */

                    else
                    {
                        if (a.support == ActionSupport.O_SUPPORT)
                            pref.o_supported = true;
                        else if (a.support == ActionSupport.I_SUPPORT)
                            pref.o_supported = false;
                        else
                        {
                            need_to_do_support_calculations = true;
                            // TODO verbose
                            // if (thisAgent->soar_verbose_flag == TRUE) {
                            // printf("\n\nin create_instantiation():
                            // need_to_do_support_calculations == TRUE!!!\n\n");
                            // xml_generate_verbose(thisAgent, "in
                            // create_instantiation():
                            // need_to_do_support_calculations == TRUE!!!");
                            // }
                        }

                    }

                }

                /*
                 * TEMPORARY HACK (Ideally this should be doable through the
                 * external kernel interface but for now using a couple of
                 * global STL lists to get this information from the rhs
                 * function to this prefference adding code)
                 * 
                 * Getting the next pref from the set of possible prefs added by
                 * the deep copy rhs function
                 */
                // TODO deep-copy
                // if ( glbDeepCopyWMEs != 0 ) {
                // wme* tempwme = glbDeepCopyWMEs;
                // pref = make_preference(thisAgent,
                // a->preference_type,
                // tempwme->id,
                // tempwme->attr,
                // tempwme->value, 0);
                // glbDeepCopyWMEs = tempwme->next;
                // deallocate_wme(thisAgent, tempwme);
                // } else {
                pref = null;
                // }
            }
        }

        /* --- reset rhs_variable_bindings array to all zeros --- */
        index = 0;
        for (; index < firer_highest_rhs_unboundvar_index; ++index)
        {
            context.rete.setRhsVariableBinding(index, null);
        }

        /* --- fill in lots of other stuff --- */
        fill_in_new_instantiation_stuff(inst, need_to_do_support_calculations, top_goal);

        /* --- print trace info: printing preferences --- */
        /* Note: can't move this up, since fill_in_new_instantiation_stuff gives
           the o-support info for the preferences we're about to print */
        // TODO trace
        //   if (trace_it && thisAgent->sysparams[TRACE_FIRINGS_PREFERENCES_SYSPARAM]) {
        //      for (pref=inst->preferences_generated; pref!=NIL; pref=pref->inst_next) {
        //         print (thisAgent, " ");
        //         print_preference (thisAgent, pref);
        //      }
        //   }
        /* mvp 5-17-94 */
        build_prohibits_list(inst);

        this.production_being_fired = null;

        /* --- build chunks/justifications if necessary --- */
        Chunker.chunk_instantiation(inst, false /* TODO thisAgent->sysparams[LEARNING_ON_SYSPARAM] != 0*/);

        /* MVP 6-8-94 */
        // TODO: callback
        //   if (!thisAgent->system_halted) {
        //      /* --- invoke callback function --- */
        //      soar_invoke_callbacks(thisAgent, 
        //            FIRING_CALLBACK,
        //            (soar_call_data) inst);
        //
        //   }
    }
    
    /**
     * This deallocates the given instantiation. This should only be invoked via
     * the possibly_deallocate_instantiation() macro.
     * 
     * recmem.cpp:757:deallocate_instantiation
     * 
     * @param inst
     */
    private void deallocate_instantiation(Instantiation inst)
    {
        int level = inst.match_goal_level;

        // #ifdef DEBUG_INSTANTIATIONS
        // if (inst->prod)
        // print_with_symbols (thisAgent, "\nDeallocate instantiation of
        // %y",inst->prod->name);
        // #endif

        for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next)
            if (cond.asPositiveCondition() != null)
            {

                /*
                 * mvp 6-22-94, modified 94.01.17 by AGR with lotsa help from
                 * GAP
                 */
                if (!cond.bt.prohibits.isEmpty())
                {
                    for (Preference pref : cond.bt.prohibits)
                    {
                        if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
                        {
                            pref.preference_remove_ref(context.prefMemory);
                        }
                        else
                        {
                            if (level > SoarConstants.TOP_GOAL_LEVEL)
                                pref.preference_remove_ref(context.prefMemory);
                        }
                    }
                    cond.bt.prohibits.clear();
                }
                /* mvp done */

                if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
                {
                    cond.bt.wme_.wme_remove_ref(context.workingMemory);
                    if (cond.bt.trace != null)
                        cond.bt.trace.preference_remove_ref(context.prefMemory);
                }
                else
                {
                    if (level > SoarConstants.TOP_GOAL_LEVEL)
                    {
                        cond.bt.wme_.wme_remove_ref(context.workingMemory);
                        if (cond.bt.trace != null)
                            cond.bt.trace.preference_remove_ref(context.prefMemory);
                    }
                }
            }

        inst.top_of_instantiated_conditions = null;//  deallocate_condition_list (thisAgent, inst->top_of_instantiated_conditions);
        inst.nots = null; //deallocate_list_of_nots (thisAgent, inst->nots);
        // TODO if (inst.prod != null) production_remove_ref (thisAgent, inst->prod);
    }
    
    /**
     * recmem.h:65:possibly_deallocate_instantiation
     * 
     * @param inst
     */
    private void possibly_deallocate_instantiation(Instantiation inst)
    {
        if (inst.preferences_generated.isEmpty() && !inst.in_ms)
            deallocate_instantiation(inst);
    }


    /**
     * This retracts the given instantiation.
     * 
     * recmem.cpp:814:retract_instantiation
     * 
     * @param inst
     */
    public void retract_instantiation(Instantiation inst)
    {
        // invoke callback function
        // TODO callback
        // soar_invoke_callbacks(thisAgent,
        // RETRACTION_CALLBACK,
        // (soar_call_data) inst);

        boolean retracted_a_preference = false;

        boolean trace_it = trace_firings_of_inst(inst);

        // retract any preferences that are in TM and aren't o-supported
        AsListItem<Preference> prefItem = inst.preferences_generated.first;

        while (prefItem != null)
        {
            AsListItem<Preference> nextItem = prefItem.next;
            Preference pref = prefItem.get();
            if (pref.in_tm && !pref.o_supported)
            {

                // TODO trace
                // if (trace_it) {
                // if (!retracted_a_preference) {
                // if (get_printer_output_column(thisAgent)!=1) print
                // (thisAgent, "\n"); /* AGR 617/634 */
                // print (thisAgent, "Retracting ");
                // print_instantiation_with_wmes (thisAgent, inst,
                // (wme_trace_type)thisAgent->sysparams[TRACE_FIRINGS_WME_TRACE_TYPE_SYSPARAM],1);
                // if (thisAgent->sysparams[TRACE_FIRINGS_PREFERENCES_SYSPARAM])
                // {
                // print (thisAgent, " -->\n");
                // xml_object( thisAgent, kTagActionSideMarker );
                // }
                // }
                // if (thisAgent->sysparams[TRACE_FIRINGS_PREFERENCES_SYSPARAM])
                // {
                // print (thisAgent, " ");
                // print_preference (thisAgent, pref);
                // }
                // }

                context.prefMemory.remove_preference_from_tm(pref);
                retracted_a_preference = true;
            }
            prefItem = nextItem;
        }

        // remove inst from list of instantiations of this production
        inst.inProdList.remove(inst.prod.instantiations);

        // if retracting a justification, excise it
        /*
         * if the reference_count on the production is 1 (or less) then the only
         * thing supporting this justification is the instantiation, hence it
         * has already been excised, and doing it again is wrong.
         */
        if (inst.prod.type == ProductionType.JUSTIFICATION_PRODUCTION_TYPE && inst.prod.reference_count > 1)
            inst.prod.excise_production(false);

        /* --- mark as no longer in MS, and possibly deallocate  --- */
        inst.in_ms = false;
        possibly_deallocate_instantiation(inst);
    }

    /**
     * This routine scans through newly_created_instantiations, asserting each
     * preference generated except for o-rejects. It also removes each
     * instantiation from newly_created_instantiations, linking each onto the
     * list of instantiations for that particular production. O-rejects are
     * bufferred and handled after everything else.
     * 
     * Note that some instantiations on newly_created_instantiations are not in
     * the match set--for the initial instantiations of chunks/justifications,
     * if they don't match WM, we have to assert the o-supported preferences and
     * throw away the rest.
     * 
     * recmem.cpp:891:assert_new_preferences
     */
    private void assert_new_preferences()
    {
        final ListHead<Preference> o_rejects = new ListHead<Preference>();

        // TODO verbose
        // /* REW: begin 09.15.96 */
        // if ((operand2_mode) &&
        // (thisAgent->soar_verbose_flag == TRUE)) {
        // printf("\n in assert_new_preferences:");
        // xml_generate_verbose(thisAgent, "in assert_new_preferences:");
        // }
        // /* REW: end 09.15.96 */

        if (SoarConstants.O_REJECTS_FIRST)
        {

            // slot *s;
            // preference *p, *next_p;

            /*
             * Do an initial loop to process o-rejects, then re-loop to process
             * normal preferences.
             */
            AsListItem<Instantiation> inst, next_inst;
            for (inst = this.newly_created_instantiations.first; inst != null; inst = next_inst)
            {
                next_inst = inst.next;

                AsListItem<Preference> pref, next_pref;
                for (pref = inst.get().preferences_generated.first; pref != null; pref = next_pref)
                {
                    next_pref = pref.next;
                    if ((pref.get().type == PreferenceType.REJECT_PREFERENCE_TYPE) && (pref.get().o_supported))
                    {
                        // o-reject: just put it in the buffer for later
                        pref.next = o_rejects.first;
                        o_rejects.first = pref;
                    }
                }
            }

            if (!o_rejects.isEmpty())
                context.prefMemory.process_o_rejects_and_deallocate_them(o_rejects.first);

            // s = find_slot(pref->id, pref->attr);
            // if (s) {
            // /* --- remove all pref's in the slot that have the same value ---
            // */
            // p = s->all_preferences;
            // while (p) {
            // next_p = p->all_of_slot_next;
            // if (p->value == pref->value)
            // remove_preference_from_tm(thisAgent, p);
            // p = next_p;
            // }
            // }
            // //preference_remove_ref (thisAgent, pref);
            // }
            // }
            // }

        }

        AsListItem<Instantiation> inst, next_inst;
        for (inst = this.newly_created_instantiations.first; inst != null; inst = next_inst)
        {
            next_inst = inst.next;
            if (inst.get().in_ms)
            {
                inst.insertAtHead(inst.get().prod.instantiations);
            }

            // Verbose
            // /* REW: begin 09.15.96 */
            // if (operand2_mode)
            // {
            // if (thisAgent->soar_verbose_flag == TRUE) {
            // print_with_symbols(thisAgent, "\n asserting instantiation: %y\n",
            // inst->prod->name);
            // char buf[256];
            // SNPRINTF(buf, 254, "asserting instantiation: %s",
            // symbol_to_string(thisAgent, inst->prod->name, true, 0, 0));
            // xml_generate_verbose(thisAgent, buf);
            // }
            // }
            // /* REW: end 09.15.96 */

            AsListItem<Preference> pref, next_pref;
            for (pref = inst.get().preferences_generated.first; pref != null; pref = next_pref)
            {
                // TODO all the pref.get()s in here is pretty ugly
                next_pref = pref.next;
                if ((pref.get().type == PreferenceType.REJECT_PREFERENCE_TYPE) && (pref.get().o_supported))
                {
                    if (SoarConstants.O_REJECTS_FIRST)
                    {
                        /* --- o-reject: just put it in the buffer for later --- */
                        pref.next = o_rejects.first;
                        o_rejects.first = pref;
                    }
                    /* REW: begin 09.15.96 */
                    /* No knowledge retrieval necessary in Operand2 */
                    /* REW: end 09.15.96 */

                }
                else if (inst.get().in_ms || pref.get().o_supported)
                {
                    /* --- normal case --- */
                    context.prefMemory.add_preference_to_tm(pref.get());

                    /* REW: begin 09.15.96 */
                    /* No knowledge retrieval necessary in Operand2 */
                    /* REW: end 09.15.96 */
                }
                else
                {
                    /*
                     * --- inst. is refracted chunk, and pref. is not
                     * o-supported: remove the preference ---
                     */

                    /*
                     * --- first splice it out of the clones list--otherwise we
                     * might accidentally deallocate some clone that happens to
                     * have refcount==0 just because it hasn't been asserted yet
                     * ---
                     */

                    if (pref.get().next_clone != null)
                        pref.get().next_clone.prev_clone = pref.get().prev_clone;
                    if (pref.get().prev_clone != null)
                        pref.get().prev_clone.next_clone = pref.get().next_clone;
                    pref.get().next_clone = pref.get().prev_clone = null;

                    /* --- now add then remove ref--this should result in deallocation */
                    pref.get().preference_add_ref();
                    pref.get().preference_remove_ref(context.prefMemory);
                }
            }
        }

        if (SoarConstants.O_REJECTS_FIRST)
        {
            if (!o_rejects.isEmpty())
                context.prefMemory.process_o_rejects_and_deallocate_them(o_rejects.first);
        }
    }

    /**
     * This routine is called from the top level to run the preference phase.
     * 
     * recmem.cpp:1035:do_preference_phase
     * 
     * @param root_goal
     * @param o_support_calculation_type
     */
    public void do_preference_phase(Identifier root_goal, int o_support_calculation_type)
    {
        /*
         * AGR 617/634: These are 2 bug reports that report the same problem,
         * namely that when 2 chunk firings happen in succession, there is an
         * extra newline printed out. The simple fix is to monitor
         * get_printer_output_column and see if it's at the beginning of a line
         * or not when we're ready to print a newline. 94.11.14
         */

        // TODO trace
        // if (thisAgent->sysparams[TRACE_PHASES_SYSPARAM]) {
        // if (thisAgent->operand2_mode == TRUE) {
        // if (thisAgent->current_phase == APPLY_PHASE) { /* it's always IE for
        // PROPOSE */
        // xml_begin_tag( thisAgent, kTagSubphase );
        // xml_att_val( thisAgent, kPhase_Name, kSubphaseName_FiringProductions
        // );
        // switch (thisAgent->FIRING_TYPE) {
        // case PE_PRODS:
        // print (thisAgent, "\t--- Firing Productions (PE) For State At Depth
        // %d ---\n", thisAgent->active_level); // SBW 8/4/2008: added
        // active_level
        // xml_att_val( thisAgent, kPhase_FiringType, kPhaseFiringType_PE );
        // break;
        // case IE_PRODS:
        // print (thisAgent, "\t--- Firing Productions (IE) For State At Depth
        // %d ---\n", thisAgent->active_level); // SBW 8/4/2008: added
        // active_level
        // xml_att_val( thisAgent, kPhase_FiringType, kPhaseFiringType_IE );
        // break;
        // }
        // std::string* levelString = to_string(thisAgent->active_level);
        // xml_att_val( thisAgent, kPhase_LevelNum, levelString->c_str()); //
        // SBW 8/4/2008: active_level for XML output mode
        // xml_end_tag( thisAgent, kTagSubphase );
        // delete levelString;
        // }
        // }
        // else
        // // the XML for this is generated in this function
        // print_phase (thisAgent, "\n--- Preference Phase ---\n",0);
        // }

        this.newly_created_instantiations.first = null;

        /* MVP 6-8-94 */
        SoarReteAssertion assertion = null;
        while ((assertion = context.soarReteListener.get_next_assertion()) != null)
        {
            // TODO check max_chunks_reached
            // if (thisAgent->max_chunks_reached) {
            // thisAgent->system_halted = TRUE;
            // soar_invoke_callbacks(thisAgent,
            // AFTER_HALT_SOAR_CALLBACK,
            // (soar_call_data) NULL);
            // return;
            // }
            create_instantiation(assertion.production, assertion.token, assertion.wme, root_goal);
        }

        assert_new_preferences();

        Instantiation inst = null;
        while ((inst = context.soarReteListener.get_next_retraction()) != null)
        {
            retract_instantiation(inst);
        }

        /* REW: begin 08.20.97 */

        /*
         * In Waterfall, if there are nil goal retractions, then we want to
         * retract them as well, even though they are not associated with any
         * particular goal (because their goal has been deleted). The
         * functionality of this separate routine could have been easily
         * combined in get_next_retraction but I wanted to highlight the
         * distinction between regualr retractions (those that can be mapped
         * onto a goal) and nil goal retractions that require a special data
         * strucutre (because they don't appear on any goal) REW.
         */

        if (context.operand2_mode && context.soarReteListener.hasNilGoalRetractions())
        {
            while ((inst = context.soarReteListener.get_next_nil_goal_retraction()) != null)
            {
                retract_instantiation(inst);
            }
        }

        /* REW: end   08.20.97 */

        // TODO trace
        //  if (thisAgent->sysparams[TRACE_PHASES_SYSPARAM]) {
        //     if (! thisAgent->operand2_mode) {
        //      print_phase (thisAgent, "\n--- END Preference Phase ---\n",1);
        //     }
        //  }
    }

}
