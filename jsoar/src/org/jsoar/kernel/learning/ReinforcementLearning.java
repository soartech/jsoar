/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.learning;

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

    
}
