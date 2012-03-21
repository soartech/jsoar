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
            
    /**
     * @param driver
     * @param db
     */
    public EpisodicMemoryDatabase(String driver, Connection db)
    {
        super(driver, db, EPMEM_SIGNATURE);
        
        getFilterMap().put("@PREFIX@", EPMEM_SCHEMA);
    }

}
