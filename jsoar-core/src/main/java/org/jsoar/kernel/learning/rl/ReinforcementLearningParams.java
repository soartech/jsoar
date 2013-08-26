/*
 * Created by Peter Lindes, 14 August 2013
 *
 */
package org.jsoar.kernel.learning.rl;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.EnumPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

/**
 * @author Peter Lindes
 */
public class ReinforcementLearningParams
{
    /**
     * Options to turn RL on and off
     */
    static enum Learning { on, off };
    
    /**
     * Options for temporal-extension
     */
    static enum TemporalExtension { on, off };
    
    /**
     * Options for RL algorithm learning policy
     */
    public static enum LearningPolicy { sarsa, q };
    
    /**
     * Options to turn hrl-discount on and off
     */
    public static enum HrlDiscount { on, off };
    
    /**
     * Options for temporal-discount
     */
    static enum TemporalDiscount { on, off };
    
    /**
     * Options for chunk-stop
     */
    public static enum ChunkStop { on, off };
    
    /**
     * How the learning rate cools over time.
     * normal_decay: default, same learning rate for each rule
     * exponential_decay: rate = rate / # updates for this rule
     * logarithmic_decay: rate = rate / log(# updates for this rule)
     * Miller, 11/14/2011
     */
    public static enum DecayMode { normal_decay, exponential_decay,
    							logarithmic_decay, delta_bar_delta_decay }
    
    /**
     * Options for meta
     */
    public static enum Meta { on, off };

    /**
     * Options for apoptosis
     */
    static enum ApoptosisChoices { apoptosis_none, apoptosis_chunks, apoptosis_rl };
    
    
    /**
     * A class to held a documentation parameter
     * and its  value.
     */
//    public class DocParam<T> {
//    	private String name;
//    	private PropertyKey<T> key;
//    	private T value;
//    	
//    	public DocParam(String name, PropertyKey<T> key, T value) {
//    		this.name = name;
//    		this.key = key;
//    		this.value = value;
//    	}
//    	
//    	public T get() {
//			return value;
//    	}
//    	
//    	public void set(Production prod, String value_str) {
//    		T value = (T) value_str;
//            props.set(key, value);
//    	}
//    }
    
    private static final String PREFIX = "rl.";
    
    private static <T> PropertyKey.Builder<T> key(String name, Class<T> type)
    {
        return PropertyKey.builder(PREFIX + name, type);
    }
    
    public static final PropertyKey<Learning> LEARNING = key("learning", Learning.class).defaultValue(Learning.off).build();
    final EnumPropertyProvider<Learning> learning = new EnumPropertyProvider<Learning>(LEARNING);

    public static final PropertyKey<TemporalExtension> TEMPORAL_EXTENSION = key("temporal-extension", TemporalExtension.class).defaultValue(TemporalExtension.on).build();
    final EnumPropertyProvider<TemporalExtension> temporal_extension = new EnumPropertyProvider<TemporalExtension>(TEMPORAL_EXTENSION);
    
    public static final PropertyKey<Double> DISCOUNT_RATE = key("discount-rate", Double.class).defaultValue(0.9).build();
    final DefaultPropertyProvider<Double> discount_rate = new DefaultPropertyProvider<Double>(DISCOUNT_RATE);

    public static final PropertyKey<LearningPolicy> LEARNING_POLICY = key("learning-policy", LearningPolicy.class).defaultValue(LearningPolicy.sarsa).build();
    final DefaultPropertyProvider<LearningPolicy> learning_policy = new DefaultPropertyProvider<LearningPolicy>(LEARNING_POLICY);
    
    public static final PropertyKey<Double> LEARNING_RATE = key("learning-rate", Double.class).defaultValue(0.3).build();
    final DefaultPropertyProvider<Double> learning_rate = new DefaultPropertyProvider<Double>(LEARNING_RATE);

    public static final PropertyKey<HrlDiscount> HRL_DISCOUNT = key("hrl-discount", HrlDiscount.class).defaultValue(HrlDiscount.off).build();
    final EnumPropertyProvider<HrlDiscount> hrl_discount = new EnumPropertyProvider<HrlDiscount>(HRL_DISCOUNT);

    public static final PropertyKey<TemporalDiscount> TEMPORAL_DISCOUNT = key("temporal-discount", TemporalDiscount.class).defaultValue(TemporalDiscount.on).build();
    final EnumPropertyProvider<TemporalDiscount> temporal_discount = new EnumPropertyProvider<TemporalDiscount>(TEMPORAL_DISCOUNT);
    
    public static final PropertyKey<Double> ET_DECAY_RATE = key("eligibility-trace-decay-rate", Double.class).defaultValue(0.0).build();
    final DefaultPropertyProvider<Double> et_decay_rate = new DefaultPropertyProvider<Double>(ET_DECAY_RATE);
    
    public static final PropertyKey<Double> ET_TOLERANCE = key("eligibility-trace-tolerance", Double.class).defaultValue(0.001).build();
    final DefaultPropertyProvider<Double> et_tolerance = new DefaultPropertyProvider<Double>(ET_TOLERANCE);
    
    //	--------------	EXPERIMENTAL	-------------------

    public static final PropertyKey<ChunkStop> CHUNK_STOP = key("chunk-stop", ChunkStop.class).defaultValue(ChunkStop.on).build();
    //	This is public so the rete can get it
    public final EnumPropertyProvider<ChunkStop> chunk_stop = new EnumPropertyProvider<ChunkStop>(CHUNK_STOP);
    
    public static final PropertyKey<DecayMode> DECAY_MODE = key("decay-mode", DecayMode.class).defaultValue(DecayMode.normal_decay).build();
    final EnumPropertyProvider<DecayMode> decay_mode = new EnumPropertyProvider<DecayMode>(DECAY_MODE);

    // Whether doc strings are used for storing metadata.
    public static final PropertyKey<Meta> META = key("meta", Meta.class).defaultValue(Meta.off).build();
    final EnumPropertyProvider<Meta> meta = new EnumPropertyProvider<Meta>(META);
    
    public static final PropertyKey<Double> META_LEARNING_RATE = key("meta-learning-rate", Double.class).defaultValue(0.1).build();
    final DefaultPropertyProvider<Double> meta_learning_rate = new DefaultPropertyProvider<Double>(META_LEARNING_RATE);
    
    // If non-null and size > 0, log all RL updates to this file.
    public static final PropertyKey<String> UPDATE_LOG_PATH = key("update-log-path", String.class).defaultValue("").build();
    final DefaultPropertyProvider<String> update_log_path = new DefaultPropertyProvider<String>(UPDATE_LOG_PATH);
    
    private final PropertyManager properties;

    public ReinforcementLearningParams(PropertyManager properties, SymbolFactory sf)
    {
        this.properties = properties;
        
        // rl initialization
        // agent.cpp:328:create_soar_agent
        properties.setProvider(LEARNING, learning);
        properties.setProvider(TEMPORAL_EXTENSION, temporal_extension);
        properties.setProvider(DISCOUNT_RATE, discount_rate);
        properties.setProvider(LEARNING_POLICY, learning_policy);
        properties.setProvider(LEARNING_RATE, learning_rate);
        properties.setProvider(HRL_DISCOUNT, hrl_discount);
        properties.setProvider(TEMPORAL_DISCOUNT, temporal_discount);
        properties.setProvider(ET_DECAY_RATE, et_decay_rate);
        properties.setProvider(ET_TOLERANCE, et_tolerance);
        
        //	--------------	EXPERIMENTAL	-------------------
        properties.setProvider(CHUNK_STOP, chunk_stop);
        properties.setProvider(DECAY_MODE, decay_mode);
        properties.setProvider(META, meta);
        properties.setProvider(META_LEARNING_RATE, meta_learning_rate);
        properties.setProvider(UPDATE_LOG_PATH, update_log_path);
    }

    public PropertyManager getProperties()
    {
        return properties;
    }

    /**
     * Retrieve a property key for an EPMEM property. Appropriately adds necessary
     * prefixes to the name to find the right key.
     * 
     * @param props the property manager
     * @param name the name of the property.
     * @return the key, or {@code null} if not found.
     */
    public static PropertyKey<?> getProperty(PropertyManager props, String name)
    {
        return props.getKey(PREFIX + name);
    }
    
    /**
     * Get a list of "documentation" parameters and their values
     * 
     * reinforcement_learning.cpp:45:get_documentation_params
     * (9.3.3+)
     * 
     * @return a list of pairs
     */
    
    public void get_documentation_params() {

    }
}
