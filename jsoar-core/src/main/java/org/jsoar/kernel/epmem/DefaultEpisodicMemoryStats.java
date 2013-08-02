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
    static final PropertyKey<Long> TIME = key("time", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> time = new DefaultPropertyProvider<Long>(TIME);
    
    // epmem_node_id_stat *next_id;
    static final PropertyKey<Long> NEXT_ID = key("next-id", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> next_id = new DefaultPropertyProvider<Long>(NEXT_ID);

    // non-cue-based-retrievals
    // soar_module::integer_stat *ncbr;
    static final PropertyKey<Long> NCBR = key("ncbr", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> ncbr = new DefaultPropertyProvider<Long>(NCBR);
    
    
    // soar_module::integer_stat *nexts;
    static final PropertyKey<Long> NEXTS = key("nexts", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> nexts = new DefaultPropertyProvider<Long>(NEXTS);

    // soar_module::integer_stat *prevs;
    static final PropertyKey<Long> PREVS = key("prevs", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> prevs = new DefaultPropertyProvider<Long>(PREVS);
    
    // soar_module::integer_stat *cbr;
    static final PropertyKey<Long> CBR = key("cbr", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> cbr = new DefaultPropertyProvider<Long>(CBR);
    
    //epmem_db_lib_version_stat* db_lib_version;
    static final PropertyKey<String> DB_VERSION = key("db_version", String.class).defaultValue("").build();
    final DefaultPropertyProvider<String> db_version = new DefaultPropertyProvider<String>(DB_VERSION);
    
    //TODO: These are supposed to be the memory stats for sql.  There is a possible hack to
    //get current usage using PRAGMA, but our driver doesn't support it.  If that can be figured
    //out, it would be relatively trivial get a fair approximation of a high water mark.  --ACN 
    //epmem_mem_usage_stat *mem_usage;
    static final PropertyKey<Long> MEM_USAGE = key("mem_usage", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> mem_usage = new DefaultPropertyProvider<Long>(MEM_USAGE);
    
    //epmem_mem_high_stat *mem_high;
    static final PropertyKey<Long> MEM_HIGH = key("mem_high", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> mem_high = new DefaultPropertyProvider<Long>(MEM_HIGH);
    
//    soar_module::integer_stat *cbr;

    //soar_module::integer_stat *ncb_wmes;
    static final PropertyKey<Long> NCB_WMES = key("ncb_wmes", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> ncb_wmes = new DefaultPropertyProvider<Long>(NCB_WMES);
    
	//TODO: Pos and neg query don't look they are wired into anything in CSoar. -ACN
    //soar_module::integer_stat *qry_pos;
    static final PropertyKey<Long> QRY_POS = key("qry_pos", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> qry_pos = new DefaultPropertyProvider<Long>(QRY_POS);
    
    //soar_module::integer_stat *qry_neg;
    static final PropertyKey<Long> QRY_NEG = key("qry_neg", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> qry_neg = new DefaultPropertyProvider<Long>(QRY_NEG);
    
    //epmem_time_id_stat *qry_ret;
    static final PropertyKey<Long> QRY_RET = key("qry_ret", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> qry_ret = new DefaultPropertyProvider<Long>(QRY_RET);
    
    //soar_module::integer_stat *qry_card;
    static final PropertyKey<Long> QRY_CARD = key("qry_card", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> qry_card = new DefaultPropertyProvider<Long>(QRY_CARD);
    
    //soar_module::integer_stat *qry_lits;
    static final PropertyKey<Long> QRY_LITS = key("qry_lits", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> qry_lits = new DefaultPropertyProvider<Long>(QRY_LITS);
    
    static final PropertyKey<Long> CONSIDERED = key("considered", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> considered = new DefaultPropertyProvider<Long>(CONSIDERED);
    
    static final PropertyKey<Long> LAST_CONSIDERED = key("last_considered", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> last_considered = new DefaultPropertyProvider<Long>(LAST_CONSIDERED);
    
    static final PropertyKey<Long> GRAPH_MATCHES = key("graph_matches", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> graph_matches = new DefaultPropertyProvider<Long>(GRAPH_MATCHES);
    
    static final PropertyKey<Long> LAST_GRAPH_MATCHES = key("last_graph_matches", Long.class).defaultValue(0L).build();
    final DefaultPropertyProvider<Long> last_graph_matches = new DefaultPropertyProvider<Long>(LAST_GRAPH_MATCHES);
    
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
        add(NCBR, ncbr);
        add(NEXTS, nexts);
        add(PREVS, prevs);
        add(CBR, cbr);
        add(DB_VERSION, db_version);
        add(MEM_USAGE, mem_usage);
        add(MEM_HIGH, mem_high);
        add(NCB_WMES, ncb_wmes);
        add(QRY_POS, qry_pos);
        add(QRY_NEG, qry_neg);
        add(QRY_RET, qry_ret);
        add(QRY_CARD, qry_card);
        add(QRY_LITS, qry_lits);
        add(CONSIDERED, considered);
        add(LAST_CONSIDERED, last_considered);
        add(GRAPH_MATCHES, graph_matches);
        add(LAST_GRAPH_MATCHES, last_graph_matches);
    }
    
    private <T> void add(PropertyKey<T> key, PropertyProvider<T> value)
    {
        this.properties.setProvider(key, value);
    }

    @SuppressWarnings("unchecked")
    public void reset()
    {
        for(@SuppressWarnings("rawtypes") PropertyKey key : keys)
        {
            properties.set(key, key.getDefaultValue());
        }
    }

    @Override
    public long getTime()
    {
        return time.get();
    }
    
    @Override
    public void setTime(long time)
    {
        this.time.set(time);
    }

    @Override
    public long getNextId()
    {
        return next_id.get();
    }

    @Override
    public void setNextId(long next_id)
    {
        this.next_id.set(next_id);
    }

}
