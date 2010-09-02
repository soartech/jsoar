/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 26, 2010
 */
package org.jsoar.kernel.modules;

import java.util.Set;

import org.jsoar.kernel.Decider;
import org.jsoar.kernel.SoarConstants;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.adaptables.AbstractAdaptable;
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
    
    public void initialize(AbstractAdaptable context)
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
     * @return
     */
    public static WmeImpl add_module_wme(WorkingMemory wm, IdentifierImpl id, SymbolImpl attr, SymbolImpl value )
    {
        final Slot my_slot = Slot.make_slot(id, attr, null);
        final WmeImpl w = wm.make_wme(id, attr, value, false); 
        my_slot.addWme(w);
        wm.add_wme_to_wm(w);

        return w;
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
     * @return
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
        inst.okay_to_variablize = true;
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

                // Prohibits defaults to null
                // cond.bt().prohibits = null;

                prev_cond = cond;
            }
        }

        return pref;
    }

}
