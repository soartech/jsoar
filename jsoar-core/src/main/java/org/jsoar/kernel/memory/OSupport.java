/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 8, 2008
 */
package org.jsoar.kernel.memory;

import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.Production.Support;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.markers.Marker;

/**
 * 
 * osupport.cpp
 * 
 * @author ray
 */
public class OSupport
{
    private final PredefinedSymbols syms;
    private final Printer printer;
    
    /**
     * agent.h:658:o_support_tc
     */
    private Marker o_support_tc;
    
    /**
     * agent.h:659:rhs_prefs_from_instantiation
     */
    private final ListHead<Preference> rhs_prefs_from_instantiation = ListHead.newInstance();
    
    /**
     * @param syms
     * @param printer
     */
    public OSupport(PredefinedSymbols syms, Printer printer)
    {
        this.syms = syms;
        this.printer = printer;
    }
    
    /**
     * osupport.cpp:63:add_to_os_tc_if_needed
     * 
     * @param sym
     */
    private void add_to_os_tc_if_needed(SymbolImpl sym)
    {
        IdentifierImpl id = sym.asIdentifier();
        if(id != null)
        {
            add_to_os_tc(id, false);
        }
    }
    
    /**
     * SBH 4/14/93
     * For NNPSCM, we must exclude the operator slot from the transitive closure of a state.
     * Do that by passing a boolean argument, "isa_state" to this routine.
     * If it isa_state, check for the operator slot before the recursive call.
     * 
     * <p>osupport.cpp:84:add_to_os_tc
     * 
     * @param id
     * @param isa_state
     */
    private void add_to_os_tc(IdentifierImpl id, boolean isa_state)
    {
        // if id is already in the TC, exit; else mark it as in the TC
        if(id.tc_number == o_support_tc)
        {
            return;
        }
        
        id.tc_number = o_support_tc;
        
        // scan through all preferences and wmes for all slots for this id
        for(WmeImpl w = id.getInputWmes(); w != null; w = w.next)
        {
            add_to_os_tc_if_needed(w.value);
        }
        for(Slot s = id.slots; s != null; s = s.next)
        {
            if((!isa_state) || (s.attr != syms.operator_symbol))
            {
                for(Preference pref = s.getAllPreferences(); pref != null; pref = pref.nextOfSlot)
                {
                    add_to_os_tc_if_needed(pref.value);
                    if(pref.type.isBinary())
                        add_to_os_tc_if_needed(pref.referent);
                }
                for(WmeImpl w = s.getWmes(); w != null; w = w.next)
                {
                    add_to_os_tc_if_needed(w.value);
                }
            }
        } /* end of for slots loop */
        // now scan through RHS prefs and look for any with this id
        for(ListItem<Preference> pit = rhs_prefs_from_instantiation.first; pit != null; pit = pit.next)
        {
            final Preference pref = pit.item;
            if(pref.id == id)
            {
                if((!isa_state) || (pref.attr != syms.operator_symbol))
                {
                    add_to_os_tc_if_needed(pref.value);
                    if(pref.type.isBinary())
                    {
                        add_to_os_tc_if_needed(pref.referent);
                    }
                }
            }
        }
        /*
         * We don't need to worry about goal/impasse wmes here, since o-support tc's
         * never start there and there's never a pointer to a goal or impasse from
         * something else.
         */
    }
    
    /**
     * Run-Time O-Support Calculation
     *
     * This routine calculates o-support for each preference for the given
     * instantiation, filling in pref.o_supported (true or false) on each one.
     *
     * <p>The following predicates are used for support calculations. In the
     * following, "lhs has some elt. ..." means the lhs has some id or value
     * at any nesting level.
     *
     * <pre>
     * lhs_oa_support:
     * (1) does lhs test (match_goal ^operator match_operator NO) ?
     * (2) mark TC (match_operator) using TM;
     * does lhs has some elt. in TC but != match_operator ?
     * (3) mark TC (match_state) using TM;
     * does lhs has some elt. in TC ?
     * lhs_oc_support:
     * (1) mark TC (match_state) using TM;
     * does lhs has some elt. in TC but != match_state ?
     * lhs_om_support:
     * (1) does lhs tests (match_goal ^operator) ?
     * (2) mark TC (match_state) using TM;
     * does lhs has some elt. in TC but != match_state ?
     *
     * rhs_oa_support:
     * mark TC (match_state) using TM+RHS;
     * if pref.id is in TC, give support
     * rhs_oc_support:
     * mark TC (inst.rhsoperators) using TM+RHS;
     * if pref.id is in TC, give support
     * rhs_om_support:
     * mark TC (inst.lhsoperators) using TM+RHS;
     * if pref.id is in TC, give support
     * </pre>
     * 
     * <pre>
     * BUGBUG the code does a check of whether the lhs tests the match state via
     * looking just at id and value fields of top-level positive cond's.
     * It doesn't look at the attr field, or at any negative or NCC's.
     * I'm not sure whether this is right or not. (It's a pretty
     * obscure case, though.)
     * </pre>
     * 
     * <p>osupport.cpp:267:calculate_support_for_instantiation_preferences
     * 
     * @param inst
     */
    public void calculate_support_for_instantiation_preferences(Instantiation inst)
    {
        WmeImpl w;
        Condition c;
        Action act;
        boolean o_support, op_elab;
        boolean operator_proposal;
        int pass;
        WmeImpl lowest_goal_wme;
        
        // TODO: verbose
        // if (thisAgent.soar_verbose_flag == true) {
        // printf("\n in calculate_support_for_instantiation_preferences:");
        // xml_generate_verbose(thisAgent, "in calculate_support_for_instantiation_preferences:");
        // }
        o_support = false;
        op_elab = false;
        
        if(inst.prod.getDeclaredSupport() == Support.DECLARED_O_SUPPORT)
        {
            o_support = true;
        }
        else if(inst.prod.getDeclaredSupport() == Support.DECLARED_I_SUPPORT)
        {
            o_support = false;
        }
        else if(inst.prod.getDeclaredSupport() == Support.UNDECLARED)
        {
            /*
             * check if the instantiation is proposing an operator. if it
             * is, then this instantiation is i-supported.
             */
            
            operator_proposal = false;
            for(act = inst.prod.getFirstAction(); act != null; act = act.next)
            {
                MakeAction ma = act.asMakeAction();
                if(ma != null && ma.attr.asSymbolValue() != null)
                {
                    if(syms.operator_symbol == ma.attr.asSymbolValue().sym &&
                            act.preference_type == PreferenceType.ACCEPTABLE)
                    {
                        operator_proposal = true;
                        o_support = false;
                        break;
                    }
                }
            }
            
            if(operator_proposal == false)
            {
                /*
                 * an operator wasn't being proposed, so now we need to test
                 * if the operator is being tested on the LHS.
                 * 
                 * i'll need to make two passes over the wmes that pertain
                 * to this instantiation. the first pass looks for the
                 * lowest goal identifier. the second pass looks for a wme
                 * of the form:
                 * (<lowest-goal-id> ^operator ...)
                 * 
                 * if such a wme is found, then this o-support = true; false
                 * otherwise.
                 * 
                 * this code is essentially identical to that in
                 * p_node_left_addition() in rete.c.
                 * 
                 * BUGBUG this check only looks at positive conditions. we
                 * haven't really decided what testing the absence of the
                 * operator will do. this code assumes that such a
                 * productions (instantiation) would get i-support.
                 */
                
                lowest_goal_wme = null;
                
                for(pass = 0; pass != 2; pass++)
                {
                    for(c = inst.top_of_instantiated_conditions; c != null; c = c.next)
                    {
                        PositiveCondition pc = c.asPositiveCondition();
                        if(pc != null)
                        {
                            w = pc.bt().wme_;
                            
                            if(pass == 0)
                            {
                                if(w.id.isGoal())
                                {
                                    if(lowest_goal_wme == null)
                                    {
                                        lowest_goal_wme = w;
                                    }
                                    else
                                    {
                                        if(w.id.level > lowest_goal_wme.id.level)
                                        {
                                            lowest_goal_wme = w;
                                        }
                                    }
                                }
                            }
                            else
                            {
                                if((w.attr == syms.operator_symbol) && (w.acceptable == false)
                                        && (w.id == lowest_goal_wme.id))
                                {
                                    // former o_support_calculation_type test site
                                    // iff RHS has only operator elaborations then it's IE_PROD,
                                    // otherwise PE_PROD, so look for non-op-elabs in the actions KJC 1/00
                                    for(act = inst.prod.getFirstAction(); act != null; act = act.next)
                                    {
                                        MakeAction ma = act.asMakeAction();
                                        if(ma != null)
                                        {
                                            RhsSymbolValue symVal = ma.id.asSymbolValue();
                                            ReteLocation reteLoc = ma.id.asReteLocation();
                                            if(symVal != null && symVal.sym == w.value)
                                            {
                                                op_elab = true;
                                            }
                                            else if(/* o_support_calculation_type == 4 && */ reteLoc != null
                                                    && w.value == reteLoc.lookupSymbol(inst.rete_token, w))
                                            {
                                                op_elab = true;
                                            }
                                            else
                                            {
                                                // this is not an operator elaboration
                                                o_support = true;
                                            }
                                        }
                                    }
                                }
                            }
                            
                        }
                    }
                }
            }
        }
        
        /* KJC 01/00: Warn if operator elabs mixed w/ applications */
        // former o_support_calculation_type (3 or 4) test site
        if(o_support)
        {
            if(op_elab)
            {
                // former o_support_calculation_type (4) test site
                // warn user about mixed actions
                printer.warn("\nWARNING: operator elaborations mixed with operator applications\n" +
                        "get i_support in prod %s", inst.prod.getName());
                
                o_support = false;
            }
        }
        
        // assign every preference the correct support
        for(Preference pref = inst.preferences_generated; pref != null; pref = pref.inst_next)
        {
            pref.o_supported = o_support;
        }
        
    }
    
    // RPM 5/2010: Removed lots of old, unused code (600-700 lines) intended to provide compile time o-support calculations.
    // Laird thinks it was originally intended to make the code faster, but that this is less of an issue on modern machines.
    // If some poor soul wants to try that stuff again, it's in the repository.
}
