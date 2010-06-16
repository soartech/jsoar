/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
public final class ReinforcementLearningCommand implements SoarCommand
{
    private final Agent agent;

    public ReinforcementLearningCommand(Agent agent)
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
        final String param = args[2]; 
        final String value = args[3];
        if("learning".equals(param))
        {
            agent.getProperties().set(ReinforcementLearning.LEARNING, "on".equals(value.toString()));
        }
        else if("learning-rate".equals(param))
        {
            agent.getProperties().set(ReinforcementLearning.LEARNING_RATE, Double.valueOf(value));
        }
        else if("discount-rate".equals(param))
        {
            agent.getProperties().set(ReinforcementLearning.DISCOUNT_RATE, Double.valueOf(value));
        }
        else
        {
            throw new SoarException("Unknown RL parameter " + args[2]);
        }
        
        // TODO reinforcement learning: Obviously, this implementation is insufficient
        return "";
    }
}