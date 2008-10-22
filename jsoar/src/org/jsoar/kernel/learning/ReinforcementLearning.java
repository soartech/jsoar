/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.learning;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.symbols.IdentifierImpl;

/**
 * @author ray
 */
public class ReinforcementLearning
{
    public static final double RL_RETURN_LONG = 0.1;
    public static final String RL_RETURN_STRING = "";
    
    public static final int RL_LEARNING_ON = 1;
    public static final int RL_LEARNING_OFF = 2;
    
    public static final int RL_LEARNING_SARSA = 1;
    public static final int RL_LEARNING_Q = 2;
    
    public static final int RL_TE_ON = 1;
    public static final int RL_TE_OFF = 2;
    
    // names of params
    public static final int RL_PARAM_LEARNING                  = 0;
    public static final int RL_PARAM_DISCOUNT_RATE             = 1;
    public static final int RL_PARAM_LEARNING_RATE             = 2;
    public static final int RL_PARAM_LEARNING_POLICY           = 3;
    public static final int RL_PARAM_ET_DECAY_RATE             = 4;
    public static final int RL_PARAM_ET_TOLERANCE              = 5;
    public static final int RL_PARAM_TEMPORAL_EXTENSION        = 6;
    public static final int RL_PARAMS                          = 7; // must be 1+ last rl param
    
    // names of stats
    public static final int RL_STAT_UPDATE_ERROR               = 0;
    public static final int RL_STAT_TOTAL_REWARD               = 1;
    public static final int RL_STAT_GLOBAL_REWARD              = 2;
    public static final int RL_STATS                           = 3; // must be 1+ last rl stat
    
    // more specific forms of no change impasse types
    // made negative to never conflict with impasse constants
    public static final int STATE_NO_CHANGE_IMPASSE_TYPE = -1;
    public static final int OP_NO_CHANGE_IMPASSE_TYPE = -2;

//////////////////////////////////////////////////////////
// RL Types
//////////////////////////////////////////////////////////
    enum rl_param_type { rl_param_string, rl_param_number, rl_param_invalid };    
    
   
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
    public static void rl_build_template_instantiation(Instantiation inst, Token tok, WmeImpl w)
    {
        // TODO Implement rl_build_template_instantiation
        throw new UnsupportedOperationException("rl_build_template_instantiation is not implemented");
    }

    /**
     * @param goal
     * @param value
     */
    public void rl_store_data(IdentifierImpl goal, Preference value)
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
    public void rl_tabulate_reward_value_for_goal(IdentifierImpl g)
    {
        // TODO Implement rl_tabulate_reward_value_for_goal
        throw new UnsupportedOperationException("rl_tabulate_reward_value_for_goal is not implemented");
    }

    /**
     * @param i
     * @param g
     */
    public void rl_perform_update(int i, IdentifierImpl g)
    {
        // TODO Implement rl_perform_update
        throw new UnsupportedOperationException("rl_perform_update is not implemented");
    }

    /**
     * Function introduced while trying to tease apart production construction
     * 
     * <p>production.cpp:1507:make_production
     * 
     * @param p
     */
    public void addProduction(Production p)
    {
        // Soar-RL stuff
        // TODO p->rl_update_count = 0;
        // TODO p->rl_rule = false;
        if ( ( p.getType() != ProductionType.JUSTIFICATION_PRODUCTION_TYPE ) && ( p.getType() != ProductionType.TEMPLATE_PRODUCTION_TYPE ) ) 
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

    /**
     * Function introduced while teasing apary excise functionality
     * 
     * <p>production.cpp:1595:excise_production
     * 
     * @param prod
     */
    public void exciseProduction(Production prod)
    {
        // Remove RL-related pointers to this production (unnecessary if rule never fired).
        //if ( prod->rl_rule && prod->firing_count ) 
        //    rl_remove_refs_for_prod( thisAgent, prod ); 
    }
    
}
