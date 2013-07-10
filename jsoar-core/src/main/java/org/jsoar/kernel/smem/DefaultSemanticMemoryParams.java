/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 1, 2010
 */
package org.jsoar.kernel.smem;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jsoar.util.properties.BooleanPropertyProvider;
import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.DoublePropertyProvider;
import org.jsoar.util.properties.EnumPropertyProvider;
import org.jsoar.util.properties.LongPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

/**
 * <p>semantic_memory.h:44:smem_param_container
 * 
 * @author ray
 */
class DefaultSemanticMemoryParams
{
    static enum Cache
    {
        small, medium, large;
    }
    static enum PageChoices
    {
        page_1k, page_2k, page_4k, page_8k, page_16k, page_32k, page_64k;
    }
    static enum Optimization { safety, performance };
    static enum MergeChoices
    {
        none, add;
    }
    static enum ActivationChoices
    {
        recency, frequency, base;
    }
    static enum BaseUpdateChoices
    {
        stable, naive, incremental;
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
    
    public static class SetWrapper <T>
    {
        protected Set<T> set;
        
        public SetWrapper()
        {
            set = new HashSet<T>();
        }
        
        public SetWrapper(Set<T> set)
        {
            this.set = set;
        }
        
        public boolean add(T object)
        {
            return set.add(object);
        }
        
        public void clear()
        {
            set.clear();
        }
        
        public boolean contains(T object)
        {
            return set.contains(object);
        }
        
        public Iterator<T> iterator()
        {
            return set.iterator();
        }
        
        public boolean isEmpty()
        {
            return set.isEmpty();
        }
        
        public String toString()
        {
            return set.toString();
        }
    }
    
    public static class SetWrapperLong extends SetWrapper<Long>
    {}
    
    static final PropertyKey<Boolean> LEARNING = key("learning", Boolean.class).defaultValue(false).build();
    final BooleanPropertyProvider learning = new BooleanPropertyProvider(LEARNING);
    
    static final PropertyKey<String> DRIVER = key("driver", String.class).defaultValue("org.sqlite.JDBC").build();
    final DefaultPropertyProvider<String> driver = new DefaultPropertyProvider<String>(DRIVER);
    
    static final PropertyKey<String> PROTOCOL = key("protocol", String.class).defaultValue("jdbc:sqlite").build();
    final DefaultPropertyProvider<String> protocol = new DefaultPropertyProvider<String>(PROTOCOL);

    static final PropertyKey<String> PATH = key("path", String.class).defaultValue(":memory:").build();
    final DefaultPropertyProvider<String> path = new DefaultPropertyProvider<String>(PATH);
    
    static final PropertyKey<Boolean> LAZY_COMMIT = key("lazy-commit", Boolean.class).defaultValue(true).build();
    final BooleanPropertyProvider lazy_commit = new BooleanPropertyProvider(LAZY_COMMIT);
    
    static final PropertyKey<Boolean> APPEND_DB = key("append-db", Boolean.class).defaultValue(true).build();
    final BooleanPropertyProvider append_db = new BooleanPropertyProvider(APPEND_DB);
    
    static final PropertyKey<PageChoices> PAGE_SIZE = key("page-size", PageChoices.class).defaultValue(PageChoices.page_8k).build();
    final EnumPropertyProvider<PageChoices> page_size = new EnumPropertyProvider<PageChoices>(PAGE_SIZE);
    
    static final PropertyKey<Long> CACHE_SIZE = key("cache-size", Long.class).defaultValue(10000L).build();
    final LongPropertyProvider cache_size = new LongPropertyProvider(CACHE_SIZE);
    
    static final PropertyKey<Optimization> OPTIMIZATION = key("optimization", Optimization.class).defaultValue(Optimization.performance).build();
    final EnumPropertyProvider<Optimization> optimization = new EnumPropertyProvider<Optimization>(OPTIMIZATION);
    
    static final PropertyKey<Long> THRESH = key("thresh", Long.class).defaultValue(100L).build();
    final LongPropertyProvider thresh = new LongPropertyProvider(THRESH);
    
    static final PropertyKey<MergeChoices> MERGE = key("merge", MergeChoices.class).defaultValue(MergeChoices.add).build();
    final EnumPropertyProvider<MergeChoices> merge = new EnumPropertyProvider<MergeChoices>(MERGE);
    
    static final PropertyKey<Boolean> ACTIVATE_ON_QUERY = key("activate-on-query", Boolean.class).defaultValue(true).build();
    final BooleanPropertyProvider activate_on_query = new BooleanPropertyProvider(ACTIVATE_ON_QUERY);
    
    static final PropertyKey<ActivationChoices> ACTIVATION_MODE = key("activation-mode", ActivationChoices.class).defaultValue(ActivationChoices.recency).build();
    final EnumPropertyProvider<ActivationChoices> activation_mode = new EnumPropertyProvider<ActivationChoices>(ACTIVATION_MODE);
    
    static final PropertyKey<Double> BASE_DECAY = key("base-decay", Double.class).defaultValue(0.5).build();
    final DoublePropertyProvider base_decay = new DoublePropertyProvider(BASE_DECAY);
    
    static final PropertyKey<BaseUpdateChoices> BASE_UPDATE = key("base-update", BaseUpdateChoices.class).defaultValue(BaseUpdateChoices.stable).build();
    final EnumPropertyProvider<BaseUpdateChoices> base_update = new EnumPropertyProvider<BaseUpdateChoices>(BASE_UPDATE);
    
    static final PropertyKey<SetWrapperLong> BASE_INCREMENTAL_THRESHES = key("base-incremental-threshes", SetWrapperLong.class).defaultValue(new SetWrapperLong()).build();
    final DefaultPropertyProvider<SetWrapperLong> base_incremental_threshes = new DefaultPropertyProvider<SetWrapperLong>(BASE_INCREMENTAL_THRESHES);
    
    static final PropertyKey<Boolean> MIRRORING = key("mirroring", Boolean.class).defaultValue(false).build();
    final BooleanPropertyProvider mirroring = new BooleanPropertyProvider(MIRRORING);
    
    private final PropertyManager properties;
    
    public DefaultSemanticMemoryParams(PropertyManager properties)
    {
        this.properties = properties;
        
        properties.setProvider(LEARNING, learning);
        properties.setProvider(DRIVER, driver);
        properties.setProvider(PROTOCOL, protocol);
        properties.setProvider(PATH, path);
        
        properties.setProvider(LAZY_COMMIT, lazy_commit);
        properties.setProvider(APPEND_DB, append_db);
        
        properties.setProvider(PAGE_SIZE, page_size);
        properties.setProvider(CACHE_SIZE, cache_size);
        
        properties.setProvider(OPTIMIZATION, optimization);
        properties.setProvider(THRESH, thresh);
        
        properties.setProvider(MERGE, merge);
        properties.setProvider(ACTIVATE_ON_QUERY, activate_on_query);
        properties.setProvider(ACTIVATION_MODE, activation_mode);
        properties.setProvider(BASE_DECAY, base_decay);
        
        properties.setProvider(BASE_UPDATE, base_update);
        properties.setProvider(BASE_INCREMENTAL_THRESHES, base_incremental_threshes);
        
        properties.setProvider(MIRRORING, mirroring);
    }
    
    public PropertyManager getProperties()
    {
        return properties;
    }
}
