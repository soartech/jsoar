/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 20, 2012
 */
package org.jsoar.kernel.epmem;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Optimization;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.util.ByRef;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.properties.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * <h2>Typedef mappings</h2>
 * <ul>
 * <li>uint64_t == long
 * <li>int64_t == long
 * <li>epmem_time_id = long
 * <li>epmem_node_id = long
 * <li>goal_stack_level == int
 * <li>tc_number = {@code Marker}
 * <li>epmem_id_pool = {@code Map<Long, Long>}
 * <li>epmem_hashed_id_pool = {@code Map<Long, Map<Long, Long>>}
 * <li>epmem_parent_id_pool = {@code Map<Long, Map<Long, Map<Long, Long>>>}
 * <li>epmem_return_id_pool = {@code Map<Long, Map<Long, Long>>}
 * <li>epmem_wme_set = {@code Set<WmeImpl>}
 * <li>epmem_symbol_stack = {@code Deque<SymbolImpl>}
 * <li>epmem_id_ref_counter = {@code Map<Long, Set<WmeImpl>>}
 * </ul>
 * @author voigtjr
 */
public class DefaultEpisodicMemory implements EpisodicMemory
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultEpisodicMemory.class);

    /**
     * episodic_memory.h:42:epmem_variable_key
     */
    private static enum epmem_variable_key
    {
        var_rit_offset_1, var_rit_leftroot_1, var_rit_rightroot_1, var_rit_minstep_1,
        var_rit_offset_2, var_rit_leftroot_2, var_rit_rightroot_2, var_rit_minstep_2,
        var_next_id
    }
    
    private static class epmem_rit_state_param
    {
        long /*soar_module::integer_stat*/ stat;
        epmem_variable_key var_key;
    }
    
    private static class epmem_rit_state
    {
        epmem_rit_state_param offset;
        epmem_rit_state_param leftroot;
        epmem_rit_state_param rightroot;
        epmem_rit_state_param minstep;

        // TODO EPMEM soar_module::timer *timer;
        PreparedStatement add_query;
    }
    
    private Adaptable context;
    private DefaultEpisodicMemoryParams params;
    private DefaultEpisodicMemoryStats stats;

    private EpisodicMemoryDatabase db;

    /** agent.h:epmem_validation */
    private /*uintptr_t*/ long epmem_validation;

    /** agent.h:epmem_node_removals */
    private Map</*epmem_node_id*/ Long, Boolean> epmem_node_removals;
    /** agent.h:epmem_node_mins */
    private List</*epmem_time_id*/Long> epmem_node_mins;
    /** agent.h:epmem_node_maxes */
    private List<Boolean> epmem_node_maxes;

    /** agent.h:epmem_edge_removals */
    private Map</*epmem_node_id*/ Long, Boolean> epmem_edge_removals;
    /** agent.h:epmem_edge_mins */
    private List</*epmem_time_id*/Long> epmem_edge_mins;
    /** agent.h:epmem_edge_maxes */
    private List<Boolean> epmem_edge_maxes;

    /** agent.h:epmem_id_repository */
    private Map<Long, Map<Long, Map<Long, Long>>> /*epmem_parent_id_pool*/ epmem_id_repository;
    /** agent.h:epmem_id_replacement */
    private Map<Long, Map<Long, Long>> /*epmem_return_id_pool*/ epmem_id_replacement;
    /** agent.h:epmem_id_ref_counts */
    private Map<Long, Set<WmeImpl>> /*epmem_id_ref_counter*/ epmem_id_ref_counts;
    /** agent.h:epmem_id_removes */
    private Deque<SymbolImpl> /*epmem_symbol_stack*/ epmem_id_removes;
    
    /** episodic_memory.h:51:EPMEM_NODEID_ROOT */
    private static final Long EPMEM_NODEID_ROOT = 0L;
    
    /** episodic_memory.h:69:EPMEM_RIT_STATE_NODE */
    private static final int EPMEM_RIT_STATE_NODE = 0;
    /** episodic_memory.h:70:EPMEM_RIT_STATE_EDGE */
    private static final int EPMEM_RIT_STATE_EDGE = 1;
    /** episodic_memory.h:64:EPMEM_RIT_OFFSET_INIT */
    private static final int EPMEM_RIT_OFFSET_INIT = -1;

    /** agent.h:904:epmem_rit_state_graph */
    private final epmem_rit_state[] epmem_rit_state_graph = new epmem_rit_state[] {new epmem_rit_state(), new epmem_rit_state()};

    //bool epmem_first_switch;

    public DefaultEpisodicMemory(Adaptable context)
    {
        this(context, null);
    }
    
    public DefaultEpisodicMemory(Adaptable context, EpisodicMemoryDatabase db)
    {
        this.context = context;
        this.db = db;
    }
    
    public void initialize()
    {
        final PropertyManager properties = Adaptables.require(DefaultEpisodicMemory.class, context, PropertyManager.class);
        params = new DefaultEpisodicMemoryParams(properties);
        stats = new DefaultEpisodicMemoryStats(properties);
        
        epmem_id_repository = Maps.newHashMap();
        epmem_id_replacement = Maps.newHashMap();
        epmem_id_ref_counts = Maps.newHashMap();
        epmem_id_removes = Lists.newLinkedList();
    }
    
    EpisodicMemoryDatabase getDatabase()
    {
        return db;
    }
    
    /**
     * Opens the SQLite database and performs all initialization required for
     * the current mode

     * <p>episodic_memory.cpp:1458:epmem_init_db
     * 
     * @throws SoarException
     */
    void epmem_init_db() throws SoarException
    {
        epmem_init_db(false);
    }
    
    

    /**
     * Opens the SQLite database and performs all initialization required for
     * the current mode
     * 
     * <p>The readonly param should only be used in experimentation where you don't
     * want to alter previous database state.
     * 
     * <p>episodic_memory.cpp:1458:epmem_init_db
     * 
     * @param readonly
     * @throws SoarException
     */
    void epmem_init_db(boolean readonly /*= false*/) throws SoarException
    {
        if (db == null)
        {
            try
            {
                epmem_init_db_ex(readonly);
            }
            catch (SQLException e)
            {
                throw new SoarException("While attaching epmem: " + e.getMessage(), e);
            }
            catch (IOException e)
            {
                throw new SoarException("While attaching epmem: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Extracted from epmem_init_db(). Take performance settings and apply then
     * to the current database.
     * 
     * <p>episodic_memory.cpp:1496:epmem_init_db
     * @throws SQLException 
     * @throws IOException 
     * @throws SoarException 
     * 
     */
    private void applyDatabasePerformanceOptions() throws SQLException, SoarException, IOException
    {
        // TODO EPMEM SMEM a lot of this database code is identical between the two modules
        // and could be factored out.
        
        // apply performance options
        
        // cache
        if(params.driver.equals("org.sqlite.JDBC"))
        {
            // TODO: Generalize this. Move to a resource somehow.
            final int cacheSize;
            switch(params.cache.get())
            {
            case small:  cacheSize = 5000;  break;   // 5MB cache
            case medium: cacheSize = 20000; break; // 20MB cache
            case large:  
            default:     cacheSize = 100000; // 100MB cache
            }
            
            final Statement s = db.getConnection().createStatement();
            try
            {
                s.execute("PRAGMA cache_size = " + cacheSize);
            }
            finally
            {
                s.close();
            }
        }
        
        // optimization
        if (params.optimization.get() == Optimization.performance)
        {
            // If /org/jsoar/kernel/smem/<driver>.performance.sql is found on 
            // the class path, execute the statements in it.
            final String perfResource = params.driver.get() + ".performance.sql";
            final InputStream perfStream = getClass().getResourceAsStream(perfResource);
            final String fullPath = "/" + getClass().getCanonicalName().replace('.', '/') + "/" + perfResource;
            if(perfStream != null)
            {
                logger.info("Applying performance settings from '" + fullPath + "'.");
                try
                {
                    JdbcTools.executeSql(db.getConnection(), perfStream, null /*no filter*/);
                }
                finally
                {
                    perfStream.close();
                }
            }
            else
            {
                logger.warn("Could not find performance resource at '" + fullPath + "'. No performance settings applied.");
            }
        }

        // TODO EPMEM page_size
    }
    
    /**
     * Private method for epmem_init_db that throws SQLException, IOException so 
     * it can wrap in SoarException and throw.
     * 
     * <p>episodic_memory.cpp:1458:epmem_init_db
     * 
     * @param readonly
     * @throws SoarException
     */
    private void epmem_init_db_ex(boolean readonly /*= false*/) throws SQLException, IOException, SoarException
    {
        if (db != null /* my_agent->epmem_db->get_status() != soar_module::disconnected */ )
        {
            return;
        }

        ////////////////////////////////////////////////////////////////////////////
        // TODO EPMEM my_agent->epmem_timers->init->start();
        ////////////////////////////////////////////////////////////////////////////

        // attempt connection
        final String jdbcUrl = params.protocol.get() + ":" + params.path.get();
        final Connection connection = JdbcTools.connect(params.driver.get(), jdbcUrl);
        final DatabaseMetaData meta = connection.getMetaData();
        logger.info("Opened database '" + jdbcUrl + "' with " + meta.getDriverName() + ":"  + meta.getDriverVersion());
        db = new EpisodicMemoryDatabase(params.driver.get(), connection);
        
        applyDatabasePerformanceOptions();
        
        
        // update validation count
        epmem_validation++;

        // setup common structures/queries
        // setup graph structures/queries
        db.structure();
        db.prepare();
        
        // initialize range tracking
        epmem_node_mins.clear();
        epmem_node_maxes.clear();
        epmem_node_removals.clear();

        epmem_edge_mins.clear();
        epmem_edge_maxes.clear();
        epmem_edge_removals.clear();
        
        epmem_id_repository.put(EPMEM_NODEID_ROOT, new HashMap<Long, Map<Long, Long>>());
        {
            Set<WmeImpl> wms_temp = Sets.newHashSet();
            wms_temp.add(null);
            epmem_id_ref_counts.put(EPMEM_NODEID_ROOT, wms_temp);
        }
        
        // initialize time
        stats.time.set(1L);
        
        // initialize next_id
        stats.next_id.set(1L);

        {
            final ByRef<Long> stored_id = ByRef.create(0L);
            if (epmem_get_variable(epmem_variable_key.var_next_id, stored_id))
            {
                stats.next_id.set(stored_id.value);
            }
            else
            {
                epmem_set_variable(epmem_variable_key.var_next_id, stats.next_id.get());
            }
        }

        // initialize rit state
        for (int i=EPMEM_RIT_STATE_NODE; i <= EPMEM_RIT_STATE_EDGE; i++)
        {
            epmem_rit_state_graph[i].offset.stat = EPMEM_RIT_OFFSET_INIT;
            epmem_rit_state_graph[i].leftroot.stat = 0;
            epmem_rit_state_graph[i].rightroot.stat = 1;
            epmem_rit_state_graph[i].minstep.stat = Long.MAX_VALUE;
        }
        epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].add_query = db.add_node_range;
        epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].add_query = db.add_edge_range;

        ////
        
        // get/set RIT variables
        {
            final ByRef<Long> var_val = ByRef.create(0L);
            
            for ( int i=EPMEM_RIT_STATE_NODE; i<=EPMEM_RIT_STATE_EDGE; i++ )
            {
                // offset
                if (epmem_get_variable(epmem_rit_state_graph[i].offset.var_key, var_val))
                {
                   epmem_rit_state_graph[i].offset.stat = var_val.value;
                }
                else
                {
                    epmem_set_variable(epmem_rit_state_graph[i].offset.var_key, epmem_rit_state_graph[i].offset.stat);
                }
        
                // leftroot
                if (epmem_get_variable(epmem_rit_state_graph[i].leftroot.var_key, var_val))
                {
                    epmem_rit_state_graph[i].leftroot.stat = var_val.value;
                }
                else
                {
                    epmem_set_variable(epmem_rit_state_graph[i].leftroot.var_key, epmem_rit_state_graph[i].leftroot.stat);
                }
        
                // rightroot
                if (epmem_get_variable(epmem_rit_state_graph[i].rightroot.var_key, var_val))
                {
                    epmem_rit_state_graph[i].rightroot.stat = var_val.value;
                }
                else
                {
                    epmem_set_variable(epmem_rit_state_graph[i].rightroot.var_key, epmem_rit_state_graph[i].rightroot.stat);
        
                }
                // minstep
                if (epmem_get_variable(epmem_rit_state_graph[i].minstep.var_key, var_val))
                {
                    epmem_rit_state_graph[i].minstep.stat = var_val.value;
                }
                else
                {
                    epmem_set_variable(epmem_rit_state_graph[i].minstep.var_key, epmem_rit_state_graph[i].minstep.stat);
                }
            }
        }
        
        ////
        
        // get max time
        {
            final PreparedStatement temp_q = db.get_max_time;
            
            final ResultSet rs = temp_q.executeQuery();
            try
            {
                if(rs.next())
                {
                    //my_agent->epmem_stats->time->set_value( temp_q->column_int( 0 ) + 1 );
                    stats.time.set(rs.getLong(0 + 1) + 1);
                }
            }
            finally
            {
                rs.close();
            }
        }

        long time_max = stats.time.get();

        // insert non-NOW intervals for all current NOW's
        // remove NOW's
        if (!readonly)
        {
            long time_last = ( time_max - 1 );

            final PreparedStatement[] now_select = new PreparedStatement[] {db.now_select_node, db.now_select_edge};
            final PreparedStatement[] now_add = new PreparedStatement[] {db.add_node_point, db.add_edge_point};
            final PreparedStatement[] now_delete = new PreparedStatement[] {db.now_delete_node, db.now_delete_edge};
            
            for (int i = EPMEM_RIT_STATE_NODE; i <= EPMEM_RIT_STATE_EDGE; i++)
            {
                final PreparedStatement temp_q = now_add[i];
                temp_q.setLong(2, time_last);

                final PreparedStatement temp_q2 = now_select[i];
                final ResultSet rs = temp_q.executeQuery();
                try
                {
                    //while ( temp_q2->execute() == soar_module::row )
                    while (rs.next())
                    {
                        //range_start = temp_q2->column_int( 1 );
                        long range_start = rs.getLong(1 + 1);

                        // point
                        if (range_start == time_last)
                        {
                            temp_q.setLong(1, rs.getLong(0 + 1));
                            temp_q.executeUpdate( /*soar_module::op_reinit*/ );
                        }
                        else
                        {
                            epmem_rit_insert_interval(range_start, time_last, rs.getLong(0 + 1), epmem_rit_state_graph[i]);
                        }
                    }
                }
                finally
                {
                    rs.close();
                }

                // remove all NOW intervals
                now_delete[i].execute();
            }
        }

        // get max id + max list
        {
            const char *minmax_select[] = { "SELECT MAX(child_id) FROM node_unique", "SELECT MAX(parent_id) FROM edge_unique" };
            std::vector<bool> *minmax_max[] = { my_agent->epmem_node_maxes, my_agent->epmem_edge_maxes };
            std::vector<epmem_time_id> *minmax_min[] = { my_agent->epmem_node_mins, my_agent->epmem_edge_mins };

            for ( int i=EPMEM_RIT_STATE_NODE; i<=EPMEM_RIT_STATE_EDGE; i++ )
            {
                temp_q = new soar_module::sqlite_statement( my_agent->epmem_db, minmax_select[i] );
                temp_q->prepare();
                temp_q->execute();
                if ( temp_q->column_type( 0 ) != soar_module::null_t )
                {
                    std::vector<bool>::size_type num_ids = temp_q->column_int( 0 );

                    minmax_max[i]->resize( num_ids, true );
                    minmax_min[i]->resize( num_ids, time_max );
                }

                delete temp_q;
                temp_q = NULL;
            }
        }
        //
        //                // get id pools
        //                {
        //                    epmem_node_id q0;
        //                    int64_t w;
        //                    epmem_node_id q1;
        //                    epmem_node_id parent_id;
        //
        //                    epmem_hashed_id_pool **hp;
        //                    epmem_id_pool **ip;
        //
        //                    temp_q = new soar_module::sqlite_statement( my_agent->epmem_db, "SELECT q0, w, q1, parent_id FROM edge_unique" );
        //                    temp_q->prepare();
        //
        //                    while ( temp_q->execute() == soar_module::row )
        //                    {
        //                        q0 = temp_q->column_int( 0 );
        //                        w = temp_q->column_int( 1 );
        //                        q1 = temp_q->column_int( 2 );
        //                        parent_id = temp_q->column_int( 3 );
        //
        //                        hp =& (*my_agent->epmem_id_repository)[ q0 ];
        //                        if ( !(*hp) )
        //                            (*hp) = new epmem_hashed_id_pool;
        //
        //                        ip =& (*(*hp))[ w ];
        //                        if ( !(*ip) )
        //                            (*ip) = new epmem_id_pool;
        //
        //                        (*(*ip))[ q1 ] = parent_id;
        //
        //                        hp =& (*my_agent->epmem_id_repository)[ q1 ];
        //                        if ( !(*hp) )
        //                            (*hp) = new epmem_hashed_id_pool;
        //                    }
        //
        //                    delete temp_q;
        //                    temp_q = NULL;
        //                }
        //            }
        //
        
        // if lazy commit, then we encapsulate the entire lifetime of the agent in a single transaction
        if (params.lazy_commit.get())
        {
            db.begin.executeUpdate( /*soar_module::op_reinit*/ );
        }

        ////////////////////////////////////////////////////////////////////////////
        // TODO EPMEM my_agent->epmem_timers->init->stop();
        ////////////////////////////////////////////////////////////////////////////
    }

    private void epmem_rit_insert_interval(long range_start, long time_last, long long1, epmem_rit_state epmem_rit_state)
    {
        // TODO Auto-generated method stub
        
    }

    /**
     * Gets an EpMem variable from the database
     * 
     * <p>episodic_memory.cpp:984:epmem_get_variable
     * 
     * @param variable_id
     * @param variable_value
     * @return
     * @throws SQLException
     */
    private boolean epmem_get_variable(epmem_variable_key variable_id, ByRef<Long> variable_value) throws SQLException
    {
        final PreparedStatement var_get = db.var_get;
    
        var_get.setInt( 1, variable_id.ordinal() );
        final ResultSet rs = var_get.executeQuery();
        try
        {
            if(rs.next())
            {
                variable_value.value = rs.getLong(0 + 1);
                return true;
            }
            else
            {
                return false;
            }
        }
        finally
        {
            rs.close();
        }
    }

    /**
     * Sets an EpMem variable in the database
     * 
     * <p>episodic_memory.cpp:1007:epmem_set_variable
     * 
     * @param variable_id
     * @param variable_value
     * @throws SQLException
     */
    private void epmem_set_variable(epmem_variable_key variable_id, long variable_value) throws SQLException
    {
        final PreparedStatement var_set = db.var_set;
    
        var_set.setLong( 1, variable_value );
        var_set.setInt( 2, variable_id.ordinal() );
    
        var_set.execute();
    }

    @Override
    public void epmem_close() throws SoarException
    {
        if (db != null)
        {
            try
            {
                // TODO this is copy-paste from smem right now, there are other things to do here
                
                // close the database
                db.getConnection().close();
                db = null;
            }
            catch (SQLException e)
            {
                throw new SoarException("While closing epmem: " + e.getMessage(), e);
            }
        }
    }

}
