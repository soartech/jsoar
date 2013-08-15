/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.learning.rl;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
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

    public static class Provider implements SoarCommandProvider
    {
        /*
         * (non-Javadoc)
         * 
         * @see
         * org.jsoar.util.commands.SoarCommandProvider#registerCommands(org.
         * jsoar.util.commands.SoarCommandInterpreter)
         */
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            interp.addCommand("rl", new ReinforcementLearningCommand(context));
        }
    }

    public ReinforcementLearningCommand(Adaptable context)
    {
        this.agent = Adaptables.require(getClass(), context, Agent.class);
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length == 1)
        {
            return doRl();
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
            doSet(key, value);
            return value;
        }
        else
        {
            throw new SoarException(String.format("%s --set name value, or %s --get name", args[0], args[0]));
        }
    }
    
    @SuppressWarnings("unchecked")
    private void doSet(PropertyKey<?> key, String value) throws SoarException
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

    private String doRl()
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        
        pw.printf(RLPrintHelper.generateHeader("", 0));
        pw.printf(RLPrintHelper.generateItem("Soar-RL learning:", "off"/*p.learning.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("temporal-extension:", "on"/*p.temporal_extension.get()*/, 40));
        pw.printf(RLPrintHelper.generateSection("Discount", 40));
        pw.printf(RLPrintHelper.generateItem("discount-rate:", "0.9"/*p.discount_rate.get()*/, 40));
        pw.printf(RLPrintHelper.generateSection("Learning", 40));
        pw.printf(RLPrintHelper.generateItem("learning-policy:", "sarsa"/*p.learning_policy.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("learning-rate:", "0.3"/*p.learning_rate.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("hrl-discount:", "off"/*p.hrl_discount.get()*/, 40));
        pw.printf(RLPrintHelper.generateSection("Eligibility Traces", 40));
        pw.printf(RLPrintHelper.generateItem("eligibility-trace-decay-rate:", "0"/*p.et_decay_rate.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("eligibility-trace-tolerance:", "0.001"/*p.et_tolerance.get()*/, 40));
        pw.printf(RLPrintHelper.generateSection("Experimental", 40));
        pw.printf(RLPrintHelper.generateItem("chunk-stop:", "on"/*p.chunk_stop.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("decay-mode:", "normal"/*p.decay_mode.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("meta:", "off"/*p.meta.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("meta-learning-rate:", "0.1"/*p.meta_learning_rate.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("update-log-path:", ""/*p.update_log_path.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("", "0", 0));
        pw.printf(RLPrintHelper.generateItem("apoptosis:", "none"/*p.apoptosis.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("apoptosis-decay:", "0.5"/*p.apoptosis_decay.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("apoptosis-thresh:", "-2"/*p.apoptosis_thresh.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("", "0", 0));
        pw.printf(RLPrintHelper.generateItem("trace:", "off"/*p.trace.get()*/, 40));
        pw.printf(RLPrintHelper.generateItem("", "0", 0));

        
        pw.flush();
        return sw.toString();
    }

}