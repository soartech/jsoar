/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 26, 2010
 */
package org.jsoar.kernel.modules;

import java.util.List;
import java.util.Set;

import org.jsoar.kernel.Decider;
import org.jsoar.kernel.SoarConstants;
import org.jsoar.kernel.lhs.BackTraceInfo;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WmeImpl.SymbolTriple;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;

/**
 * 
 * <p>soar_module.h, soar_module.cpp
 * 
 * @author ray
 */
public class SoarModule
{
    private WorkingMemory wm;
    private Decider decider;
    
    public SoarModule()
    {
        
    }
    
    public void initialize(Adaptable context)
    {
        wm = Adaptables.require(getClass(), context, WorkingMemory.class);
        decider = Adaptables.require(getClass(), context, Decider.class);
    }
    
    /**
     * <p>soar_module.cpp:add_module_wme
     * 
     * @param id
     * @param attr
     * @param value
     * @return the new wme
     */
    public static WmeImpl add_module_wme(WorkingMemory wm, IdentifierImpl id, SymbolImpl attr, SymbolImpl value )
    {
        final Slot my_slot = Slot.make_slot(id, attr, null);
        final WmeImpl w = wm.make_wme(id, attr, value, false); 
        my_slot.addWme(w);
        wm.add_wme_to_wm(w);

        return w;
    }
    
    public WmeImpl add_module_wme(IdentifierImpl id, SymbolImpl attr, SymbolImpl value)
    {
        return add_module_wme(wm,id,attr,value);
    }

    /**
     * <p>soar_module.cpp:remove_module_wme
     * 
     * @param w
     */
    public void remove_module_wme(WmeImpl w )
    {
        final Slot my_slot = Slot.find_slot((IdentifierImpl) w.getIdentifier(), (SymbolImpl) w.getAttribute() );

        if ( my_slot != null )
        {
            my_slot.removeWme(w);

            if ( w.gds != null ) 
            {
                if ( w.gds.getGoal() != null )
                {      
                    decider.gds_invalid_so_remove_goal(w, "While removing a module WME");
                    
                    /* NOTE: the call to remove_wme_from_wm will take care of checking if GDS should be removed */
                }
            }

            wm.remove_wme_from_wm(w);
        }
    }

    /**
     * <p>soar_module:make_fake_preference
     * 
     * @param state
     * @param id
     * @param attr
     * @param value
     * @param conditions
     * @return the new preference
     */
    public static Preference make_fake_preference(IdentifierImpl state, IdentifierImpl id, SymbolImpl attr, SymbolImpl value, Set<WmeImpl> conditions )
    {
        // make fake preference
        final Preference pref = new Preference(PreferenceType.ACCEPTABLE, id, attr, value, null);
        pref.o_supported = true;

        // make fake instantiation
        Instantiation inst = new Instantiation(null, null, null);
        pref.setInstantiation(inst);
        inst.match_goal = state;
        inst.match_goal_level = state.level;
        inst.reliable = true;
        inst.backtrace_number = 0;
        inst.in_ms = false;
        inst.GDS_evaluated_already = false;
        
        Condition prev_cond = null;    
        {
            for(WmeImpl p : conditions)
            {
                // construct the condition
                final PositiveCondition cond = new PositiveCondition();
                cond.prev = prev_cond;
                cond.next = null;
                if ( prev_cond != null )
                {
                    prev_cond.next = cond;
                }
                else
                {
                    inst.top_of_instantiated_conditions = cond;
                    inst.bottom_of_instantiated_conditions = cond;
                    inst.nots = null;
                }
                cond.id_test = SymbolImpl.makeEqualityTest(p.id);
                cond.attr_test = SymbolImpl.makeEqualityTest(p.attr);
                cond.value_test = SymbolImpl.makeEqualityTest(p.value);
                cond.test_for_acceptable_preference = p.acceptable;
                cond.bt().wme_ = p;

                // Nothing to do here in JSoar
                // #ifndef DO_TOP_LEVEL_REF_CTS
                // if ( inst->match_goal_level > TOP_GOAL_LEVEL )
                // #endif
                // {
                //    wme_add_ref( (*p) );
                // }           
                
                cond.bt().level = p.id.level;
                cond.bt().trace = p.preference;
                
                if ( cond.bt().trace != null )
                {
                    if(!SoarConstants.DO_TOP_LEVEL_REF_CTS || inst.match_goal_level > SoarConstants.TOP_GOAL_LEVEL)
                    {
                        cond.bt().trace.preference_add_ref();
                    }
                }               

                // CDPS defaults to null
                // cond.bt().CDPS = null;

                prev_cond = cond;
            }
        }

        return pref;
    }
    
    /**
     * soar_module:81:
     * instantiation* make_fake_instantiation( agent* my_agent, Symbol* state, wme_set* conditions, symbol_triple_list* actions )
     * 
     * @param state
     * @param conditions
     * @param actions
     * @return
     */
    public static Instantiation make_fake_instantiation(SymbolImpl state, Set<WmeImpl> conditions, List<SymbolTriple> actions){
     // make fake instantiation
        Instantiation inst = new Instantiation(null, null, null);
        //allocate_with_pool( my_agent, &( my_agent->instantiation_pool ), &inst );
        inst.nextInProdList = inst.prevInProdList = null;
        inst.match_goal = state.asIdentifier();
        inst.match_goal_level = state.asIdentifier().level;
        //This is missing from the instantiation classes, and it looks liek all the
        //ported code that was using has written in out.  --ACN
        //inst.reliable = true;
        inst.backtrace_number = 0;
        inst.in_ms = false;
        inst.GDS_evaluated_already = false;

        // create preferences
        inst.preferences_generated = null;
        {
            Preference pref;

            for ( SymbolTriple a_it: actions)
            {
                pref = new Preference( 
                        PreferenceType.ACCEPTABLE, 
                        a_it.id.asIdentifier(), 
                        a_it.attr, 
                        a_it.value, 
                        null/*NIL*/ 
                    );
                pref.o_supported = true;
                //symbol_add_ref( pref->id );
                //symbol_add_ref( pref->attr );
                //symbol_add_ref( pref->value );

                pref.inst = inst;
                pref.inst_next = pref.inst_prev = null;
                
                //insert_at_head_of_dll( inst->preferences_generated, pref, inst_next, inst_prev );
                pref.inst_next = inst.preferences_generated;
                pref.inst_prev = null;//NIL
                if(inst.preferences_generated != null)
                {
                    inst.preferences_generated.inst_prev = pref;
                }
                inst.preferences_generated = pref;
            }
        }

        // create conditions
        {
            PositiveCondition cond = null;
            Condition prev_cond = null;

            for ( WmeImpl c_it: conditions )
            {
                // construct the condition
                //allocate_with_pool( my_agent, &( my_agent->condition_pool ), &cond );
                cond = new PositiveCondition();
                //cond->type = POSITIVE_CONDITION;
                cond.prev = prev_cond;
                cond.next = null;
                if ( prev_cond != null )
                {
                    prev_cond.next = cond;
                }
                else
                {
                    inst.top_of_instantiated_conditions = cond;
                    inst.bottom_of_instantiated_conditions = cond;
                    inst.nots = null;
                }
                cond.id_test = c_it.id; //make_equality_test
                cond.attr_test = c_it.attr; //make_equality_test
                cond.value_test = c_it.value; //make_equality_test
                cond.test_for_acceptable_preference = c_it.acceptable;
                
                final BackTraceInfo conditionBacktraceInfo = cond.bt();
                conditionBacktraceInfo.wme_ = c_it;

                /*
                #ifndef DO_TOP_LEVEL_REF_CTS
                if ( inst->match_goal_level > TOP_GOAL_LEVEL )
                #endif
                {
                    wme_add_ref( (*c_it) );
                }           
                 */
                
                conditionBacktraceInfo.level = c_it.id.level;
                conditionBacktraceInfo.trace = c_it.preference;
                
                /*
                if ( cond->bt.trace )
                {
                    #ifndef DO_TOP_LEVEL_REF_CTS
                    if ( inst->match_goal_level > TOP_GOAL_LEVEL )
                    #endif
                    {
                        preference_add_ref( cond->bt.trace );
                    }
                }               
                 */

                //TODO: Comment this in after the merge
                //conditionBacktraceInfo.CDPS = NULL;

                prev_cond = cond;
            }
        }

        return inst;
    }
}
