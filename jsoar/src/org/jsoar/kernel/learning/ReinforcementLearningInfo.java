/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 14, 2008
 */
package org.jsoar.kernel.learning;

import org.jsoar.kernel.ImpasseType;

/**
 * reinforcement_learning.h:99:rl_data
 * 
 * @author ray
 */
public class ReinforcementLearningInfo
{
    // Initial values from decide.cpp:2092:decide_context_slot
//  TODO  rl_et_map *eligibility_traces;
//  TODO  ::list *prev_op_rl_rules;
    double previous_q = 0.0;
    double reward = 0.0;
    int reward_age = 0;    // the number of steps since a cycle containing rl rules
    int num_prev_op_rl_rules = 0;
    int step = 0;          // the number of steps the current operator has been installed at the goal
    public ImpasseType impasse_type = ImpasseType.NONE;    // if this goal is an impasse, what type

}
