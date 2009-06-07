/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
final class ReinforcementLearningCommand implements SoarCommand
{
    private final Agent agent;

    ReinforcementLearningCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        if(args.length != 4)
        {
            // TODO illegal arguments
            throw new SoarException(String.format("%s --set learning [on|off]", args[0]));
        }
        
        // TODO reinforcement learning: Obviously, this implementation is insufficient
        final ReinforcementLearning rl = Adaptables.adapt(agent, ReinforcementLearning.class);
        rl.rl_set_parameter(ReinforcementLearning.RL_PARAM_LEARNING, 
                "on".equals(args[3].toString()) ? ReinforcementLearning.RL_LEARNING_ON :
                    ReinforcementLearning.RL_LEARNING_ON );
        return "";
    }
}