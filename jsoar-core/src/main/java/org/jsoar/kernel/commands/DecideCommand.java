/*
 * Copyright (c) 2017 Soar Technology, Inc.
 */
package org.jsoar.kernel.commands;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.DecisionManipulation;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.exploration.Exploration;
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
import org.jsoar.util.PrintHelper;
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
public final class DecideCommand implements SoarCommand {
	private static final int DISPLAY_COLUMNS = 55;

	private final Agent agent;

	private Exploration exploration;
	private DecisionManipulation decisionManipulation;

	public DecideCommand(Agent agent) {
		this.agent = agent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jsoar.util.commands.SoarCommand#execute(org.jsoar.util.commands.
	 * SoarCommandContext, java.lang.String[])
	 */
	@Override
    public String execute(SoarCommandContext context, String[] args) throws SoarException
    {
    	
        this.exploration = Adaptables.adapt(agent, Exploration.class);
        this.decisionManipulation = Adaptables.adapt(agent, DecisionManipulation.class);

        if (args.length == 1)
        {
            return currentDecideSettings();
        }

        final String name = args[1];
        
        try
        {
        	// According to the Soar Manual, version 9.6.0, these are the valid first
        	// arguments to the "decide" command:
        	// numeric-indifferent-mode
        	// indifferent-selection
        	// predict
        	// select
        	// set-random-seed
            if (name.equals("numeric-indifferent-mode"))
            {
            	if (args.length == 2)
            	{
            		return currentNumericIndifferentMode();
            	}
            	else if (args[2].startsWith("--"))
            	{
            		String mode = args[2].substring(2);
            		if (exploration.exploration_set_numeric_indifferent_mode(mode))
            		{
            			final String result = "Set decide numeric-indifferent-mode to " + mode;
            			if (args.length == 3)
            			{
            				return result;
            			}
            			else
            			{
            				return result + "\nAdditional arguments to 'decide' command ignored";
            			}
            		}
            		else
            		{
                        throw new SoarException("Illegal value for decide numeric-indifferent-mode: " + args[2]);            		           			
            		}
            	}
            	else
            	{
                    throw new SoarException("Illegal value for decide numeric-indifferent-mode: " + args[2]);            		           			
            	}
            }
            else if (name.equals("indifferent-selection"))
            {
            	if (args.length == 2)
            	{
            		// If the only argument is "indifferent-selection", the user is just asking
            		// for the current setting
            		// TODO check whether this matches the behavior in CSoar
            		return currentPolicy();
            	}
            	else if (args[2].startsWith("--"))
            	{
            		// If there isa a second argument, it must start with --, and it must be one of the following:
            		// A policy: --boltzmann --epsilon-greedy --first --last --softmax
            		//   (These are defined in the enumeration Exploration.Policy)
            		// A parameter: --epsilon --temperature
            		//   (These are defined Exploration constructor, using the exploratioon_add_parameter
            		//    method, and are stored as instances of the ExplorationParameter class)
            		// A parameter subcommand: --reduction-policy --reduction-rate 
            		// A decision subcommand: --auto-reduce --stats
            		
            		// TODO JSoar's enumeration of Exploration.Policy also includes a policy
            		// for --uniform-random, but this is no longer included in the latest version
            		// of CSoar. For now, I've written this so it accepts
            		// any policy that is defined in Exploration.Policy, so the code here shouldn't
            		// have to change if we remove uniform-random from the Enumeration to get
            		// things in sync.
            		final String policyOrParam = args[2].substring(2);
            		// exploration_set_policy returns false if the desired policy is not in the eligible
            		// Exploration.Policy enumeration
            		if (exploration.exploration_set_policy(policyOrParam))
            		{
            			final String result = "Set decide indifferent-selection policy to " + policyOrParam;
            			if (args.length == 3)
            			{
            				return result;
            			}
            			else
            			{
            				return result + "\nAdditional arguments to 'decide' command ignored";
            			}
            		}
            		else if (exploration.exploration_valid_parameter(policyOrParam))
            		{
            			if (args.length == 3)
            			{
            				// User is requesting the current value of one of the parameters
            				return currentParameterValue(policyOrParam);
            			}
            			else
            			{
            				// User is requesting to set a new value for one of the parameters
            				double newValue;
            				try
            				{
            					newValue = Double.parseDouble(args[3]);
            				} 
            				catch (NumberFormatException e)
            				{
            	                throw new SoarException(String.format("%s is not a valid double: %s", args[3], e.getMessage()));
            				}
            				if (exploration.exploration_valid_parameter_value(policyOrParam, newValue))
            				{
            					if (exploration.exploration_set_parameter_value(policyOrParam, newValue))
            					{
                					final String result = "Set decide " + policyOrParam + " parameter value to " + newValue;
                					if (args.length == 4)
                					{
                        				return result;
                        			}
                        			else
                        			{
                        				return result + "\nAdditional arguments to 'decide' command ignored";
                        			}
            					}
            					else
            					{
            						throw new SoarException("Unknown error trying to set decide " + policyOrParam + " parameter value");
            					}
            				}
            				else
            				{
            					throw new SoarException("Illegal value for decide " + policyOrParam + " parameter value");
            				}
            			}
            		}
            		// The third argument wasn't a valid policy name or parameter name, so next we
            		// see if it is a parameter subcommand
            		else if (policyOrParam.equals("reduction-policy") || policyOrParam.equals("p"))
            		{
            			if (args.length == 3)
            			{
            				throw new SoarException("decide indifferent-selection --reduction-policy must specify a parameter name");
            			}
            			if (args[3].startsWith("--"))
            			{
                 			final String parameter = args[3].substring(2);
                 			if (exploration.exploration_valid_parameter(parameter))
                 			{
                 				if (args.length == 4)
                 				{
                 					// User is asking for the current reduction-policy for this parameter
                 					return currentParameterReductionPolicy(parameter);
                 				}
                 				// User is asking to set the current reduction-policy for this parameter
                 				if (args[4].startsWith("--"))
                 				{
                 					final String reductionPolicy = args[4].substring(2);
                 					if (exploration.exploration_set_reduction_policy(parameter, reductionPolicy))
                 					{
                 						final String result = "Set " + parameter + " reduction policy to " + args[4];
                 						if (args.length == 5)
                 						{
                 							return result;
                 						}
	                 					else
	                            		{
	                 						return result + "\nAdditional arguments to 'decide' command ignored";
	                            		}
                 					}
                 					else
                 					{
                 						throw new SoarException("Illegal value for " + parameter + " reduction policy: " + args[4]);
                 					}
                 				}
                 				else
                 				{
             						throw new SoarException("Illegal value for " + parameter + " reduction policy: " + args[4]);
                 				}
                 			}
                 			else
                 			{
                 				throw new SoarException("Unknown parameter name: " + args[3]);
                 			}
            			}
            			else
            			{
            				throw new SoarException("Illegal parameter specification");
            			}
            		}
            		else if (policyOrParam.equals("reduction-rate") || policyOrParam.equals("r"))
            		{
            			if (args.length == 3)
            			{
            				throw new SoarException("decide indifferent-selection --reduction-rate must specify a parameter name");
            			}            			
            			if (args[3].startsWith("--"))
            			{
                 			final String parameter = args[3].substring(2);
                 			if (exploration.exploration_valid_parameter(parameter))
                 			{
                 				if (args.length == 4)
                 				{
                        			throw new SoarException("decide indifferent-selection --reduction-rate --" + parameter + " must specify a policy name");
                 				}
                        		if (args[4].startsWith("--"))
                        		{
                             		final String reductionPolicy = args[4].substring(2);
                             		if (exploration.exploration_valid_reduction_policy(parameter, reductionPolicy))
                             		{
                             			if (args.length == 5)
                             			{
                         					// User is asking for the current reduction-rate for this parameter and policy
                         					return currentParameterReductionRate(parameter, reductionPolicy);
                             			}
                             			else
                             			{
                         					// User is asking to set the current reduction-rate for this parameter and policy
                            				double newValue;
                            				try
                            				{
                            					newValue = Double.parseDouble(args[5]);
                            				} 
                            				catch (NumberFormatException e)
                            				{
                            	                throw new SoarException(String.format("%s is not a valid double: %s", args[5], e.getMessage()));
                            				}
                         					if (exploration.exploration_set_reduction_rate(parameter, reductionPolicy, newValue))
                         					{
                                    			final String result = "Set " + parameter + " " + reductionPolicy + " reduction rate to " + newValue;
                                    			if (args.length == 6)
                                    			{
                                    				return result;
                                    			}
                                    			else
                                    			{
                                    				return result + "\nAdditional arguments to 'decide' command ignored";
                                    			}
                         					}
                                    		else
                                    		{
                                    			throw new SoarException("Illegal value for " + parameter + " " + reductionPolicy + " reduction rate: " + newValue);
                                    		}
                             			}
                             		}
                             		else
                             		{
                         				throw new SoarException("Unknown reduction policy name: " + args[4]);                             			
                             		}
                 				}
                    			else
                    			{
                    				throw new SoarException("Illegal reduction policy specification");
                    			}
                 			}
                 			else
                 			{
                 				throw new SoarException("Unknown parameter name: " + args[3]);
                 			}
            			}
            			else
            			{
            				throw new SoarException("Illegal parameter specification");
            			}
            		}
            		// The third argument wasn't a valid policy name, parameter name, or
            		// parameter subcommand, so check if it is a decision subcommand
            		else if (policyOrParam.equals("auto-reduce"))
            		{
            			if (args.length == 3)
            			{
            				return currentAutoReduceSetting();
            			}
            			if (args[3].equals("--on"))
            			{
            				exploration.exploration_set_auto_update(true);
                			final String result = "Enable decide indifferent-selection auto-upate";
                			if (args.length == 4)
                			{
                				return result;
                			}
                			else
                			{
                				return result + "\nAdditional arguments to 'decide' command ignored";
                			}
            			}
            			else if (args[3].equals("--off"))
            			{
            				exploration.exploration_set_auto_update(false);
                			final String result = "Disabale decide indifferent-selection auto-upate";
                			if (args.length == 4)
                			{
                				return result;
                			}
                			else
                			{
                				return result + "\nAdditional arguments to 'decide' command ignored";
                			}            				
            			}
            			else
            			{
            				throw new SoarException ("Illegal argument to decide indifferent-selection --auto-reduce: " +args[3]);
            			}
            		}
            		else if (policyOrParam.equals("stats"))
            		{
            			// TODO
            			return "Decide indifferent-selection --stats command not implemented in JSoar yet";
            		}
            		else
            		{
                		// There is a third argument to the indifferent-selection subcommand, but we do
                		// not recognize it
                        throw new SoarException("Unrecognized argument to decide indifferent-selection: " + args[2]);            		           			
            		}
            	}
            	else
            	{
                    throw new SoarException("Illegal value for decide indifferent-selection: " + args[2]);            		           			
            	}
            }
            else if (name.equals("predict"))
            {
            	return decisionManipulation.predict_get();
            }
            else if (name.equals("select"))
            {
            	if (args.length == 2) 
            	{
            		String my_selection = decisionManipulation.select_get_operator();
            		if (my_selection == null)
            		{
            			return "No operator selected.";
            		}
            		else
            		{
            			return my_selection;
            		}
            	}
            	else
            	{
            		decisionManipulation.select_next_operator(args[3]);
            		return "Operator " + args[3] + " will be selected.";
            	}
            }
            else if (name.equals("set-random-seed") || name.equals("srand"))
            {
            	// Mimic the srand command, which this replaces in Soar 8.6.0
            	long seed;
            	if (args.length == 2)
            	{
                    seed = System.nanoTime();
            	}
            	else
            	{
                    try
                    {
                        seed = Long.parseLong(args[2]);
                    }
                    catch(NumberFormatException e)
                    {
                        throw new SoarException(String.format("%s is not a valid integer: %s", args[1], e.getMessage()));
                    }
                }
                agent.getRandom().setSeed(seed);
                return "Random number generator seed set to " + seed;
           }
            else
            {
                throw new SoarException("Unknown decide sub-command '" + name + "'");
            }
        }
        catch(IllegalArgumentException e) // this is thrown by the enums if a bad value is passed in
        {
            throw new SoarException("Invalid value.");
        }
    }
	@Override
	public Object getCommand() {
		//todo - when implementing picocli, return the runnable
		return null;
	}
	private String currentDecideSettings() {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);

		pw.printf(PrintHelper.generateHeader("Decide Summary", DISPLAY_COLUMNS));
		pw.printf(currentNumericIndifferentMode());
		pw.printf(PrintHelper.generateSection("Discount", DISPLAY_COLUMNS));
		pw.printf(currentPolicy());
		pw.printf(currentAutoReduceSetting());
		pw.printf(currentParameterValue("epsilon"));
		pw.printf(currentParameterReductionPolicy("epsilon"));
		pw.printf(currentParameterReductionRate("epsilon", "exponential"));
		pw.printf(currentParameterReductionRate("epsilon", "linear"));
		pw.printf(currentParameterValue("temperature"));
		pw.printf(currentParameterReductionPolicy("temperature"));
		pw.printf(currentParameterReductionRate("temperature", "exponential"));
		pw.printf(currentParameterReductionRate("temperature", "linear"));
		pw.printf(PrintHelper.generateSection("Discount", DISPLAY_COLUMNS));

		pw.flush();
		return sw.toString();
	}

	private String currentNumericIndifferentMode() {
		return PrintHelper.generateItem("Numeric indifference mode:",
				exploration.exploration_get_numeric_indifferent_mode().getModeName(), DISPLAY_COLUMNS);
	}

	private String currentPolicy() {
		return PrintHelper.generateItem("Exploration Policy:", exploration.exploration_get_policy().getPolicyName(), DISPLAY_COLUMNS);
	}

	private String currentParameterValue(String name) {
		return PrintHelper.generateItem(name.substring(0, 1).toUpperCase() + name.substring(1) + ":",
				exploration.exploration_get_parameter_value(name), DISPLAY_COLUMNS);
	}

	private String currentParameterReductionPolicy(String name) {
		return PrintHelper.generateItem(name.substring(0, 1).toUpperCase() + name.substring(1) + " Reduction Policy:",
				exploration.exploration_get_reduction_policy(name).getPolicyName(), DISPLAY_COLUMNS);
	}

	private String currentParameterReductionRate(String name, String policy) {
		return PrintHelper.generateItem(
				name.substring(0, 1).toUpperCase() + name.substring(1) + " " + policy.substring(0, 1).toUpperCase()
						+ policy.substring(1) + " Reduction Rate:",
				exploration.exploration_get_reduction_rate(name, policy), DISPLAY_COLUMNS);
	}

	private String currentAutoReduceSetting() {
		return PrintHelper.generateItem("Automatic Policy Parameter Reduction:",
				exploration.exploration_get_auto_update() ? "on" : "off", DISPLAY_COLUMNS);
	}


}