/*
 * Copyright (c) 2013 Bob Marinier <marinier@gmail.com>
 *
 * Created on Feb 8, 2013
 */

package org.jsoar.kernel.wma;

import org.jsoar.util.properties.BooleanPropertyProvider;
import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.EnumPropertyProvider;
import org.jsoar.util.properties.IntegerPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

/**
 * <p>wma.h:62:wma_param_container
 * @author bob.marinier
 *
 */
public class DefaultWorkingMemoryActivationParams
{
    static enum ForgettingChoices { off, naive, bsearch, approx };
    static enum ForgetWmeChoices { all, lti };
    static enum TimerLevels { off, one };
    
    private static final String PREFIX = "wma.params.";
    
    /**
     * Retrieve a property key for a WMA property. Appropriately adds necessary
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
    
    private static <T> PropertyKey.Builder<T> key(String name, Class<T> type)
    {
        return PropertyKey.builder(PREFIX + name, type);
    }
    static final PropertyKey<Boolean> ACTIVATION = key("activation", Boolean.class).defaultValue(false).build();
    public final BooleanPropertyProvider activation = new BooleanPropertyProvider(ACTIVATION);
    
    static final PropertyKey<Double> DECAY_RATE = key("decay_rate", Double.class).defaultValue(-0.5).build();
    public final DefaultPropertyProvider<Double> decay_rate = new DefaultPropertyProvider<Double>(DECAY_RATE);
    
    static final PropertyKey<Double> DECAY_THRESH = key("decay_thresh", Double.class).defaultValue(-2.0).build();
    public final DefaultPropertyProvider<Double> decay_thresh = new DefaultPropertyProvider<Double>(DECAY_THRESH);
    
    static final PropertyKey<Boolean> PETROV_APPROX = key("petrov_approx", Boolean.class).defaultValue(false).build();
    public final BooleanPropertyProvider petrov_approx = new BooleanPropertyProvider(PETROV_APPROX);
    
    static final PropertyKey<ForgettingChoices> FORGETTING_CHOICES = key("forgetting_choices", ForgettingChoices.class).defaultValue(ForgettingChoices.off).build();
    public final EnumPropertyProvider<ForgettingChoices> forgetting = new EnumPropertyProvider<ForgettingChoices>(FORGETTING_CHOICES);
    
    static final PropertyKey<ForgetWmeChoices> FORGET_WME_CHOICES = key("forget_wme_choices", ForgetWmeChoices.class).defaultValue(ForgetWmeChoices.all).build();
    public final EnumPropertyProvider<ForgetWmeChoices> forget_wme = new EnumPropertyProvider<ForgetWmeChoices>(FORGET_WME_CHOICES);
    
    static final PropertyKey<Boolean> FAKE_FORGETTING = key("fake_forgetting", Boolean.class).defaultValue(false).build();
    public final BooleanPropertyProvider fake_forgetting = new BooleanPropertyProvider(FAKE_FORGETTING);
    
    
    /**
     *  performance
     */
    static final PropertyKey<TimerLevels> TIMERS = key("timers", TimerLevels.class).defaultValue(TimerLevels.off).build();
    public final EnumPropertyProvider<TimerLevels> timers = new EnumPropertyProvider<TimerLevels>(TIMERS);
    
        
    static final PropertyKey<Integer> MAX_POW_CACHE = key("max_pow_cache", Integer.class).defaultValue(10).build();
    public final IntegerPropertyProvider max_pow_cache = new IntegerPropertyProvider(MAX_POW_CACHE);
    
    
    private final PropertyManager properties;
    
    public DefaultWorkingMemoryActivationParams(PropertyManager properties)
    {
        this.properties = properties;
        
        properties.setProvider(ACTIVATION, activation);
        properties.setProvider(DECAY_RATE, decay_rate);
        properties.setProvider(DECAY_THRESH, decay_thresh);
        properties.setProvider(PETROV_APPROX, petrov_approx);
        properties.setProvider(FORGETTING_CHOICES, forgetting);
        properties.setProvider(FORGET_WME_CHOICES, forget_wme);
        properties.setProvider(FAKE_FORGETTING, fake_forgetting);
        properties.setProvider(TIMERS, timers);
        properties.setProvider(MAX_POW_CACHE, max_pow_cache);
    }
    
    public PropertyManager getProperties()
    {
        return properties;
    }
}
