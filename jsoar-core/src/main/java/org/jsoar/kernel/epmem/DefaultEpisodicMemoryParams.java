/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import java.util.HashSet;
import java.util.Set;

import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.properties.BooleanPropertyProvider;
import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.DoublePropertyProvider;
import org.jsoar.util.properties.EnumPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;

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
     * Policy for committing data to disk
     */
    static enum Optimization { safety, performance };
    /**
     * Size of memory pages used in the SQLite cache
     */
    static enum Cache { small, medium, large; }
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
    
    static enum MergeChoices { merge_none, merge_add };
    
    static enum GraphMatchChoices { on, off };
    
    static enum GmOrderingChoices { undefined, dfs, mcv };
    
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
    
    static final PropertyKey<Double> BALANCE = key("balance", Double.class).defaultValue(1.0).build();
    final DoublePropertyProvider balance = new DoublePropertyProvider(BALANCE);
    
    static final PropertyKey<String> PATH = key("path", String.class).defaultValue(":memory:").build();
    final DefaultPropertyProvider<String> path = new DefaultPropertyProvider<String>(PATH);
    
    static final PropertyKey<Cache> CACHE = key("cache", Cache.class).defaultValue(Cache.small).build();
    final EnumPropertyProvider<Cache> cache = new EnumPropertyProvider<Cache>(CACHE);
    
    static final PropertyKey<Optimization> OPTIMIZATION = key("optimization", Optimization.class).defaultValue(Optimization.performance).build();
    final EnumPropertyProvider<Optimization> optimization = new EnumPropertyProvider<Optimization>(OPTIMIZATION);
    
    // TODO: what should the default phase be?
    static final PropertyKey<Phase> PHASE = key("phase", Phase.class).defaultValue(Phase.output).build();
    final EnumPropertyProvider<Phase> phase = new EnumPropertyProvider<Phase>(PHASE);
    
    static final PropertyKey<Learning> LEARNING = key("learning", Learning.class).defaultValue(Learning.off).build();
    final EnumPropertyProvider<Learning> learning = new EnumPropertyProvider<Learning>(LEARNING);
    
    static final PropertyKey<Force> FORCE = key("force", Force.class).defaultValue(Force.off).build();
    final EnumPropertyProvider<Force> force = new EnumPropertyProvider<Force>(FORCE);
    
    static final PropertyKey<Trigger> TRIGGER = key("trigger", Trigger.class).defaultValue(Trigger.dc).build();
    final EnumPropertyProvider<Trigger> trigger = new EnumPropertyProvider<Trigger>(TRIGGER);
    
    static final PropertyKey<MergeChoices> MERGE = key("merge", MergeChoices.class).defaultValue(MergeChoices.merge_none).build();
    final EnumPropertyProvider<MergeChoices> merge = new EnumPropertyProvider<MergeChoices>(MERGE);
    
    static final PropertyKey<GraphMatchChoices> GRAPH_MATCH = key("graph_match", GraphMatchChoices.class).defaultValue(GraphMatchChoices.on).build();
    final EnumPropertyProvider<GraphMatchChoices> graph_match = new EnumPropertyProvider<GraphMatchChoices>(GRAPH_MATCH);
    
    static final PropertyKey<GmOrderingChoices> GM_ORDERING= key("gm_ordering", GmOrderingChoices.class).defaultValue(GmOrderingChoices.undefined).build();
    final EnumPropertyProvider<GmOrderingChoices> gm_ordering = new EnumPropertyProvider<GmOrderingChoices>(GM_ORDERING);
    
    private final PropertyManager properties;

    public DefaultEpisodicMemoryParams(PropertyManager properties, SymbolFactory sf)
    {
        this.properties = properties;
        
        properties.setProvider(DRIVER, driver);
        properties.setProvider(PROTOCOL, protocol);
        properties.setProvider(PATH, path);

        properties.setProvider(LAZY_COMMIT, lazy_commit);
        properties.setProvider(CACHE, cache);
        properties.setProvider(OPTIMIZATION, optimization);
        
        properties.setProvider(PHASE, phase);
        properties.setProvider(LEARNING, learning);
        properties.setProvider(FORCE, force);
        properties.setProvider(TRIGGER, trigger);
        
        properties.setProvider(MERGE, merge);
        properties.setProvider(GRAPH_MATCH, graph_match);
        properties.setProvider(GM_ORDERING, gm_ordering);
        
        properties.setProvider(BALANCE, balance);
        
        // exclude ^epmem and ^smem attributes from being added to epmem by default
        exclusions.add((SymbolImpl) sf.createString("epmem"));
        exclusions.add((SymbolImpl) sf.createString("smem"));
    }

    public PropertyManager getProperties()
    {
        return properties;
    }

}
