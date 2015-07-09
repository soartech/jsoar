/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 1, 2010
 */
package org.jsoar.kernel.smem;

import android.content.Context;

import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.DoublePropertyProvider;
import org.jsoar.util.properties.EnumPropertyProvider;
import org.jsoar.util.properties.LongPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>semantic_memory.h:44:smem_param_container
 * 
 * @author ray
 */
class DefaultSemanticMemoryParams
{
    static enum PageChoices
    {
        page_1k, page_2k, page_4k, page_8k, page_16k, page_32k, page_64k;
        
        @Override
        public String toString()
        {
            switch (this)
            {
            case page_1k:
                return "1k";
            case page_2k:
                return "2k";
            case page_4k:
                return "4k";
            case page_8k:
                return "8k";
            case page_16k:
                return "16k";
            case page_32k:
                return "32k";
            case page_64k:
                return "64k";
            default:
                throw new IllegalArgumentException();
            }
        }
    }
    static enum Optimization { safety, performance };
    static enum MergeChoices
    {
        none, add;
    }
    static enum ActivationChoices
    {
        recency, frequency, base_level("base-level");
        
        // some options have dashes in them, but we can't put those in the enum name, so we need a mapping
        private final String realName;
        
        private ActivationChoices()
        {
            this.realName = this.name();
        }
        
        private ActivationChoices(String realName)
        {
            this.realName = realName;
        }
        
        @Override
        public String toString()
        {
            return realName;
        }
        
        public static ActivationChoices getEnum(String value)
        {
            if(value == null)
                throw new IllegalArgumentException();
            for(ActivationChoices ac : ActivationChoices.values())
            {
                if(value.equals(ac.toString())) return ac;
            }
            throw new IllegalArgumentException();
        }
    }
    static enum BaseUpdateChoices
    {
        stable, naive, incremental;
    }
    static enum ActivateOnQueryChoices
    {
        on, off
    }
    static enum MirroringChoices
    {
        on, off
    }
    static enum LazyCommitChoices
    {
        on, off
    }
    static enum LearningChoices
    {
        on, off
    }
    static enum AppendDatabaseChoices
    {
        on, off
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
    
    public static abstract class SetWrapper <T>
    {
        protected Set<T> set;
    }
    
    public static class SetWrapperLong extends SetWrapper<Long>
    {
        public SetWrapperLong()
        {
            set = new HashSet<Long>();
        }
        
        public SetWrapperLong(Set<Long> set)
        {
            this.set = set;
        }
        
        public boolean add(Long object)
        {
            return set.add(object);
        }
        
        public void clear()
        {
            set.clear();
        }
        
        public boolean contains(Long object)
        {
            return set.contains(object);
        }
        
        public Iterator<Long> iterator()
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
        
        public Set<Long> valueOf(String value)
        {
            Long longValue = Long.parseLong(value);
            
            if (set.contains(longValue))
            {
                set.remove(longValue);
            }
            else
            {
                set.add(longValue);
            }
            
            return new HashSet<Long>(set);
        }
        
        public SetWrapperLong toSetWrapper(String value)
        {
            return new SetWrapperLong(valueOf(value));
        }
    }
    
    static final PropertyKey<LearningChoices> LEARNING = key("learning", LearningChoices.class).defaultValue(LearningChoices.off).build();
    final EnumPropertyProvider<LearningChoices> learning = new EnumPropertyProvider<LearningChoices>(LEARNING);

    static final PropertyKey<String> DRIVER = key("driver", String.class).defaultValue("org.sqlite.JDBC").build();
    final DefaultPropertyProvider<String> driver = new DefaultPropertyProvider<String>(DRIVER);

    static final PropertyKey<String> PROTOCOL = key("protocol", String.class).defaultValue("jdbc:sqlite").build();
    final DefaultPropertyProvider<String> protocol = new DefaultPropertyProvider<String>(PROTOCOL);

    static final PropertyKey<String> PATH = key("path", String.class).build();
    final DefaultPropertyProvider<String> path = new DefaultPropertyProvider<String>(PATH);
    
    static final PropertyKey<LazyCommitChoices> LAZY_COMMIT = key("lazy-commit", LazyCommitChoices.class).defaultValue(LazyCommitChoices.on).build();
    final EnumPropertyProvider<LazyCommitChoices> lazy_commit = new EnumPropertyProvider<LazyCommitChoices>(LAZY_COMMIT);
    
    static final PropertyKey<AppendDatabaseChoices> APPEND_DB = key("append-database", AppendDatabaseChoices.class).defaultValue(AppendDatabaseChoices.on).build();
    final EnumPropertyProvider<AppendDatabaseChoices> append_db = new EnumPropertyProvider<AppendDatabaseChoices>(APPEND_DB);
    
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
    
    static final PropertyKey<ActivateOnQueryChoices> ACTIVATE_ON_QUERY = key("activate-on-query", ActivateOnQueryChoices.class).defaultValue(ActivateOnQueryChoices.on).build();
    final EnumPropertyProvider<ActivateOnQueryChoices> activate_on_query = new EnumPropertyProvider<ActivateOnQueryChoices>(ACTIVATE_ON_QUERY);
    
    static final PropertyKey<ActivationChoices> ACTIVATION_MODE = key("activation-mode", ActivationChoices.class).defaultValue(ActivationChoices.recency).build();
    final EnumPropertyProvider<ActivationChoices> activation_mode = new EnumPropertyProvider<ActivationChoices>(ACTIVATION_MODE);
    
    static final PropertyKey<Double> BASE_DECAY = key("base-decay", Double.class).defaultValue(0.5).build();
    final DoublePropertyProvider base_decay = new DoublePropertyProvider(BASE_DECAY);
    
    static final PropertyKey<BaseUpdateChoices> BASE_UPDATE = key("base-update", BaseUpdateChoices.class).defaultValue(BaseUpdateChoices.stable).build();
    final EnumPropertyProvider<BaseUpdateChoices> base_update = new EnumPropertyProvider<BaseUpdateChoices>(BASE_UPDATE);
    
    static final PropertyKey<SetWrapperLong> BASE_INCREMENTAL_THRESHES = key("base-incremental-threshes", SetWrapperLong.class).defaultValue(new SetWrapperLong()).build();
    final DefaultPropertyProvider<SetWrapperLong> base_incremental_threshes = new DefaultPropertyProvider<SetWrapperLong>(BASE_INCREMENTAL_THRESHES);
    
    static final PropertyKey<MirroringChoices> MIRRORING = key("mirroring", MirroringChoices.class).defaultValue(MirroringChoices.off).build();
    final EnumPropertyProvider<MirroringChoices> mirroring = new EnumPropertyProvider<MirroringChoices>(MIRRORING);
    
    private final PropertyManager properties;
    
    public DefaultSemanticMemoryParams(PropertyManager properties, Context androidContext)
    {
        this.properties = properties;
        
        properties.setProvider(LEARNING, learning);
        properties.setProvider(DRIVER, driver);
        properties.setProvider(PROTOCOL, protocol);
        path.set(androidContext.getFilesDir().getAbsolutePath() + File.separator + "soar.db");
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
