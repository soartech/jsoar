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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Optimization;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Phase;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.modules.SoarModule;
import org.jsoar.kernel.smem.SemanticMemoryStateInfo;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.ByRef;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;
import org.jsoar.util.properties.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * <h2>Variance from CSoar Implementation</h2>
 * <p>
 * The smem_data_struct that was added to every identifier in CSoar is instead
 * maintained in a map from id to {@link EpisodicMemoryStateInfo} in this class.
 * 
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
 * 
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
        var_rit_offset_2, var_rit_leftroot_2, var_rit_rightroot_2, var_rit_minstep_2, var_next_id
    }

    /**
     * episodic_memory.h:30:EPMEM_MEMID_NONE
     */
    public static final long EPMEM_MEMID_NONE = 0;

    /**
     * episodic_memory.h:52:EPMEM_NODEID_BAD
     */
    public static final long EPMEM_NODEID_BAD = -1;

    /**
     * episodic_memory.h:53:EPMEM_HASH_ACCEPTABLE
     */
    public static final long EPMEM_HASH_ACCEPTABLE = 1;

    /**
     * episodic_memory.h:63:EPMEM_RIT_ROOT
     */
    private static final long EPMEM_RIT_ROOT = 0;
    
    /**
     * episodic_memory.h:65:EPMEM_LN_2
     */
    private static final double EPMEM_LN_2 = 0.693147180559945;
    
    private static class epmem_rit_state_param
    {
        long /* soar_module::integer_stat */stat;

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
    private Agent agent;

    private DefaultEpisodicMemoryParams params;
    private DefaultEpisodicMemoryStats stats;

    private Decider decider;
    SymbolFactoryImpl symbols;
    EpisodicMemoryDatabase db;

    /** agent.h:epmem_validation */
    private/* uintptr_t */long epmem_validation = 0;

    /** agent.h:epmem_node_removals */
    private Map</* epmem_node_id */Long, Boolean> epmem_node_removals;

    /** agent.h:epmem_node_mins */
    private List</* epmem_time_id */Long> epmem_node_mins;

    /** agent.h:epmem_node_maxes */
    private List<Boolean> epmem_node_maxes;

    /** agent.h:epmem_edge_removals */
    private Map</* epmem_node_id */Long, Boolean> epmem_edge_removals;

    /** agent.h:epmem_edge_mins */
    private List</* epmem_time_id */Long> epmem_edge_mins;

    /** agent.h:epmem_edge_maxes */
    private List<Boolean> epmem_edge_maxes;

    /** agent.h:epmem_id_repository */
    private Map<Long, Map<Long, Map<Long, Long>>> /* epmem_parent_id_pool */epmem_id_repository;

    /** agent.h:epmem_id_replacement */
    private Map<Long, Map<Long, Long>> /* epmem_return_id_pool */epmem_id_replacement;

    /** agent.h:epmem_id_ref_counts */
    private Map<Long, Set<WmeImpl>> /* epmem_id_ref_counter */epmem_id_ref_counts;

    /** agent.h:epmem_id_removes */
    private Deque<SymbolImpl> /* epmem_symbol_stack */epmem_id_removes;

    /** agent.h:epmem_wme_adds */
    private final Set<IdentifierImpl> /* epmem_symbol_set */epmem_wme_adds = new HashSet<IdentifierImpl>();

    /** agent.h:epmem_promotions */
    private final Set<SymbolImpl> /* epmem_symbol_set */epmem_promotions = new HashSet<SymbolImpl>();
    
    /** episodic_memory.h:51:EPMEM_NODEID_ROOT */
    private static final Long EPMEM_NODEID_ROOT = 0L;

    /** episodic_memory.h:69:EPMEM_RIT_STATE_NODE */
    private static final int EPMEM_RIT_STATE_NODE = 0;

    /** episodic_memory.h:70:EPMEM_RIT_STATE_EDGE */
    private static final int EPMEM_RIT_STATE_EDGE = 1;

    /** episodic_memory.h:64:EPMEM_RIT_OFFSET_INIT */
    private static final int EPMEM_RIT_OFFSET_INIT = -1;

    /** agent.h:904:epmem_rit_state_graph */
    private final epmem_rit_state[] epmem_rit_state_graph = new epmem_rit_state[] { new epmem_rit_state(),
            new epmem_rit_state() };

    // bool epmem_first_switch;

    EpisodicMemorySymbols predefinedSyms;

    private final Map<IdentifierImpl, EpisodicMemoryStateInfo> stateInfos = 
            new HashMap<IdentifierImpl, EpisodicMemoryStateInfo>();
    
    private final SoarModule soarModule = new SoarModule();

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
     * This is called when the agent is initialized. This code here in CSoar is
     * usually run at agent creation.
     */
    public void initialize()
    {
        agent = Adaptables.adapt(context, Agent.class);
        symbols = Adaptables.require(DefaultEpisodicMemory.class, context, SymbolFactoryImpl.class);

        final PropertyManager properties = Adaptables.require(DefaultEpisodicMemory.class, context,
                PropertyManager.class);
        decider = Adaptables.adapt(context, Decider.class);
        params = new DefaultEpisodicMemoryParams(properties, symbols);
        stats = new DefaultEpisodicMemoryStats(properties);

        predefinedSyms = new EpisodicMemorySymbols(symbols);
        
        // CK: not implementing timers
        // src/agent.cpp:369: newAgent->epmem_timers = new
        // epmem_timer_container( newAgent );

        // src/agent.cpp:393: newAgent->epmem_node_removals = new
        // epmem_id_removal_map();
        epmem_node_removals = Maps.newHashMap();
        // src/agent.cpp:375: newAgent->epmem_node_mins = new
        // std::vector<epmem_time_id>();
        epmem_node_mins = Lists.newArrayList();
        // src/agent.cpp:376: newAgent->epmem_node_maxes = new
        // std::vector<bool>();
        epmem_node_maxes = Lists.newArrayList();
        // CK: initialization depends on whether USE_MEM_POOL_ALLOCATORS is
        // defined
        // src/agent.cpp:386: newAgent->epmem_edge_removals =
        // new epmem_id_removal_map( std::less< epmem_node_id >(),
        // soar_module::soar_memory_pool_allocator< std::pair< epmem_node_id,
        // bool > >( newAgent ) );
        // src/agent.cpp:394: newAgent->epmem_edge_removals = new
        // epmem_id_removal_map();
        epmem_edge_removals = Maps.newHashMap();
        // src/agent.cpp:378: newAgent->epmem_edge_mins = new
        // std::vector<epmem_time_id>();
        epmem_edge_mins = Lists.newArrayList();
        // src/agent.cpp:379: newAgent->epmem_edge_maxes = new
        // std::vector<bool>();
        epmem_edge_maxes = Lists.newArrayList();

        epmem_id_repository = Maps.newHashMap();
        epmem_id_replacement = Maps.newHashMap();
        epmem_id_ref_counts = Maps.newHashMap();
        epmem_id_removes = Lists.newLinkedList();

        // TODO implement these?
        // src/agent.cpp:372: newAgent->epmem_stmts_common = NULL;
        // src/agent.cpp:373: newAgent->epmem_stmts_graph = NULL;
        // src/agent.cpp:403: newAgent->epmem_first_switch = true;

        // this is done in creation instead: src/agent.cpp:388:
        // newAgent->epmem_wme_adds = new epmem_symbol_set( std::less< Symbol*
        // >(), soar_module::soar_memory_pool_allocator< Symbol* >( newAgent )
        // );

        // CK: don't need memory pools in java:
        // src/agent.cpp:389: newAgent->epmem_promotions = new epmem_symbol_set(
        // std::less< Symbol* >(), soar_module::soar_memory_pool_allocator<
        // Symbol* >( newAgent ) );
        // src/agent.cpp:391: newAgent->epmem_id_removes = new
        // epmem_symbol_stack( soar_module::soar_memory_pool_allocator< Symbol*
        // >( newAgent ) );
        // src/agent.cpp:399: newAgent->epmem_id_removes = new
        // epmem_symbol_stack();
        
        // CK: in smem this is called from smem_attach, there is no equivalent
        // function in episodic_memory.cpp
        epmem_init_db_catch();
        
        soarModule.initialize(context);
    }

    EpisodicMemoryDatabase getDatabase()
    {
        return db;
    }
    
    DefaultEpisodicMemoryParams getParams()
    {
        return params;
    }

    /**
     * Opens the SQLite database and performs all initialization required for
     * the current mode
     * 
     * <p>
     * episodic_memory.cpp:1458:epmem_init_db
     * 
     * @throws SoarException
     */
    void epmem_init_db() throws SoarException
    {
        epmem_init_db(false);
    }

    /**
     * similar to epmem_init_db except this catches and logs the exception
     */
    private void epmem_init_db_catch()
    {
        try
        {
            epmem_init_db();
        }
        catch (SoarException e)
        {
            logger.error("While initializing epmem: " + e.getMessage(), e);
            agent.getPrinter().error("While initializing epmem: " + e.getMessage());
        }
    }

    /**
     * Opens the SQLite database and performs all initialization required for
     * the current mode
     * 
     * <p>
     * The readonly param should only be used in experimentation where you don't
     * want to alter previous database state.
     * 
     * <p>
     * episodic_memory.cpp:1458:epmem_init_db
     * 
     * @param readonly
     * @throws SoarException
     */
    void epmem_init_db(boolean readonly /* = false */) throws SoarException
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
     * <p>
     * episodic_memory.cpp:1496:epmem_init_db
     * 
     * @throws SQLException
     * @throws IOException
     * @throws SoarException
     * 
     */
    private void applyDatabasePerformanceOptions() throws SQLException, SoarException, IOException
    {
        // TODO EPMEM SMEM a lot of this database code is identical between the
        // two modules
        // and could be factored out.

        // apply performance options

        // cache
        if (params.driver.equals("org.sqlite.JDBC"))
        {
            // TODO: Generalize this. Move to a resource somehow.
            final int cacheSize;
            switch (params.cache.get())
            {
            case small:
                cacheSize = 5000;
                break; // 5MB cache
            case medium:
                cacheSize = 20000;
                break; // 20MB cache
            case large:
            default:
                cacheSize = 100000; // 100MB cache
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
            if (perfStream != null)
            {
                logger.info("Applying performance settings from '" + fullPath + "'.");
                try
                {
                    JdbcTools.executeSql(db.getConnection(), perfStream, null /* no filter */);
                }
                finally
                {
                    perfStream.close();
                }
            }
            else
            {
                logger.warn("Could not find performance resource at '" + fullPath
                        + "'. No performance settings applied.");
            }
        }

        // TODO EPMEM page_size
    }

    private void initMinMax(long time_max, PreparedStatement minmax_select, List<Boolean> minmax_max,
            List<Long> minmax_min) throws SQLException
    {
        final ResultSet rs = minmax_select.executeQuery();
        try
        {
            while (rs.next())
            {
                // if ( temp_q->column_type( 0 ) != soar_module::null_t )
                if (db.column_type(rs.getMetaData().getColumnType(0 + 1)) != EpisodicMemoryDatabase.value_type.null_t)
                {
                    // std::vector<bool>::size_type num_ids =
                    // temp_q->column_int( 0 );
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
     * <p>
     * episodic_memory.cpp:1458:epmem_init_db
     * 
     * @param readonly
     * @throws SoarException
     */
    private void epmem_init_db_ex(boolean readonly /* = false */) throws SQLException, IOException, SoarException
    {
        if (db != null /*
                        * my_agent->epmem_db->get_status() !=
                        * soar_module::disconnected
                        */)
        {
            return;
        }

        // //////////////////////////////////////////////////////////////////////////
        // TODO EPMEM my_agent->epmem_timers->init->start();
        // //////////////////////////////////////////////////////////////////////////

        // attempt connection
        final String jdbcUrl = params.protocol.get() + ":" + params.path.get();
        final Connection connection = JdbcTools.connect(params.driver.get(), jdbcUrl);
        final DatabaseMetaData meta = connection.getMetaData();
        logger.info("Opened database '" + jdbcUrl + "' with " + meta.getDriverName() + ":" + meta.getDriverVersion());
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
        for (int i = EPMEM_RIT_STATE_NODE; i <= EPMEM_RIT_STATE_EDGE; i++)
        {
            epmem_rit_state_graph[i].offset.stat = EPMEM_RIT_OFFSET_INIT;
            epmem_rit_state_graph[i].leftroot.stat = 0;
            epmem_rit_state_graph[i].rightroot.stat = 1;
            epmem_rit_state_graph[i].minstep.stat = Long.MAX_VALUE;
        }
        epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].add_query = db.add_node_range;
        epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].add_query = db.add_edge_range;

        // //

        // get/set RIT variables
        {
            final ByRef<Long> var_val = ByRef.create(0L);

            for (int i = EPMEM_RIT_STATE_NODE; i <= EPMEM_RIT_STATE_EDGE; i++)
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
                    epmem_set_variable(epmem_rit_state_graph[i].leftroot.var_key,
                            epmem_rit_state_graph[i].leftroot.stat);
                }

                // rightroot
                if (epmem_get_variable(epmem_rit_state_graph[i].rightroot.var_key, var_val))
                {
                    epmem_rit_state_graph[i].rightroot.stat = var_val.value;
                }
                else
                {
                    epmem_set_variable(epmem_rit_state_graph[i].rightroot.var_key,
                            epmem_rit_state_graph[i].rightroot.stat);

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

        // //

        // get max time
        {
            final PreparedStatement temp_q = db.get_max_time;

            final ResultSet rs = temp_q.executeQuery();
            try
            {
                if (rs.next())
                {
                    // my_agent->epmem_stats->time->set_value(
                    // temp_q->column_int( 0 ) + 1 );
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
            long time_last = (time_max - 1);

            final PreparedStatement[] now_select = new PreparedStatement[] { db.now_select_node, db.now_select_edge };
            final PreparedStatement[] now_add = new PreparedStatement[] { db.add_node_point, db.add_edge_point };
            final PreparedStatement[] now_delete = new PreparedStatement[] { db.now_delete_node, db.now_delete_edge };

            for (int i = EPMEM_RIT_STATE_NODE; i <= EPMEM_RIT_STATE_EDGE; i++)
            {
                final PreparedStatement temp_q = now_add[i];
                temp_q.setLong(2, time_last);

                final PreparedStatement temp_q2 = now_select[i];
                final ResultSet rs = temp_q2.executeQuery();
                try
                {
                    // while ( temp_q2->execute() == soar_module::row )
                    while (rs.next())
                    {
                        // range_start = temp_q2->column_int( 1 );
                        long range_start = rs.getLong(1 + 1);

                        // point
                        if (range_start == time_last)
                        {
                            temp_q.setLong(1, rs.getLong(0 + 1));
                            temp_q.executeUpdate( /* soar_module::op_reinit */);
                        }
                        else
                        {
                            epmem_rit_insert_interval(range_start, time_last, rs.getLong(0 + 1),
                                    epmem_rit_state_graph[i]);
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

                    // create new epmem_hashed_id_pool for q0 if it doesn't
                    // exist in epmem_id_repository
                    // hp = &(*my_agent->epmem_id_repository)[ q0 ];
                    // if ( !(*hp) )
                    // (*hp) = new epmem_hashed_id_pool;
                    hp = epmem_id_repository.get(q0);
                    if (hp == null)
                    {
                        hp = Maps.newHashMap();
                        epmem_id_repository.put(q0, hp);
                    }

                    // ip = &(*(*hp))[ w ];
                    // if ( !(*ip) )
                    // (*ip) = new epmem_id_pool;
                    ip = hp.get(w);
                    if (ip == null)
                    {
                        ip = Maps.newHashMap();
                        hp.put(w, ip);
                    }

                    // (*(*ip))[ q1 ] = parent_id;
                    ip.put(q1, parent_id);

                    // hp = &(*my_agent->epmem_id_repository)[ q1 ];
                    // if ( !(*hp) )
                    // (*hp) = new epmem_hashed_id_pool;
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

        // capture augmentations of top-state as the sole set of adds,
        // which catches up to what would have been incremental encoding
        // to this point
        {
            epmem_wme_adds.add(decider.top_state); // my_agent->epmem_wme_adds->insert(
                                                   // my_agent->top_state );
        }

        // if lazy commit, then we encapsulate the entire lifetime of the agent
        // in a single transaction
        if (params.lazy_commit.get())
        {
            db.begin.executeUpdate( /* soar_module::op_reinit */);
        }

        // //////////////////////////////////////////////////////////////////////////
        // TODO EPMEM my_agent->epmem_timers->init->stop();
        // //////////////////////////////////////////////////////////////////////////
    }

    private void epmem_rit_insert_interval(long lower, long upper, long id, epmem_rit_state rit_state) throws SQLException
    {
        // initialize offset
        long offset = rit_state.offset.stat;
        if ( offset == EPMEM_RIT_OFFSET_INIT )
        {
            offset = lower;

            // update database
            epmem_set_variable( rit_state.offset.var_key, offset );

            // update stat
            rit_state.offset.stat = offset;
        }

        // get node
        long node;
        {
            long left_root = rit_state.leftroot.stat;
            long right_root = rit_state.rightroot.stat;
            long min_step = rit_state.minstep.stat;

            // shift interval
            long l = ( lower - offset );
            long u = ( upper - offset );

            // update left_root
            if ( ( u < EPMEM_RIT_ROOT ) && ( l <= ( 2 * left_root ) ) )
            {
                left_root = (long) Math.pow( -2.0, Math.floor( Math.log( -l ) / EPMEM_LN_2 ) );

                // update database
                epmem_set_variable( rit_state.leftroot.var_key, left_root );

                // update stat
                rit_state.leftroot.stat = left_root;
            }

            // update right_root
            if ( ( l > EPMEM_RIT_ROOT ) && ( u >= ( 2 * right_root ) ) )
            {
                right_root = (long) Math.pow( 2.0, Math.floor( Math.log( u ) / EPMEM_LN_2 ) );

                // update database
                epmem_set_variable( rit_state.rightroot.var_key, right_root );

                // update stat
                rit_state.rightroot.stat = right_root;
            }

            // update min_step
            EpmemRitForkNodeResult forkNodeResult = epmem_rit_fork_node( l, u, /*true,*/ rit_state );
            long step = forkNodeResult.step;
            node = forkNodeResult.node;

            if ( ( node != EPMEM_RIT_ROOT ) && ( step < min_step ) )
            {
                min_step = step;

                // update database
                epmem_set_variable( rit_state.minstep.var_key, min_step );

                // update stat
                rit_state.minstep.stat = min_step;
            }
        }

        // perform insert
        // ( node, start, end, id )
        rit_state.add_query.setLong( 1, node );
        rit_state.add_query.setLong( 2, lower );
        rit_state.add_query.setLong( 3, upper );
        rit_state.add_query.setLong( 4, id );
        rit_state.add_query.executeUpdate(/*soar_module::op_reinit*/);
    }
    
    private static final class EpmemRitForkNodeResult
    {
        public final long node;
        public final long step;
        
        public EpmemRitForkNodeResult(long node, long step)
        {
            this.node = node;
            this.step = step;
        }
    }
    
    private final EpmemRitForkNodeResult epmem_rit_fork_node(long lower, long uppper, epmem_rit_state rit_state)
    {
        return null;
    }

    /**
     * Gets an EpMem variable from the database
     * 
     * <p>
     * episodic_memory.cpp:984:epmem_get_variable
     * 
     * @param variable_id
     * @param variable_value
     * @return
     * @throws SQLException
     */
    boolean epmem_get_variable(epmem_variable_key variable_id, ByRef<Long> variable_value) throws SQLException
    {
        final PreparedStatement var_get = db.var_get;

        var_get.setInt(1, variable_id.ordinal());
        final ResultSet rs = var_get.executeQuery();
        try
        {
            if (rs.next())
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
     * <p>
     * episodic_memory.cpp:1007:epmem_set_variable
     * 
     * @param variable_id
     * @param variable_value
     * @throws SQLException
     */
    void epmem_set_variable(epmem_variable_key variable_id, long variable_value) throws SQLException
    {
        final PreparedStatement var_set = db.var_set;

        var_set.setInt(1, variable_id.ordinal());
        var_set.setLong(2, variable_value);

        var_set.execute();
    }

    @Override
    public void epmem_close() throws SoarException
    {
        if (db != null)
        {
            try
            {
                // TODO this is copy-paste from smem right now, there are other
                // things to do here

                // close the database
                db.getConnection().close();
                db = null;
            }
            catch (SQLException e)
            {
                throw new SoarException("While closing epmem: " + e.getMessage(), e);
            }
        }

        // episodic_memory.cpp:1415:my_agent->epmem_wme_adds->clear();
        epmem_wme_adds.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jsoar.kernel.epmem.EpisodicMemory#initializeNewContext(org.jsoar.
     * kernel.memory.WorkingMemory, org.jsoar.kernel.symbols.IdentifierImpl)
     */
    @Override
    public void initializeNewContext(WorkingMemory wm, IdentifierImpl id)
    {
        this.stateInfos.put(id, new EpisodicMemoryStateInfo(this, wm, id));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jsoar.kernel.smem.EpisodicMemory#epmem_reset(org.jsoar.kernel.symbols
     * .IdentifierImpl)
     */
    @Override
    public void epmem_reset(IdentifierImpl state)
    {
        // episodic_memory.cpp:1470:epmem_reset()
        if (state == null)
        {
            state = decider.top_goal;
        }

        while (state != null)
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
     * 
     * @see org.jsoar.kernel.epmem.EpisodicMemory#epmem_go()
     */
    @Override
    public void epmem_go()
    {
        epmem_go(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.epmem.EpisodicMemory#epmem_go(boolean)
     */
    @Override
    public void epmem_go(boolean allow_store)
    {
        // my_agent->epmem_timers->total->start();
        //
        // #ifndef EPMEM_EXPERIMENT
        //
        // if ( allow_store )
        // {
        // epmem_consider_new_episode( my_agent );
        // }
        // epmem_respond_to_cmd( my_agent );
        if (allow_store)
        {
            epmem_consider_new_episode();
        }
        try
        {
            epmem_respond_to_cmd();
        }
        catch (SoarException e)
        {
            logger.error("While responding to epmem command: " + e.getMessage(), e);
            agent.getPrinter().error("While responding to epmem command: " + e.getMessage());
        }
        //
        // #else // EPMEM_EXPERIMENT
        //
        // _epmem_exp( my_agent );
        // epmem_respond_to_cmd( my_agent );
        //
        // #endif // EPMEM_EXPERIMENT
        //
        // my_agent->epmem_timers->total->stop();
    }

    /**
     * Based upon trigger/force parameter settings, potentially records a new
     * episode
     * 
     * <p>
     * episodic_memory.cpp:epmem_consider_new_episode( agent *my_agent )
     */
    private boolean epmem_consider_new_episode()
    {
        // ////////////////////////////////////////////////////////////////////////////
        // my_agent->epmem_timers->trigger->start();
        // ////////////////////////////////////////////////////////////////////////////
        //
        boolean new_memory = false;

        if (params.force.get() == DefaultEpisodicMemoryParams.Force.off)
        {
            switch (params.trigger.get())
            {
            case output:
                // examine all commands on the output-link for any
                // // that appeared since last memory was recorded
                EpisodicMemoryStateInfo stateInfo = stateInfos.get(decider.top_goal);
                for (Wme wme : agent.getInputOutput().getPendingCommands())
                {
                    if (wme.getTimetag() > stateInfo.last_ol_time)
                    {
                        new_memory = true;
                        stateInfo.last_ol_time = wme.getTimetag();
                    }
                }
                break;
            case dc:
                new_memory = true;
                break;
            case none:
                new_memory = false;
                break;
            }
        }
        else
        {
            new_memory = params.force.get() == DefaultEpisodicMemoryParams.Force.remember;
            params.force.set(DefaultEpisodicMemoryParams.Force.off);
        }

        // ////////////////////////////////////////////////////////////////////////////
        // my_agent->epmem_timers->trigger->stop();
        // ////////////////////////////////////////////////////////////////////////////

        if (new_memory)
        {
            try
            {
                epmem_new_episode();
            }
            catch (SQLException e)
            {
                logger.error("While recording new epmem episode: " + e.getMessage(), e);
                agent.getPrinter().error("While recording new epmem episode: " + e.getMessage());
            }
        }

        return new_memory;
    }

    /**
     * <p>
     * episodic_memory.cpp:2473:epmem_new_episode( agent *my_agent )
     * 
     * @throws SQLException
     */
    private void epmem_new_episode() throws SQLException
    {
        // if this is the first episode, initialize db components
        epmem_init_db_catch();

        // add the episode only if db is properly initialized2
        if (this.db == null)
            return;

        // //////////////////////////////////////////////////////////////////////////
        // my_agent->epmem_timers->storage->start();
        // //////////////////////////////////////////////////////////////////////////

        long time_counter = stats.getTime();// my_agent->epmem_stats->time->get_value();

        // // provide trace output
        // if ( my_agent->sysparams[ TRACE_EPMEM_SYSPARAM ] )
        // {
        // char buf[256];
        //
        // SNPRINTF( buf, 254, "NEW EPISODE: %ld", static_cast<long
        // int>(time_counter) );
        //
        // print( my_agent, buf );
        // xml_generate_warning( my_agent, buf );
        // }

        // perform storage
        {
            // seen nodes (non-identifiers) and edges (identifiers)
            Queue<Long> epmem_node = new LinkedList<Long>();
            Queue<Long> epmem_edge = new LinkedList<Long>();

            // walk appropriate levels
            {
                // prevents infinite loops
                final Marker tc = DefaultMarker.create(); // get_new_tc_number( my_agent );

                // children of the current identifier
                List<WmeImpl> wmes = null;

                // breadth first search state
                Queue<SymbolImpl> parent_syms = new LinkedList<SymbolImpl>();
                SymbolImpl parent_sym = null;
                Queue<Long> parent_ids = new LinkedList<Long>();
                long parent_id;

                // cross-level information
                Map<WmeImpl, EpisodicMemoryIdReservation> id_reservations = new HashMap<WmeImpl, EpisodicMemoryIdReservation>();
                Set<SymbolImpl> new_identifiers = new HashSet<SymbolImpl>();

                // start with new WMEs attached to known identifiers
                for (IdentifierImpl id_p : epmem_wme_adds)
                {
                    // make sure the WME is valid
                    // it can be invalid if a child WME was added, but then the
                    // parent was removed, setting the epmem_id to
                    // EPMEM_NODEID_BAD
                    if (id_p.epmem_id != EPMEM_NODEID_BAD)
                    {
                        parent_syms.add(id_p);
                        parent_ids.add(id_p.epmem_id);
                        while (!parent_syms.isEmpty())
                        {
                            parent_sym = parent_syms.poll();
                            parent_id = parent_ids.poll();
                            wmes = epmem_get_augs_of_id(parent_sym, tc);
                            if (!wmes.isEmpty())
                            {
                                _epmem_store_level(parent_syms, parent_ids, tc, wmes, parent_id, time_counter,
                                        id_reservations, new_identifiers, epmem_node, epmem_edge);
                            }
                            wmes = null;
                        }
                    }
                }
            }

            // all inserts
            {
                long temp_node;

                // #ifdef EPMEM_EXPERIMENT
                // epmem_dc_interval_inserts = epmem_node.size() +
                // epmem_edge.size();
                // #endif

                // nodes
                while (!epmem_node.isEmpty())
                {
                    temp_node = epmem_node.element();

                    // add NOW entry
                    // id = ?, start = ?
                    db.add_edge_now.setLong(1, temp_node); // my_agent->epmem_stmts_graph->add_node_now->bind_int(
                    // 1, (*temp_node) );
                    db.add_node_now.setLong(2, time_counter); // my_agent->epmem_stmts_graph->add_node_now->bind_int(
                    // 2, time_counter
                    // );
                    db.add_node_now.executeUpdate(/* soar_module::op_reinit */); // my_agent->epmem_stmts_graph->add_node_now->execute(
                    // soar_module::op_reinit
                    // );

                    // update min
                    epmem_node_mins.set((int) temp_node - 1, time_counter);

                    epmem_node.poll();
                }

                // edges
                while (!epmem_edge.isEmpty())
                {
                    temp_node = epmem_edge.element();

                    // add NOW entry
                    // id = ?, start = ?
                    db.add_edge_now.setLong(1, temp_node); // my_agent->epmem_stmts_graph->add_edge_now->bind_int(
                    // 1, (*temp_node) );
                    db.add_edge_now.setLong(2, time_counter); // my_agent->epmem_stmts_graph->add_edge_now->bind_int(
                    // 2, time_counter
                    // );
                    db.add_edge_now.executeUpdate(/* soar_module::op_reinit */);// my_agent->epmem_stmts_graph->add_edge_now->execute(
                    // soar_module::op_reinit
                    // );

                    // update min
                    epmem_edge_mins.set((int) temp_node - 1, time_counter);

                    db.update_edge_unique_last.setLong(1, Long.MAX_VALUE); // my_agent->epmem_stmts_graph->update_edge_unique_last->bind_int(
                    // 1,
                    // LLONG_MAX
                    // );
                    db.update_edge_unique_last.setLong(2, temp_node); // my_agent->epmem_stmts_graph->update_edge_unique_last->bind_int(
                    // 2,
                    // *temp_node
                    // );
                    db.update_edge_unique_last.executeUpdate(/*
                     * soar_module::
                     * op_reinit
                     */); // my_agent->epmem_stmts_graph->update_edge_unique_last->execute(
                    // soar_module::op_reinit
                    // );

                    epmem_edge.poll();
                }
            }

            //all removals
            {
                long /*epmem_time_id*/ range_start;
                long /*epmem_time_id*/ range_end;

                //#ifdef EPMEM_EXPERIMENT
                //epmem_dc_interval_removes = 0;
                //#endif

                // nodes
                for(Map.Entry<Long, Boolean> r : epmem_node_removals.entrySet())
                {
                    if(r.getValue())
                    {
                        //#ifdef EPMEM_EXPERIMENT
                        //epmem_dc_interval_removes++;
                        //#endif

                        // remove NOW entry
                        // id = ?
                        db.delete_edge_now.setLong(1, r.getKey());
                        db.delete_edge_now.executeUpdate(/*soar_module::op_reinit*/);

                        range_start = epmem_node_mins.get((int)(r.getKey()-1));
                        range_end = ( time_counter - 1 );

                        // point (id, start)
                        if ( range_start == range_end )
                        {
                            db.add_node_point.setLong(1, r.getKey());
                            db.add_node_point.setLong(2, range_start);
                            db.add_edge_point.executeUpdate(/*soar_module::op_reinit*/);
                        }
                        // node
                        else
                        {
                            epmem_rit_insert_interval(range_start, range_end, r.getKey(), epmem_rit_state_graph[EPMEM_RIT_STATE_NODE]);
                        }

                        // update max
                        epmem_node_maxes.set((int)(r.getKey()-1), true);
                    }
                }
                epmem_node_removals.clear();

                // edges
                for(Map.Entry<Long, Boolean> r : epmem_edge_removals.entrySet()) 
                {
                    if ( r.getValue() )
                    {
                        //#ifdef EPMEM_EXPERIMENT
                        //epmem_dc_interval_removes++;
                        //#endif

                        // remove NOW entry
                        // id = ?
                        db.delete_edge_now.setLong(1, r.getKey());
                        db.delete_edge_now.executeUpdate(/*soar_module::op_reinit*/);

                        range_start = epmem_edge_mins.get((int)(r.getKey()-1));
                        range_end = ( time_counter - 1 );

                        db.update_edge_unique_last.setLong(1, range_end);
                        db.update_edge_unique_last.setLong(2, r.getKey());
                        db.update_edge_unique_last.executeUpdate(/*soar_module::op_reinit*/);

                        // point (id, start)
                        if ( range_start == range_end )
                        {
                            db.add_edge_point.setLong(1, r.getKey());
                            db.add_edge_point.setLong(2, range_start);
                            db.add_edge_point.executeUpdate(/*soar_module::op_reinit*/);
                        }
                        // node
                        else
                        {
                            epmem_rit_insert_interval(range_start, range_end, r.getKey(), epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE]);
                        }

                        // update max
                        epmem_edge_maxes.set((int)(r.getKey() - 1), true);
                    }
                }
                epmem_edge_removals.clear();
            }

            // all in-place lti promotions
            {
                for(SymbolImpl p_it : epmem_promotions)
                {
                    if((p_it.asIdentifier().smem_time_id == time_counter ) && (
                            p_it.asIdentifier().id_smem_valid == epmem_validation ) )
                    {
                        _epmem_promote_id(p_it, time_counter );
                    }
                    //SJK: I don't believe this is necessary; see DefaultSemanticMemory.java:1648
                    //symbol_remove_ref( my_agent, (*p_it) );
                }
                epmem_promotions.clear();
            }

            // add the time id to the times table
            db.add_time.setLong(1, time_counter);
            db.add_time.executeUpdate(/*soar_module::op_reinit*/);

            stats.setTime( time_counter + 1 );

            // update time wme on all states
            {
                SymbolImpl state = decider.bottom_goal;
                SymbolImpl my_time_sym = symbols.createInteger(time_counter+1);

                while ( state != null )
                {
                    EpisodicMemoryStateInfo stateInfo = stateInfos.get(state.asIdentifier());
                    if ( stateInfo.epmem_time_wme != null )
                    {
                        soarModule.remove_module_wme(stateInfo.epmem_time_wme);
                    }

                    stateInfo.epmem_time_wme = soarModule.add_module_wme(stateInfo.epmem_header,
                            predefinedSyms.epmem_sym_present_id, my_time_sym);

                    state = state.asIdentifier().goalInfo.higher_goal;
                }
                //SJK: again, I'm guessing this is unnecessary
                //symbol_remove_ref( my_agent, my_time_sym );
            }

            // clear add/remove maps
            {
                epmem_wme_adds.clear();
            }
        }

        // ////////////////////////////////////////////////////////////////////////////
        // my_agent->epmem_timers->storage->stop();
        // ////////////////////////////////////////////////////////////////////////////
    }

    /**
     * This routine gets all wmes rooted at an id.
     * 
     * <p>episodic_memory.cpp:870:epmem_wme_list *epmem_get_augs_of_id( Symbol * id, tc_number tc )
     * @param sym
     * @param tc
     * @return
     */
    private List<WmeImpl> epmem_get_augs_of_id(SymbolImpl sym, Marker tc)
    {
        List<WmeImpl> /* epmem_wme_list */return_val = Lists.newLinkedList();

        // augs only exist for identifiers
        final IdentifierImpl id = sym.asIdentifier();
        if (id != null && id.tc_number != tc)
        {
            id.tc_number = tc;

            // impasse wmes
            // for ( w=id->id.impasse_wmes; w!=NIL; w=w->next )
            // {
            // return_val->push_back( w );
            // }
            // TODO make sure this is the correct way to get impasse_wmes
            if (id.isGoal())
            {
                Iterator<Wme> it = id.getWmes();
                if (it != null)
                {
                    while (it.hasNext())
                    {
                        return_val.add((WmeImpl) it.next());
                    }
                }
            }

            // input wmes
            if (id.getInputWmes() != null)
            {
                for (WmeImpl wi = id.getInputWmes(); wi.next != null; wi = wi.next)
                {
                    return_val.add(wi);
                }
            }

            // regular wmes
            for (Slot s = id.slots; s != null; s = s.next)
            {
                for (WmeImpl w = s.getWmes(); w != null; w = w.next)
                {
                    return_val.add(w);
                }
                for (WmeImpl w = s.getAcceptablePreferenceWmes(); w != null; w = w.next)
                {
                    return_val.add(w);
                }
            }
        }
        
        return return_val;
    }

    /**
     * three cases for sharing ids amongst identifiers in two passes: <br>
     * <ol>
     * <li>value known in phase one (try reservation)</li>
     * <li>value unknown in phase one, but known at phase two (try assignment
     * adhering to constraint)</li>
     * <li>value unknown in phase one/two (if anything is left, unconstrained
     * assignment)</li>
     * </ol>
     * <p>
     * episodic_memory.cpp:2045:inline void _epmem_store_level( agent* my_agent,
     * std::queue< Symbol* >&parent_syms, std::queue< epmem_node_id >&
     * parent_ids, tc_number tc, epmem_wme_list::iterator w_b,
     * epmem_wme_list::iterator w_e, epmem_node_id parent_id, epmem_time_id
     * time_counter, std::map< wme*, epmem_id_reservation* >& id_reservations,
     * std::set< Symbol* >& new_identifiers, std::queue< epmem_node_id >&
     * epmem_node, std::queue< epmem_node_id >& epmem_edge )
     * 
     * @param parent_syms
     * @param parent_ids
     * @param tc
     * @param wmes
     * @param parent_id
     * @param time_counter
     * @param id_reservations
     * @param new_identifiers
     * @param epmem_node
     * @param epmem_edge
     * @throws SQLException 
     */
    void _epmem_store_level( 
            Queue<SymbolImpl> parent_syms, 
            Queue<Long> parent_ids, 
            Marker tc, 
            List<WmeImpl> w_p, 
            long parent_id, 
            long time_counter,
            Map<WmeImpl, EpisodicMemoryIdReservation > id_reservations, 
            Set< SymbolImpl > new_identifiers, 
            Queue< Long > epmem_node, 
            Queue< Long > epmem_edge ) throws SQLException
    {
    	boolean value_known_apriori = false;
    	
    	// temporal hash
    	long /*epmem_hash_id*/ my_hash;	// attribute
    	long /*epmem_hash_id*/ my_hash2;	// value
    	
        // id repository
        Map<Long, Long> /*epmem_id_pool*/ my_id_repo;
        Map<Long, Long> /*epmem_id_pool*/ my_id_repo2 = null;
        // epmem_id_pool::iterator pool_p;
        EpisodicMemoryIdReservation r_p;
    	EpisodicMemoryIdReservation new_id_reservation;
    	
        // identifier recursion
        // CK: these are unused
        // List<WmeImpl> w_p2;
        // boolean good_recurse = false;
    	
    	// find WME ID for WMEs whose value is an identifier and has a known epmem id 
    	// (prevents ordering issues with unknown children)
    	for(WmeImpl wme : w_p)
    	{
    		// skip over WMEs already in the system
    		if( wme.epmem_id != EPMEM_NODEID_BAD && wme.epmem_valid == epmem_validation)
    		{
    		    continue;
    		}
    		
    		if( wme.value.asIdentifier() != null && 
    		        ( wme.value.asIdentifier().epmem_id != EPMEM_NODEID_BAD && 
    		          wme.value.asIdentifier().epmem_valid == epmem_validation ) &&
    		        wme.value.asIdentifier().smem_lti == 0 )
    		{
    		    // prevent exclusions from being recorded
    		    if(params.exclusions.contains(wme.attr))
    		    {
    		        continue;
    		    }
    		    
    		    // if still here, create reservation (case 1)
                if(wme.acceptable)
                {
                    new_id_reservation = new EpisodicMemoryIdReservation(EPMEM_NODEID_BAD, EPMEM_HASH_ACCEPTABLE);
                }
                else
                {
                    new_id_reservation = new EpisodicMemoryIdReservation(EPMEM_NODEID_BAD, epmem_temporal_hash(wme.attr));
                }
                
                // try to find appropriate reservation
                my_id_repo = epmem_id_repository.get(parent_id).get(new_id_reservation.my_hash);
                if(my_id_repo != null)
                {
                    // TODO make sure std::pair::first is the key and std::pair::second is the value
                    long wmeEpmemId = wme.value.asIdentifier().epmem_id;
                    new_id_reservation.my_id = my_id_repo.get(wmeEpmemId);
                    my_id_repo.remove(wmeEpmemId);
                }
                else
                {
                    // add repository
                    my_id_repo = Maps.newHashMap();
                }
                
                new_id_reservation.my_pool = my_id_repo;
                id_reservations.put(wme, new_id_reservation);
                new_id_reservation = null;
    		}
    	}
    	
    	for(WmeImpl wme : w_p)
    	{
    	    // skip over WMEs already in the system
            if( wme.epmem_id != EPMEM_NODEID_BAD && wme.epmem_valid == epmem_validation)
            {
                continue;
            }

            // prevent exclusions from being recorded
            if(params.exclusions.contains(wme.attr))
            {
                continue;
            }
            
            final IdentifierImpl wmeValueId = wme.value.asIdentifier();
            if (wmeValueId != null)
            {
                wme.epmem_valid = epmem_validation;
                wme.epmem_id = EPMEM_NODEID_BAD;
                
                my_hash = 0;
                my_id_repo = null;

                // if the value already has an epmem_id, the WME ID would have already been assigned above 
                // (ie. the epmem_id of the VALUE is KNOWN APRIORI [sic])
                // however, it's also possible that the value is known but no WME ID is given 
                // (eg. (<s> ^foo <a> ^bar <a>)); this is case 2
                // CK: C++ code:
                // value_known_apriori = ( ( (*w_p)->value->id.epmem_id != EPMEM_NODEID_BAD ) 
                //  && ( (*w_p)->value->id.epmem_valid == my_agent->epmem_validation ) );
                value_known_apriori = (wmeValueId.epmem_id != EPMEM_NODEID_BAD && wmeValueId.epmem_valid == epmem_validation);
                
                // if long-term identifier as value, special processing (we may need to promote, we don't add to/take from any pools
                if (wmeValueId.smem_lti != 0)
                {
                    // find the lti or add new one
                    if(!value_known_apriori)
                    {
                        wmeValueId.epmem_id = EPMEM_NODEID_BAD;
                        wmeValueId.epmem_valid = epmem_validation;
                        
                        // try to find
                        {
                            final PreparedStatement ps = db.find_lti;
                            ps.setLong(1, wmeValueId.getNameLetter());
                            ps.setLong(1, wmeValueId.getNameNumber());
                            final ResultSet rs = ps.executeQuery();
                            try
                            {
                                if (rs.first())
                                {
                                    wmeValueId.epmem_id = rs.getLong(0);
                                }
                            }
                            finally
                            {
                                rs.close();
                            }
                            // CK: no reinitialize for PreparedStatement
                            // my_agent->epmem_stmts_graph->find_lti->reinitialize();
                        }

                        // add if necessary
                        if(wmeValueId.epmem_id == EPMEM_NODEID_BAD)
                        {
                            wmeValueId.epmem_id = stats.getNextId();
                            stats.setNextId(wmeValueId.epmem_id + 1L);
                            epmem_set_variable(epmem_variable_key.var_next_id, wmeValueId.epmem_id + 1L);

                            // add repository
                            Map<Long, Map<Long, Long>> epmem_hashed_id_pool = Maps.newHashMap();
                            epmem_id_repository.put(wmeValueId.epmem_id, epmem_hashed_id_pool);
                            
                            _epmem_promote_id(wmeValueId, time_counter);
                        }
                    }

                    // now perform deliberate edge search
                    // ltis don't use the pools, so we make a direct search in
                    // the edge_unique table
                    // if failure, drop below and use standard channels
                    {
                        // get temporal hash
                        if ( wme.acceptable )
                        {
                            my_hash = EPMEM_HASH_ACCEPTABLE;
                        }
                        else
                        {
                            my_hash = epmem_temporal_hash( wme.attr );
                        }
                        
                        // q0, w, q1
                        final PreparedStatement ps = db.find_edge_unique;
                        ps.setLong(1, parent_id);
                        ps.setLong(2, my_hash);
                        ps.setLong(3, wmeValueId.epmem_id);
                        
                        final ResultSet rs = ps.executeQuery();
                        
                        try
                        {
                            if (rs.first())
                            {
                                wme.epmem_id = rs.getLong(0);
                            }
                        }
                        finally
                        {
                            rs.close();
                        }
                        // CK: no reinitialize for PreparedStatement
                        // my_agent->epmem_stmts_graph->find_edge_unique_shared->reinitialize();
                    }
                }
                else
                {
                    // in the case of a known value, we already have a reservation (case 1)
                    if ( value_known_apriori )
                    {
                        r_p = id_reservations.get(wme);
                        
                        if(r_p != null)
                        {
                            my_hash = r_p.my_hash;
                            my_id_repo2 = r_p.my_pool;
                            
                            if (r_p.my_id != EPMEM_NODEID_BAD)
                            {
                                wme.epmem_id = r_p.my_id;
                                epmem_id_replacement.put(wme.epmem_id, my_id_repo2);
                            }

                            // delete reservation and map entry
                            id_reservations.remove(wme);
                        }
                        // OR a shared identifier at the same level, in which
                        // case we need an exact match (case 2)
                        else
                        {
                            // get temporal hash
                            if (wme.acceptable)
                            {
                                my_hash = EPMEM_HASH_ACCEPTABLE;
                            }
                            else
                            {
                                my_hash = epmem_temporal_hash(wme.attr);
                            }
                            
                            // try to get an id that matches new information
                            my_id_repo = epmem_id_repository.get(parent_id).get(my_hash);
                            if(my_id_repo != null)
                            {
                                if(!my_id_repo.isEmpty())
                                {
                                    Iterator<Long> it = my_id_repo.keySet().iterator();
                                    while(it.hasNext())
                                    {
                                        final Long first = it.next();
                                        if (first == wmeValueId.epmem_id)
                                        {
                                            final Long second = my_id_repo.get(first);
                                            wme.epmem_id = second;
                                            it.remove();
                                            epmem_id_replacement.put(wme.epmem_id, my_id_repo);
                                            break;
                                        }
                                    }
                                }
                            }
                            else
                            {
                                // add repository
                                my_id_repo = Maps.newHashMap();
                            }

                            // keep the address for later (used if w->epmem_id was not assigned)
                            my_id_repo2 = my_id_repo;
                        }
                    }
                    // case 3
                    else
                    {
                        // UNKNOWN identifier
                        new_identifiers.add(wme.value);

                        // get temporal hash
                        if (wme.acceptable)
                        {
                            my_hash = EPMEM_HASH_ACCEPTABLE;
                        }
                        else
                        {
                            my_hash = epmem_temporal_hash(wme.attr);
                        }

                        // try to find node
                        my_id_repo = epmem_id_repository.get(parent_id).get(my_hash);
                        if (my_id_repo != null)
                        {
                            // if something leftover, try to use it
                            if (!my_id_repo.isEmpty())
                            {
                                Iterator<Long> it = my_id_repo.keySet().iterator();
                                while (it.hasNext())
                                {
                                    final Long first = it.next();
                                    // the ref set for this epmem_id may not be there if the pools were regenerated from a previous DB
                                    // a non-existant ref set is the equivalent of a ref count of 0 (ie. an empty ref set)
                                    // so we allow the identifier from the pool to be used
                                    if (epmem_id_ref_counts.get(first) == null || epmem_id_ref_counts.get(first).size() == 0)
                                    {
                                        final Long second = my_id_repo.get(first);
                                        wme.epmem_id = second;
                                        wmeValueId.epmem_id = first;
                                        wmeValueId.epmem_valid = epmem_validation;
                                        it.remove();
                                        epmem_id_replacement.put(wme.epmem_id, my_id_repo);
                                        break;
                                    }
                                }
                            }
                        }
                        else
                        {
                            // add repository
                            my_id_repo = Maps.newHashMap();
                        }
                        
                        // keep the address for later (used if w->epmem_id was not assgined)
                        my_id_repo2 = my_id_repo;
                    }
                }
                
                // add wme if no success above
                if (wme.epmem_id == EPMEM_NODEID_BAD)
                {
                    // can't use value_known_apriori, since value may have been assigned (lti, id repository via case 3)
                    if (wmeValueId.epmem_id == EPMEM_NODEID_BAD || wmeValueId.epmem_valid != epmem_validation)
                    {
                        // update next id
                        wmeValueId.epmem_id = stats.getNextId();
                        wmeValueId.epmem_valid = epmem_validation;
                        stats.setNextId(wmeValueId.epmem_id + 1L);
                        epmem_set_variable(epmem_variable_key.var_next_id, wmeValueId.epmem_id + 1L);

                        // add repository for possible future children
                        Map<Long, Map<Long, Long>> epmem_hashed_id_pool = Maps.newHashMap();
                        epmem_id_repository.put(wmeValueId.epmem_id, epmem_hashed_id_pool);
                        
                        Set<WmeImpl> epmem_wme_set = Sets.newHashSet();
                        epmem_id_ref_counts.put(wmeValueId.epmem_id, epmem_wme_set);
                    }
                    
                    // insert (q0,w,q1)
                    final PreparedStatement ps = db.add_edge_unique;
                    ps.setLong(1, parent_id);
                    ps.setLong(2, my_hash);
                    ps.setLong(3, wmeValueId.epmem_id);
                    // TODO: will this be a problem if different from C++ max?
                    ps.setLong(4, Long.MAX_VALUE);
                    
                    // CK: not all database drivers support this
                    final ResultSet rs = ps.getGeneratedKeys();
                    try
                    {
                        if (rs.next())
                        {
                            wme.epmem_id = rs.getLong(1);
                        }
                        else
                        {
                            // throw an exception if we were not able to get the row id of the insert
                            throw new SQLException("ps.getGeneratedKeys failed!");
                        }
                    }
                    finally
                    {
                        rs.close();
                    }

                    if (wmeValueId.smem_lti == 0)
                    {
                        // replace the epmem_id and wme id in the right place
                        epmem_id_replacement.put(wme.epmem_id, my_id_repo2);
                    }
                    
                    // new nodes definitely start
                    epmem_edge.add(wme.epmem_id);
                    epmem_edge_mins.add(time_counter);
                    epmem_edge_maxes.add(false);
                }
                else
                {
                 // definitely don't remove
                    epmem_edge_removals.put(wme.epmem_id, false);

                    // we add ONLY if the last thing we did was remove
                    if(epmem_edge_maxes.get((int)(wme.epmem_id-1L)))
                    {
                        epmem_edge.add(wme.epmem_id);
                        epmem_edge_maxes.set((int)(wme.epmem_id-1L), false);
                    }
                }

                // at this point we have successfully added a new wme
                // whose value is an identifier.  If the value was
                // unknown at the beginning of this episode, then we need
                // to update its ref count for each WME added (thereby catching
                // up with ref counts that would have been accumulated via wme adds)
                if (new_identifiers.contains(wme.value))
                {
                    // because we could have bypassed the ref set before, we need to create it here
                    if (epmem_id_ref_counts.get(wmeValueId.epmem_id).size() == 0)
                    {
                        Set<WmeImpl> epmem_wme_set = Sets.newHashSet();
                        epmem_id_ref_counts.put(wmeValueId.epmem_id, epmem_wme_set);
                    }
                    epmem_id_ref_counts.get(wmeValueId.epmem_id).add(wme);
                }

                // if the value has not been iterated over, continue to augmentations
                if(wmeValueId.tc_number != tc)
                {
                    parent_syms.add(wme.value);
                    parent_ids.add(wmeValueId.epmem_id);
                }
            }
            else
            {
                // have we seen this node in this database?
                if (wme.epmem_id == EPMEM_NODEID_BAD || wme.epmem_valid != epmem_validation)
                {
                    wme.epmem_id = EPMEM_NODEID_BAD;
                    wme.epmem_valid = epmem_validation;

                    my_hash = epmem_temporal_hash(wme.attr);
                    my_hash2 = epmem_temporal_hash(wme.value);

                    // try to get node id
                    {
                        // parent_id=? AND attr=? AND value=?
                        final PreparedStatement ps = db.find_node_unique;
                        ps.setLong(1, parent_id);
                        ps.setLong(2, my_hash);
                        ps.setLong(3, my_hash2);

                        final ResultSet rs = ps.executeQuery();
                        try
                        {
                            if (rs.next())
                            {
                                wme.epmem_id = rs.getLong(0);
                            }
                        }
                        finally
                        {
                            rs.close();
                        }

                        // CK: no reinitialize for PreparedStatment
                        // my_agent->epmem_stmts_graph->find_node_unique->reinitialize();
                    }
                    
                    // act depending on new/existing feature
                    if (wme.epmem_id == EPMEM_NODEID_BAD)
                    {
                        // insert (parent_id,attr,value)
                        final PreparedStatement ps = db.add_node_unique;
                        ps.setLong(1, parent_id);
                        ps.setLong(2, my_hash);
                        ps.setLong(3, my_hash2);

                        // CK: not all database drivers support this
                        final ResultSet rs = ps.getGeneratedKeys();
                        try
                        {
                            if (rs.next())
                            {
                                wme.epmem_id = rs.getLong(1);
                            }
                            else
                            {
                                // throw an exception if we were not able to get the row id of the insert
                                throw new SQLException("ps.getGeneratedKeys failed!");
                            }
                        }
                        finally
                        {
                            rs.close();
                        }

                        // new nodes definitely start
                        epmem_node.add(wme.epmem_id);
                        epmem_node_mins.add(time_counter);
                        epmem_node_maxes.add(false);
                    }
                    else
                    {
                        // definitely don't remove
                        epmem_node_removals.put(wme.epmem_id, false);

                        // add ONLY if the last thing we did was add
                        if( epmem_node_maxes.get((int)(wme.epmem_id-1L)) )
                        {
                            epmem_node.add(wme.epmem_id);
                            epmem_node_maxes.set((int)(wme.epmem_id - 1L), false);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * <p>episodic_memory.cpp:2031:inline void _epmem_promote_id( agent* my_agent, Symbol* id, epmem_time_id t )
     * @param id
     * @param t
     * @throws SQLException 
     */
    void _epmem_promote_id( SymbolImpl id, long /*epmem_time_id*/ t ) throws SQLException
    {
        final PreparedStatement ps = db.promote_id;
        ps.setLong(1, id.asIdentifier().epmem_id);
        ps.setLong(2, id.asIdentifier().getNameLetter());
        ps.setLong(3, id.asIdentifier().getNameNumber());
        ps.setLong(4, t);
        ps.executeUpdate();
    }
    
    /**
     * emem_temporal_hash with default value of add_on_fail (true)
     * @param sym
     * @return
     * @throws SQLException 
     */
    private long /*epmem_hash_id*/ epmem_temporal_hash(SymbolImpl sym) throws SQLException
    {
        return epmem_temporal_hash(sym, true);
    }
    
    /**
     * Returns a temporally unique integer representing a symbol constant.
     * <p> episodic_memory.cpp:1928:epmem_hash_id epmem_temporal_hash( agent *my_agent, Symbol *sym, bool add_on_fail = true )
     * @param sym
     * @param add_on_fail
     * @return
     * @throws SQLException 
     */
    private long /*epmem_hash_id*/ epmem_temporal_hash(SymbolImpl sym, boolean add_on_fail /*= true*/) throws SQLException
    {
        long /*epmem_hash_id*/ return_val = 0;
        
        // ////////////////////////////////////////////////////////////////////////////
        // my_agent->epmem_timers->hash->start();
        // ////////////////////////////////////////////////////////////////////////////
        
        if( sym.asString() != null ||
            sym.asDouble() != null ||
            sym.asInteger() != null)
        {
            //if ( ( !sym->common.epmem_hash ) || ( sym->common.epmem_valid != my_agent->epmem_validation ) )
            if (!(sym.epmem_hash_id == 0) || (sym.epmem_valid != epmem_validation))
            {
                sym.epmem_hash_id = 0;
                sym.epmem_valid = epmem_validation;

                // basic process:
                // - search
                // - if found, return
                // - else, add

                final PreparedStatement hash_get = db.hash_get;
                
                hash_get.setLong(1, Symbols.getSymbolType(sym));
                
                switch (Symbols.getSymbolType(sym))
                {
                case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                    hash_get.setString(2, sym.asString().getValue());
                    break;
                case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                    hash_get.setLong(2, sym.asInteger().getValue());
                    break;

                case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                    hash_get.setDouble(2, sym.asDouble().getValue());
                    break;
                }

                final ResultSet hash_get_rs = hash_get.executeQuery();
                try
                {
                    if (hash_get_rs.next())
                    {
                        return_val = hash_get_rs.getLong(0);
                    }
                }
                finally
                {
                    hash_get_rs.close();
                }
                
                if (return_val == 0 && add_on_fail)
                {
                    final PreparedStatement hash_add = db.hash_add;

                    hash_add.setLong(1, Symbols.getSymbolType(sym));

                    switch (Symbols.getSymbolType(sym))
                    {
                    case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                        hash_add.setString(2, sym.asString().getValue());
                        break;
                    case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                        hash_add.setLong(2, sym.asInteger().getValue());
                        break;

                    case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                        hash_add.setDouble(2, sym.asDouble().getValue());
                        break;
                    }

                    // CK: not all database drivers support this
                    final ResultSet hash_add_rs = hash_add.getGeneratedKeys();
                    try
                    {
                        if (hash_add_rs.next())
                        {
                            return_val = hash_add_rs.getLong(1);
                        }
                        else
                        {
                            // throw an exception if we were not able to get the
                            // row id of the insert
                            throw new SQLException("ps.getGeneratedKeys failed!");
                        }
                    }
                    finally
                    {
                        hash_add_rs.close();
                    }
                }
                
                // cache results for later re-use
                sym.epmem_hash_id = return_val;
                sym.epmem_valid = epmem_validation;
            }

            return_val = sym.epmem_hash_id;
        }

        // ////////////////////////////////////////////////////////////////////////////
        // my_agent->epmem_timers->hash->stop();
        // ////////////////////////////////////////////////////////////////////////////

        return return_val;
    }

    /**
     * Implements the Soar-EpMem API
     * 
     * <p>episodic_memory.cpp:5238:void epmem_respond_to_cmd( agent *my_agent )
     * @throws SoarException 
     */
    private void epmem_respond_to_cmd() throws SoarException
    {
        // if this is before the first episode, initialize db components
        if (db == null)
        {
            epmem_init_db();
        }

        // CK: exception will be thrown above if not initialized
        // respond to query only if db is properly initialized
        // if ( my_agent->epmem_db->get_status() != soar_module::connected )
        // {
        // return;
        // }

        // start at the bottom and work our way up
        // (could go in the opposite direction as well)
        IdentifierImpl state = decider.bottom_goal;

        List<WmeImpl> wmes;
        List<WmeImpl> cmds;

        Set<WmeImpl> /* soar_module::wme_set */cue_wmes = Sets.newHashSet();
        List<WmeImpl> /* soar_module::symbol_triple_list */meta_wmes = Lists.newArrayList();
        List<WmeImpl> /* soar_module::symbol_triple_list */retrieval_wmes = Lists.newArrayList();

        long /* epmem_time_id */retrieve = 0;
        SymbolImpl next = null;
        SymbolImpl previous = null;
        SymbolImpl query = null;
        SymbolImpl neg_query = null;
        List<Long> /*epmem_time_list*/ prohibit = Lists.newLinkedList();
        long /*epmem_time_id*/ before = 0;
        long /*epmem_time_id*/ after = 0;

        Set<SymbolImpl> currents = Sets.newHashSet();

        boolean good_cue = false;
        int path = 0;

        long /* uint64_t */wme_count;
        boolean new_cue;

        boolean do_wm_phase = false;
        
        while ( state != null )
        {
            final EpisodicMemoryStateInfo epmem_info = epmem_info(state);
            // ////////////////////////////////////////////////////////////////////////////
            // my_agent->epmem_timers->api->start();
            // ////////////////////////////////////////////////////////////////////////////
            
            // make sure this state has had some sort of change to the cmd
            new_cue = false;
            wme_count = 0;
            cmds = null;
            {
                Marker tc = DefaultMarker.create(); // get_new_tc_number( my_agent )
                Queue<SymbolImpl> syms = new LinkedList<SymbolImpl>();
                SymbolImpl parent_sym;

                // initialize BFS at command
                syms.add(epmem_info.epmem_cmd_header);

                while (!syms.isEmpty())
                {
                    // get state
                    parent_sym = syms.poll();
                    
                    // get children of the current identifier
                    wmes = epmem_get_augs_of_id(parent_sym, tc);
                    {
                        for (final WmeImpl wme : wmes)
                        {
                            wme_count++;

                            if (wme.timetag > epmem_info.last_cmd_time)
                            {
                                new_cue = true;
                                epmem_info.last_cmd_time = wme.timetag;
                            }

                            if (wme.value.asIdentifier() != null)
                            {
                                syms.add(wme.value.asIdentifier());
                            }
                        }
                        
                        // free space from aug list
                        if (cmds == null)
                        {
                            cmds = wmes;
                        }
                        else
                        {
                            wmes = null;
                        }
                    }
                }
                
                // see if any WMEs were removed
                if (epmem_info.last_cmd_count != wme_count)
                {
                    new_cue = true;
                    epmem_info.last_cmd_count = wme_count;
                }
                
                if (new_cue)
                {
                    // clear old results
                    epmem_clear_result(state);

                    do_wm_phase = true;
                }
            }
            
            // a command is issued if the cue is new
            // and there is something on the cue
            if (new_cue && wme_count != 0)
            {
                _epmem_respond_to_cmd_parse(cmds, good_cue, path, retrieve, next, 
                        previous, query, neg_query, prohibit, before, after, currents, cue_wmes);
                
                // ////////////////////////////////////////////////////////////////////////////
                // my_agent->epmem_timers->api->stop();
                // ////////////////////////////////////////////////////////////////////////////
                
                retrieval_wmes.clear();
                meta_wmes.clear();

                // process command
                if (good_cue)
                {
                    // retrieve
                    if (path == 1)
                    {
                        epmem_install_memory(
                                state, 
                                retrieve, 
                                meta_wmes,
                                retrieval_wmes);

                        // add one to the ncbr stat
                        stats.ncbr.set(stats.ncbr.get() + 1L);
                    }
                    // previous or next
                    else if (path == 2)
                    {
                        if (next != null)
                        {
                            epmem_install_memory(
                                    state, 
                                    epmem_next_episode(epmem_info.last_memory), 
                                    meta_wmes, 
                                    retrieval_wmes);

                            // add one to the next stat
                            stats.nexts.set(stats.nexts.get() + 1L);
                        }
                        else
                        {
                            epmem_install_memory(
                                    state, 
                                    epmem_previous_episode(epmem_info.last_memory), 
                                    meta_wmes, 
                                    retrieval_wmes);

                            // add one to the prev stat
                            stats.prevs.set(stats.prevs.get() + 1L);
                        }
                        
                        if (epmem_info.last_memory == EPMEM_MEMID_NONE)
                        {
                            epmem_buffer_add_wme(
                                    meta_wmes, 
                                    epmem_info.epmem_result_header, 
                                    predefinedSyms.epmem_sym_failure, 
                                    ((next != null) ? (next) : (previous)));
                        }
                        else
                        {
                            epmem_buffer_add_wme(
                                    meta_wmes, 
                                    epmem_info.epmem_result_header, 
                                    predefinedSyms.epmem_sym_success, 
                                    ((next != null) ? (next) : (previous)));
                        }
                    }
                    // query
                    else if (path == 3)
                    {
                        epmem_process_query(
                                state, 
                                query, 
                                neg_query, 
                                prohibit, 
                                before, 
                                after, 
                                currents, 
                                cue_wmes, 
                                meta_wmes, 
                                retrieval_wmes);

                        // add one to the cbr stat
                        stats.cbr.set(stats.cbr.get() + 1L);
                    }
                }
                else
                {
                    epmem_buffer_add_wme(meta_wmes, 
                            epmem_info.epmem_result_header, 
                            predefinedSyms.epmem_sym_status, 
                            predefinedSyms.epmem_sym_bad_cmd);
                }
                
                // clear prohibit list
                prohibit.clear();
                
                //CK: this is not implemented in smem - added to c++ after port?
                if (!retrieval_wmes.isEmpty() || !meta_wmes.isEmpty())
                {
                    // process preference assertion en masse
                    epmem_process_buffered_wmes(state, cue_wmes, meta_wmes, retrieval_wmes);

                    // CK: should not be necessary in JSoar
                    // clear cache
//                    {
//                        for ( WmeImpl w : retrieval_wmes)
//                        {
//                            symbol_remove_ref( w.id );
//                            symbol_remove_ref( w.attr );
//                            symbol_remove_ref( w.value );
//                        }
//                        for ( mw_it=meta_wmes.begin(); mw_it!=meta_wmes.end(); mw_it++ )
//                        {
//                            symbol_remove_ref( my_agent, (*mw_it)->id );
//                            symbol_remove_ref( my_agent, (*mw_it)->attr );
//                            symbol_remove_ref( my_agent, (*mw_it)->value );
//
//                            delete (*mw_it);
//                        }
//                        meta_wmes.clear();
//                    }
                    // process wm changes on this state
                    do_wm_phase = true;
                }

                // clear cue wmes
                cue_wmes.clear();
            }
            else
            {
                // ////////////////////////////////////////////////////////////////////////////
                // my_agent->epmem_timers->api->stop();
                // ////////////////////////////////////////////////////////////////////////////
            }
            
         // free space from command aug list
            if ( cmds != null )
            {
                cmds = null;
            }

            state = state.goalInfo.higher_goal;
        }
        
        if (do_wm_phase)
        {
            // ////////////////////////////////////////////////////////////////////////////
            // my_agent->epmem_timers->wm_phase->start();
            // ////////////////////////////////////////////////////////////////////////////

            decider.do_working_memory_phase();

            // ////////////////////////////////////////////////////////////////////////////
            // my_agent->epmem_timers->wm_phase->stop();
            // ////////////////////////////////////////////////////////////////////////////
        }
    }
    
    /**
     * <p>
     * episodic_memory.cpp:992:inline void epmem_process_buffered_wmes( agent*
     * my_agent, Symbol* state, soar_module::wme_set& cue_wmes,
     * soar_module::symbol_triple_list& meta_wmes,
     * soar_module::symbol_triple_list& retrieval_wmes )
     * 
     * @param state
     * @param cue_wmes
     * @param meta_wmes
     * @param retrieval_wmes
     */
    private void epmem_process_buffered_wmes(
            IdentifierImpl state, 
            Set<WmeImpl> cue_wmes, 
            List<WmeImpl> meta_wmes, 
            List<WmeImpl> retrieval_wmes)
    {
        _epmem_process_buffered_wme_list( state, cue_wmes, meta_wmes, epmem_info(state).epmem_wmes );
        _epmem_process_buffered_wme_list( state, cue_wmes, retrieval_wmes, null );
    }

    /**
     * <p>
     * episodic_memory.cpp:912:inline void _epmem_process_buffered_wme_list(
     * agent* my_agent, Symbol* state, soar_module::wme_set& cue_wmes,
     * soar_module::symbol_triple_list& my_list, epmem_wme_stack* epmem_wmes )
     * 
     * @param state
     * @param cue_wmes
     * @param retrieval_wmes
     * @param epmem_wmes
     */
    private void _epmem_process_buffered_wme_list(
            IdentifierImpl state, 
            Set<WmeImpl> cue_wmes, 
            List<WmeImpl> my_list, 
            Deque<Preference> epmem_wmes)
    {
        if (my_list.isEmpty())
        {
            return;
        }
        
        // TODO find out if this function is necessary in JSoar (same for epmem_process_buffered_wmes)
    }

    /**
     * Call epmem_process_query with level = 3 (default in C++)
     */
    private void epmem_process_query(IdentifierImpl state, SymbolImpl query, SymbolImpl neg_query, List<Long> prohibit, long before, 
            long after, Set<SymbolImpl> currents, Set<WmeImpl> cue_wmes, List<WmeImpl> meta_wmes, List<WmeImpl> retrieval_wmes)
    {
        epmem_process_query(state, query, neg_query, prohibit, before, after, currents, cue_wmes, meta_wmes, retrieval_wmes, 3);
    }
    
    /**
     * <p>
     * episodic_memory.cpp:3869:void epmem_process_query(agent *my_agent, Symbol
     * *state, Symbol *pos_query, Symbol *neg_query, epmem_time_list& prohibits,
     * epmem_time_id before, epmem_time_id after, epmem_symbol_set& currents,
     * soar_module::wme_set& cue_wmes, soar_module::symbol_triple_list&
     * meta_wmes, soar_module::symbol_triple_list& retrieval_wmes, int level=3)
     * 
     * @param state
     * @param query
     * @param neg_query
     * @param prohibit
     * @param before
     * @param after
     * @param currents
     * @param cue_wmes
     * @param meta_wmes
     * @param retrieval_wmes
     */
    private void epmem_process_query(
            IdentifierImpl state, 
            SymbolImpl query, 
            SymbolImpl neg_query, 
            List<Long> prohibit, 
            long before, 
            long after,
            Set<SymbolImpl> currents, 
            Set<WmeImpl> cue_wmes, 
            List<WmeImpl> meta_wmes, 
            List<WmeImpl> retrieval_wmes,
            int level /*=3*/)
    {
        // TODO Auto-generated method stub
    }

    /**
     * <p>
     * episodic_memory.cpp:998:inline void epmem_buffer_add_wme(
     * soar_module::symbol_triple_list& my_list, Symbol* id, Symbol* attr,
     * Symbol* value )
     * 
     * @param my_list
     * @param id
     * @param attr
     * @param value
     */
    private void epmem_buffer_add_wme(
            List<WmeImpl> my_list, 
            IdentifierImpl id, 
            SymbolImpl attr, 
            SymbolImpl value)
    {
//        my_list.push_back( new soar_module::symbol_triple( id, attr, value ) );
//
//        symbol_add_ref( id );
//        symbol_add_ref( attr );
//        symbol_add_ref( value );
        
    }

    /**
     * 
     * @param last_memory
     * @return
     */
    private long epmem_previous_episode(long last_memory)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * 
     * @param last_memory
     * @return
     */
    private long epmem_next_episode(long last_memory)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * 
     * @param state
     * @param retrieve
     * @param meta_wmes
     * @param retrieval_wmes
     */
    private void epmem_install_memory(IdentifierImpl state, long retrieve, List<WmeImpl> meta_wmes, List<WmeImpl> retrieval_wmes)
    {
        // TODO Auto-generated method stub
    }

    /**
     * <p>
     * episodic_memory.cpp:5063:void inline _epmem_respond_to_cmd_parse( agent*
     * my_agent, epmem_wme_list* cmds, bool& good_cue, int& path, epmem_time_id&
     * retrieve, Symbol*& next, Symbol*& previous, Symbol*& query, Symbol*&
     * neg_query, epmem_time_list& prohibit, epmem_time_id& before,
     * epmem_time_id& after, epmem_symbol_set& currents, soar_module::wme_set&
     * cue_wmes )
     * 
     * @param cmds
     * @param good_cue
     * @param path
     * @param retrieve
     * @param next
     * @param previous
     * @param query
     * @param neg_query
     * @param prohibit
     * @param before
     * @param after
     * @param currents
     * @param cue_wmes
     */
    private void _epmem_respond_to_cmd_parse(
            List<WmeImpl> cmds, 
            boolean good_cue, 
            int path, 
            long retrieve, 
            SymbolImpl next, 
            SymbolImpl previous,
            SymbolImpl query, 
            SymbolImpl neg_query, 
            List<Long> prohibit, 
            long before, 
            long after, 
            Set<SymbolImpl> currents, 
            Set<WmeImpl> cue_wmes)
    {
        // TODO Auto-generated method stub
    }

    /**
     * Removes any WMEs produced by EpMem resulting from a command
     * 
     * <p>episodic_memory.cpp:1449:void epmem_clear_result( agent *my_agent, Symbol *state )
     * @param state
     */
    private void epmem_clear_result(IdentifierImpl state)
    {
        // TODO Auto-generated method stub
        
    }

    private EpisodicMemoryStateInfo epmem_info(IdentifierImpl state)
    {
        return stateInfos.get(state);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.epmem.EpisodicMemory#epmem_enabled()
     */
    @Override
    public boolean epmem_enabled()
    {
        // CK: C++ code
        // return ( my_agent->epmem_params->learning->get_value() == soar_module::on );
        return (params.learning.get() == DefaultEpisodicMemoryParams.Learning.on);
    }

    @Override
    public boolean encodeInOutputPhase()
    {
        return (params.phase.get() == Phase.output);
    }

    @Override
    public boolean encodeInSelectionPhase()
    {
        return (params.phase.get() == Phase.selection);
    }

    @Override
    public long epmem_validation()
    {
        return epmem_validation;
    }

    @Override
    public boolean addIdRefCount(long id, WmeImpl w)
    {
        Set<WmeImpl> wmes = epmem_id_ref_counts.get(id);
        if(wmes==null)
            return false;
        
        wmes.add(w);
        return true;
    }

    @Override
    public void addWme(IdentifierImpl id)
    {
        epmem_wme_adds.add(id);
    }
}
