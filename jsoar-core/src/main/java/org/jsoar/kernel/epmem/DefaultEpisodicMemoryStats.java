/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import java.util.HashSet;
import java.util.Set;

import org.jsoar.util.properties.DefaultPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.properties.PropertyProvider;

/**
 * <p>episodic_memory.h:176:epmem_stat_container
 * @author voigtjr
 */
class DefaultEpisodicMemoryStats implements EpisodicMemoryStatistics
{
    private static final String PREFIX = "epmem.stats.";
    
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
    
    private static <T> PropertyKey.Builder<T> key(String name, Class<T> type)
    {
        return PropertyKey.builder(PREFIX + name, type);
    }
    
    // epmem_time_id_stat *time;
    static final PropertyKey<Long> TIME = key("time", Long.class).defaultValue(1L).build();
    final DefaultPropertyProvider<Long> time = new DefaultPropertyProvider<Long>(TIME);
    
    // epmem_node_id_stat *next_id;
    static final PropertyKey<Long> NEXT_ID = key("next-id", Long.class).defaultValue(1L).build();
    final DefaultPropertyProvider<Long> next_id = new DefaultPropertyProvider<Long>(NEXT_ID);

//    epmem_db_lib_version_stat* db_lib_version;
//    epmem_mem_usage_stat *mem_usage;
//    epmem_mem_high_stat *mem_high;
//    soar_module::integer_stat *cbr;
//    soar_module::integer_stat *nexts;
//    soar_module::integer_stat *prevs;
//    soar_module::integer_stat *ncb_wmes;
//
//    soar_module::integer_stat *qry_pos;
//    soar_module::integer_stat *qry_neg;
//    epmem_time_id_stat *qry_ret;
//    soar_module::integer_stat *qry_card;
//    soar_module::integer_stat *qry_lits;
//
//    soar_module::integer_stat *rit_offset_1;
//    soar_module::integer_stat *rit_left_root_1;
//    soar_module::integer_stat *rit_right_root_1;
//    soar_module::integer_stat *rit_min_step_1;
//
//    soar_module::integer_stat *rit_offset_2;
//    soar_module::integer_stat *rit_left_root_2;
//    soar_module::integer_stat *rit_right_root_2;
//    soar_module::integer_stat *rit_min_step_2;

    private final PropertyManager properties;
    private final Set<PropertyKey<?>> keys = new HashSet<PropertyKey<?>>();

    public DefaultEpisodicMemoryStats(PropertyManager properties)
    {
        this.properties = properties;

        add(TIME, time);
        add(NEXT_ID, next_id);
    }
    
    private <T> void add(PropertyKey<T> key, PropertyProvider<T> value)
    {
        this.properties.setProvider(key, value);
    }

    @SuppressWarnings("unchecked")
    public void reset()
    {
        for(PropertyKey key : keys)
        {
            properties.set(key, key.getDefaultValue());
        }
    }


}
