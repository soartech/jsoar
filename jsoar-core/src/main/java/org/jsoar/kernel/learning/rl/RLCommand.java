package org.jsoar.kernel.learning.rl;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.ApoptosisChoices;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.ChunkStop;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.DecayMode;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.HrlDiscount;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.Learning;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.LearningPolicy;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.Meta;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.TemporalDiscount;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.TemporalExtension;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.Trace;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.PicocliSoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This is the implementation of the "rl" command.
 * 
 * @author austin.brehob
 */
public class RLCommand extends PicocliSoarCommand
{
    public static class Provider implements SoarCommandProvider
    {
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            interp.addCommand("rl", new RLCommand((Agent) context));
        }
    }
    
    public RLCommand(Agent agent)
    {
        super(agent, new RL(agent));
    }
    
    @Command(name = "rl", description = "Controls how numeric indifferent preference "
            + "values in RL rules are updated via reinforcement learning", subcommands = { HelpCommand.class })
    static public class RL implements Runnable
    {
        private final Agent agent;
        private final ReinforcementLearning rl;
        
        public RL(Agent agent)
        {
            this.agent = agent;
            this.rl = Adaptables.require(getClass(), agent, ReinforcementLearning.class);
        }
        
        @Option(names = { "-s", "--set" }, description = "Sets the given parameter value")
        String setParam = null;
        
        @Parameters(arity = "0..1", description = "The new value of the parameter")
        String newVal = null;
        
        @Option(names = { "-g", "--get" }, description = "Prints the current setting of the given parameter")
        String getParam = null;
        
        @Override
        public void run()
        {
            if(setParam != null)
            {
                if(newVal == null)
                {
                    agent.getPrinter().startNewLine().print("Error: no parameter value provided");
                    return;
                }
                agent.getPrinter().startNewLine().print(doSet(setParam, newVal));
            }
            else if(getParam != null)
            {
                agent.getPrinter().startNewLine().print(doGet(getParam));
            }
            else
            {
                agent.getPrinter().startNewLine().print(doRl());
            }
            // TODO: Soar Manual version 9.6.0 includes RL options "-t/--trace" and "-S/--stats"
        }
        
        private String doSet(String paramToSet, String value)
        {
            final PropertyManager props = rl.getParams().getProperties();
            
            try
            {
                if(paramToSet.equals("learning"))
                {
                    props.set(ReinforcementLearningParams.LEARNING, Learning.valueOf(value));
                }
                else if(paramToSet.equals("discount-rate"))
                {
                    props.set(ReinforcementLearningParams.DISCOUNT_RATE, Double.parseDouble(value));
                    return "Set discount-rate to " + Double.parseDouble(value);
                }
                else if(paramToSet.equals("learning-policy"))
                {
                    // TODO
                    if(value.equals("off-policy-gq-lambda") || value.equals("on-policy-gq-lambda"))
                    {
                        agent.getPrinter().startNewLine().print("RL learning-policy '" + value
                                + "' has not yet been implemented in JSoar");
                        return "";
                    }
                    props.set(ReinforcementLearningParams.LEARNING_POLICY, LearningPolicy.valueOf(value));
                    return "Set learning-policy to " + LearningPolicy.valueOf(value);
                }
                else if(paramToSet.equals("step-size-parameter"))
                {
                    // TODO
                    agent.getPrinter().startNewLine().print("RL GQ parameter 'step-size-parameter' "
                            + "has not yet been implemented in JSoar");
                    return "";
                }
                else if(paramToSet.equals("learning-rate"))
                {
                    props.set(ReinforcementLearningParams.LEARNING_RATE, Double.parseDouble(value));
                    return "Set learning-rate to " + Double.parseDouble(value);
                }
                else if(paramToSet.equals("hrl-discount"))
                {
                    props.set(ReinforcementLearningParams.HRL_DISCOUNT, HrlDiscount.valueOf(value));
                    return "Set hrl-discount to " + HrlDiscount.valueOf(value);
                }
                else if(paramToSet.equals("temporal-discount"))
                {
                    props.set(ReinforcementLearningParams.TEMPORAL_DISCOUNT, TemporalDiscount.valueOf(value));
                    return "Set temporal-discount to " + TemporalDiscount.valueOf(value);
                }
                else if(paramToSet.equals("temporal-extension"))
                {
                    props.set(ReinforcementLearningParams.TEMPORAL_EXTENSION, TemporalExtension.valueOf(value));
                    return "Set temporal-extension to " + TemporalExtension.valueOf(value);
                }
                else if(paramToSet.equals("eligibility-trace-decay-rate"))
                {
                    props.set(ReinforcementLearningParams.ET_DECAY_RATE, Double.parseDouble(value));
                    return "Set eligibility-trace-decay-rate to " + Double.parseDouble(value);
                }
                else if(paramToSet.equals("eligibility-trace-tolerance"))
                {
                    props.set(ReinforcementLearningParams.ET_TOLERANCE, Double.parseDouble(value));
                    return "Set eligibility-trace-tolerance to " + Double.parseDouble(value);
                }
                else if(paramToSet.equals("chunk-stop"))
                {
                    props.set(ReinforcementLearningParams.CHUNK_STOP, ChunkStop.valueOf(value));
                    return "Set chunk-stop to " + ChunkStop.valueOf(value);
                }
                else if(paramToSet.equals("decay-mode"))
                {
                    props.set(ReinforcementLearningParams.DECAY_MODE, DecayMode.valueOf(value));
                    return "Set decay-mode to " + DecayMode.valueOf(value);
                }
                else if(paramToSet.equals("meta"))
                {
                    props.set(ReinforcementLearningParams.META, Meta.valueOf(value));
                    return "Set meta to " + Meta.valueOf(value);
                }
                else if(paramToSet.equals("meta-learning-rate"))
                {
                    props.set(ReinforcementLearningParams.META_LEARNING_RATE, Double.parseDouble(value));
                    return "Set meta-learning-rate to " + Double.parseDouble(value);
                }
                else if(paramToSet.equals("update-log-path"))
                {
                    props.set(ReinforcementLearningParams.UPDATE_LOG_PATH, value);
                    return "Set update-log-path to " + value;
                }
                else if(paramToSet.equals("apoptosis"))
                {
                    props.set(ReinforcementLearningParams.APOPTOSIS, ApoptosisChoices.getEnum(value));
                    return "Set apoptosis to " + ApoptosisChoices.getEnum(value);
                }
                else if(paramToSet.equals("apoptosis-decay"))
                {
                    props.set(ReinforcementLearningParams.APOPTOSIS_DECAY, Double.parseDouble(value));
                    return "Set apoptosis-decay to " + Double.parseDouble(value);
                }
                else if(paramToSet.equals("apoptosis-thresh"))
                {
                    props.set(ReinforcementLearningParams.APOPTOSIS_THRESH, Double.parseDouble(value));
                    return "Set apoptosis-thresh to " + Double.parseDouble(value);
                }
                else if(paramToSet.equals("trace"))
                {
                    props.set(ReinforcementLearningParams.TRACE, Trace.valueOf(value));
                    return "Set trace to " + Trace.valueOf(value);
                }
                else
                {
                    agent.getPrinter().startNewLine().print("Unknown rl parameter '" + paramToSet + "'");
                    return "";
                }
            }
            catch(IllegalArgumentException e) // this is thrown by the enums if a bad value is passed in
            {
                agent.getPrinter().startNewLine().print("Invalid value.");
            }
            
            return "";
        }
        
        private String doGet(String paramToGet)
        {
            final PropertyKey<?> key = ReinforcementLearningParams.getProperty(
                    rl.getParams().getProperties(), paramToGet);
            if(key == null)
            {
                agent.getPrinter().startNewLine().print("Unknown parameter '" + paramToGet + "'");
                return "";
            }
            return rl.getParams().getProperties().get(key).toString();
        }
        
        private String doRl()
        {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            
            final ReinforcementLearningParams p = rl.getParams();
            pw.printf(RLPrintHelper.generateHeader("", 0));
            pw.printf(RLPrintHelper.generateItem("Soar-RL learning:", p.learning.get(), 40));
            pw.printf(RLPrintHelper.generateItem("temporal-extension:", p.temporal_extension.get(), 40));
            pw.printf(RLPrintHelper.generateSection("Discount", 40));
            pw.printf(RLPrintHelper.generateItem("discount-rate:", p.discount_rate.get(), 40));
            pw.printf(RLPrintHelper.generateSection("Learning", 40));
            pw.printf(RLPrintHelper.generateItem("learning-policy:", p.learning_policy.get(), 40));
            pw.printf(RLPrintHelper.generateItem("learning-rate:", p.learning_rate.get(), 40));
            pw.printf(RLPrintHelper.generateItem("hrl-discount:", p.hrl_discount.get(), 40));
            // This is commented out here since the CSoar report does not show it,
            // even though it is there in CSoar as well.
            // pw.printf(RLPrintHelper.generateItem("temporal-discount:", p.temporal_discount.get(), 40));
            pw.printf(RLPrintHelper.generateSection("Eligibility Traces", 40));
            pw.printf(RLPrintHelper.generateItem("eligibility-trace-decay-rate:", p.et_decay_rate.get(), 40));
            pw.printf(RLPrintHelper.generateItem("eligibility-trace-tolerance:", p.et_tolerance.get(), 40));
            pw.printf(RLPrintHelper.generateSection("Experimental", 40));
            pw.printf(RLPrintHelper.generateItem("chunk-stop:", p.chunk_stop.get(), 40));
            pw.printf(RLPrintHelper.generateItem("decay-mode:", p.decay_mode.get(), 40));
            pw.printf(RLPrintHelper.generateItem("meta:", p.meta.get(), 40));
            pw.printf(RLPrintHelper.generateItem("meta-learning-rate:", p.meta_learning_rate.get(), 40));
            pw.printf(RLPrintHelper.generateItem("update-log-path:", p.update_log_path.get(), 40));
            pw.printf(RLPrintHelper.generateItem("", "0", 0));
            
            // The following are not implemented yet, except for being faked here
            pw.printf(RLPrintHelper.generateItem("apoptosis:", p.apoptosis.get(), 40));
            pw.printf(RLPrintHelper.generateItem("apoptosis-decay:", p.apoptosis_decay.get(), 40));
            pw.printf(RLPrintHelper.generateItem("apoptosis-thresh:", p.apoptosis_thresh.get(), 40));
            pw.printf(RLPrintHelper.generateItem("", "0", 0));
            pw.printf(RLPrintHelper.generateItem("trace:", p.trace.get(), 40));
            pw.printf(RLPrintHelper.generateItem("", "0", 0));
            
            pw.flush();
            return sw.toString();
        }
    }
}
