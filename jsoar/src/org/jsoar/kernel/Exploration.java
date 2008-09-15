/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;

/**
 * exploration.cpp
 * 
 * @author ray
 */
public class Exploration
{

    /**
     * @param s
     * @param candidates
     * @return
     */
    public Preference exploration_choose_according_to_policy(Slot s, Preference candidates)
    {
        // TODO implement exploration_choose_according_to_policy
        throw new UnsupportedOperationException("exploration_choose_according_to_policy not implemented");
    }

    /**
     * 
     */
    public void exploration_update_parameters()
    {
        // TODO implement exploration_update_parameters
        throw new UnsupportedOperationException("exploration_update_parameters not implemented");
    }

}
