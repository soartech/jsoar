/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 11, 2010
 */
package org.jsoar.kernel.epmem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

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
    static final String EPMEM_SCHEMA = "epmem2_";
    static final String EPMEM_SIGNATURE = EPMEM_SCHEMA + "epmem_signature";
    
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
    
    // episodic_memory.cpp:854
    final private static String poolDummy = "SELECT ? as start";
    final PreparedStatementFactory pool_dummy;
    
    String epmem_find_edge_queries[/* 2 */][/* 2 */] = 
    {
        {
            "SELECT wc_id, value_s_id, ? FROM @PREFIX@epmem_wmes_constant  WHERE parent_n_id=? AND attribute_s_id=?",
            "SELECT wc_id, value_s_id, ? FROM @PREFIX@epmem_wmes_constant  WHERE parent_n_id=? AND attribute_s_id=? AND value_s_id=?" 
        },
        {
            "SELECT wi_id, child_n_id, last_episode_id FROM @PREFIX@epmem_wmes_identifier WHERE parent_n_id=? AND attribute_s_id=? AND ?<last_episode_id ORDER BY last_episode_id DESC",
            "SELECT wi_id, child_n_id, last_episode_id FROM @PREFIX@epmem_wmes_identifier WHERE parent_n_id=? AND attribute_s_id=? AND child_n_id=? AND ?<last_episode_id" 
        } 
    };
    final PreparedStatementFactory[][] pool_find_edge_queries;
    
    // Because the DB records when things are /inserted/, we need to offset
    // the start by 1 to /remove/ them at the right time. Ditto to even
    // include those intervals correctly
    String epmem_find_interval_queries[/*2*/][/*2*/][/*3*/] =
    {
        {
            {
                "SELECT (e.start_episode_id - 1) AS start FROM @PREFIX@epmem_wmes_constant_range e WHERE e.wc_id=? AND e.start_episode_id<=? ORDER BY e.start_episode_id DESC",
                "SELECT (e.start_episode_id - 1) AS start FROM @PREFIX@epmem_wmes_constant_now e WHERE e.wc_id=? AND e.start_episode_id<=? ORDER BY e.start_episode_id DESC",
                "SELECT (e.episode_id - 1) AS start FROM @PREFIX@epmem_wmes_constant_point e WHERE e.wc_id=? AND e.episode_id<=? ORDER BY e.episode_id DESC"
            },
            {
                "SELECT e.end_episode_id AS end FROM @PREFIX@epmem_wmes_constant_range e WHERE e.wc_id=? AND e.end_episode_id>0 AND e.start_episode_id<=? ORDER BY e.end_episode_id DESC",
                "SELECT ? AS end FROM @PREFIX@epmem_wmes_constant_now e WHERE e.wc_id=? AND e.start_episode_id<=? ORDER BY e.start_episode_id DESC",
                "SELECT e.episode_id AS end FROM @PREFIX@epmem_wmes_constant_point e WHERE e.wc_id=? AND e.episode_id<=? ORDER BY e.episode_id DESC"
            }
        },
        {
            {
                "SELECT (e.start_episode_id - 1) AS start FROM @PREFIX@epmem_wmes_identifier_range e WHERE e.wi_id=? AND e.start_episode_id<=? ORDER BY e.start_episode_id DESC",
                "SELECT (e.start_episode_id - 1) AS start FROM @PREFIX@epmem_wmes_identifier_now e WHERE e.wi_id=? AND e.start_episode_id<=? ORDER BY e.start_episode_id DESC",
                "SELECT (e.episode_id - 1) AS start FROM @PREFIX@epmem_wmes_identifier_point e WHERE e.wi_id=? AND e.episode_id<=? ORDER BY e.episode_id DESC"
            },
            {
                "SELECT e.end_episode_id AS end FROM @PREFIX@epmem_wmes_identifier_range e WHERE e.wi_id=? AND e.end_episode_id>0 AND e.start_episode_id<=? ORDER BY e.end_episode_id DESC",
                "SELECT ? AS end FROM @PREFIX@epmem_wmes_identifier_now e WHERE e.wi_id=? AND e.start_episode_id<=? ORDER BY e.start_episode_id DESC",
                "SELECT e.episode_id AS end FROM @PREFIX@epmem_wmes_identifier_point e WHERE e.wi_id=? AND e.episode_id<=? ORDER BY e.episode_id DESC"
            }
        },
    };
    final PreparedStatementFactory[][][] pool_find_interval_queries;
    
    // episodic_memory.cpp:854
    // notice that the start and end queries in epmem_find_lti_queries are _asymetric_
    // in that the the starts have ?<e.start and the ends have ?<=e.start
    // this small difference means that the start of the very first interval
    // (ie. the one where the start is at or before the promotion time) will be ignored
    // then we can simply add a single epmem_interval to the queue, and it will
    // terminate any LTI interval appropriately
    String epmem_find_lti_queries[/*2*/][/*3*/] =
    {
        {
            "SELECT (e.start_episode_id - 1) AS start FROM @PREFIX@epmem_wmes_identifier_range e WHERE e.wi_id=? AND ?<e.start_episode_id AND e.start_episode_id<=? ORDER BY e.start_episode_id DESC",
            "SELECT (e.start_episode_id - 1) AS start FROM @PREFIX@epmem_wmes_identifier_now e WHERE e.wi_id=? AND ?<e.start_episode_id AND e.start_episode_id<=? ORDER BY e.start_episode_id DESC",
            "SELECT (e.episode_id - 1) AS start FROM @PREFIX@epmem_wmes_identifier_point e WHERE e.wi_id=? AND ?<e.episode_id AND e.episode_id<=? ORDER BY e.episode_id DESC"
        },
        {
            "SELECT e.end_episode_id AS end FROM @PREFIX@epmem_wmes_identifier_range e WHERE e.wi_id=? AND e.end_episode_id>0 AND ?<=e.start_episode_id AND e.start_episode_id<=? ORDER BY e.end_episode_id DESC",
            "SELECT ? AS end FROM @PREFIX@epmem_wmes_identifier_now e WHERE e.wi_id=? AND ?<=e.start_episode_id AND e.start_episode_id<=? ORDER BY e.start_episode_id",
            "SELECT e.episode_id AS end FROM @PREFIX@epmem_wmes_identifier_point e WHERE e.wi_id=? AND ?<=e.episode_id AND e.episode_id<=? ORDER BY e.episode_id DESC"
        }
    };
    final PreparedStatementFactory[][] pool_find_lti_queries;
    
    /**
     * @param driver
     * @param db
     */
    public EpisodicMemoryDatabase(String driver, Connection db)
    {
        super(driver, db, EPMEM_SIGNATURE);
        
        Map<String, String> filter = getFilterMap();
        filter.put("@PREFIX@", EPMEM_SCHEMA);
        
        //The bounds for these loops are #defined constants in the C, but they are out of scope here and
        //the point is to populate one array using the other, so lets just do that. -ACN
        pool_find_interval_queries = new PreparedStatementFactory[epmem_find_interval_queries.length][][];
        for ( int j=0; j<epmem_find_interval_queries.length; j++ )
        {
            pool_find_interval_queries[j] = new PreparedStatementFactory[epmem_find_interval_queries[j].length][];
            for ( int k=0; k<epmem_find_interval_queries[j].length; k++ )
            {
                pool_find_interval_queries[j][k] = new PreparedStatementFactory[epmem_find_interval_queries[j][k].length];
                for( int m=0; m<epmem_find_interval_queries[j][k].length; m++ )
                {
                    pool_find_interval_queries[ j ][ k ][ m ] = 
                        new PreparedStatementFactory( epmem_find_interval_queries[ j ][ k ][ m ], db, filter );
                }
            }
        }
        
        //The bounds for these loops are #defined constants in the C, but they are out of scope here and
        //the point is to populate one array using the other, so lets just do that. -ACN
        pool_dummy = new PreparedStatementFactory(poolDummy, db, filter);
        pool_find_lti_queries = new PreparedStatementFactory[epmem_find_lti_queries.length][];
        for ( int k=0; k<epmem_find_lti_queries.length; k++ )
        {
            pool_find_lti_queries[k] = new PreparedStatementFactory[epmem_find_lti_queries[k].length];
            for( int m=0; m<epmem_find_lti_queries[k].length; m++ )
            {
                pool_find_lti_queries[ k ][ m ] = 
                    new PreparedStatementFactory( epmem_find_lti_queries[ k ][ m ], db, filter );
            }
        }
        
        //The bounds for these loops are #defined constants in the C, but they are out of scope here and
        //the point is to populate one array using the other, so lets just do that. -ACN
        pool_find_edge_queries = new PreparedStatementFactory[epmem_find_edge_queries.length][];
        for ( int i=0; i < epmem_find_edge_queries.length; i++) 
        {
            pool_find_edge_queries[i] = new PreparedStatementFactory[epmem_find_edge_queries[i].length];
            for ( int j=0; j < epmem_find_edge_queries[i].length; j++ )
            {
                pool_find_edge_queries[i][j] =
                        new PreparedStatementFactory( epmem_find_edge_queries[i][j], db, filter );
            }
        }
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
    
    /**
     * Some of the queries in Epmem are instantiated, paramatized,
     * and have their results accessed in parallel of unknown
     * periods of time.  This allows us to instantiate multiple 
     * prepared statements using the same query string.
     * The reflection scheme provided form AbstractSoarDatabase can
     * only provide PreparedStatements, which cannot be cloned, and
     * the sql cannot be retrieved from them.  This means we can't 
     * use it to populate these queries.
     * 
     * TODO: See if there is a cleaner way to set this up.
     * 
     * @author ACNickels
     *
     */
    public static class PreparedStatementFactory
    {
        final private String sql;
        final private Connection db;
        
        protected PreparedStatementFactory(String sql, Connection db, Map<String, String> filter)
        {
            this.db = db;
            for(String key: filter.keySet()){
                sql = sql.replace(key, filter.get(key));
            }
            this.sql = sql;
        }
        
        public PreparedStatement request(){
            try
            {
                return new SoarPreparedStatement(db.prepareStatement(sql), sql);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            return null;
        }
    }
}
