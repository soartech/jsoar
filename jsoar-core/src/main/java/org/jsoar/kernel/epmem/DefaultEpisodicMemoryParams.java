/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import android.content.Context;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.DoublePropertyProvider;
import org.jsoar.util.properties.EnumPropertyProvider;
import org.jsoar.util.properties.LongPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author voigtjr
 */
class DefaultEpisodicMemoryParams
{
    /**
     * Set of attributes which are excluded from epmem.
     */
    Set<SymbolImpl> exclusions = new HashSet<SymbolImpl>();
    /**
     * Set of attributes which are included in epmem.
     */
    Set<SymbolImpl> inclusions = new HashSet<SymbolImpl>();
    /**
     * Policy for committing data to disk
     */
    static enum Optimization { safety, performance };
    /**
     * Size of pages used for SQLite
     */
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
    /**
     * Decision cycle phase to encode new episodes and process epmem link commands
     */
    static enum Phase { output, selection };
    /**
     * Episodic memory enabled
     */
    static enum Learning { on, off };
    /**
     * Forces episode encoding/ignoring in the next storage phase
     */
    static enum Force { remember, ignore, off };
    /**
     * How episode encoding is triggered
     */
    static enum Trigger { none, output, dc };
    
    static enum MergeChoices { none, add };
    
    static enum GraphMatchChoices { on, off };
    
    static enum GmOrderingChoices { undefined, dfs, mcv };
    
    static enum AppendDatabaseChoices { on, off };
    
    static enum LazyCommitChoices { on, off };
    
    private static final String PREFIX = "epmem.params.";
    
    private static <T> PropertyKey.Builder<T> key(String name, Class<T> type)
    {
        return PropertyKey.builder(PREFIX + name, type);
    }

    static final PropertyKey<String> DRIVER = key("driver", String.class).defaultValue("org.sqlite.JDBC").build();
    final DefaultPropertyProvider<String> driver = new DefaultPropertyProvider<String>(DRIVER);

    static final PropertyKey<String> PROTOCOL = key("protocol", String.class).defaultValue("jdbc:sqlite").build();
    final DefaultPropertyProvider<String> protocol = new DefaultPropertyProvider<String>(PROTOCOL);

    static final PropertyKey<LazyCommitChoices> LAZY_COMMIT = key("lazy-commit", LazyCommitChoices.class).defaultValue(LazyCommitChoices.off).build();
    final EnumPropertyProvider<LazyCommitChoices> lazy_commit = new EnumPropertyProvider<LazyCommitChoices>(LAZY_COMMIT);
    
    static final PropertyKey<Double> BALANCE = key("balance", Double.class).defaultValue(1.0).build();
    final DoublePropertyProvider balance = new DoublePropertyProvider(BALANCE);
    
    static final PropertyKey<String> PATH = key("path", String.class).defaultValue(EpisodicMemoryDatabase.IN_MEMORY_PATH).build();
    final DefaultPropertyProvider<String> path = new DefaultPropertyProvider<String>(PATH);
    
    static final PropertyKey<PageChoices> PAGE_SIZE = key("page-size", PageChoices.class).defaultValue(PageChoices.page_8k).build();
    final EnumPropertyProvider<PageChoices> page_size = new EnumPropertyProvider<PageChoices>(PAGE_SIZE);
    
    static final PropertyKey<Long> CACHE_SIZE = key("cache-size", Long.class).defaultValue(10000L).build();
    final LongPropertyProvider cache_size = new LongPropertyProvider(CACHE_SIZE);
    
    static final PropertyKey<Optimization> OPTIMIZATION = key("optimization", Optimization.class).defaultValue(Optimization.performance).build();
    final EnumPropertyProvider<Optimization> optimization = new EnumPropertyProvider<Optimization>(OPTIMIZATION);
    
    static final PropertyKey<AppendDatabaseChoices> APPEND_DB= key("append-database", AppendDatabaseChoices.class).defaultValue(AppendDatabaseChoices.off).build();
    final EnumPropertyProvider<AppendDatabaseChoices> append_database = new EnumPropertyProvider<AppendDatabaseChoices>(APPEND_DB);
    
    static final PropertyKey<Phase> PHASE = key("phase", Phase.class).defaultValue(Phase.output).build();
    final EnumPropertyProvider<Phase> phase = new EnumPropertyProvider<Phase>(PHASE);
    
    static final PropertyKey<Learning> LEARNING = key("learning", Learning.class).defaultValue(Learning.off).build();
    final EnumPropertyProvider<Learning> learning = new EnumPropertyProvider<Learning>(LEARNING);
    
    static final PropertyKey<Force> FORCE = key("force", Force.class).defaultValue(Force.off).build();
    final EnumPropertyProvider<Force> force = new EnumPropertyProvider<Force>(FORCE);
    
    static final PropertyKey<Trigger> TRIGGER = key("trigger", Trigger.class).defaultValue(Trigger.dc).build();
    final EnumPropertyProvider<Trigger> trigger = new EnumPropertyProvider<Trigger>(TRIGGER);
    
    static final PropertyKey<MergeChoices> MERGE = key("merge", MergeChoices.class).defaultValue(MergeChoices.none).build();
    final EnumPropertyProvider<MergeChoices> merge = new EnumPropertyProvider<MergeChoices>(MERGE);
    
    static final PropertyKey<GraphMatchChoices> GRAPH_MATCH = key("graph_match", GraphMatchChoices.class).defaultValue(GraphMatchChoices.on).build();
    final EnumPropertyProvider<GraphMatchChoices> graph_match = new EnumPropertyProvider<GraphMatchChoices>(GRAPH_MATCH);
    
    static final PropertyKey<GmOrderingChoices> GM_ORDERING= key("gm_ordering", GmOrderingChoices.class).defaultValue(GmOrderingChoices.undefined).build();
    final EnumPropertyProvider<GmOrderingChoices> gm_ordering = new EnumPropertyProvider<GmOrderingChoices>(GM_ORDERING);
    
    private final PropertyManager properties;

    public DefaultEpisodicMemoryParams(PropertyManager properties, SymbolFactory sf, Context androidContext)
    {
        this.properties = properties;
        
        properties.setProvider(DRIVER, driver);
        properties.setProvider(PROTOCOL, protocol);
        //We can't know this before runtime in android
//        path.set(androidContext.getFilesDir().getAbsolutePath() + File.separator + "soar.db");
        properties.setProvider(PATH, path);

        properties.setProvider(LAZY_COMMIT, lazy_commit);
        properties.setProvider(CACHE_SIZE, cache_size);
        properties.setProvider(OPTIMIZATION, optimization);
        
        properties.setProvider(PHASE, phase);
        properties.setProvider(LEARNING, learning);
        properties.setProvider(FORCE, force);
        properties.setProvider(TRIGGER, trigger);
        
        properties.setProvider(MERGE, merge);
        properties.setProvider(GRAPH_MATCH, graph_match);
        properties.setProvider(GM_ORDERING, gm_ordering);
        
        properties.setProvider(BALANCE, balance);
        properties.setProvider(APPEND_DB, append_database);
        
        // exclude ^epmem and ^smem attributes from being added to epmem by default
        exclusions.add((SymbolImpl) sf.createString("epmem"));
        exclusions.add((SymbolImpl) sf.createString("smem"));
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
