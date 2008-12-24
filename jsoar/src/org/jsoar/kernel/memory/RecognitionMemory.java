/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 10, 2008
 */
package org.jsoar.kernel.memory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DecisionCycle;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionSupport;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.SoarConstants;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.rete.ConditionsAndNots;
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
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.Arguments;
import org.jsoar.util.ListItem;
import org.jsoar.util.ListHead;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.timing.ExecutionTimers;

/**
 * Recognition Memory (Firer and Chunker) Routines (Does not include the Rete
 * net)
 * 
 * <p>Init_firer() and init_chunker() should be called at startup time, to do
 * initialization.
 * 
 * <p>Do_preference_phase() runs the entire preference phases. This is called from
 * the top-level control in main.c.
 * 
 * <p>Possibly_deallocate_instantiation() checks whether an instantiation can be
 * deallocated yet, and does so if possible. This is used whenever the
 * (implicit) reference count on the instantiation decreases.
 * 
 * <p>recmem.cpp (also some prefmem.cpp)
 * 
 * @author ray
 */
public class RecognitionMemory
{
    private final Agent context;
    private Chunker chunker;
    private DecisionCycle decisionCycle;
    private Rete rete;
    
    /**
     * agent.h:174:firer_highest_rhs_unboundvar_index
     */
    private int firer_highest_rhs_unboundvar_index;
    
    /**
     * agent.h:571:newly_created_instantiations
     */
    public final ListHead<Instantiation> newly_created_instantiations = ListHead.newInstance();
    
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
    public SavedFiringType FIRING_TYPE;
    
    private final RhsFunctionContext rhsFuncContext = new RhsFunctionContextImpl();
    
    /**
     * List of preferences created by currently executing RHS function
     */
    private final LinkedList<Preference> rhsFunctionPreferences = new LinkedList<Preference>();
    
    /**
     * Preference type of currently executing action, used when instantiating preferences from
     * RHS functions
     */
    private PreferenceType rhsFunctionPreferenceType;
    
    /**
     * @param context
     */
    public RecognitionMemory(Agent context)
    {
        this.context = context;
    }

    /**
     * @return The production currently being fired
     */
    public Production getProductionBeingFired()
    {
        return production_being_fired;
    }
    
    /**
     * As the "firer", this object is in charge of implementing the {@link RhsFunctionContext}
     * that is passed to RHS functions when they are executed.
     * 
     * @return The RHS function context used when RHS functions are executed.
     */
    public RhsFunctionContext getRhsFunctionContext()
    {
        return rhsFuncContext;
    }
    
    public void initialize()
    {
        this.chunker = Adaptables.adapt(context, Chunker.class);
        this.decisionCycle = Adaptables.adapt(context, DecisionCycle.class);
        this.rete = Adaptables.adapt(context, Rete.class);
    }
    
    /**
     * <p>init_soar.cpp:297:reset_statistics
     * <p>init_soar.cpp:436:reinitialize_soar
     */
    public void reset()
    {
        this.production_firing_count = 0;
        
        if (context.operand2_mode)
        {
            this.FIRING_TYPE = SavedFiringType.IE_PRODS; /* KJC 10.05.98 was PE */
        }
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
            PositiveCondition pc = cond.asPositiveCondition();
            if(pc != null)
            {
                pc.bt.clearProhibits();
            }
            if (pc != null && pc.bt.trace != null)
            {
                if (pc.bt.trace.slot != null)
                {
                    Preference pref = pc.bt.trace.slot.getPreferencesByType(PreferenceType.PROHIBIT);
                    while (pref != null)
                    {
                        Preference new_pref = null;
                        if (pref.inst.match_goal_level == inst.match_goal_level && pref.isInTempMemory())
                        {
                            pc.bt.addProhibit(pref);
                        }
                        else
                        {
                            new_pref = Preference.find_clone_for_level(pref, inst.match_goal_level);
                            if (new_pref != null)
                            {
                                if (new_pref.isInTempMemory())
                                {
                                    pc.bt.addProhibit(new_pref);
                                }
                            }
                        }
                        pref = pref.next;
                    }
                }
            }
        }
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
     * <p>recmem.cpp:149:find_match_goal
     * 
     * @param inst
     */
    private static void find_match_goal(Instantiation inst)
    {
        IdentifierImpl lowest_goal_so_far = null;
        int lowest_level_so_far = -1;
        for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                IdentifierImpl id = pc.bt.wme_.id;
                if (id.isa_goal)
                {
                    if (pc.bt.level > lowest_level_so_far)
                    {
                        lowest_goal_so_far = id;
                        lowest_level_so_far = pc.bt.level;
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
    public SymbolImpl instantiate_rhs_value(RhsValue rv, int new_id_level, char new_id_letter, Token tok, WmeImpl w)
    {
        RhsSymbolValue rsv = rv.asSymbolValue();
        if (rsv != null)
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
            SymbolImpl sym = this.rete.getRhsVariableBinding(index);

            if (sym == null)
            {
                sym = context.syms.make_new_identifier(new_id_letter, new_id_level);
                this.rete.setRhsVariableBinding(index, sym);
                return sym;
            }
            else if (sym.asVariable() != null)
            {
                Variable v = sym.asVariable();
                new_id_letter = v.getFirstLetter();
                sym = context.syms.make_new_identifier(new_id_letter, new_id_level);
                this.rete.setRhsVariableBinding(index, sym);
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
            return rl.lookupSymbol(tok, w);
        }

        RhsFunctionCall fc = rv.asFunctionCall();
        if (fc == null)
        {
            throw new IllegalStateException("Unknow RhsValue type: " + rv);
        }
        
        // build up list of argument values
        List<Symbol> arguments = new ArrayList<Symbol>(fc.getArguments().size());
        boolean nil_arg_found = false;
        for (RhsValue arg : fc.getArguments())
        {
            SymbolImpl instArg = instantiate_rhs_value(arg, new_id_level, new_id_letter, tok, w);
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
            ExecutionTimers.pause(context.getTotalKernelTimer());
            ExecutionTimers.update(context.getTotalCpuTimer());

            try
            {
                return (SymbolImpl) context.getRhsFunctions().execute(fc.getName().getValue(), arguments);
            }
            catch (RhsFunctionException e)
            {
                context.getPrinter().error("Error: " + e.getMessage());
            }
            finally
            {
                ExecutionTimers.start(context.getTotalKernelTimer());
            }
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
    private Preference execute_action(Action a, Token tok, WmeImpl w)
    {
        rhsFunctionPreferenceType = a.preference_type;
        
        FunctionAction fa = a.asFunctionAction();
        if (fa != null)
        {
    		instantiate_rhs_value(fa.getCall(), -1, 'v', tok, w);
            return null;
        }

        MakeAction ma = a.asMakeAction();

        SymbolImpl idSym = instantiate_rhs_value(ma.id, -1, 's', tok, w);
        if (idSym == null)
        {
            return null; // goto abort_execute_action;
        }
        IdentifierImpl id = idSym.asIdentifier();
        if (id == null)
        {
            context.getPrinter().error("RHS makes a preference for %s (not an identifier)\n", id);
            return null; // goto abort_execute_action;
        }

        SymbolImpl attr = instantiate_rhs_value(ma.attr, id.level, 'a', tok, w);
        if (attr == null)
        {
            return null;
        }
        
        char first_letter = attr.getFirstLetter();

        SymbolImpl value = instantiate_rhs_value(ma.value, id.level, first_letter, tok, w);
        if (value == null)
        {
            return null; // goto abort_execute_action;
        }

        SymbolImpl referent = null;
        if (a.preference_type.isBinary())
        {
            referent = instantiate_rhs_value(ma.referent, id.level, first_letter, tok, w);
            if (referent == null)
            {
                return null; // goto abort_execute_action;
            }
        }

        /* --- RBD 4/17/95 added stuff to handle attribute_preferences_mode --- */
        if (((a.preference_type != PreferenceType.ACCEPTABLE) && (a.preference_type != PreferenceType.REJECT))
                && (!(id.isa_goal && (attr == context.predefinedSyms.operator_symbol))))
        {
            if ((context.attribute_preferences_mode == 2) || (context.operand2_mode == true))
            {
                context.getPrinter().error("attribute preference" +
                		" other than +/- for %s ^%s -- ignoring it.", id, attr);
                return null;
            }
            else if (context.attribute_preferences_mode == 1)
            {
                context.getPrinter().warn("\nWarning: attribute preference" +
                        " other than +/- for %s ^%s -- ignoring it.", id, attr);
                
                // TODO xml
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
     * various information.
     *     
     * <p>At input, the instantiation should have:
     * <ul>
     *   <li>preferences_generated filled in; 
     *   <li>instantiated conditions filled in;
     *   <li>top-level positive conditions should have bt.wme_, bt.level, and
     *      bt.trace filled in, but bt.wme_ and bt.trace shouldn't have their
     *      reference counts incremented yet.
     * </ul>
     * 
     * This routine does the following:
     * <ul>
     *   <li>increments reference count on production;
     *   <li>fills in match_goal and match_goal_level;
     *   <li>for each top-level positive cond:
     *   <ul>
     *        <li>replaces bt.trace with the preference for the correct level,
     *        <li>updates reference counts on bt.pref and bt.wmetraces and wmes
     *   </ul>
     *   <li>for each preference_generated, adds that pref to the list of all
     *       pref's for the match goal
     *   <li>fills in backtrace_number;
     *   <li>if "need_to_do_support_calculations" is TRUE, calculates o-support
     *       for preferences_generated;
     * </ul>
     *   
     * <p>recmem.cpp:385:fill_in_new_instantiation_stuff
     * 
     * @param inst
     * @param need_to_do_support_calculations
     * @param top_goal
     */
    public void fill_in_new_instantiation_stuff(Instantiation inst, boolean need_to_do_support_calculations,
            final IdentifierImpl top_goal)
    {

        inst.prod.production_add_ref();

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
                // TODO This should all be a method on pc.bt. What should it be called?
                if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
                {
                    // (removed in jsoar) pc.bt.wme_.wme_add_ref();
                }
                else
                {
                    if (level > SoarConstants.TOP_GOAL_LEVEL)
                    {
                        // (removed in jsoar) pc.bt.wme_.wme_add_ref();
                    }
                }
                // if trace is for a lower level, find one for this level
                if (pc.bt.trace != null)
                {
                    if (pc.bt.trace.inst.match_goal_level > level)
                    {
                        pc.bt.trace = Preference.find_clone_for_level(pc.bt.trace, level);
                    }
                    if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
                    {
                        if (pc.bt.trace != null)
                            pc.bt.trace.preference_add_ref();
                    }
                    else
                    {
                        if ((pc.bt.trace != null) && (level > SoarConstants.TOP_GOAL_LEVEL))
                            pc.bt.trace.preference_add_ref();
                    }
                }
            }
        }

        if (inst.match_goal != null)
        {
            for (ListItem<Preference> p = inst.preferences_generated.first; p != null; p = p.next)
            {
                p.item.all_of_goal.insertAtHead(inst.match_goal.preferences_from_goal);
                p.item.on_goal_list = true;
            }
        }
        inst.backtrace_number = 0;

        final OSupport osupport = context.osupport;
        final int o_support_calculation_type = osupport.o_support_calculation_type;
        
        if (o_support_calculation_type == 0 || o_support_calculation_type == 3 || o_support_calculation_type == 4)
        {
            // do calc's the normal Soar 6 way
            if (need_to_do_support_calculations)
                osupport.calculate_support_for_instantiation_preferences(inst, top_goal, context.operand2_mode);
        }
        else if (o_support_calculation_type == 1)
        {
            if (need_to_do_support_calculations)
                osupport.calculate_support_for_instantiation_preferences(inst, top_goal, context.operand2_mode);
            
            // do calc's both ways, warn on differences
            if ((inst.prod.declared_support != ProductionSupport.DECLARED_O_SUPPORT)
                    && (inst.prod.declared_support != ProductionSupport.DECLARED_I_SUPPORT))
            {
                // At this point, we've done them the normal way. To look
                // for differences, save o-support flags on a list, then do
                // Doug's calculations, then compare and restore saved flags.
                List<Preference> saved_flags = new ArrayList<Preference>();
                for (ListItem<Preference> pref = inst.preferences_generated.first; pref != null; pref = pref.next)
                {
                    saved_flags.add(pref.item.o_supported ? pref.item : null);
                }
                // Note: I just used add() above, so the list isn't backwards in Java
                // saved_flags = destructively_reverse_list (saved_flags);
                osupport.dougs_calculate_support_for_instantiation_preferences(inst);
                boolean difference_found = false;
                int savedFlagsIndex = 0;
                for (ListItem<Preference> it = inst.preferences_generated.first; it != null; it = it.next)
                {
                    final Preference pref = it.item;
                    final Preference saved = saved_flags.get(savedFlagsIndex++);
                    boolean b = (saved != null ? true : false);
                    if (pref.o_supported != b)
                        difference_found = true;
                    pref.o_supported = b;
                }
                if (difference_found)
                {
                    context.getPrinter().warn("\n*** O-support difference found in production %s", inst.prod.getName());
                }
            }
        }
        else
        {
            // do calc's Doug's way
            if ((inst.prod.declared_support != ProductionSupport.DECLARED_O_SUPPORT)
                    && (inst.prod.declared_support != ProductionSupport.DECLARED_I_SUPPORT))
            {
                osupport.dougs_calculate_support_for_instantiation_preferences(inst);
            }
        }
    }
    
    /**
     * This builds the instantiation for a new match, and adds it to
     * newly_created_instantiations. It also calls chunk_instantiation() to do
     * any necessary chunk or justification building.
     * 
     * <p>recmem.cpp:548:create_instantiation
     * 
     * @param prod
     * @param tok
     * @param w
     * @param top_goal
     */
    private void create_instantiation(Production prod, Token tok, WmeImpl w, IdentifierImpl top_goal)
    {
        final Trace trace = context.getTrace();
        
        // RPM workaround for bug #139: don't fire justifications
    	// code moved to do_preference_phase
    	assert prod.getType() != ProductionType.JUSTIFICATION;

        Instantiation inst = new Instantiation(prod, tok, w);
        inst.inProdList.insertAtHead(this.newly_created_instantiations);

        if (context.operand2_mode)
        {
            trace.print(Category.VERBOSE, "\n in create_instantiation: %s", inst.prod.getName());
        }

        this.production_being_fired = inst.prod;
        prod.firing_count++; // TODO move into Instantiation constructor
        this.production_firing_count++;

        // build the instantiated conditions, and bind LHS variables
        ConditionsAndNots cans = this.rete.p_node_to_conditions_and_nots(prod.getReteNode(), tok, w, false);
        inst.top_of_instantiated_conditions = cans.top;
        inst.bottom_of_instantiated_conditions = cans.bottom;
        inst.nots = cans.nots;

        // record the level of each of the wmes that was positively tested
        for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            final PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                pc.bt.level = pc.bt.wme_.id.level;
                pc.bt.trace = pc.bt.wme_.preference;
            }
        }

        boolean trace_it = trace.isEnabled(inst.prod.getType().getTraceCategory());
        if(trace_it)
        {
            trace.startNewLine().print("Firing %s", inst);
        }
        
        // initialize rhs_variable_bindings array with names of variables
        // (if there are any stored on the production -- for chunks there won't be any)
        int index = 0;
        for (Variable c : prod.rhs_unbound_variables)
        {
            this.rete.setRhsVariableBinding(index, c);
            index++;
        }
        this.firer_highest_rhs_unboundvar_index = index - 1;

        // Before executing the RHS actions, tell the user that the
        // phases has changed to output by printing the arrow
        if(trace_it && trace.isEnabled(Category.FIRINGS_PREFERENCES))
        {
            trace.print(" -->\n");
        }

        // execute the RHS actions, collect the results
        assert inst.preferences_generated.isEmpty();
        boolean need_to_do_support_calculations = false;
        for (Action a = prod.action_list; a != null; a = a.next)
        {
            Preference pref = null;
            if (prod.getType() != ProductionType.TEMPLATE)
            {
                pref = execute_action(a, tok, w);
            }
            else
            {
                pref = null;
                /* SymbolImpl *result = */context.rl.rl_build_template_instantiation(inst, tok, w);
            }

            /*
             * SoarTech changed from an IF stmt to a WHILE loop to support
             * GlobalDeepCpy
             */
            while (pref != null)
            {
                pref.setInstantiation(inst);
                
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
                    else
                    {
                        if (a.support == ActionSupport.O_SUPPORT)
                            pref.o_supported = true;
                        else if (a.support == ActionSupport.I_SUPPORT)
                            pref.o_supported = false;
                        else
                        {
                            need_to_do_support_calculations = true;
                            trace.print(Category.VERBOSE, "\n\nin create_instantiation(): need_to_do_support_calculations == TRUE!!!\n\n");
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
                // pref = make_preference(thisAgent, a->preference_type, tempwme->id, tempwme->attr, tempwme->value, 0);
                // glbDeepCopyWMEs = tempwme->next;
                // deallocate_wme(thisAgent, tempwme);
                // } else {
                //pref = null;
                // }
                pref = !rhsFunctionPreferences.isEmpty() ? rhsFunctionPreferences.pop() : null;
            }
        }

        // reset rhs_variable_bindings array to all zeros
        index = 0;
        for (; index <= firer_highest_rhs_unboundvar_index; ++index)
        {
            this.rete.setRhsVariableBinding(index, null);
        }

        // fill in lots of other stuff
        fill_in_new_instantiation_stuff(inst, need_to_do_support_calculations, top_goal);

        // print trace info: printing preferences 
        // Note: can't move this up, since fill_in_new_instantiation_stuff gives
        // the o-support info for the preferences we're about to print
        if (trace_it && trace.isEnabled(Category.FIRINGS_PREFERENCES))
        {
            for (Preference pref : inst.preferences_generated)
            {
                trace.print(" %s", pref);
            }
        }
        
        build_prohibits_list(inst);

        this.production_being_fired = null;

        // build chunks/justifications if necessary
        this.chunker.chunk_instantiation(inst, this.chunker.isLearningOn());

        // TODO callback FIRING_CALLBACK
        //   if (!thisAgent->system_halted) {
        //      soar_invoke_callbacks(thisAgent, FIRING_CALLBACK, (soar_call_data) inst);
        //   }
    }
    
    /**
     * Returns true if the function create_instantiation should run for this production.
     * Used to delay firing of matches in the inner preference loop.
     * 
     * @param prod
     * @param tok
     * @param w
     * @return false to abort firing
     */
    private boolean shouldCreateInstantiation(Production prod, Token tok, WmeImpl w)
    {
        if (context.decider.active_level == context.decider.highest_active_level)
        {
        	return true;
        }
        
        if (prod.getType() == ProductionType.TEMPLATE)
        {
        	return true;
        }

        // Scan RHS identifiers for their levels, don't fire those at or higher than the change level
        for (Action a = prod.action_list; a != null; a = a.next)
        {
        	// These next three calls get the identifier which has the level,
        	// skipping anything that isn't an identifier.
        	MakeAction ma = a.asMakeAction();
            if (ma == null)
            {
            	continue;
            }
            
            // Skip unbound variables
            if (ma.id.asUnboundVariable() != null)
            {
            	continue;
            }
            
            // Get the symbol or determine that it is a function call
            SymbolImpl idSym = null;
            RhsSymbolValue rsv = ma.id.asSymbolValue();
            if (rsv != null)
            {
            	idSym = rsv.getSym();
            } 
            else
            {
                ReteLocation rl = ma.id.asReteLocation();
                if (rl != null)
                {
                	idSym = rl.lookupSymbol(tok, w);
                }
                else
                {
                	// It's a function call, skip it.
                	continue;
                }
            }
            assert idSym != null;

            IdentifierImpl id = idSym.asIdentifier();
            if (id == null)
            {
            	continue;
            }
            
            if (id.level <= context.decider.change_level)
            {
            	context.getTrace().print(Category.WATERFALL, "*** Waterfall: aborting firing because (%s * *)" +
            			" level %d is on or higher (lower int) than change level %d\n", 
            			id, id.level, context.decider.change_level);
            	return false;
            }
        }

        return true;
    }
    
    /**
     * This deallocates the given instantiation. This should only be invoked via
     * the possibly_deallocate_instantiation() macro.
     * 
     * <p>recmem.cpp:757:deallocate_instantiation
     * 
     * @param inst
     */
    private void deallocate_instantiation(Instantiation inst)
    {
        int level = inst.match_goal_level;

        // #ifdef DEBUG_INSTANTIATIONS
        // if (inst->prod)
        // print_with_symbols (thisAgent, "\nDeallocate instantiation of %y",inst->prod->name);
        // #endif

        for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next)
        {
            final PositiveCondition pc = cond.asPositiveCondition();
            if (pc != null)
            {
                if (pc.bt.hasProhibits())
                {
                    for (Preference pref : pc.bt)
                    {
                        if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
                        {
                            pref.preference_remove_ref(this);
                        }
                        else
                        {
                            if (level > SoarConstants.TOP_GOAL_LEVEL)
                                pref.preference_remove_ref(this);
                        }
                    }
                    pc.bt.clearProhibits();
                }

                if (SoarConstants.DO_TOP_LEVEL_REF_CTS)
                {
                    // (removed in jsoar) pc.bt.wme_.wme_remove_ref(context.workingMemory);
                    if (pc.bt.trace != null)
                        pc.bt.trace.preference_remove_ref(this);
                }
                else
                {
                    if (level > SoarConstants.TOP_GOAL_LEVEL)
                    {
                        // (removed in jsoar) pc.bt.wme_.wme_remove_ref(context.workingMemory);
                        if (pc.bt.trace != null)
                            pc.bt.trace.preference_remove_ref(this);
                    }
                }
            }
        }

        inst.top_of_instantiated_conditions = null;//  deallocate_condition_list (thisAgent, inst->top_of_instantiated_conditions);
        inst.nots = null; //deallocate_list_of_nots (thisAgent, inst->nots);
        if (inst.prod != null) 
        {
            inst.prod.production_remove_ref();
        }
        // TODO: Instantiation is deallocated here. Can we help GC?
    }
    
    /**
     * <p>recmem.h:65:possibly_deallocate_instantiation
     * 
     * @param inst
     */
    void possibly_deallocate_instantiation(Instantiation inst)
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
    private void retract_instantiation(Instantiation inst)
    {
        // TODO callback RETRACTION_CALLBACK
        // soar_invoke_callbacks(thisAgent, RETRACTION_CALLBACK, (soar_call_data) inst);

        boolean retracted_a_preference = false;

        final Trace trace = context.getTrace();
        final boolean trace_it = trace.isEnabled(inst.prod.getType().getTraceCategory());

        // retract any preferences that are in TM and aren't o-supported
        ListItem<Preference> prefItem = inst.preferences_generated.first;

        while (prefItem != null)
        {
            final ListItem<Preference> nextItem = prefItem.next;
            final Preference pref = prefItem.item;
            if (pref.isInTempMemory() && !pref.o_supported)
            {
                if (trace_it) {
                    if (!retracted_a_preference) 
                    {
                        trace.startNewLine().print(inst.prod.getType().getTraceCategory(), "Retracting %s", inst);
                        trace.print(Category.FIRINGS_PREFERENCES, " -->\n");
                    }
                    trace.print(Category.FIRINGS_PREFERENCES, " %s", pref);
                }
                remove_preference_from_tm(pref);
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
        if (inst.prod.getType() == ProductionType.JUSTIFICATION && inst.prod.getReferenceCount() > 1)
            context.getProductions().exciseProduction(inst.prod, false);

        // mark as no longer in MS, and possibly deallocate
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
     * <p>Note that some instantiations on newly_created_instantiations are not in
     * the match set--for the initial instantiations of chunks/justifications,
     * if they don't match WM, we have to assert the o-supported preferences and
     * throw away the rest.
     * 
     * <p>recmem.cpp:891:assert_new_preferences
     */
    private void assert_new_preferences()
    {
        final Trace trace = context.getTrace();
        // Note: In CSoar, this list is just build up using the next link in the
        // Preference object. When I tried to do that, I was getting some occasional
        // weird behavior. So, since this list is really supposed to be independent
        // for this function anyway, why not just use a normal list? Yay.
        final LinkedList<Preference> o_rejects = new LinkedList<Preference>();

        if (context.operand2_mode)
        {
            trace.print(Category.VERBOSE, "\n in assert_new_preferences:");
        }

        if (SoarConstants.O_REJECTS_FIRST)
        {
            // Do an initial loop to process o-rejects, then re-loop to process
            // normal preferences.
            ListItem<Instantiation> inst, next_inst;
            for (inst = this.newly_created_instantiations.first; inst != null; inst = next_inst)
            {
                next_inst = inst.next;

                ListItem<Preference> pref, next_pref;
                for (pref = inst.item.preferences_generated.first; pref != null; pref = next_pref)
                {
                    next_pref = pref.next;
                    if ((pref.item.type == PreferenceType.REJECT) && (pref.item.o_supported))
                    {
                        // o-reject: just put it in the buffer for later
                        o_rejects.push(pref.item);
                    }
                }
            }

            if (!o_rejects.isEmpty())
                process_o_rejects_and_deallocate_them(o_rejects);
            
            // Note: In CSoar there is some random code commented out at this point. Is it important? Who knows?
        }

        ListItem<Instantiation> inst, next_inst;
        for (inst = this.newly_created_instantiations.first; inst != null; inst = next_inst)
        {
            next_inst = inst.next;
            if (inst.item.in_ms)
            {
                inst.insertAtHead(inst.item.prod.instantiations);
            }

             if (context.operand2_mode)
             {
                 trace.print(Category.VERBOSE, "\n asserting instantiation: %s\n", inst.item.prod.getName());
             }

            ListItem<Preference> pref, next_pref;
            for (pref = inst.item.preferences_generated.first; pref != null; pref = next_pref)
            {
                // TODO all the pref.items in here are pretty ugly
                next_pref = pref.next;
                if ((pref.item.type == PreferenceType.REJECT) && (pref.item.o_supported))
                {
                    if (!SoarConstants.O_REJECTS_FIRST)
                    {
                        // o-reject: just put it in the buffer for later
                        o_rejects.push(pref.item);
                    }
                    // No knowledge retrieval necessary in Operand2

                }
                else if (inst.item.in_ms || pref.item.o_supported)
                {
                    // normal case
                    add_preference_to_tm(pref.item);

                    // No knowledge retrieval necessary in Operand2
                }
                else
                {
                    // inst. is refracted chunk, and pref. is not o-supported: remove the preference

                    // first splice it out of the clones list--otherwise we 
                    // might accidentally deallocate some clone that happens to
                    // have refcount==0 just because it hasn't been asserted yet
                    if (pref.item.next_clone != null)
                        pref.item.next_clone.prev_clone = pref.item.prev_clone;
                    if (pref.item.prev_clone != null)
                        pref.item.prev_clone.next_clone = pref.item.next_clone;
                    pref.item.next_clone = pref.item.prev_clone = null;

                    // now add then remove ref--this should result in deallocation
                    pref.item.preference_add_ref();
                    pref.item.preference_remove_ref(this);
                }
            }
        }

        if (!SoarConstants.O_REJECTS_FIRST)
        {
            if (!o_rejects.isEmpty())
                process_o_rejects_and_deallocate_them(o_rejects);
        }
    }
    
    /**
     * Process_o_rejects_and_deallocate_them() handles the processing of
     * o-supported reject preferences. This routine is called from the firer and
     * passed a list of all the o-rejects generated in the current preference
     * phases (the list is linked via the "next" fields on the preference
     * structures). This routine removes all preferences for matching values
     * from TM, and deallocates the o-reject preferences when done.
     * 
     * prefmem.cpp:330:process_o_rejects_and_deallocate_them
     * 
     * @param o_rejects
     */
    private void process_o_rejects_and_deallocate_them(List<Preference> o_rejects)
    {
        for (Preference pref : o_rejects)
        {
            // prevents it from being deallocated if it's a clone of some other 
            // pref we're about to remove
            pref.preference_add_ref(); 
            // #ifdef DEBUG_PREFS
            // print (thisAgent, "\nO-reject posted at 0x%8x: ",(unsigned
            // long)pref);
            // print_preference (thisAgent, pref);
            // #endif
        }

        for(Preference pref : o_rejects)
        {
            Slot s = Slot.find_slot(pref.id, pref.attr);
            if (s != null)
            {
                // remove all pref's in the slot that have the same value
                Preference p = s.getAllPreferences();
                while (p != null)
                {
                    final Preference next_p = p.nextOfSlot;
                    if (p.value == pref.value)
                    {
                        remove_preference_from_tm(p);
                    }
                    p = next_p;
                }
            }
            pref.preference_remove_ref(this);
        }
    }
    
    /**
     * Add_preference_to_tm() adds a given preference to preference memory (and
     * hence temporary memory). 
     * 
     * prefmem.cpp:203:add_preference_to_tm
     * 
     * @param pref
     */
    private void add_preference_to_tm(Preference pref)
    {
        // #ifdef DEBUG_PREFS
        // print (thisAgent, "\nAdd preference at 0x%8x: ",(unsigned long)pref);
        // print_preference (thisAgent, pref);
        // #endif

        // JC: This will retrieve the slot for pref->id if it already exists
        Slot s = Slot.make_slot(pref.id, pref.attr, context.predefinedSyms.operator_symbol);
        s.addPreference(pref);

        // other miscellaneous stuff
        pref.preference_add_ref();

        context.tempMemory.mark_slot_as_changed(s);

        // update identifier levels
        IdentifierImpl valueId = pref.value.asIdentifier();
        if (valueId != null)
        {
            context.decider.post_link_addition (pref.id, valueId);
        }

        if (pref.type.isBinary())
        {
            IdentifierImpl refId = pref.referent.asIdentifier();
            if (refId != null)
            {
                context.decider.post_link_addition (pref.id, refId);
            }
        }

        // if acceptable/require pref for context slot, we may need to add a wme
        // later
        if (s.isa_context_slot
                && (pref.type == PreferenceType.ACCEPTABLE || 
                    pref.type == PreferenceType.REQUIRE))
        {
            context.decider.mark_context_slot_as_acceptable_preference_changed (s);
        }
    }

    /**
     * removes a given preference from PM and TM.
     * 
     * prefmem.cpp:282:remove_preference_from_tm
     * 
     * @param pref
     */
    public void remove_preference_from_tm(Preference pref)
    {
        Slot s = pref.slot;

        // #ifdef DEBUG_PREFS
        // print (thisAgent, "\nRemove preference at 0x%8x: ",(unsigned
        // long)pref);
        // print_preference (thisAgent, pref);
        // #endif

        // remove preference from the list for the slot
        s.removePreference(pref);

        // other miscellaneous stuff

        context.tempMemory.mark_slot_as_changed(s);

        /// if acceptable/require pref for context slot, we may need to remove a wme later
        if ((s.isa_context_slot)
                && ((pref.type == PreferenceType.ACCEPTABLE) || (pref.type == PreferenceType.REQUIRE)))
        {
            context.decider.mark_context_slot_as_acceptable_preference_changed(s);
        }

        // update identifier levels
        IdentifierImpl valueId = pref.value.asIdentifier();
        if (valueId != null)
        {
            context.decider.post_link_removal (pref.id, valueId);
        }
        if (pref.type.isBinary())
        {
            IdentifierImpl refId = pref.referent.asIdentifier();
            if (refId != null)
            {
                context.decider.post_link_removal (pref.id, refId);
            }
        }

        // deallocate it and clones if possible
        pref.preference_remove_ref(this);
    }

    /**
     * This routine is called from the top level to run the preference phases.
     * 
     * <p>recmem.cpp:1035:do_preference_phase
     * 
     * @param top_goal
     * @param o_support_calculation_type
     */
    public void do_preference_phase(IdentifierImpl top_goal, int o_support_calculation_type)
    {
        /*
         * AGR 617/634: These are 2 bug reports that report the same problem,
         * namely that when 2 chunk firings happen in succession, there is an
         * extra newline printed out. The simple fix is to monitor
         * get_printer_output_column and see if it's at the beginning of a line
         * or not when we're ready to print a newline. 94.11.14
         */

        final Trace trace = context.getTrace();
        if (trace.isEnabled(Category.PHASES))
        {
            if (context.operand2_mode)
            {
                if (this.decisionCycle.current_phase == Phase.APPLY)
                { /* it's always IE for PROPOSE */
                    switch (FIRING_TYPE)
                    {
                    case PE_PRODS:
                        trace.print("--- Firing Productions (PE) For State At Depth %d ---\n",
                                context.decider.active_level);
                        break;
                    case IE_PRODS:
                        trace.print("--- Firing Productions (IE) For State At Depth %d ---\n",
                                context.decider.active_level);
                        break;
                    }
                }
            }
            else
                // the XML for this is generated in this function
                trace.print("\n--- Preference Phase ---\n");
        }
        
        // Save previous active level for usage on next elaboration cycle.
        context.decider.highest_active_level = context.decider.active_level;
        context.decider.highest_active_goal = context.decider.active_goal;

        context.decider.change_level = context.decider.highest_active_level;
        context.decider.next_change_level = context.decider.highest_active_level;
        
        // FIXME: should not do this inner elaboration loop for soar 7 mode.
        for (;;)
        {
        	// Inner elaboration loop
        	context.decider.change_level = context.decider.next_change_level;
        	
	        if (trace.isEnabled(Category.WATERFALL))
	        {
	        	trace.print("--- Inner Elaboration Phase, active level: %d",
	        			context.decider.active_level);
	        	if (context.decider.active_goal != null)
	        	{
		        	trace.print(" (%s)", context.decider.active_goal);
	        	}
	        	trace.print(" ---\n");
	        }
	        
	        this.newly_created_instantiations.clear();
	    	
	        SoarReteAssertion assertion = null;
	        boolean assertionsExist = false;
	        while ((assertion = context.soarReteListener.postpone_assertion()) != null)
	        {
	        	assertionsExist = true;
	        	
	            if(this.chunker.isMaxChunksReached()) 
	            {
	            	context.soarReteListener.consume_last_postponed_assertion();
	                this.decisionCycle.halt("Max chunks reached");
	                return;
	            }
	            
	            if (assertion.production.getType() == ProductionType.JUSTIFICATION)
	            {
	            	context.soarReteListener.consume_last_postponed_assertion();

	            	// don't fire justifications
	            	continue;
	            }
	            
	            if (shouldCreateInstantiation(assertion.production, assertion.token, assertion.wme))
	            {
	            	context.soarReteListener.consume_last_postponed_assertion();
	            	create_instantiation(assertion.production, assertion.token, assertion.wme, top_goal);
	            }
	        }
	        
	        // New waterfall model: something fired or is pending to fire at this level, 
	        // so this active level becomes the next change level.
	        if (assertionsExist)
	        {
	            if (context.decider.active_level > context.decider.next_change_level) 
	            {
	            	context.decider.next_change_level = context.decider.active_level;
	            }
	        }
	        
	        // New waterfall model: push unfired matches back on to the assertion lists
	        context.soarReteListener.restore_postponed_assertions();
	        
	        assert_new_preferences();
	
	        // update accounting
	        this.decisionCycle.inner_e_cycle_count++;
	        
	        if (context.decider.active_goal == null)
	        {
	        	trace.print(Category.WATERFALL, " inner preference loop doesn't have active goal.\n");
	        	break;
	        }
	        
	        if (context.decider.active_goal.lower_goal == null)
	        {
	        	trace.print(Category.WATERFALL, " inner preference loop at bottom goal.\n");
	        	break;
	        }
	        
	        try
	        {
	            if (this.decisionCycle.current_phase == Phase.APPLY)
	            {
	    	        context.decider.active_goal = context.consistency.highest_active_goal_apply(context.decider.active_goal.lower_goal);
	            }
	            else if (this.decisionCycle.current_phase == Phase.PROPOSE)
	            {
	            	// PROPOSE
	            	context.decider.active_goal = context.consistency.highest_active_goal_propose(context.decider.active_goal.lower_goal);
	            } 
	        } 
	        catch (IllegalStateException e)
	        {
	        	// FIXME: highest_active_goal_x functions are intended to be used only when it is
	        	// guaranteed that the agent is not at quiescence.
	        	context.decider.active_goal = null;
	        	trace.print(Category.WATERFALL, " inner preference loop finished but not at quiescence.\n");
	        	break;
	        }
            
	        assert context.decider.active_goal != null;
        	context.decider.active_level = context.decider.active_goal.level;
        } // inner elaboration loop/cycle end

        // Restore previous active level.
        context.decider.active_level = context.decider.highest_active_level;
        context.decider.active_goal = context.decider.highest_active_goal;
        
        Instantiation inst = null;
        while ((inst = context.soarReteListener.get_next_retraction()) != null)
        {
            retract_instantiation(inst);
        }

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
        
        if(!context.operand2_mode)
        {
            Phase.PREFERENCE.trace(trace, false);
        }
    }

    private class RhsFunctionContextImpl implements RhsFunctionContext
    {
        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionContext#getSymbols()
         */
        @Override
        public SymbolFactory getSymbols()
        {
            return context.syms;
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionContext#addWme(org.jsoar.kernel.symbols.Identifier, org.jsoar.kernel.symbols.Symbol, org.jsoar.kernel.symbols.Symbol)
         */
        @Override
        public void addWme(Identifier id, Symbol attr, Symbol value)
        {
            Arguments.checkNotNull(id, "id");
            Arguments.checkNotNull(attr, "attr");
            Arguments.checkNotNull(value, "value");
            
            Preference p = new Preference(rhsFunctionPreferenceType, 
                                          (IdentifierImpl) id, (SymbolImpl) attr, (SymbolImpl) value, 
                                          null);
            rhsFunctionPreferences.add(p);
        }
        
    }
}
