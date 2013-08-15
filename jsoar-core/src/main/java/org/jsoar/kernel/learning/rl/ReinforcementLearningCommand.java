/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.learning.rl;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

/**
 * Implementation of the "rl" command.
 * 
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
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length == 1)
        {
            // TODO print all params
            
            return "Not implemented yet. Use 'properties' command.";
        }
        
        if(args.length < 3 || args.length > 4)
        {
            throw new SoarException(String.format("%s --set name value, or %s --get name", args[0], args[0]));
        }
        
        final String param = args[2];
        final PropertyKey<?> key = ReinforcementLearning.getProperty(agent.getProperties(), param);
        if(key == null)
        {
            throw new SoarException("Unknown RL parameter " + param);
        }
        
        final String op = args[1];
        if(args.length == 3 && op.equals("--get"))
        {
            return agent.getProperties().get(key).toString();
        }
        else if(args.length == 4 && op.equals("--set"))
        {
            final String value = args[3];
            set(key, value);
            return value;
        }
        else
        {
            throw new SoarException(String.format("%s --set name value, or %s --get name", args[0], args[0]));
        }
    }
    
    @SuppressWarnings("unchecked")
    private void set(PropertyKey<?> key, String value) throws SoarException
    {
        // TODO generalize this and add parameter checking.
        final PropertyManager  props = agent.getProperties();
        if(Double.class.equals(key.getType()))
        {
            props.set((PropertyKey<Double>) key, Double.valueOf(value));
        }
        else if(Boolean.class.equals(key.getType()))
        {
            props.set((PropertyKey<Boolean>) key, "on".equals(value));
        }
        else
        {
            throw new SoarException("Don't know how to set RL parameter '" + key + "' to value '" + value + "'");
        }
    }
}