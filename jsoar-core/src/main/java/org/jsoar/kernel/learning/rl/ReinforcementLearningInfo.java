/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 14, 2008
 */
package org.jsoar.kernel.learning.rl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.jsoar.kernel.Production;

/**
 * reinforcement_learning.h:99:rl_data
 * 
 * @author ray
 */
public class ReinforcementLearningInfo
{
    // Initial values from decide.cpp:2092:decide_context_slot
    
    /**
     * traces associated with productions
     */
    public final Map<Production, Double> eligibility_traces = new HashMap<Production, Double>();
    /**
     * rl rules associated with the previous operator
     */
    public final LinkedList<Production> prev_op_rl_rules = new LinkedList<Production>();
    
    /**
     * q-value of the previous state
     */
    double previous_q = 0.0;
    /**
     * accumulated discounted reward
     */
    double reward = 0.0;
    
    /**
     * the number of steps since a cycle containing rl rules
     */
    long gap_age;
    /**
     * the number of steps in a subgoal
     */
    long hrl_age;

}
