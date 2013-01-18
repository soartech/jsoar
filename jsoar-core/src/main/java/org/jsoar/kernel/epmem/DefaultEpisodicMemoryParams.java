/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import org.jsoar.util.properties.BooleanPropertyProvider;
import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.EnumPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

/**
 * @author voigtjr
 */
public class DefaultEpisodicMemoryParams
{
    static enum Optimization { safety, performance };
    static enum Cache {
        small, medium, large;
    }
    static enum Phase { output, selection };
    
    private static final String PREFIX = "epmem.params.";
    
    private static <T> PropertyKey.Builder<T> key(String name, Class<T> type)
    {
        return PropertyKey.builder(PREFIX + name, type);
    }
    
    static final PropertyKey<String> DRIVER = key("driver", String.class).defaultValue("org.sqlite.JDBC").build();
    final DefaultPropertyProvider<String> driver = new DefaultPropertyProvider<String>(DRIVER);
    
    static final PropertyKey<String> PROTOCOL = key("protocol", String.class).defaultValue("jdbc:sqlite").build();
    final DefaultPropertyProvider<String> protocol = new DefaultPropertyProvider<String>(PROTOCOL);

    static final PropertyKey<Boolean> LAZY_COMMIT = key("lazy-commit", Boolean.class).defaultValue(true).build();
    final BooleanPropertyProvider lazy_commit = new BooleanPropertyProvider(LAZY_COMMIT);
    
    static final PropertyKey<String> PATH = key("path", String.class).defaultValue(":memory:").build();
    final DefaultPropertyProvider<String> path = new DefaultPropertyProvider<String>(PATH);
    
    static final PropertyKey<Cache> CACHE = key("cache", Cache.class).defaultValue(Cache.small).build();
    final EnumPropertyProvider<Cache> cache = new EnumPropertyProvider<Cache>(CACHE);
    
    static final PropertyKey<Optimization> OPTIMIZATION = key("optimization", Optimization.class).defaultValue(Optimization.performance).build();
    final EnumPropertyProvider<Optimization> optimization = new EnumPropertyProvider<Optimization>(OPTIMIZATION);
    
    // TODO: what should the default phase be?
    static final PropertyKey<Phase> PHASE = key("phase", Phase.class).defaultValue(Phase.output).build();
    final EnumPropertyProvider<Phase> phase = new EnumPropertyProvider<Phase>(PHASE);

    private final PropertyManager properties;

    public DefaultEpisodicMemoryParams(PropertyManager properties)
    {
        this.properties = properties;
        
        properties.setProvider(DRIVER, driver);
        properties.setProvider(PROTOCOL, protocol);
        properties.setProvider(PATH, path);

        properties.setProvider(LAZY_COMMIT, lazy_commit);
        properties.setProvider(CACHE, cache);
        properties.setProvider(OPTIMIZATION, optimization);
        
        properties.setProvider(PHASE, phase);
    }

    public PropertyManager getProperties()
    {
        return properties;
    }

}
