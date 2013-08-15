/*
 * Created by Peter Lindes, 14 August 2013
 *
 */
package org.jsoar.kernel.learning.rl;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.properties.BooleanPropertyProvider;
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
     * Options for RL algorithm
     */
    public static enum LearningPolicy { sarsa, q };
    
    /**
     * Options to turn hrl-discount on and off
     */
    static enum HrlDiscount { on, off };
    
    /**
     * How the learning rate cools over time.
     * normal_decay: default, same learning rate for each rule
     * exponential_decay: rate = rate / # updates for this rule
     * logarithmic_decay: rate = rate / log(# updates for this rule)
     * Miller, 11/14/2011
     */
    static enum Decay { normal_decay, exponential_decay,
    							logarithmic_decay, delta_bar_delta_decay }
    /**
     * Options for apoptosis
     */
    static enum ApoptosisChoices { apoptosis_none, apoptosis_chunks, apoptosis_rl };
    
    private static final String PREFIX = "rl.";
    
    private static <T> PropertyKey.Builder<T> key(String name, Class<T> type)
    {
        return PropertyKey.builder(PREFIX + name, type);
    }
    
    public static final PropertyKey<Learning> LEARNING = key("learning", Learning.class).defaultValue(Learning.off).build();
    final EnumPropertyProvider<Learning> learning = new EnumPropertyProvider<Learning>(LEARNING);
    
    public static final PropertyKey<Double> DISCOUNT_RATE = key("discount-rate", Double.class).defaultValue(0.9).build();
    final DefaultPropertyProvider<Double> discount_rate = new DefaultPropertyProvider<Double>(DISCOUNT_RATE);
    
    public static final PropertyKey<Double> LEARNING_RATE = key("learning-rate", Double.class).defaultValue(0.3).build();
    final DefaultPropertyProvider<Double> learning_rate = new DefaultPropertyProvider<Double>(LEARNING_RATE);

    public static final PropertyKey<LearningPolicy> LEARNING_POLICY = key("learning-policy", LearningPolicy.class).defaultValue(LearningPolicy.sarsa).build();
    final DefaultPropertyProvider<LearningPolicy> learning_policy = new DefaultPropertyProvider<LearningPolicy>(LEARNING_POLICY);
    
    public static final PropertyKey<Double> ET_DECAY_RATE = key("eligibility-trace-decay-rate", Double.class).defaultValue(0.0).build();
    final DefaultPropertyProvider<Double> et_decay_rate = new DefaultPropertyProvider<Double>(ET_DECAY_RATE);
    
    public static final PropertyKey<Double> ET_TOLERANCE = key("eligibility-trace-tolerance", Double.class).defaultValue(0.001).build();
    final DefaultPropertyProvider<Double> et_tolerance = new DefaultPropertyProvider<Double>(ET_TOLERANCE);

    public static final PropertyKey<Boolean> TEMPORAL_EXTENSION = key("temporal-extension", Boolean.class).defaultValue(true).build();
    final BooleanPropertyProvider temporal_extension = new BooleanPropertyProvider(TEMPORAL_EXTENSION);

    public static final PropertyKey<HrlDiscount> HRL_DISCOUNT = key("hrl-discount", HrlDiscount.class).defaultValue(HrlDiscount.on).build();
    final EnumPropertyProvider<HrlDiscount> hrl_discount = new EnumPropertyProvider<HrlDiscount>(HRL_DISCOUNT);

    public static final PropertyKey<Boolean> TEMPORAL_DISCOUNT = key("temporal-discount", Boolean.class).defaultValue(true).build();
    final BooleanPropertyProvider temporal_discount = new BooleanPropertyProvider(TEMPORAL_DISCOUNT);
    
    private final PropertyManager properties;

    public ReinforcementLearningParams(PropertyManager properties, SymbolFactory sf)
    {
        this.properties = properties;
        
        // rl initialization
        // agent.cpp:328:create_soar_agent
        properties.setProvider(LEARNING, learning);
        properties.setProvider(DISCOUNT_RATE, discount_rate);
        properties.setProvider(LEARNING_RATE, learning_rate);
        properties.setProvider(LEARNING_POLICY, learning_policy);
        properties.setProvider(ET_DECAY_RATE, et_decay_rate);
        properties.setProvider(ET_TOLERANCE, et_tolerance);
        properties.setProvider(TEMPORAL_EXTENSION, temporal_extension);
        properties.setProvider(TEMPORAL_DISCOUNT, temporal_discount);
        properties.setProvider(HRL_DISCOUNT, hrl_discount);
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
}
