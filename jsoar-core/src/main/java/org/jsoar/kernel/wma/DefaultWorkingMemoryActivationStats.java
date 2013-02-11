/*
 * Copyright (c) 2013 Bob Marinier <marinier@gmail.com>
 *
 * Created on Feb 9, 2013
 */
package org.jsoar.kernel.wma;

import java.util.HashSet;
import java.util.Set;

import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.properties.PropertyProvider;

/**
 * 
 * <p>wma.h:114:wma_stat_container
 * <p>wma.cpp:163:wma_stat_container
 * @author bob.marinier
 */
public class DefaultWorkingMemoryActivationStats implements WorkingMemoryActivationStatistics
{
    private static final String PREFIX = "wma.stats.";
    
    /**
     * Retrieve a property key for an SMEM property. Appropriately adds necessary
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

    static final PropertyKey<Long> FORGOTTEN_WMES = key("forgotten_wmes", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> forgotten_wmes = new DefaultPropertyProvider<Long>(FORGOTTEN_WMES);
    
    private final PropertyManager properties;
    private final Set<PropertyKey<?>> keys = new HashSet<PropertyKey<?>>();
    
    public DefaultWorkingMemoryActivationStats(PropertyManager properties)
    {
        this.properties = properties;
        add(FORGOTTEN_WMES, forgotten_wmes);
    }

    private <T> void add(PropertyKey<T> key, PropertyProvider<T> value)
    {
        this.properties.setProvider(key, value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void reset()
    {
        for(PropertyKey key : keys)
        {
            properties.set(key, key.getDefaultValue());
        }
    }

    @Override
    public long getForgottenWmes()
    {
        return forgotten_wmes.get();
    }

}
