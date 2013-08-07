/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 11, 2010
 */
package org.jsoar.kernel.epmem;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.db.AbstractSoarDatabase;
import org.jsoar.util.db.SoarPreparedStatement;

/**
 * Database helper class for epmem.
 * 
 * @author ray
 */
final class EpisodicMemoryDatabase extends AbstractSoarDatabase
{
    enum value_type { null_t, int_t, double_t, text_t };
    
    // empty table used to verify proper structure
    static final String EPMEM_SCHEMA = "epmem_";
    
    static final String EPMEM_SCHEMA_VERSION = "2.0";
    
    // These are all the prepared statements for EPMEM. They're filled in via reflection
    // from statements.properties.
    
    // epmem_common_statement_container
    PreparedStatement begin;
    PreparedStatement commit;
    PreparedStatement rollback;
    
    SoarPreparedStatement backup;
    SoarPreparedStatement restore;
    
    PreparedStatement var_get;
    PreparedStatement var_set;
    
    PreparedStatement rit_add_left;
    PreparedStatement rit_truncate_left;
    PreparedStatement rit_add_right;
    PreparedStatement rit_truncate_right;
    
    PreparedStatement hash_rev_int;
    PreparedStatement hash_rev_float;
    PreparedStatement hash_rev_str;
    PreparedStatement hash_get_int;
    PreparedStatement hash_get_float;
    PreparedStatement hash_get_str;
    PreparedStatement hash_get_type;
    PreparedStatement hash_add_type;
    PreparedStatement hash_add_int;
    PreparedStatement hash_add_float;
    PreparedStatement hash_add_str;
    
    // epmem_graph_statement_container
    PreparedStatement add_node;
    PreparedStatement add_time;
    
    //
    
    PreparedStatement add_epmem_wmes_constant_now;
    PreparedStatement delete_epmem_wmes_constant_now;
    PreparedStatement add_epmem_wmes_constant_point;
    PreparedStatement add_epmem_wmes_constant_range;
    
    PreparedStatement add_epmem_wmes_constant;
    PreparedStatement find_epmem_wmes_constant;
    
    //
    
    PreparedStatement add_epmem_wmes_identifier_now;
    PreparedStatement delete_epmem_wmes_identifier_now;
    PreparedStatement add_epmem_wmes_identifier_point;
    PreparedStatement add_epmem_wmes_identifier_range;
    
    PreparedStatement add_epmem_wmes_identifier;
    PreparedStatement find_epmem_wmes_identifier;
    PreparedStatement find_epmem_wmes_identifier_shared;
    
    //
    
    PreparedStatement valid_episode;
    PreparedStatement next_episode;
    PreparedStatement prev_episode;
    
    PreparedStatement get_wmes_with_identifier_values;
    PreparedStatement get_wmes_with_constant_values;
    
    //
    
    PreparedStatement promote_id;
    PreparedStatement find_lti;
    PreparedStatement find_lti_promotion_time;
    
    //
    
    PreparedStatement drop_epmem_nodes;
    PreparedStatement drop_epmem_episodes;
    PreparedStatement drop_epmem_wmes_constant_now;
    PreparedStatement drop_epmem_wmes_identifier_now;
    PreparedStatement drop_epmem_wmes_constant_point;
    PreparedStatement drop_epmem_wmes_identifier_point;
    PreparedStatement drop_epmem_wmes_constant_range;
    PreparedStatement drop_epmem_wmes_identifier_range;
    PreparedStatement drop_epmem_wmes_constant;
    PreparedStatement drop_epmem_wmes_identifier;
    PreparedStatement drop_epmem_lti;
    PreparedStatement drop_epmem_persistent_variables;
    PreparedStatement drop_epmem_rit_left_nodes;
    PreparedStatement drop_epmem_rit_right_nodes;
    PreparedStatement drop_epmem_symbols_type;
    PreparedStatement drop_epmem_symbols_integer;
    PreparedStatement drop_epmem_symbols_float;
    PreparedStatement drop_epmem_symbols_string;
    
    PreparedStatement update_epmem_wmes_identifier_last_episode_id;

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
    
    PreparedStatement database_version;
    PreparedStatement set_schema_version;
    PreparedStatement get_schema_version;
    
    // episodic_memory.cpp:854
    SoarPreparedStatement pool_dummy;
    
    SoarPreparedStatement pool_find_edge_queries_0_0;
    SoarPreparedStatement pool_find_edge_queries_0_1;
    SoarPreparedStatement pool_find_edge_queries_1_0;
    SoarPreparedStatement pool_find_edge_queries_1_1;
    public SoarPreparedStatement[][] pool_find_edge_queries;
    
    SoarPreparedStatement pool_find_interval_queries_0_0_0;
    SoarPreparedStatement pool_find_interval_queries_0_0_1;
    SoarPreparedStatement pool_find_interval_queries_0_0_2;
    SoarPreparedStatement pool_find_interval_queries_0_1_0;
    SoarPreparedStatement pool_find_interval_queries_0_1_1;
    SoarPreparedStatement pool_find_interval_queries_0_1_2;
    SoarPreparedStatement pool_find_interval_queries_1_0_0;
    SoarPreparedStatement pool_find_interval_queries_1_0_1;
    SoarPreparedStatement pool_find_interval_queries_1_0_2;
    SoarPreparedStatement pool_find_interval_queries_1_1_0;
    SoarPreparedStatement pool_find_interval_queries_1_1_1;
    SoarPreparedStatement pool_find_interval_queries_1_1_2;
    public SoarPreparedStatement[][][] pool_find_interval_queries;
    
    SoarPreparedStatement pool_find_lti_queries_0_0;
    SoarPreparedStatement pool_find_lti_queries_0_1;
    SoarPreparedStatement pool_find_lti_queries_0_2;
    SoarPreparedStatement pool_find_lti_queries_1_0;
    SoarPreparedStatement pool_find_lti_queries_1_1;
    SoarPreparedStatement pool_find_lti_queries_1_2;
    public SoarPreparedStatement[][] pool_find_lti_queries;
    
    public void prepare() throws SoarException, IOException
    {
        //Reflect the prepared statements in
        super.prepare();
        //Assign them to the proper arrays
        pool_find_edge_queries = new SoarPreparedStatement[][]
                {
                    {
                        pool_find_edge_queries_0_0,
                        pool_find_edge_queries_0_1
                    },
                    {
                        pool_find_edge_queries_1_0,
                        pool_find_edge_queries_1_1
                    }
                };
        pool_find_interval_queries = new SoarPreparedStatement[][][]
            {
                {
                    {
                        pool_find_interval_queries_0_0_0,
                        pool_find_interval_queries_0_0_1,
                        pool_find_interval_queries_0_0_2
                    },
                    {
                        pool_find_interval_queries_0_1_0,
                        pool_find_interval_queries_0_1_1,
                        pool_find_interval_queries_0_1_2
                    }
                },
                {
                    {
                        pool_find_interval_queries_1_0_0,
                        pool_find_interval_queries_1_0_1,
                        pool_find_interval_queries_1_0_2
                    },
                    {
                        pool_find_interval_queries_1_1_0,
                        pool_find_interval_queries_1_1_1,
                        pool_find_interval_queries_1_1_2
                    }
                },
            };
        pool_find_lti_queries = new SoarPreparedStatement[][]
            {
                {
                    pool_find_lti_queries_0_0,
                    pool_find_lti_queries_0_1,
                    pool_find_lti_queries_0_2
                },
                {
                    pool_find_lti_queries_1_0,
                    pool_find_lti_queries_1_1,
                    pool_find_lti_queries_1_2
                }
            };
    }
    
    /**
     * @param driver
     * @param db
     */
    public EpisodicMemoryDatabase(String driver, Connection db)
    {
        super(driver, db);
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
    
    public void dropEpmemTables() throws SQLException{
        drop_epmem_nodes.execute();
        drop_epmem_episodes.execute();
        drop_epmem_wmes_constant_now.execute();
        drop_epmem_wmes_identifier_now.execute();
        drop_epmem_wmes_constant_point.execute();
        drop_epmem_wmes_identifier_point.execute();
        drop_epmem_wmes_constant_range.execute();
        drop_epmem_wmes_identifier_range.execute();
        drop_epmem_wmes_constant.execute();
        drop_epmem_wmes_identifier.execute();
        drop_epmem_lti.execute();
        drop_epmem_persistent_variables.execute();
        drop_epmem_rit_left_nodes.execute();
        drop_epmem_rit_right_nodes.execute();
        drop_epmem_symbols_type.execute();
        drop_epmem_symbols_integer.execute();
        drop_epmem_symbols_float.execute();
        drop_epmem_symbols_string.execute();
    }
    
    public boolean backupDb(String fileName) throws SQLException
    {
        boolean returnValue = false;
        
        if (this.getConnection().getAutoCommit())
        {
            commit.execute();
            begin.execute();
        }
        
        // See sqlite-jdbc notes
        String query = backup.getQuery() + " \"" + fileName + "\"";
        this.getConnection().createStatement().executeUpdate(query);
        
        returnValue = true;
        
        if (this.getConnection().getAutoCommit())
        {
            commit.execute();
            begin.execute();
        }
        
        return returnValue;
    }
}
