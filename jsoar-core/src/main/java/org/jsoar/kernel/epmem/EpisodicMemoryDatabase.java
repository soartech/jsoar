/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 11, 2010
 */
package org.jsoar.kernel.epmem;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.jsoar.util.db.AbstractSoarDatabase;

/**
 * Database helper class for epmem.
 * 
 * @author ray
 */
final class EpisodicMemoryDatabase extends AbstractSoarDatabase
{
    enum value_type { null_t, int_t, double_t, text_t };
    
    // empty table used to verify proper structure
    static final String EPMEM_SCHEMA = "epmem2_";
    static final String EPMEM_SIGNATURE = EPMEM_SCHEMA + "signature";
    
    // These are all the prepared statements for EPMEM. They're filled in via reflection
    // from statements.properties.
    
    // epmem_common_statement_container
    PreparedStatement begin;
    PreparedStatement commit;
    PreparedStatement rollback;
    PreparedStatement var_get;
    PreparedStatement var_set;
    PreparedStatement rit_add_left;
    PreparedStatement rit_truncate_left;
    PreparedStatement rit_add_right;
    PreparedStatement rit_truncate_right;
    PreparedStatement hash_get;
    PreparedStatement hash_add;
    
    // epmem_graph_statement_container
    PreparedStatement add_time;
    PreparedStatement add_node_now;
    PreparedStatement delete_node_now;
    PreparedStatement add_node_point;
    PreparedStatement add_node_range;
    PreparedStatement add_node_unique;
    PreparedStatement find_node_unique;
    PreparedStatement add_edge_now;
    PreparedStatement delete_edge_now;
    PreparedStatement add_edge_point;
    PreparedStatement add_edge_range;
    PreparedStatement add_edge_unique;
    PreparedStatement find_edge_unique;
    PreparedStatement find_edge_unique_shared;
    PreparedStatement valid_episode;
    PreparedStatement next_episode;
    PreparedStatement prev_episode;
    PreparedStatement get_nodes;
    PreparedStatement get_edges;
    PreparedStatement promote_id;
    PreparedStatement find_lti;

    // episodic_memory.cpp:1703:epmem_init_db
    PreparedStatement get_max_time;
    // episodic_memory.cpp:1719:epmem_init_db
    PreparedStatement now_select_node;
    // episodic_memory.cpp:1719:epmem_init_db
    PreparedStatement now_select_edge;
    // episodic_memory.cpp:1721:epmem_init_db
    PreparedStatement now_delete_node;
    // episodic_memory.cpp:1721:epmem_init_db
    PreparedStatement now_delete_edge;
    // episodic_memory.cpp:1761:epmem_init_db
    PreparedStatement minmax_select_node;
    // episodic_memory.cpp:1761:epmem_init_db
    PreparedStatement minmax_select_edge;
    // episodic_memory.cpp:1794:epmem_init_db
    PreparedStatement edge_unique_select;
            
    /**
     * @param driver
     * @param db
     */
    public EpisodicMemoryDatabase(String driver, Connection db)
    {
        super(driver, db, EPMEM_SIGNATURE);
        
        getFilterMap().put("@PREFIX@", EPMEM_SCHEMA);
    }
    
    /**
     * soardb.h:460:column_type
     * 
     * Similar to what column_type is doing in soar_module except this is operating directly on the type
     * instead of the column itself. This might be a confusingly bad idea to change what the parameter means here.
     * TODO EPMEM document this hack
     * 
     * @param col
     * @return
     */
    value_type column_type(int jdbcColumnType)
    {
        value_type return_val = EpisodicMemoryDatabase.value_type.null_t;

        switch (jdbcColumnType)
        {
        // TODO EPMEM Not sure which is valid here
        case java.sql.Types.BIGINT:
        case java.sql.Types.SMALLINT:
        case java.sql.Types.TINYINT:
        case java.sql.Types.INTEGER:
            return_val = EpisodicMemoryDatabase.value_type.int_t;
            break;

        case java.sql.Types.DOUBLE:
        case java.sql.Types.FLOAT:
            return_val = EpisodicMemoryDatabase.value_type.double_t;
            break;

        case java.sql.Types.VARCHAR:
            return_val = EpisodicMemoryDatabase.value_type.text_t;
            break;
        }

        return return_val;
    }

}
