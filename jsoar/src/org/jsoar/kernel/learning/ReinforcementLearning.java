/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.learning;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.Token;

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

    
}
