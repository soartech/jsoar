package org.jsoar.kernel.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DecisionManipulation;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.util.PrintHelper;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * This is the implementation of the "decide" command.
 * @author austin.brehob
 */
public class DecideCommand implements SoarCommand
{
    private static final int DISPLAY_COLUMNS = 55;
    private final Agent agent;

    public DecideCommand(Agent agent)
    {
        this.agent = agent;
    }

    @Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
        Utils.parseAndRun(agent, new Decide(agent), args);

        return "";
    }


    @Command(name="decide", description="Commands and settings related to "
            + "the selection of operators during the Soar decision process",
            subcommands={HelpCommand.class,
                         DecideCommand.IndifferentSelection.class,
                         DecideCommand.NumericIndifferentMode.class,
                         DecideCommand.Predict.class,
                         DecideCommand.Select.class,
                         DecideCommand.SetRandomSeed.class,
                         DecideCommand.SRand.class})
    static public class Decide implements Runnable
    {
        private Agent agent;
        private Exploration exploration;
        private DecisionManipulation decisionManipulation;

        public Decide(Agent agent)
        {
            this.agent = agent;
            this.exploration = Adaptables.adapt(agent, Exploration.class);
            this.decisionManipulation = Adaptables.adapt(agent, DecisionManipulation.class);
        }

        @Override
        public void run()
        {
            printCurrentDecideSettings();
        }

        private void printCurrentDecideSettings() {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);

            pw.printf(PrintHelper.generateHeader("Decide Summary", DISPLAY_COLUMNS));
            pw.printf(currentNumericIndifferentMode(exploration));
            pw.printf(PrintHelper.generateSection("Discount", DISPLAY_COLUMNS));
            pw.printf(currentPolicy(exploration));
            pw.printf(currentAutoReduceSetting(exploration));
            pw.printf(currentParameterValue(exploration, "epsilon"));
            pw.printf(currentParameterReductionPolicy(exploration, "epsilon"));
            pw.printf(currentParameterReductionRate(exploration, "epsilon", "exponential"));
            pw.printf(currentParameterReductionRate(exploration, "epsilon", "linear"));
            pw.printf(currentParameterValue(exploration, "temperature"));
            pw.printf(currentParameterReductionPolicy(exploration, "temperature"));
            pw.printf(currentParameterReductionRate(exploration, "temperature", "exponential"));
            pw.printf(currentParameterReductionRate(exploration, "temperature", "linear"));
            pw.printf(PrintHelper.generateSection("Discount", DISPLAY_COLUMNS));

            pw.flush();
            agent.getPrinter().startNewLine().print(sw.toString());
        }
    }


    @Command(name="indifferent-selection", description="Allows the user to set options relating to "
            + "selection between operator proposals that are mutually indifferent in preference memory",
            subcommands={HelpCommand.class})
    static public class IndifferentSelection implements Runnable
    {
        @ParentCommand
        Decide parent; // injected by picocli

        @Option(names={"-b", "--boltzmann"}, description="Sets the exploration policy to 'boltzmann'")
        boolean boltzmannPolicy = false;

        @Option(names={"-E", "--epsilon-greedy"}, description="Sets the "
                + "exploration policy to 'epsilon-greedy'")
        boolean epsilonGreedyPolicy = false;

        @Option(names={"-f", "--first"}, description="Sets the exploration policy to 'first'")
        boolean firstPolicy = false;

        @Option(names={"-l", "--last"}, description="Sets the exploration policy to 'last'")
        boolean lastPolicy = false;

        @Option(names={"-s", "--softmax"}, description="Sets the exploration policy to 'softmax'")
        boolean softmaxPolicy = false;

        @Option(names={"-e", "--epsilon"}, description="Prints or updates the epsilon value")
        boolean epsilon = false;

        @Option(names={"-t", "--temperature"}, description="Prints or updates temperature value")
        boolean temperature = false;

        @Option(names={"-p", "--reduction-policy"}, description="Prints or updates the "
                + "reduction policy for the given parameter")
        String reductionPolicyParam = null;

        @Option(names={"-r", "--reduction-rate"}, description="Prints or updates the "
                + "reduction rate for the given parameter")
        String reductionRateParam = null;

        @Option(names={"-a", "--auto-reduce"}, description="Prints or toggles automatic "
                + "policy parameter reduction")
        boolean autoReduce = false;

        @Option(names={"-S", "--stats"}, description="Prints summary of decision settings")
        boolean printStats = false;

        @Parameters(index="0", arity="0..1", description="New epsilon/temperature value; or exploration "
                + "parameter reduction policy: 'linear' or 'exponential'; or toggles auto-reduce: "
                + "'on' or 'off'")
        private String param = null;

        @Parameters(index="1", arity="0..1", description="New exploration parameter reduction rate")
        private Double reductionRate = null;


        @Override
        public void run()
        {
            // If the user specified multiple options...
            if ((boltzmannPolicy ? 1 : 0) + (epsilonGreedyPolicy ? 1 : 0) + (firstPolicy ? 1 : 0)
                    + (lastPolicy ? 1 : 0) + (softmaxPolicy ? 1 : 0) + (epsilon ? 1 : 0)
                    + (temperature ? 1 : 0) + ((reductionPolicyParam != null) ? 1 : 0)
                    + ((reductionRateParam != null) ? 1 : 0) + (autoReduce ? 1 : 0)
                    + (printStats ? 1 : 0) >= 2)
            {
                parent.agent.getPrinter().startNewLine().print("indifferent-selection "
                        + "takes only one option at a time.");
                return;
            }

            // decide indifferent-selection --<policyName>
            if (boltzmannPolicy || epsilonGreedyPolicy || firstPolicy || lastPolicy || softmaxPolicy)
            {
                String policyName = "boltzmann";
                if (epsilonGreedyPolicy)
                {
                    policyName = "epsilon-greedy";
                }
                else if (firstPolicy)
                {
                    policyName = "first";
                }
                else if (lastPolicy)
                {
                    policyName = "last";
                }
                else if (softmaxPolicy)
                {
                    policyName = "softmax";
                }

                if (parent.exploration.exploration_set_policy(policyName))
                {
                    parent.agent.getPrinter().startNewLine().print("Set decide "
                            + "indifferent-selection policy to " + policyName);
                }
                else
                {
                    parent.agent.getPrinter().startNewLine().print("Failed to set decide "
                            + "indifferent-selection policy to " + policyName);
                }
            }

            // decide indifferent-selection --epsilon/--temperature ...
            else if (epsilon || temperature)
            {
                String parameterName = "epsilon";
                if (temperature)
                {
                    parameterName = "temperature";
                }

                // decide indifferent-selection --epsilon/--temperature
                if (param == null)
                {
                    parent.agent.getPrinter().startNewLine().print(
                            currentParameterValue(parent.exploration, parameterName));
                }
                // decide indifferent-selection --epsilon/--temperature <newValue>
                else
                {
                    Double newValue = null;

                    try
                    {
                        newValue = Double.parseDouble(param);

                        if (parent.exploration.exploration_valid_parameter_value(parameterName, newValue))
                        {
                            if (parent.exploration.exploration_set_parameter_value(parameterName, newValue))
                            {
                                parent.agent.getPrinter().startNewLine().print("Set decide "
                                        + parameterName + " parameter value to " + newValue);
                            }
                            else
                            {
                                parent.agent.getPrinter().startNewLine().print("Unknown error trying "
                                        + "to set decide " + parameterName + " parameter value");
                            }
                        }
                        else
                        {
                            parent.agent.getPrinter().startNewLine().print("Illegal value "
                                    + "for decide " + parameterName + " parameter value");
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        parent.agent.getPrinter().startNewLine().print(String.format(
                                "%s is not a valid double: %s", param, e.getMessage()));
                    }
                }
            }

            // decide indifferent-selection --reduction-policy epsilon/temperature ...
            else if (reductionPolicyParam != null)
            {
                if (parent.exploration.exploration_valid_parameter(reductionPolicyParam))
                {
                    // decide indifferent-selection --reduction-policy epsilon/temperature
                    if (param == null)
                    {
                        parent.agent.getPrinter().startNewLine().print(currentParameterReductionPolicy(
                                parent.exploration, reductionPolicyParam));
                    }
                    // decide indifferent-selection --reduction-policy epsilon/temperature exponential/linear
                    else
                    {
                        if (parent.exploration.exploration_set_reduction_policy(reductionPolicyParam, param))
                        {
                            parent.agent.getPrinter().startNewLine().print("Set "+ reductionPolicyParam
                                    + " reduction policy to " + param);
                        }
                        else
                        {
                            parent.agent.getPrinter().startNewLine().print("Illegal value for "
                                    + reductionPolicyParam + " reduction policy: " + param);
                        }
                    }
                }
                else
                {
                    parent.agent.getPrinter().startNewLine().print("Unknown "
                            + "parameter name: " + reductionPolicyParam);
                }
            }

            // decide indifferent-selection --reduction-rate epsilon/temperature ...
            else if (reductionRateParam != null)
            {
                if (parent.exploration.exploration_valid_parameter(reductionRateParam))
                {
                    // decide indifferent-selection --reduction-rate epsilon/temperature
                    if (param == null)
                    {
                        parent.agent.getPrinter().startNewLine().print("Error: exploration "
                                + "parameter reduction policy must be specified");
                    }
                    // decide indifferent-selection --reduction-rate
                    // epsilon/temperature exponential/linear ...
                    else
                    {
                        if (parent.exploration.exploration_valid_reduction_policy(reductionRateParam, param))
                        {
                            // decide indifferent-selection --reduction-rate
                            // epsilon/temperature exponential/linear
                            if (reductionRate == null)
                            {
                                parent.agent.getPrinter().startNewLine().print(currentParameterReductionRate(
                                        parent.exploration, reductionRateParam, param));
                            }
                            // decide indifferent-selection --reduction-rate
                            // epsilon/temperature exponential/linear <newRate>
                            else
                            {
                                if (parent.exploration.exploration_set_reduction_rate(
                                        reductionRateParam, param, reductionRate))
                                {
                                    parent.agent.getPrinter().startNewLine().print("Set "
                                            + reductionRateParam + " " + param + " reduction "
                                            + "rate to " + reductionRate);
                                }
                                else
                                {
                                    parent.agent.getPrinter().startNewLine().print("Illegal value for "
                                            + reductionRateParam + " " + param + " reduction rate: "
                                            + reductionRate);
                                }
                            }
                        }
                        else
                        {
                            parent.agent.getPrinter().startNewLine().print("Unknown "
                                    + "reduction policy name: " + param);
                        }
                    }
                }
                else
                {
                    parent.agent.getPrinter().startNewLine().print("Unknown "
                            + "parameter name: " + reductionRateParam);
                }
            }

            // decide indifferent-selection --auto-reduce ...
            else if (autoReduce)
            {
                if (param == null)
                {
                    parent.agent.getPrinter().startNewLine().print(
                            currentAutoReduceSetting(parent.exploration));
                }
                else if (param.equals("on"))
                {
                    parent.exploration.exploration_set_auto_update(true);
                    parent.agent.getPrinter().startNewLine().print("Enabled "
                            + "decide indifferent-selection auto-update");
                }
                else if (param.equals("off"))
                {
                    parent.exploration.exploration_set_auto_update(false);
                    parent.agent.getPrinter().startNewLine().print("Disabled "
                            + "decide indifferent-selection auto-update");
                }
                else
                {
                    parent.agent.getPrinter().startNewLine().print("Illegal argument to "
                            + "decide indifferent-selection --auto-reduce: " + param);
                }
            }

            // decide indifferent-selection --stats
            else if (printStats)
            {
                final StringWriter sw = new StringWriter();
                final PrintWriter pw = new PrintWriter(sw);

                pw.printf(currentPolicy(parent.exploration));
                pw.printf(currentAutoReduceSetting(parent.exploration));
                pw.printf(currentParameterValue(parent.exploration, "epsilon"));
                pw.printf(currentParameterReductionPolicy(parent.exploration, "epsilon"));
                pw.printf(currentParameterReductionRate(parent.exploration, "epsilon", "exponential"));
                pw.printf(currentParameterReductionRate(parent.exploration, "epsilon", "linear"));
                pw.printf(currentParameterValue(parent.exploration, "temperature"));
                pw.printf(currentParameterReductionPolicy(parent.exploration, "temperature"));
                pw.printf(currentParameterReductionRate(parent.exploration, "temperature", "exponential"));
                pw.printf(currentParameterReductionRate(parent.exploration, "temperature", "linear"));

                pw.flush();
                parent.agent.getPrinter().startNewLine().print(sw.toString());
            }

            // decide indifferent-selection
            else
            {
                parent.agent.getPrinter().startNewLine().print(currentPolicy(parent.exploration));
            }
        }
    }

    @Command(name="numeric-indifferent-mode", description="Sets how multiple numeric indifferent preference "
            + "values given to an operator are combined into a single value for use in random selection",
            subcommands={HelpCommand.class})
    static public class NumericIndifferentMode implements Runnable
    {
        @ParentCommand
        Decide parent; // injected by picocli

        @Option(names={"-a", "--avg"}, description="Combines multiple preference values via an average")
        boolean average = false;

        @Option(names={"-s", "--sum"}, description="Combines multiple preference values via a sum")
        boolean sum = false;

        @Override
        public void run()
        {
            // decide numeric-indifferent-mode --avg
            if (average)
            {
                if (parent.exploration.exploration_set_numeric_indifferent_mode("avg"))
                {
                    parent.agent.getPrinter().startNewLine().print("Set decide "
                            + "numeric-indifferent-mode to avg");
                }
                else
                {
                    parent.agent.getPrinter().startNewLine().print("Failed to set "
                            + "numeric-indifferent-mode to avg");
                }
            }
            // decide numeric-indifferent-mode --sum
            else if (sum)
            {
                if (parent.exploration.exploration_set_numeric_indifferent_mode("sum"))
                {
                    parent.agent.getPrinter().startNewLine().print("Set decide "
                            + "numeric-indifferent-mode to sum");
                }
                else
                {
                    parent.agent.getPrinter().startNewLine().print("Failed to set "
                            + "numeric-indifferent-mode to sum");
                }
            }
            // decide numeric-indifferent-mode
            else
            {
                parent.agent.getPrinter().startNewLine().print(
                        currentNumericIndifferentMode(parent.exploration));
            }
        }
    }


    @Command(name="predict", description="Based upon current operator proposals, determines "
            + "which operator will be chosen during the next decision phase",
            subcommands={HelpCommand.class})
    static public class Predict implements Runnable
    {
        @ParentCommand
        Decide parent; // injected by picocli

        @Override
        public void run()
        {
            parent.agent.getPrinter().startNewLine().print(parent.decisionManipulation.predict_get());
        }
    }


    @Command(name="select", description="Forces the selection of an operator whose ID "
            + "is supplied as an argument during the next decision phase",
            subcommands={HelpCommand.class})
    static public class Select implements Runnable
    {
        @ParentCommand
        Decide parent; // injected by picocli

        @Parameters(index="0", arity="0..1", description="The operator's identifier")
        private String operatorID = null;

        @Override
        public void run()
        {
            // decide select
            if (operatorID == null)
            {
                String my_selection = parent.decisionManipulation.select_get_operator();
                if (my_selection == null)
                {
                    parent.agent.getPrinter().startNewLine().print("No operator selected.");
                }
                else
                {
                    parent.agent.getPrinter().startNewLine().print(my_selection);
                }
            }
            // decide select <identifier>
            else
            {
                parent.decisionManipulation.select_next_operator(operatorID);
                parent.agent.getPrinter().startNewLine().print("Operator "
                        + operatorID + " will be selected.");
            }
        }
    }


    @Command(name="set-random-seed", description="Seeds the random number generator with the passed seed",
            subcommands={HelpCommand.class})
    static public class SetRandomSeed implements Runnable
    {
        @ParentCommand
        Decide parent; // injected by picocli

        @Parameters(index="0", arity="0..1", description="The seed for the random number generator")
        private Long seed = null;

        @Override
        public void run()
        {
            if (seed == null)
            {
                seed = System.nanoTime();
            }

            parent.agent.getRandom().setSeed(seed);
            parent.agent.getPrinter().startNewLine().print("Random number generator seed set to " + seed);
        }
    }


    @Command(name="srand", description="Alias to set-random-seed",
            subcommands={HelpCommand.class})
    static public class SRand implements Runnable
    {
        @ParentCommand
        Decide parent; // injected by picocli

        @Parameters(index="0", arity="0..1", description="The seed for the random number generator")
        private Long seed = null;

        @Override
        public void run()
        {
            if (seed == null)
            {
                seed = System.nanoTime();
            }

            parent.agent.getRandom().setSeed(seed);
            parent.agent.getPrinter().startNewLine().print("Random number generator seed set to " + seed);
        }
    }


    private static String currentNumericIndifferentMode(Exploration exploration) {
        return PrintHelper.generateItem("Numeric indifference mode:",
                exploration.exploration_get_numeric_indifferent_mode().getModeName(), DISPLAY_COLUMNS);
    }

    private static String currentPolicy(Exploration exploration) {
        return PrintHelper.generateItem("Exploration Policy:",
                exploration.exploration_get_policy().getPolicyName(), DISPLAY_COLUMNS);
    }

    private static String currentParameterValue(Exploration exploration, String name) {
        return PrintHelper.generateItem(name.substring(0, 1).toUpperCase() + name.substring(1) + ":",
                exploration.exploration_get_parameter_value(name), DISPLAY_COLUMNS);
    }

    private static String currentParameterReductionPolicy(Exploration exploration, String name) {
        return PrintHelper.generateItem(name.substring(0, 1).toUpperCase() + name.substring(1) +
                " Reduction Policy:", exploration.exploration_get_reduction_policy(name).getPolicyName(),
                DISPLAY_COLUMNS);
    }

    private static String currentParameterReductionRate(Exploration exploration, String name, String policy) {
        return PrintHelper.generateItem(
                name.substring(0, 1).toUpperCase() + name.substring(1) + " " +
                policy.substring(0, 1).toUpperCase() + policy.substring(1) + " Reduction Rate:",
                exploration.exploration_get_reduction_rate(name, policy), DISPLAY_COLUMNS);
    }

    private static String currentAutoReduceSetting(Exploration exploration) {
        return PrintHelper.generateItem("Automatic Policy Parameter Reduction:",
                exploration.exploration_get_auto_update() ? "on" : "off", DISPLAY_COLUMNS);
    }
}
