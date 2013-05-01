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
    PreparedStatement find_lti_promotion_time;
    PreparedStatement update_edge_unique_last;

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
    
    // episodic_memory.cpp:854
    final private static String poolDummy = "SELECT ? as start";
    final PreparedStatementFactory pool_dummy;
    
    String epmem_find_edge_queries[/* 2 */][/* 2 */] = 
    {
        {
            "SELECT child_id, value, ? FROM @PREFIX@node_unique WHERE parent_id=? AND attrib=?",
            "SELECT child_id, value, ? FROM @PREFIX@node_unique WHERE parent_id=? AND attrib=? AND value=?" 
        },
        {
            "SELECT parent_id, q1, last FROM @PREFIX@edge_unique WHERE q0=? AND w=? AND ?<last ORDER BY last DESC",
            "SELECT parent_id, q1, last FROM @PREFIX@edge_unique WHERE q0=? AND w=? AND q1=? AND ?<last" 
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
                "SELECT (e.start - 1) AS start FROM @PREFIX@node_range e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC",
                "SELECT (e.start - 1) AS start FROM @PREFIX@node_now e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC",
                "SELECT (e.start - 1) AS start FROM @PREFIX@node_point e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC"
            },
            {
                "SELECT e.end AS end FROM @PREFIX@node_range e WHERE e.id=? AND e.end>0 AND e.start<=? ORDER BY e.end DESC",
                "SELECT ? AS end FROM @PREFIX@node_now e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC",
                "SELECT e.start AS end FROM @PREFIX@node_point e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC"
            }
        },
        {
            {
                "SELECT (e.start - 1) AS start FROM @PREFIX@edge_range e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC",
                "SELECT (e.start - 1) AS start FROM @PREFIX@edge_now e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC",
                "SELECT (e.start - 1) AS start FROM @PREFIX@edge_point e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC"
            },
            {
                "SELECT e.end AS end FROM @PREFIX@edge_range e WHERE e.id=? AND e.end>0 AND e.start<=? ORDER BY e.end DESC",
                "SELECT ? AS end FROM @PREFIX@edge_now e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC",
                "SELECT e.start AS end FROM @PREFIX@edge_point e WHERE e.id=? AND e.start<=? ORDER BY e.start DESC"
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
            "SELECT (e.start - 1) AS start FROM @PREFIX@edge_range e WHERE e.id=? AND ?<e.start AND e.start<=? ORDER BY e.start DESC",
            "SELECT (e.start - 1) AS start FROM @PREFIX@edge_now e WHERE e.id=? AND ?<e.start AND e.start<=? ORDER BY e.start DESC",
            "SELECT (e.start - 1) AS start FROM @PREFIX@edge_point e WHERE e.id=? AND ?<e.start AND e.start<=? ORDER BY e.start DESC"
        },
        {
            "SELECT e.end AS end FROM @PREFIX@edge_range e WHERE e.id=? AND e.end>0 AND ?<=e.start AND e.start<=? ORDER BY e.end DESC",
            "SELECT ? AS end FROM @PREFIX@edge_now e WHERE e.id=? AND ?<=e.start AND e.start<=? ORDER BY e.start",
            "SELECT e.start AS end FROM @PREFIX@edge_point e WHERE e.id=? AND ?<=e.start AND e.start<=? ORDER BY e.start DESC"
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
                return db.prepareStatement(sql);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            return null;
        }
    }
}
