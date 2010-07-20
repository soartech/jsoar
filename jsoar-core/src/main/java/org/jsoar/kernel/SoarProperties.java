/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.runtime.WaitInfo;
import org.jsoar.runtime.WaitRhsFunction;
import org.jsoar.util.properties.PropertyKey;

/**
 * Declaration of per-agent Soar configuration properties.
 * 
 * @author ray
 */
public class SoarProperties
{
    // TODO Reinforcement Learning
    // TODO Exploration
    
    /**
     * Property which stores the name of an agent.
     * 
     * @see Agent#getName()
     */
    public static final PropertyKey<String> NAME = PropertyKey.builder("name", String.class).build();
    
    /**
     * agent.h:740:waitsnc
     * @see Decider
     */
    public static final PropertyKey<Boolean> WAITSNC = PropertyKey.builder("waitsnc", Boolean.class).defaultValue(false).build();
    
    /**
     * <p>gsysparams.h::MAX_NIL_OUTPUT_CYCLES_SYSPARAM
     * @see DecisionCycle
     */
    public static final PropertyKey<Integer> MAX_NIL_OUTPUT_CYCLES = PropertyKey.builder("max-nil-output-cycles", Integer.class).defaultValue(15).build();
    
    /**
     * <p>gsysparam.h:MAX_ELABORATIONS_SYSPARAM
     * 
     * @see Consistency
     */
    public static final PropertyKey<Integer> MAX_ELABORATIONS = PropertyKey.builder("max-elaborations", Integer.class).defaultValue(100).build();
    
    /**
     * <p>gsysparam.h:164:MAX_GOAL_DEPTH
     * <p>Defaults to 100 in init_soar()
     * @see Decider
     */
    public static final PropertyKey<Integer> MAX_GOAL_DEPTH = PropertyKey.builder("max-goal-depth", Integer.class).defaultValue(100).build();

    
    /**
     * <p>gsysparam.h:179:CHUNK_THROUGH_LOCAL_NEGATIONS_SYSPARAM
     * <p>Defaults to true in init_soar()
     */
    public static final PropertyKey<Boolean> CHUNK_THROUGH_LOCAL_NEGATIONS = PropertyKey.builder("chunk-through-local-negations", Boolean.class).defaultValue(true).build();
    
    /**
     * <p>gsysparam.h:143:USE_LONG_CHUNK_NAMES
     * <p>Defaults to true in init_soar() 
     */
    public static final PropertyKey<Boolean> USE_LONG_CHUNK_NAMES = PropertyKey.builder("use-long-chunk-names", Boolean.class).defaultValue(true).build();
    
    /**
     * <p>agent.h:535:chunk_name_prefix
     * <p>Defaults to "chunk" in init_soar()
     */
    public static final PropertyKey<String> CHUNK_NAME_PREFIX = PropertyKey.builder("chunk-name-prefix", String.class).defaultValue("chunk").build();
    
    /**
     * <p>gsysparam.h:123:LEARNING_ON_SYSPARAM
     * <p>Defaults to false in init_soar()
     */
    public static final PropertyKey<Boolean> LEARNING_ON = PropertyKey.builder("learning-on", Boolean.class).defaultValue(false).build();
    
    /**
     * <p>gsysparam.h:126:LEARNING_ALL_GOALS_SYSPARAM
     * <p>Defaults to true in init_soar()
     */
    public static final PropertyKey<Boolean> LEARNING_ALL_GOALS = PropertyKey.builder("learning-all-goals", Boolean.class).defaultValue(true).build();
    /**
     * <p>gsysparam.h:125:LEARNING_EXCEPT_SYSPARAM
     * <p>Defaults to false in init_soar
     */
    public static final PropertyKey<Boolean> LEARNING_EXCEPT = PropertyKey.builder("learning-except", Boolean.class).defaultValue(false).build();
    
    /**
     * <p>gsysparam.h:124:LEARNING_ONLY_SYSPARAM
     * <p>Defaults to false in init_soar
     */
    public static final PropertyKey<Boolean> LEARNING_ONLY = PropertyKey.builder("learning-only", Boolean.class).defaultValue(false).build();
    
    /**
     * <p>gsysparam.h:140:EXPLAIN_SYSPARAM
     * <p>Defaults to false in init_soar()
     */
    public static final PropertyKey<Boolean> EXPLAIN = PropertyKey.builder("explain", Boolean.class).defaultValue(false).build();
    
    /**
     * agent.h:687:o_support_calculation_type
     */
    public static final PropertyKey<Integer> O_SUPPORT_MODE = PropertyKey.builder("o-support-mode", Integer.class).defaultValue(4).build();
    
    public static final PropertyKey<Phase> CURRENT_PHASE = PropertyKey.builder("current-phase", Phase.class).defaultValue(Phase.INPUT).build();
    /**
     * Property containing the current stop phase
     * 
     * @see AgentRunController#setStopPhase(Phase)
     */
    public static final PropertyKey<Phase> STOP_PHASE = PropertyKey.builder("stop-phase", Phase.class).defaultValue(Phase.INPUT).build();
    
    /**
     * <p>agent.h::d_cycle_count
     * 
     * @see DecisionCycle
     */
    public static final PropertyKey<Long> D_CYCLE_COUNT = counter("d_cycle_count");
    
    /**
     * <p>agent.h::e_cycle_count
     * 
     * @see DecisionCycle
     */
    public static final PropertyKey<Long> E_CYCLE_COUNT = counter("e_cycle_count");

    /**
     * <p>agent.h::decision_phases_count
     * 
     * @see DecisionCycle
     */
    public static final PropertyKey<Long> DECISION_PHASES_COUNT = counter("decision_phases_count");

    /**
     * <p>agent.h::inner_e_cycle_count
     * 
     * @see DecisionCycle
     */
    public static final PropertyKey<Long> INNER_E_CYCLE_COUNT = counter("inner_e_cycle_count");

    /**
     * <p>agent.h::pe_cycle_count
     * 
     * @see DecisionCycle
     */
    public static final PropertyKey<Long> PE_CYCLE_COUNT = counter("pe_cycle_count");

    /**
     * <p>agent.h::gent.h:367:production_firing_count
     * 
     * @see RecognitionMemory
     */
    public static final PropertyKey<Long> PRODUCTION_FIRING_COUNT = counter("production_firing_count");

    /**
     * <p>agent.h::wme_addition_count
     * 
     * @see WorkingMemory
     */
    public static final PropertyKey<Long> WME_ADDITION_COUNT = counter("wme_addition_count");
    
    /**
     * <p>agent.h::wme_removal_count
     * 
     * @see WorkingMemory
     */
    public static final PropertyKey<Long> WME_REMOVAL_COUNT = counter("wme_removal_count");

    /**
     * <p>agent.h::max_wm_size
     * 
     * @see WorkingMemory
     */
    public static final PropertyKey<Long> MAX_WM_SIZE = counter("max_wm_size");
    
    /**
     * <p>agent.h::cumulative_wm_size
     * 
     * @see WorkingMemory
     */
    public static final PropertyKey<Long> CUMULATIVE_WM_SIZE = counter("cumulative_wm_size");
    
    /**
     * <p>agent.h::num_wm_sizes_accumulated
     * 
     * @see WorkingMemory
     */
    public static final PropertyKey<Long> NUM_WM_SIZES_ACCUMULATED = counter("num_wm_sizes_accumulated");
    
    /**
     * True if the agent is currently running. This property is not set on a raw 
     * {@link Agent}. It is only set by higher-level run controllers such as 
     * {@link ThreadedAgent}.
     * 
     * @see ThreadedAgent
     */
    public static final PropertyKey<Boolean> IS_RUNNING = PropertyKey.builder("is_running", Boolean.class).readonly(true).boundable(true).build();
    
    /**
     * Current agent wait state info, e.g. set by {@link WaitRhsFunction}
     */
    public static final PropertyKey<WaitInfo> WAIT_INFO = PropertyKey.builder("wait_info", WaitInfo.class).readonly(true).defaultValue(WaitInfo.NOT_WAITING).build();
    
    private static PropertyKey<Long> counter(String name)
    {
        return PropertyKey.builder(name, Long.class).boundable(false).readonly(true).defaultValue(0L).build();
    }
}
