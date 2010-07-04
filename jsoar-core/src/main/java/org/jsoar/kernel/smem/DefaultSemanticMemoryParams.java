/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 1, 2010
 */
package org.jsoar.kernel.smem;

import org.jsoar.util.properties.BooleanPropertyProvider;
import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.EnumPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

/**
 * <p>semantic_memory.h:44:smem_param_container
 * 
 * @author ray
 */
class DefaultSemanticMemoryParams
{
    static enum Optimization { safety, performance };
    static enum Cache {
        small, medium, large;
    }
    
    private static final String PREFIX = "smem.params.";
    
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
    static final PropertyKey<Boolean> LEARNING = key("learning", Boolean.class).defaultValue(false).build();
    final BooleanPropertyProvider learning = new BooleanPropertyProvider(LEARNING);
    
    static final PropertyKey<String> DRIVER = key("driver", String.class).defaultValue("org.sqlite.JDBC").build();
    final DefaultPropertyProvider<String> driver = new DefaultPropertyProvider<String>(DRIVER);

    static final PropertyKey<String> PATH = key("path", String.class).defaultValue("jdbc:sqlite::memory:").build();
    final DefaultPropertyProvider<String> path = new DefaultPropertyProvider<String>(PATH);
    
    static final PropertyKey<Boolean> LAZY_COMMIT = key("lazy-commit", Boolean.class).defaultValue(true).build();
    final BooleanPropertyProvider lazy_commit = new BooleanPropertyProvider(LAZY_COMMIT);
    
    static final PropertyKey<Cache> CACHE = key("cache", Cache.class).defaultValue(Cache.small).build();
    final EnumPropertyProvider<Cache> cache = new EnumPropertyProvider<Cache>(CACHE);
    
    static final PropertyKey<Optimization> OPTIMIZATION = key("optimization", Optimization.class).defaultValue(Optimization.performance).build();
    final EnumPropertyProvider<Optimization> optimization = new EnumPropertyProvider<Optimization>(OPTIMIZATION);
    
    static final PropertyKey<Long> THRESH = key("thresh", Long.class).defaultValue(100L).build();
    final DefaultPropertyProvider<Long> thresh = new DefaultPropertyProvider<Long>(THRESH);
    
    private final PropertyManager properties;
    
    public DefaultSemanticMemoryParams(PropertyManager properties)
    {
        this.properties = properties;
        
        properties.setProvider(LEARNING, learning);
        properties.setProvider(DRIVER, driver);
        properties.setProvider(PATH, path);
        
        properties.setProvider(LAZY_COMMIT, lazy_commit);
        properties.setProvider(CACHE, cache);
        properties.setProvider(OPTIMIZATION, optimization);
        properties.setProvider(THRESH, thresh);
    }
    
    public PropertyManager getProperties()
    {
        return properties;
    }

}
