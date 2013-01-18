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

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Optimization;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Phase;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.smem.DefaultSemanticMemory;
import org.jsoar.kernel.smem.SemanticMemoryStateInfo;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
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
    
    /**
     * episodic_memory.h:30:EPMEM_MEMID_NONE
     */
    public static final long EPMEM_MEMID_NONE = 0;
    
    private static class epmem_rit_state_param
    {
        long /*soar_module::integer_stat*/ stat;
        epmem_variable_key var_key = epmem_variable_key.var_rit_offset_1;
    }
    
    private static class epmem_rit_state
    {
        epmem_rit_state_param offset = new epmem_rit_state_param();
        epmem_rit_state_param leftroot = new epmem_rit_state_param();
        epmem_rit_state_param rightroot = new epmem_rit_state_param();
        epmem_rit_state_param minstep = new epmem_rit_state_param();

        // TODO EPMEM soar_module::timer *timer;
        PreparedStatement add_query;
    }
    
    private Adaptable context;
    private DefaultEpisodicMemoryParams params;
    private DefaultEpisodicMemoryStats stats;
    private Decider decider;

    SymbolFactoryImpl symbols;
    
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
    
    private final Map<IdentifierImpl, EpisodicMemoryStateInfo> stateInfos = new HashMap<IdentifierImpl, EpisodicMemoryStateInfo>();

    public DefaultEpisodicMemory(Adaptable context)
    {
        this(context, null);
    }
    
    public DefaultEpisodicMemory(Adaptable context, EpisodicMemoryDatabase db)
    {
        this.context = context;
        this.db = db;
    }
    
    /**
     * This is called when the agent is initialized. This code here in CSoar is usually run at 
     * agent creation.
     */
    public void initialize()
    {
        final PropertyManager properties = Adaptables.require(DefaultEpisodicMemory.class, context, PropertyManager.class);
        decider = Adaptables.adapt(context, Decider.class);
        params = new DefaultEpisodicMemoryParams(properties);
        stats = new DefaultEpisodicMemoryStats(properties);
        
        symbols = Adaptables.require(DefaultEpisodicMemory.class, context, SymbolFactoryImpl.class);
        
        // CK: not implementing timers
        // src/agent.cpp:369:  newAgent->epmem_timers = new epmem_timer_container( newAgent );
        
        // CK: in smem this is called from smem_attach, there is no equivalent function in episodic_memory.cpp
        try 
        {
			epmem_init_db();
			// database connection closed in Agent#dispose()
		} catch (SoarException e) {
			// TODO throw this to agent? thrown if there is a problem connecting to database
			// smem logs/prints like this:
			logger.error("While initializing epmem: " + e.getMessage(), e);
			// the context passed to the constructor from Agent is a reference to the Agent object
			Agent agent = Adaptables.adapt(this.context, Agent.class);
			if(agent!=null)
			    agent.getPrinter().error("While initializing epmem: " + e.getMessage());
		}
    
//        src/agent.cpp:393:  newAgent->epmem_node_removals = new epmem_id_removal_map();
        epmem_node_removals = Maps.newHashMap();
//        src/agent.cpp:375:  newAgent->epmem_node_mins = new std::vector<epmem_time_id>();
        epmem_node_mins = Lists.newArrayList();
//        src/agent.cpp:376:  newAgent->epmem_node_maxes = new std::vector<bool>();
        epmem_node_maxes = Lists.newArrayList();
// CK: initialization depends on whether USE_MEM_POOL_ALLOCATORS is defined
//        src/agent.cpp:386:  newAgent->epmem_edge_removals = 
//          new epmem_id_removal_map( std::less< epmem_node_id >(), 
//                soar_module::soar_memory_pool_allocator< std::pair< epmem_node_id, bool > >( newAgent ) );
//        src/agent.cpp:394:  newAgent->epmem_edge_removals = new epmem_id_removal_map();
        epmem_edge_removals = Maps.newHashMap();
//        src/agent.cpp:378:  newAgent->epmem_edge_mins = new std::vector<epmem_time_id>();    
        epmem_edge_mins = Lists.newArrayList();
//        src/agent.cpp:379:  newAgent->epmem_edge_maxes = new std::vector<bool>();
        epmem_edge_maxes = Lists.newArrayList();

        epmem_id_repository = Maps.newHashMap();
        epmem_id_replacement = Maps.newHashMap();
        epmem_id_ref_counts = Maps.newHashMap();
        epmem_id_removes = Lists.newLinkedList();
        
        // TODO implement these?
//        src/agent.cpp:372:  newAgent->epmem_stmts_common = NULL;
//        src/agent.cpp:373:  newAgent->epmem_stmts_graph = NULL;       
//        src/agent.cpp:402:  newAgent->epmem_validation = 0;
//        src/agent.cpp:403:  newAgent->epmem_first_switch = true;
        
        // CK: don't need memory pools in java:
//        src/agent.cpp:388:  newAgent->epmem_wme_adds = new epmem_symbol_set( std::less< Symbol* >(), soar_module::soar_memory_pool_allocator< Symbol* >( newAgent ) );
//        src/agent.cpp:389:  newAgent->epmem_promotions = new epmem_symbol_set( std::less< Symbol* >(), soar_module::soar_memory_pool_allocator< Symbol* >( newAgent ) );
//        src/agent.cpp:391:  newAgent->epmem_id_removes = new epmem_symbol_stack( soar_module::soar_memory_pool_allocator< Symbol* >( newAgent ) );
//        src/agent.cpp:396:  newAgent->epmem_wme_adds = new epmem_symbol_set();
//        src/agent.cpp:397:  newAgent->epmem_promotions = new epmem_symbol_set();
//        src/agent.cpp:399:  newAgent->epmem_id_removes = new epmem_symbol_stack();
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
    
    private void initMinMax(long time_max, PreparedStatement minmax_select, List<Boolean> minmax_max, List<Long> minmax_min) throws SQLException
    {
        final ResultSet rs = minmax_select.executeQuery();
        try
        {
            while (rs.next())
            {
                // if ( temp_q->column_type( 0 ) != soar_module::null_t )
                if (db.column_type(rs.getMetaData().getColumnType(0 + 1)) != EpisodicMemoryDatabase.value_type.null_t)
                {
                    // std::vector<bool>::size_type num_ids = temp_q->column_int( 0 );
                    int num_ids = rs.getInt(0 + 1);
                    
                    for (int i = 0; i < num_ids; i++)
                    {
                        // minmax_max[i]->resize( num_ids, true );
                        minmax_max.add(Boolean.TRUE);
                        // minmax_min[i]->resize( num_ids, time_max );
                        minmax_min.add(time_max);
                    }
                }
            }
        }
        finally
        {
            rs.close();
        }
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
                final ResultSet rs = temp_q2.executeQuery();
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
            // Removed two-element iteration because java collections hate that
            // episodic_memory.cpp:1761
            initMinMax(time_max, db.minmax_select_node, epmem_node_maxes, epmem_node_mins);
            initMinMax(time_max, db.minmax_select_edge, epmem_edge_maxes, epmem_edge_mins);
            // episodic_memory.cpp:1780
        }
        
        // get id pools
        {
            long q0;
            long w;
            long q1;
            long parent_id;

            // epmem_hashed_id_pool **hp;
            Map<Long, Map<Long, Long>> hp;
            // epmem_id_pool **ip;
            Map<Long, Long> ip;

            PreparedStatement temp_q = db.edge_unique_select;
            final ResultSet rs = temp_q.executeQuery();
            try
            {
                // while ( temp_q->execute() == soar_module::row )
                while (rs.next())
                {
                    q0 = rs.getLong(0 + 1);
                    w = rs.getLong(1 + 1);
                    q1 = rs.getLong(2 + 1);
                    parent_id = rs.getLong(3 + 1);

                    // create new epmem_hashed_id_pool for q0 if it doesn't exist in epmem_id_repository
                    // hp = &(*my_agent->epmem_id_repository)[ q0 ];
                    // if ( !(*hp) )
                    //     (*hp) = new epmem_hashed_id_pool;
                    hp = epmem_id_repository.get(q0);
                    if (hp == null)
                    {
                        hp = Maps.newHashMap();
                        epmem_id_repository.put(q0, hp);
                    }

                    //   ip = &(*(*hp))[ w ];
                    //   if ( !(*ip) )
                    //       (*ip) = new epmem_id_pool;
                    ip = hp.get(w);
                    if (ip == null)
                    {
                        ip = Maps.newHashMap();
                        hp.put(w, ip);
                    }
                    
                    //   (*(*ip))[ q1 ] = parent_id;
                    ip.put(q1, parent_id);
                    
                    //   hp = &(*my_agent->epmem_id_repository)[ q1 ];
                    //   if ( !(*hp) )
                    //       (*hp) = new epmem_hashed_id_pool; 
                    hp = epmem_id_repository.get(q1);
                    if (hp == null)
                    {
                        hp = Maps.newHashMap();
                        epmem_id_repository.put(q1, hp);
                    }
                }
            }
            finally
            {
                rs.close();
            }
        }
        
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
    
    /*
     * (non-Javadoc)
     * @see org.jsoar.kernel.epmem.EpisodicMemory#initializeNewContext(org.jsoar.kernel.memory.WorkingMemory, org.jsoar.kernel.symbols.IdentifierImpl)
     */
    @Override
    public void initializeNewContext(WorkingMemory wm, IdentifierImpl id)
    {
    	this.stateInfos.put(id, new EpisodicMemoryStateInfo());
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.EpisodicMemory#epmem_reset(org.jsoar.kernel.symbols.IdentifierImpl)
     */
    @Override
    public void epmem_reset(IdentifierImpl state)
    {
        // episodic_memory.cpp:1470:epmem_reset()
        if ( state == null )
        {
            state = decider.top_goal;
        }
        
        while( state != null) 
        {
            final EpisodicMemoryStateInfo data = stateInfos.remove(state);
            
            data.last_ol_time = 0;
            
            data.last_cmd_time = 0;
            data.last_cmd_count = 0;

            data.last_memory = EPMEM_MEMID_NONE;

            // this will be called after prefs from goal are already removed,
            // so just clear out result stack
            data.epmem_wmes.clear();

            state = state.goalInfo.lower_goal;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.jsoar.kernel.epmem.EpisodicMemory#epmem_go(boolean)
     */
	@Override
	public void epmem_go(boolean allow_store) {
		// TODO stub
	}

	/*
	 * (non-Javadoc)
	 * @see org.jsoar.kernel.epmem.EpisodicMemory#epmem_enabled()
	 */
	@Override
	public boolean epmem_enabled() {
		// CK: C++ code
//		return ( my_agent->epmem_params->learning->get_value() == soar_module::on );
		// TODO stub
		return false;
	}

	@Override
	public Phase getPhase() {
		return params.phase.get();
	}
}
