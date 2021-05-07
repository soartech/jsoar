/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel.exploration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.exploration.ExplorationParameter.ReductionPolicy;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.LearningPolicy;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.symbols.DoubleSymbolImpl;
import org.jsoar.kernel.symbols.IntegerSymbolImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.util.adaptables.Adaptables;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 *
 * <p>exploration.cpp
 *
 * @author ray
 */
public class Exploration {
  /**
   * kernel.h:147:ni_mode
   *
   * @author ray
   */
  public static enum NumericIndifferentMode {
    NUMERIC_INDIFFERENT_MODE_AVG("avg"),
    NUMERIC_INDIFFERENT_MODE_SUM("sum");

    private final String modeName;

    private NumericIndifferentMode(String modeName) {
      this.modeName = modeName;
    }

    public String getModeName() {
      return modeName;
    }

    public static NumericIndifferentMode findNumericIndifferentMode(String modeName) {
      for (NumericIndifferentMode p : values()) {
        if (p.modeName.equals(modeName)) {
          return p;
        }
      }
      return null;
    }
  }

  /**
   * Ways to Do User-Select
   *
   * <p>gsysparam.h:78:USER_SELECT_
   *
   * @author ray
   */
  public static enum Policy {
    USER_SELECT_BOLTZMANN("boltzmann"), /* boltzmann algorithm, with respect to temperature */
    USER_SELECT_E_GREEDY(
        "epsilon-greedy"), /* with probability epsilon choose random, otherwise greedy */
    USER_SELECT_FIRST("first"), /* just choose the first candidate item */
    USER_SELECT_LAST("last"), /* choose the last item   AGR 615 */
    USER_SELECT_RANDOM("random-uniform"), /* pick one at random */
    USER_SELECT_SOFTMAX(
        "softmax"); /* pick one at random, probabalistically biased by numeric preferences */

    private final String policyName;

    private Policy(String policyName) {
      this.policyName = policyName;
    }

    /**
     * exploration.cpp:50:exploration_convert_policy
     *
     * @return the policy name
     */
    public String getPolicyName() {
      return policyName;
    }

    /**
     * exploration.cpp:68:exploration_convert_policy
     *
     * @param policyName
     * @return the policy, or {@code null} if not found
     */
    public static Policy findPolicy(String policyName) {
      for (Policy p : values()) {
        if (p.policyName.equals(policyName)) {
          return p;
        }
      }
      return null;
    }
  }

  private final Agent context;
  private ReinforcementLearning rl;

  /** USER_SELECT_MODE_SYSPARAM */
  private Policy userSelectMode = Policy.USER_SELECT_SOFTMAX;
  /** USER_SELECT_REDUCE_SYSPARAM */
  private boolean autoUpdate = false;

  /**
   * agent.h:748:numeric_indifferent_mode
   *
   * <p>Initialized to NUMERIC_INDIFFERENT_MODE_SUM in create_soar_agent()
   */
  private NumericIndifferentMode numeric_indifferent_mode =
      NumericIndifferentMode.NUMERIC_INDIFFERENT_MODE_SUM;

  /**
   * Changed from array to map indexed by name
   *
   * <p>agent.h:752:exploration_params
   */
  private Map<String, ExplorationParameter> parameters =
      new HashMap<String, ExplorationParameter>();

  /** @param context */
  public Exploration(Agent context) {
    this.context = context;

    // exploration initialization
    // agent.cpp:307:create_agent
    exploration_add_parameter(0.1, new ExplorationValidateEpsilon(), "epsilon");
    exploration_add_parameter(25, new ExplorationValidateTemperature(), "temperature");
  }

  public void initialize() {
    this.rl = Adaptables.adapt(context, ReinforcementLearning.class);
  }

  /**
   * exploration.cpp:89:exploration_set_policy
   *
   * @param policy_name
   * @return true if the policy was set
   */
  public boolean exploration_set_policy(String policy_name) {
    Policy policy = Policy.findPolicy(policy_name);

    if (policy != null) return exploration_set_policy(policy);

    return false;
  }

  /**
   * exploration.cpp:99:exploration_set_policy
   *
   * @param policy
   * @return true if the policy was set
   */
  public boolean exploration_set_policy(Policy policy) {
    // TODO throw exception?
    if (policy != null) {
      userSelectMode = policy;
      return true;
    }

    return false;
  }

  /**
   * exploration.cpp:113:exploration_get_policy
   *
   * @return the current policy
   */
  public Policy exploration_get_policy() {
    return userSelectMode;
  }

  public boolean exploration_set_numeric_indifferent_mode(String mode_name) {
    NumericIndifferentMode mode = NumericIndifferentMode.findNumericIndifferentMode(mode_name);

    if (mode != null) return exploration_set_numeric_indifferent_mode(mode);

    return false;
  }

  public boolean exploration_set_numeric_indifferent_mode(NumericIndifferentMode mode) {
    // TODO throw exception?
    if (mode != null) {
      numeric_indifferent_mode = mode;
      return true;
    }

    return false;
  }

  public NumericIndifferentMode exploration_get_numeric_indifferent_mode() {
    return numeric_indifferent_mode;
  }

  /**
   * exploration.cpp:121:exploration_add_parameter
   *
   * @param value
   * @param val_func
   * @param name
   * @return the new parameter
   */
  public ExplorationParameter exploration_add_parameter(
      double value, ExplorationValueFunction val_func, String name) {
    // new parameter entry
    ExplorationParameter newbie = new ExplorationParameter();
    newbie.value = value;
    newbie.name = name;
    newbie.reduction_policy = ReductionPolicy.EXPLORATION_REDUCTION_EXPONENTIAL;
    newbie.val_func = val_func;
    newbie.rates.put(ReductionPolicy.EXPLORATION_REDUCTION_EXPONENTIAL, 1.0);
    newbie.rates.put(ReductionPolicy.EXPLORATION_REDUCTION_LINEAR, 0.0);

    parameters.put(name, newbie);

    return newbie;
  }

  /**
   * exploration.cpp:168:exploration_get_parameter_value
   *
   * @param parameter
   * @return value of the parameter
   */
  public double exploration_get_parameter_value(String parameter) {
    ExplorationParameter param = parameters.get(parameter);
    return param != null ? param.value : 0.0;
  }

  /**
   * exploration_valid_parameter
   *
   * @param name of the parameter
   * @return whether this parameter name is a valid exploration parameter
   */
  public boolean exploration_valid_parameter(String name) {
    ExplorationParameter param = parameters.get(name);
    if (param == null) return false;

    return true;
  }

  /**
   * exploration.cpp:204:exploration_valid_parameter_value
   *
   * @param name parameter name
   * @param value parameter value
   * @return true if the value is valid
   */
  public boolean exploration_valid_parameter_value(String name, double value) {
    ExplorationParameter param = parameters.get(name);
    if (param == null) return false;

    return param.val_func.call(value);
  }

  /**
   * exploration.cpp:213:exploration_valid_parameter_value
   *
   * @param parameter parameter object
   * @param value new value
   * @return true if the value is valid
   */
  boolean exploration_valid_parameter_value(ExplorationParameter parameter, double value) {
    if (parameter != null) {
      return parameter.val_func.call(value);
    }

    return false;
  }

  /**
   * exploration.cpp:224:exploration_set_parameter_value
   *
   * @param name parameter name
   * @param value new value
   * @return true if the parameter was set successfully
   */
  public boolean exploration_set_parameter_value(String name, double value) {
    ExplorationParameter param = parameters.get(name);
    if (param == null) return false;

    param.value = value;

    return true;
  }

  /**
   * exploration.cpp:235:exploration_set_parameter_value
   *
   * @param parameter the parameter object
   * @param value the new double value
   * @return true if the parameter was set successfully
   */
  boolean exploration_set_parameter_value(ExplorationParameter parameter, double value) {
    if (parameter != null) {
      parameter.value = value;
      return true;
    }
    return false;
  }

  /**
   * exploration.cpp:249:exploration_get_auto_update
   *
   * @return true if auto update is enabled
   */
  public boolean exploration_get_auto_update() {
    return autoUpdate;
  }

  /**
   * exploration.cpp:257:exploration_set_auto_update
   *
   * @param setting new auto update setting
   * @return true
   */
  public boolean exploration_set_auto_update(boolean setting) {
    this.autoUpdate = setting;

    return true;
  }

  /** exploration.cpp:267:exploration_update_parameters */
  public void exploration_update_parameters() {
    if (exploration_get_auto_update()) {
      for (ExplorationParameter p : parameters.values()) {
        p.update();
      }
    }
  }

  /**
   * exploration.cpp:322:exploration_get_reduction_policy
   *
   * @param parameter parameter name
   * @return the reudction policy
   */
  public ReductionPolicy exploration_get_reduction_policy(String parameter) {
    ExplorationParameter param = parameters.get(parameter);

    return param != null ? param.reduction_policy : null;
  }

  /**
   * exploration.cpp:331:exploration_get_reduction_policy
   *
   * @param parameter parameter object
   * @return reduction policy
   */
  ReductionPolicy exploration_get_reduction_policy(ExplorationParameter parameter) {
    return parameter != null ? parameter.reduction_policy : null;
  }

  public boolean exploration_valid_reduction_policy(String parameter, String policy_name) {
    ExplorationParameter param = parameters.get(parameter);
    if (param == null) {
      return false;
    }
    ReductionPolicy policy = ReductionPolicy.findPolicy(policy_name);

    if (policy == null) {
      return false;
    }

    return true;
  }

  /**
   * exploration:375:exploration_set_reduction_policy
   *
   * @param parameter parameter name
   * @param policy_name policy name
   * @return true if the reduction policy was set
   */
  public boolean exploration_set_reduction_policy(String parameter, String policy_name) {
    ExplorationParameter param = parameters.get(parameter);
    if (param == null) {
      return false;
    }
    ReductionPolicy policy = ReductionPolicy.findPolicy(policy_name);

    if (policy == null) {
      return false;
    }

    param.reduction_policy = policy;

    return true;
  }

  /**
   * exploration_get_reduction_rate
   *
   * @param parameter parameter name
   * @param policy_name policy name
   * @return the current reduction rate value (or 0.0 if something goes wrong)
   */
  public double exploration_get_reduction_rate(String parameter, String policy_name) {
    ExplorationParameter param = parameters.get(parameter);

    if (param == null) {
      return 0.0;
    }

    ReductionPolicy policy = ReductionPolicy.findPolicy(policy_name);
    if (policy == null) {
      return 0.0;
    }
    return param.getReductionRate(policy);
  }

  /**
   * exploration.cpp:468:exploration_set_reduction_rate
   *
   * @param parameter parameter name
   * @param policy_name policy name
   * @param reduction_rate reduction rate
   * @return true if the reduction rate was set
   */
  public boolean exploration_set_reduction_rate(
      String parameter, String policy_name, double reduction_rate) {
    ExplorationParameter param = parameters.get(parameter);
    if (param == null) {
      return false;
    }
    ReductionPolicy policy = ReductionPolicy.findPolicy(policy_name);
    if (policy == null) {
      return false;
    }
    return param.setReductionRate(policy, reduction_rate);
  }

  /**
   * exploration.cpp:497:exploration_choose_according_to_policy
   *
   * @param s the slot
   * @param candidates list of preference candidates, using {@link Preference#next_candidate}
   * @return the chosen preference
   */
  public Preference exploration_choose_according_to_policy(Slot s, Preference candidates) {
    Policy exploration_policy = exploration_get_policy();

    // get preference values for each candidate
    for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
      exploration_compute_value_of_candidate(cand, s, 0.0);

    final boolean my_rl_enabled = rl.rl_enabled();
    final LearningPolicy my_learning_policy =
        my_rl_enabled
            ? context.getProperties().get(ReinforcementLearningParams.LEARNING_POLICY)
            : LearningPolicy.q;
    double top_value = candidates.numeric_value;
    boolean top_rl = candidates.rl_contribution;

    // should find highest valued candidate in q-learning
    if (my_rl_enabled && my_learning_policy == ReinforcementLearningParams.LearningPolicy.q) {
      for (Preference cand = candidates; cand != null; cand = cand.next_candidate) {
        if (cand.numeric_value > top_value) {
          top_value = cand.numeric_value;
          top_rl = cand.rl_contribution;
        }
      }
    }

    Preference return_val = null;
    switch (exploration_policy) {
      case USER_SELECT_FIRST:
        return_val = candidates;
        break;

      case USER_SELECT_LAST:
        for (return_val = candidates;
            return_val.next_candidate != null;
            return_val = return_val.next_candidate)
          ;
        break;

      case USER_SELECT_RANDOM:
        return_val = exploration_randomly_select(candidates);
        break;

      case USER_SELECT_SOFTMAX:
        return_val = exploration_probabilistically_select(candidates);
        break;

      case USER_SELECT_E_GREEDY:
        return_val = exploration_epsilon_greedy_select(candidates);
        break;

      case USER_SELECT_BOLTZMANN:
        return_val = exploration_boltzmann_select(candidates);
        break;
    }

    // should perform update here for chosen candidate in sarsa
    // should perform update here for chosen candidate in sarsa
    if (my_rl_enabled) {
      rl.rl_tabulate_reward_values();

      if (my_learning_policy == ReinforcementLearningParams.LearningPolicy.sarsa) {
        rl.rl_perform_update(return_val.numeric_value, return_val.rl_contribution, s.id);
      } else if (my_learning_policy == ReinforcementLearningParams.LearningPolicy.q) {
        rl.rl_perform_update(top_value, top_rl, s.id);

        if (return_val.numeric_value != top_value) ReinforcementLearning.rl_watkins_clear(s.id);
      }
    }

    return return_val;
  }

  /** exploration.cpp:557:exploration_randomly_select */
  private Preference exploration_randomly_select(Preference candidates) {
    // select at random
    int cand_count = Preference.countCandidates(candidates);
    int chosen_num = context.getRandom().nextInt(cand_count);
    // chosen_num = SoarRandInt( cand_count - 1 );

    return Preference.getCandidate(candidates, chosen_num);
  }

  /** exploration.cpp:582:exploration_probabilistically_select */
  private Preference exploration_probabilistically_select(Preference candidates) {
    double total_probability = 0;

    // count up positive numbers
    for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
      if (cand.numeric_value > 0) total_probability += cand.numeric_value;

    // if nothing positive, resort to random
    if (total_probability == 0) return exploration_randomly_select(candidates);

    // choose a random preference within the distribution
    double rn = context.getRandom().nextDouble(); // SoarRand();
    double selected_probability = rn * total_probability;
    double current_sum = 0;

    // select the candidate based upon the chosen preference
    for (Preference cand = candidates; cand != null; cand = cand.next_candidate) {
      if (cand.numeric_value > 0) {
        current_sum += cand.numeric_value;
        if (selected_probability <= current_sum) return cand;
      }
    }

    return null;
  }

  /*
   * Select a candidate whose Q-value is Q_i with probability
   *
   * e^(Q_i / t) / sum(j=1 to n, e^(Q_j / t)).
   *
   * Since Q values can get very large or very small (negative values),
   * overflow and underflow problems can occur when calculating the
   * exponentials. This is avoided by subtracting a constant k from
   * all exponent values involved. This doesn't affect the actual
   * probabilities with which candidates are chosen, because subtracting
   * a constant from an exponent is equivalent to dividing by the base
   * raised to that constant, and the divisors cancel out during the
   * calculation.
   *
   * k is chosen to be Q_max / t. This means that the values of all
   * numerator exponentials are at most 1, and the value of the sum in the
   * denominator is between 1 and n. This gets rid of the overflow problem
   * completely, and in the cases where underflow will occur, the actual
   * probability of the action being considered will be so small (< 10^-300)
   * that it's negligible.
   *
   * <p>exploration.cpp:621:exploration_boltzmann_select
   */
  Preference exploration_boltzmann_select(Preference candidates) {
    double t = exploration_get_parameter_value("temperature");
    double maxq;
    Preference c;

    maxq = candidates.numeric_value;
    for (c = candidates.next_candidate; c != null; c = c.next_candidate) {
      if (maxq < c.numeric_value) maxq = c.numeric_value;
    }

    double exptotal = 0.0;
    List<Double> expvals = new LinkedList<Double>();

    for (c = candidates; c != null; c = c.next_candidate) {
      // equivalent to exp((c.numeric_value / t) - (maxq / t)) but safer against overflow
      double v = Math.exp((c.numeric_value - maxq) / t);
      expvals.add(v);
      exptotal += v;
    }

    // output trace information
    final Trace trace = context.getTrace();
    if (trace.isEnabled(Category.INDIFFERENT)) {
      ListIterator<Double> i = expvals.listIterator();
      for (c = candidates; c != null; c = c.next_candidate) {
        double prob = i.next() / exptotal;
        trace.print("\n Candidate %s:  ", c.value);
        trace.print("Value (Sum) = %f, (Prob) = %f", c.numeric_value, prob);
        //                            xml_begin_tag( my_agent, kTagCandidate );
        //                            xml_att_val( my_agent, kCandidateName, c.value );
        //                            xml_att_val( my_agent, kCandidateType, kCandidateTypeSum );
        //                            xml_att_val( my_agent, kCandidateValue, c.numeric_value );
        //                            xml_att_val( my_agent, kCandidateExpValue, prob );
        //                            xml_end_tag( my_agent, kTagCandidate );
      }
    }

    double r = context.getRandom().nextDouble() * exptotal;
    double sum = 0.0;

    ListIterator<Double> i = expvals.listIterator();
    for (c = candidates, i = expvals.listIterator(); c != null; c = c.next_candidate) {
      sum += i.next();
      if (sum >= r) return c;
    }

    return null;
  }

  /** exploration.cpp:723:exploration_epsilon_greedy_select */
  private Preference exploration_epsilon_greedy_select(Preference candidates) {
    // TODO this seems weird
    double epsilon =
        exploration_get_parameter_value("epsilon" /* (const long) EXPLORATION_PARAM_EPSILON */);

    final Trace trace = context.getTrace();
    if (trace.isEnabled(Category.INDIFFERENT)) {
      for (Preference cand = candidates; cand != null; cand = cand.next_candidate) {
        trace.print("\n Candidate %s:  Value (Sum) = %f", cand.value, cand.numeric_value);
      }
    }

    if (context.getRandom().nextDouble() /*SoarRand()*/ < epsilon)
      return exploration_randomly_select(candidates);
    else return exploration_get_highest_q_value_pref(candidates);
  }

  /** exploration.cpp:752:exploration_get_highest_q_value_pref */
  private Preference exploration_get_highest_q_value_pref(Preference candidates) {
    Preference top_cand = candidates;
    double top_value = candidates.numeric_value;
    int num_max_cand = 0;

    for (Preference cand = candidates; cand != null; cand = cand.next_candidate) {
      if (cand.numeric_value > top_value) {
        top_value = cand.numeric_value;
        top_cand = cand;
        num_max_cand = 1;
      } else if (cand.numeric_value == top_value) num_max_cand++;
    }

    if (num_max_cand == 1) return top_cand;
    else {
      // if operators tied for highest Q-value, select among tied set at random
      int chosen_num =
          context.getRandom().nextInt(num_max_cand); //  SoarRandInt( num_max_cand - 1 );

      Preference cand = candidates;
      while (cand.numeric_value != top_value) cand = cand.next_candidate;

      while (chosen_num != 0) {
        cand = cand.next_candidate;
        chosen_num--;

        while (cand.numeric_value != top_value) cand = cand.next_candidate;
      }

      return cand;
    }
  }

  /**
   * exploration.cpp:798:exploration_compute_value_of_candidate
   *
   * @param cand candidate preference
   * @param s the slot
   * @param default_value default value to use (Defaults to 0.0 in CSoar)
   */
  public void exploration_compute_value_of_candidate(
      Preference cand, Slot s, double default_value) {
    if (cand == null) return;

    // initialize candidate values
    cand.total_preferences_for_candidate = 0;
    cand.numeric_value = 0;
    cand.rl_contribution = false;

    // all numeric indifferents
    for (Preference pref = s.getPreferencesByType(PreferenceType.NUMERIC_INDIFFERENT);
        pref != null;
        pref = pref.next) {
      if (cand.value == pref.value) {
        cand.total_preferences_for_candidate += 1;
        cand.numeric_value += get_number_from_symbol(pref.referent);

        if (pref.inst.prod.rlRuleInfo != null) {
          cand.rl_contribution = true;
        }
      }
    }

    // all binary indifferents
    for (Preference pref = s.getPreferencesByType(PreferenceType.BINARY_INDIFFERENT);
        pref != null;
        pref = pref.next) {
      if (cand.value == pref.value) {
        cand.total_preferences_for_candidate += 1;
        cand.numeric_value += get_number_from_symbol(pref.referent);
      }
    }

    // if no contributors, provide default
    if (cand.total_preferences_for_candidate == 0) {
      cand.numeric_value = default_value;
      cand.total_preferences_for_candidate = 1;
    }

    // accomodate average mode
    if (numeric_indifferent_mode == NumericIndifferentMode.NUMERIC_INDIFFERENT_MODE_AVG)
      cand.numeric_value = cand.numeric_value / cand.total_preferences_for_candidate;
  }

  /**
   * misc.cpp:34:get_number_from_symbol
   *
   * @param s the symbol
   * @return the double value of the symbol if it is numeric
   */
  public static double get_number_from_symbol(SymbolImpl s) {
    DoubleSymbolImpl f = s.asDouble();
    if (f != null) {
      return f.getValue();
    }
    IntegerSymbolImpl i = s.asInteger();
    if (i != null) {
      return i.getValue();
    }
    return 0.0;
  }
}
