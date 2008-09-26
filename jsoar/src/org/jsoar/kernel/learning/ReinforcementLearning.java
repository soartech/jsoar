/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.learning;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.symbols.Identifier;

/**
 * @author ray
 */
public class ReinforcementLearning
{
    private boolean enabled = false;
    
    /**
     * reinforcement_learning.cpp:695:rl_enabled
     * 
     * @return
     */
    public boolean rl_enabled()
    {
        return enabled;
    }
    
    /**
     * @param inst
     * @param tok
     * @param w
     */
    public static void rl_build_template_instantiation(Instantiation inst, Token tok, Wme w)
    {
        // TODO Implement rl_build_template_instantiation
        throw new UnsupportedOperationException("rl_build_template_instantiation is not implemented");
    }

    /**
     * @param goal
     * @param value
     */
    public void rl_store_data(Identifier goal, Preference value)
    {
        // TODO Implement rl_store_data
        throw new UnsupportedOperationException("rl_store_data is not implemented");
    }

    /**
     * 
     */
    public void rl_tabulate_reward_values()
    {
        // TODO Implement rl_tabulate_reward_values
        throw new UnsupportedOperationException("rl_tabulate_reward_values is not implemented");
    }

    /**
     * @param g
     */
    public void rl_tabulate_reward_value_for_goal(Identifier g)
    {
        // TODO Implement rl_tabulate_reward_value_for_goal
        throw new UnsupportedOperationException("rl_tabulate_reward_value_for_goal is not implemented");
    }

    /**
     * @param i
     * @param g
     */
    public void rl_perform_update(int i, Identifier g)
    {
        // TODO Implement rl_perform_update
        throw new UnsupportedOperationException("rl_perform_update is not implemented");
    }

    /**
     * Function introduced while trying to tease apart production construction
     * 
     * production.cpp:1507:make_production
     * 
     * @param p
     */
    public void addProduction(Production p)
    {
        // Soar-RL stuff
        // TODO p->rl_update_count = 0;
        // TODO p->rl_rule = false;
        if ( ( p.type != ProductionType.JUSTIFICATION_PRODUCTION_TYPE ) && ( p.type != ProductionType.TEMPLATE_PRODUCTION_TYPE ) ) 
        {
            // TODO p->rl_rule = rl_valid_rule( p );  
        }
        // TODO rl_update_template_tracking( thisAgent, name->sc.name );
        
        // TODO - parser.cpp
//        if ( prod_type == ProductionType.TEMPLATE_PRODUCTION_TYPE )
//        {
//            if ( !rl_valid_template( p ) )
//            {
//                print_with_symbols( thisAgent, "Invalid Soar-RL template (%y)\n\n", name );
//                excise_production( thisAgent, p, false );
//                return null;
//            }
//        }
    }

    
}
