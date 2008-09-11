/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 10, 2008
 */
package org.jsoar.kernel.memory;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.SoarConstants;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.rhs.Action;
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
    private final Rete rete;
    private final PredefinedSymbols syms;
    private int attribute_preferences_mode;
    private boolean operand2_mode;
    
    private int firer_highest_rhs_unboundvar_index;
    
    
    /**
     * @param rete
     * @param syms
     */
    public RecognitionMemory(Rete rete, PredefinedSymbols syms)
    {
        this.rete = rete;
        this.syms = syms;
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
            Symbol sym = rete.getRhsVariableBinding(index);

            if (sym == null)
            {
                sym = syms.getSyms().make_new_identifier(new_id_letter, new_id_level);
                rete.setRhsVariableBinding(index, sym);
                return sym;
            }
            else if (sym.asVariable() != null)
            {
                Variable v = sym.asVariable();
                new_id_letter = v.getFirstLetter();
                sym = syms.getSyms().make_new_identifier(new_id_letter, new_id_level);
                rete.setRhsVariableBinding(index, sym);
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
                && (!(id.isa_goal && (attr == syms.operator_symbol))))
        {
            if ((this.attribute_preferences_mode == 2) || (this.operand2_mode == true))
            {
                // TODO Print error
                // print_with_symbols (thisAgent, "\nError: attribute preference
                // other than +/- for %y ^%y -- ignoring it.", id, attr);
                return null; // goto abort_execute_action;
            }
            else if (this.attribute_preferences_mode == 1)
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
}
