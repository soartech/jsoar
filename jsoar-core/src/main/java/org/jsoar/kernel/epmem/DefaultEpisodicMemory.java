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
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GmOrderingChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.GraphMatchChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.MergeChoices;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Optimization;
import org.jsoar.kernel.epmem.DefaultEpisodicMemoryParams.Phase;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WmeImpl.SymbolTriple;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.modules.SoarModule;
import org.jsoar.kernel.smem.DefaultSemanticMemory;
import org.jsoar.kernel.symbols.IdentifierImpl;
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

import com.google.common.collect.HashMultimap;
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
     * episodic_memory.h:55:EPMEM_NODE_POS
     * int, so it can be switched on
     */
    public static final int EPMEM_NODE_POS = 0;
    
    /**
     * episodic_memory.h:56:EPMEM_NODE_NEG
     * int, so it can be switched on
     */
    public static final int EPMEM_NODE_NEG = 1;
    
    /**
     * episodic_memory.h:57:EPMEM_RANGE_START
     */
    public static final int EPMEM_RANGE_START = 0;

    /**
     * episodic_memory.h:58:EPMEM_RANGE_END
     */
    public static final int EPMEM_RANGE_END = 1;
    
    /**
     * episodic_memory.h:59:EPMEM_RANGE_EP
     */
    public static final int EPMEM_RANGE_EP = 0;
    
    /**
     * episodic_memory.h:60:EPMEM_RANGE_NOW
     */
    public static final long EPMEM_RANGE_NOW = 1;
    
    /**
     * episodic_memory.h:61:EPMEM_RANGE_POINT
     */
    public static final long EPMEM_RANGE_POINT = 2;
    
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
    private DefaultSemanticMemory smem;
    private Chunker chunker;
    
    private DefaultEpisodicMemoryParams params;
    DefaultEpisodicMemoryStats stats;

    private Decider decider;
    SymbolFactoryImpl symbols;
    EpisodicMemoryDatabase db;
    
    private RecognitionMemory recognitionMemory;

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
        smem = Adaptables.require(DefaultEpisodicMemory.class, context, DefaultSemanticMemory.class);
        recognitionMemory = Adaptables.require(DefaultEpisodicMemory.class, context, RecognitionMemory.class);
        chunker = Adaptables.require(DefaultEpisodicMemory.class, context, Chunker.class);
        
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
        
        /*
         * rit_state_graph stuff taken from:
         * episodic_memory.cpp:257:epmem_stat_container::epmem_stat_container( agent *new_agent ): soar_module::stat_container( new_agent )
         * 
         * must be done before epmem_init_db_catch()
         */
        
        /////////////////////////////
        // connect to rit state
        /////////////////////////////
        
        // graph
        //epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].offset.stat = rit_offset_1;
        epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].offset.var_key = epmem_variable_key.var_rit_offset_1;
        //epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].leftroot.stat = rit_left_root_1;
        epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].leftroot.var_key = epmem_variable_key.var_rit_leftroot_1;
        //epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].rightroot.stat = rit_right_root_1;
        epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].rightroot.var_key = epmem_variable_key.var_rit_rightroot_1;
        //epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].minstep.stat = rit_min_step_1;
        epmem_rit_state_graph[EPMEM_RIT_STATE_NODE].minstep.var_key = epmem_variable_key.var_rit_minstep_1;

        //epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].offset.stat = rit_offset_2;
        epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].offset.var_key = epmem_variable_key.var_rit_offset_2;
        //epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].leftroot.stat = rit_left_root_2;
        epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].leftroot.var_key = epmem_variable_key.var_rit_leftroot_2;
        //epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].rightroot.stat = rit_right_root_2;
        epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].rightroot.var_key = epmem_variable_key.var_rit_rightroot_2;
        //epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].minstep.stat = rit_min_step_2;
        epmem_rit_state_graph[EPMEM_RIT_STATE_EDGE].minstep.var_key = epmem_variable_key.var_rit_minstep_2;
        
        // CK: in smem this is called from smem_attach, there is no equivalent
        // function in episodic_memory.cpp
        //epmem_init_db_catch();
        
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
        if (db != null /* my_agent->epmem_db->get_status() !=soar_module::disconnected */)
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
    
    /**
     * episodic_memory.cpp: 1068: 
     * int64_t epmem_rit_fork_node( int64_t lower, 
     *      int64_t upper, 
     *      bool / *bounds_offset* /,
     *      int64_t *step_return, 
     *      epmem_rit_state *rit_state 
     * )
     * 
     * @param lower
     * @param uppper
     * The C has an *int64_t here, but were just going to return it in the java
     * @param rit_state
     * @return EpmemRitForkNodeResult In the C, there is a pass by reference int, 
     * so Java will simulate that with a return class.
     */
    private final EpmemRitForkNodeResult epmem_rit_fork_node(long lower, long upper, epmem_rit_state rit_state)
    {
        //The cpp contains this set of comments:
        
        // never called
        /*if ( !bounds_offset )
          {
          int64_t offset = rit_state->offset.stat->get_value();

          lower = ( lower - offset );
          upper = ( upper - offset );
          }*/
        
        //:end comment set -ACN
        
        // descend the tree down to the fork node
        long node = EPMEM_RIT_ROOT;
        if (upper < EPMEM_RIT_ROOT)
        {
            node = rit_state.leftroot.stat;
        }
        else if (lower > EPMEM_RIT_ROOT)
        {
            node = rit_state.rightroot.stat;
        }

        long step;
        for (step = (((node >= 0) ? (node) : (-1 * node)) / 2); step >= 1; step /= 2)
        {
            if (upper < node)
            {
                node -= step;
            }
            else if (node < lower)
            {
                node += step;
            }
            else
            {
                break;
            }
        }

        // never used
        // if ( step_return != NULL )
        //{
            //(*step_return) = step;  We don't need this in java
        //}

        return new EpmemRitForkNodeResult(node, step);
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
        catch (SQLException e)
        {
            logger.error("While responding to epmem command: " + e.getMessage(), e);
            agent.getPrinter().error("While responding to epmem command: " + e.getMessage());
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
                                _epmem_store_level(
                                        parent_syms,
                                        parent_ids,
                                        tc,
                                        wmes,
                                        parent_id,
                                        time_counter,
                                        id_reservations,
                                        new_identifiers,
                                        epmem_node,
                                        epmem_edge
                                    );
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
                    db.add_node_now.setLong(1, temp_node); // my_agent->epmem_stmts_graph->add_node_now->bind_int(
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
            for(WmeImpl w=id.goalInfo != null ? id.goalInfo.getImpasseWmes() : null; w!=null; w=w.next)
            {
                return_val.add(w);
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
                            ps.setLong(2, wmeValueId.getNameNumber());
                            final ResultSet rs = ps.executeQuery();
                            try
                            {
                                if (rs.first())
                                {
                                    wmeValueId.epmem_id = rs.getLong(0 + 1);
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
                                wme.epmem_id = rs.getLong(0 + 1);
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
                    ps.execute();
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
                                wme.epmem_id = rs.getLong(0 + 1);
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
                        ps.execute();
                        // CK: not all database drivers support this
                        final ResultSet rs = ps.getGeneratedKeys();
                        try
                        {
                            if (rs.next())
                            {
                                //TODO  Check that this is intentional
                                //(*w_p)->epmem_id = (epmem_node_id) my_agent->epmem_db->last_insert_rowid();
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
     * <p> episodic_memory.cpp:1928:
     * epmem_hash_id epmem_temporal_hash( 
     *      agent *my_agent, 
     *      Symbol *sym, 
     *      bool add_on_fail = true 
     *  )
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
                        return_val = hash_get_rs.getLong(0 + 1);
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
                    hash_add.execute();
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
     * @throws SQLException 
     */
    private void epmem_respond_to_cmd() throws SoarException, SQLException
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
        List<SymbolTriple> /* soar_module::symbol_triple_list */meta_wmes = Lists.newArrayList();
        List<SymbolTriple> /* soar_module::symbol_triple_list */retrieval_wmes = Lists.newArrayList();

        ByRef<Long> /* epmem_time_id */retrieve = ByRef.create(0L);
        SymbolImpl next = null;
        SymbolImpl previous = null;
        SymbolImpl query = null;
        SymbolImpl neg_query = null;
        List<Long> /*epmem_time_list*/ prohibit = Lists.newLinkedList();
        ByRef<Long> /*epmem_time_id*/ before = ByRef.create(0L);
        ByRef<Long> /*epmem_time_id*/ after = ByRef.create(0L);

        Set<SymbolImpl> currents = Sets.newHashSet();

        ByRef<Boolean> good_cue = ByRef.create(false);
        ByRef<Integer> path = ByRef.create(0);

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
                if (good_cue.value)
                {
                    // retrieve
                    if (path.value == 1)
                    {
                        epmem_install_memory(
                                state, 
                                retrieve.value, 
                                meta_wmes,
                                retrieval_wmes);

                        // add one to the ncbr stat
                        stats.ncbr.set(stats.ncbr.get() + 1L);
                    }
                    // previous or next
                    else if (path.value == 2)
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
                    else if (path.value == 3)
                    {
                        epmem_process_query(
                                state, 
                                query, 
                                neg_query, 
                                prohibit, 
                                before.value, 
                                after.value, 
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
            List<SymbolTriple> meta_wmes, 
            List<SymbolTriple> retrieval_wmes)
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
            List<SymbolTriple> my_list, 
            Deque<Preference> epmem_wmes)
    {
        if (my_list.isEmpty())
        {
            return;
        }
        
        Instantiation inst = SoarModule.make_fake_instantiation( state, cue_wmes, my_list );

        for ( Preference pref = inst.preferences_generated; pref != null; pref=pref.inst_next )
        {
            // add the preference to temporary memory
            if ( recognitionMemory.add_preference_to_tm( pref ) )
            {
                // add to the list of preferences to be removed
                // when the goal is removed
                //insert_at_head_of_dll( state->id.preferences_from_goal, pref, all_of_goal_next, all_of_goal_prev );
                Preference header = state.goalInfo.preferences_from_goal;
                pref.all_of_goal_next = header;
                pref.all_of_goal_prev = null;//NIL
                if(header != null)
                {
                    header.all_of_goal_prev = pref;
                }
                state.goalInfo.preferences_from_goal = pref;
                
                pref.on_goal_list = true;

                if ( epmem_wmes != null )
                {
                    // if this is a meta wme, then it is completely local
                    // to the state and thus we will manually remove it
                    // (via preference removal) when the time comes
                    epmem_wmes.add( pref );
                }
            }
            else
            {
                pref.preference_add_ref( );
                pref.preference_remove_ref( recognitionMemory );
            }
        }

        //if ( !epmem_wmes )
        if ( epmem_wmes == null )
        {
            // otherwise, we submit the fake instantiation to backtracing
            // such as to potentially produce justifications that can follow
            // it to future adventures (potentially on new states)
            final ByRef<Instantiation> my_justification_list = null;//NIL;
            chunker.chunk_instantiation( inst, false, my_justification_list );

            // if any justifications are created, assert their preferences manually
            // (copied mainly from assert_new_preferences with respect to our circumstances)
            // TODO: Why is this here?  The compiler has a very valid point that it has to be null
            if ( my_justification_list.value != null/*NIL*/ )
            {
                Preference just_pref = null;//NIL;
                Instantiation next_justification = null;//NIL;

                for ( Instantiation my_justification = my_justification_list.value;
                        my_justification != null;//NIL;
                        my_justification=next_justification )
                {
                    next_justification = my_justification.nextInProdList;

                    if ( my_justification.in_ms )
                    {
                        my_justification.insertAtHeadOfProdList(my_justification.prod.instantiations);
                    }

                    for ( just_pref=my_justification.preferences_generated; just_pref!=null/*NIL*/; just_pref=just_pref.inst_next )
                    {
                        if ( recognitionMemory.add_preference_to_tm( just_pref ) )
                        {
                            //TODO: WMA.  This is commented out in SMEM as well, and it doesn't appear
                            //to be anywhere in the code. -ACN
                            /*
                            if ( wma_enabled( my_agent ) )
                            {
                                wma_activate_wmes_in_pref( my_agent, just_pref );
                            }
                            */
                        }
                        else
                        {
                            just_pref.preference_add_ref() ;
                            just_pref.preference_remove_ref(recognitionMemory);
                        }
                    }
                }
            }
        }
    }

    /**
     * Call epmem_process_query with level = 3 (default in C++)
     * @throws SQLException 
     * @throws SoarException 
     */
    private void epmem_process_query(IdentifierImpl state, SymbolImpl query, SymbolImpl neg_query, List<Long> prohibit, long before, 
            long after, Set<SymbolImpl> currents, Set<WmeImpl> cue_wmes, List<SymbolTriple> meta_wmes, List<SymbolTriple> retrieval_wmes) throws SQLException, SoarException
    {
        epmem_process_query(state, query, neg_query, prohibit, before, after, currents, cue_wmes, meta_wmes, retrieval_wmes, 3);
    }
    
    //TODO: All of the classes from here down to epmem_process_query, are added specifically
    //for that.  See if they can be combined in some logical fashion. -ACN    
    private static class EpmemLiteral
    {
        SymbolImpl id_sym;
        SymbolImpl value_sym;
        long/*int*/ is_neg_q;
        long/*int*/ value_is_id;
        boolean is_leaf;
        boolean is_current;
        long/*epmem_node_id*/ w;
        long/*epmem_node_id*/ q1;
        double weight;
        Set<EpmemLiteral> parents;
        Set<EpmemLiteral> children;        
        NavigableSet<EpmemNodePair>/*epmem_node_pair_set*/ matches;
        Map<Long, Integer>/*epmem_node_int_map*/ values;
    }
    
    /**
     * Based on episodic_memory:559
     * @author ACNickels
     */
    private static class EpmemTriple implements Comparable<EpmemTriple>
    {
        long q0;
        long w;
        long q1;
        
        public EpmemTriple(
                long q0,
                long w,
                long q1
        )
        {
            this.q0 = q0;
            this.w = w;
            this.q1 = q1;
        }
        
        @Override
        public int compareTo(EpmemTriple other)
        {
            if (q0 != other.q0)
            {
                return (q0 < other.q0)?-1:1;
            } 
            else if (w != other.w) 
            {
                return (w < other.w)?-1:1;
            }
            else if(q1 != other.q1)
            {
                return (q1 < other.q1)?-1:1;
            }
            return 0;
        }
    }
    
    //It looks like this may contain unused fields, so lets add them as they are used.  -ACN
    private static class EpmemPEdge
    {
        EpmemTriple triple;
        int value_is_id;
        boolean has_noncurrent;
        Set<EpmemLiteral>/*epmem_literal_set*/ literals;
        PreparedStatement/*soar_module::pooled_sqlite_statement**/ sql;
        ResultSet sqlResults;
        long/*epmem_time_id*/ time;
    }
    //It looks like this may contain unused fields, so lets add them as they are used.  -ACN
    private static class EpmemUEdge
    {
        EpmemTriple triple;
        long value_is_id;
        boolean has_noncurrent;
        long activation_count;
        Set<EpmemPEdge>/*epmem_pedge_set*/ pedges;
        long intervals;
        boolean activated;
    }
    //It looks like this may contain unused fields, so lets add them as they are used.  -ACN
    private static class EpmemInterval
    {
        EpmemUEdge uedge;
        int is_end_point;
        PreparedStatement sql;
        ResultSet sqlResult;
        long/*epmem_time_id*/ time;
    }
    
    private static class EpmemSymbolNodePair
    {        
        public EpmemSymbolNodePair(SymbolImpl id_sym, long parent)
        {
            first = id_sym;
            second = parent;
        }
       
        final SymbolImpl first;
        final long /*epmem_node_id*/ second;
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((first == null) ? 0 : first.hashCode());
            result = prime * result + (int) (second ^ (second >>> 32));
            return result;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EpmemSymbolNodePair other = (EpmemSymbolNodePair) obj;
            if (first == null)
            {
                if (other.first != null)
                    return false;
            }
            else if (!first.equals(other.first))
                return false;
            if (second != other.second)
                return false;
            return true;
        }
    }
    
    private static class EpmemNodePair implements Comparable<EpmemNodePair>
    {
        public EpmemNodePair(long parent, long child)
        {
            this.first = parent;
            this.second = child;
        }
        final long /*epmem_node_id*/ first;
        final long /*epmem_node_id*/ second;
        
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (first ^ (first >>> 32));
            result = prime * result + (int) (second ^ (second >>> 32));
            return result;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EpmemNodePair other = (EpmemNodePair) obj;
            if (first != other.first)
                return false;
            if (second != other.second)
                return false;
            return true;
        }
        
        @Override
        public int compareTo(EpmemNodePair other)
        {
            if (this.first != other.first)
            {
                return (this.first < other.first)?-1:1;
            } 
            else if (this.second != other.second) 
            {
                return (this.second < other.second)?-1:1;
            }
            return 0;
        }
    }
    /**
     * <p>
     * episodic_memory.cpp:3869:void 
     * epmem_process_query(
     *      agent *my_agent, 
     *      Symbol *state, 
     *      Symbol *pos_query, 
     *      Symbol *neg_query, 
     *      epmem_time_list& prohibits,
     *      epmem_time_id before, 
     *      epmem_time_id after, 
     *      epmem_symbol_set& currents,
     *      soar_module::wme_set& cue_wmes, 
     *      soar_module::symbol_triple_list& meta_wmes, 
     *      soar_module::symbol_triple_list& retrieval_wmes, 
     *      int level=3
     *  )
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
     * @throws SQLException 
     * @throws SoarException 
     */
    private void epmem_process_query(
            IdentifierImpl state, 
            SymbolImpl pos_query, 
            SymbolImpl neg_query, 
            List<Long> prohibits, 
            long before, 
            long after,
            Set<SymbolImpl> currents, 
            Set<WmeImpl> cue_wmes, 
            List<SymbolTriple> meta_wmes, 
            List<SymbolTriple> retrieval_wmes,
            int level /*=3*/
    ) throws SQLException, SoarException
    {
        // a query must contain a positive cue
        if (pos_query == null) 
        {
            epmem_buffer_add_wme(
                    meta_wmes, 
                    epmem_info(state).epmem_result_header, 
                    predefinedSyms.epmem_sym_status, 
                    predefinedSyms.epmem_sym_bad_cmd
                );
            return;
        }

        // before and after, if specified, must be valid relative to each other
        if (before != EPMEM_MEMID_NONE && after != EPMEM_MEMID_NONE && before <= after) 
        {
            epmem_buffer_add_wme(
                    meta_wmes, 
                    epmem_info(state).epmem_result_header, 
                    predefinedSyms.epmem_sym_status, 
                    predefinedSyms.epmem_sym_bad_cmd
                );
            return;
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug("\n==========================\n");
        }
        
        //my_agent->epmem_timers->query->start();

        // sort probibit's
        if (!prohibits.isEmpty()) 
        {
            //std::sort(prohibits.begin(), prohibits.end());
            Collections.sort(prohibits);
        }
        int[] test = { 2, 2 };
        // epmem options
        boolean do_graph_match = (params.graph_match.get() == GraphMatchChoices.on);
        GmOrderingChoices gm_order = params.gm_ordering.get();

        // variables needed for cleanup
        Map<WmeImpl, EpmemLiteral> /*epmem_wme_literal_map*/ literal_cache = new HashMap<WmeImpl, EpmemLiteral>();
        Map<EpmemTriple, EpmemPEdge>/*epmem_triple_pedge_map*/[] pedge_caches = new Map[2];
        /*
        #ifdef USE_MEM_POOL_ALLOCATORS
            epmem_triple_uedge_map uedge_caches[2] = {
                epmem_triple_uedge_map(std::less<epmem_triple>(), soar_module::soar_memory_pool_allocator<std::pair<const epmem_triple, epmem_uedge*> >(my_agent)),
                epmem_triple_uedge_map(std::less<epmem_triple>(), soar_module::soar_memory_pool_allocator<std::pair<const epmem_triple, epmem_uedge*> >(my_agent))
            };
            epmem_interval_set interval_cleanup = epmem_interval_set(std::less<epmem_interval*>(), soar_module::soar_memory_pool_allocator<epmem_interval*>(my_agent));
        #else
        */
        //TODO:  We are interpreting this as a static initialization of a map.  
        //       Make sure that that is the intention. -ACN
        //epmem_triple_uedge_map uedge_caches[2] = {epmem_triple_uedge_map(), epmem_triple_uedge_map()};
        SortedMap<EpmemTriple, EpmemUEdge>/*epmem_triple_uedge_map*/ uedge_caches[] = new SortedMap[2];
        uedge_caches[0] = new TreeMap<EpmemTriple, EpmemUEdge>();
        uedge_caches[1] = new TreeMap<EpmemTriple, EpmemUEdge>();
        
        Set<EpmemInterval> /*epmem_interval_set*/ interval_cleanup = new HashSet<EpmemInterval>();
        //#endif

        //This comment is left here from the C code: // TODO additional indices

        // variables needed for building the DNF
        EpmemLiteral root_literal = new EpmemLiteral();
        //allocate_with_pool(my_agent, &(my_agent->epmem_literal_pool), &root_literal);
        Set<EpmemLiteral>/*epmem_literal_set*/ leaf_literals = new HashSet<EpmemLiteral>();

        // priority queues for interval walk
        //epmem_pedge_pq pedge_pq;
        PriorityQueue<EpmemPEdge> pedge_pq =
                //11 is the default size a queue without a comparator uses
                new PriorityQueue<EpmemPEdge>(
                        11, 
                        //Comparator code comes from episodic_memory.h: 617
                        //The java implementation sorts in reverse order, becuase the Java and C
                        //queue implementations sort their elements in reverse orders.
                        new Comparator<EpmemPEdge>()
                        {
                            public int compare(EpmemPEdge a, EpmemPEdge b)
                            {
                                if (a.time != b.time) 
                                {
                                    return (a.time < b.time)?1:-1;
                                } 
                                else 
                                {
                                    /*
                                     * TODO: This is either an arbitrary tie break, or it is
                                     * intentionally comparing the triple field of the edges.  It
                                     * looks arbitrary, but there is an operator< funtion declared on
                                     * the triple, which appears to be where the functionality for this
                                     * comparrison is coming from.
                                     */
                                    //return (a < b);
                                    return (a.triple.compareTo(b.triple));
                                }
                            }
                        }
                    );
        //epmem_interval_pq interval_pq;
        PriorityQueue<EpmemInterval> interval_pq =
                new PriorityQueue<EpmemInterval>(
                        11,
                        //Comparator based on episodic_memory.h: 627
                        //The java implementation sorts in reverse order, becuase the Java and C
                        //queue implementations sort their elements in reverse orders.
                        new Comparator<EpmemInterval>()
                        {
                            public int compare(EpmemInterval a, EpmemInterval b)
                            {
                                if (a.time != b.time) {
                                    return (a.time < b.time)?1:-1;
                                }
                                else if (a.is_end_point == b.is_end_point) 
                                {
                                    /*
                                     * TODO: This is either an arbitrary tie break, or it is
                                     * intentionally comparing the triple field of the edges.  It
                                     * looks arbitrary, but there is an operator< funtion declared on
                                     * the triple, which appears to be where the functionality for this
                                     * comparrison is coming from.
                                     */
                                    return (a.uedge.triple.compareTo(b.uedge.triple));
                                }
                                else 
                                {
                                    // arbitrarily put starts before ends
                                    return (a.is_end_point == EPMEM_RANGE_START)?1:-1;
                                }
                            }
                        }
                    );

        // variables needed to track satisfiability
        // number of literals with a certain symbol as its value
        Map<SymbolImpl, Integer>/*epmem_symbol_int_map*/ symbol_num_incoming 
            = new HashMap<SymbolImpl, Integer>();
        // number of times a symbol is matched by a node
        Map<EpmemSymbolNodePair, Integer>/*epmem_symbol_node_pair_int_map*/ symbol_node_count
            = new HashMap<EpmemSymbolNodePair, Integer>();

        // various things about the current and the best episodes
        long/*epmem_time_id*/ best_episode = EPMEM_MEMID_NONE;
        double best_score = 0;
        boolean best_graph_matched = false;
        long/*long int*/ best_cardinality = 0;
        Map<EpmemLiteral, EpmemNodePair>/*epmem_literal_node_pair_map*/ best_bindings
            = new HashMap<EpmemLiteral, EpmemNodePair>();
        double current_score = 0;
        long/*long int*/ current_cardinality = 0;

        // variables needed for graphmatch
        //Java's Deque does not allow for random access (we need to sort it) so we
        //will use a linked list instead.  LinkedList has a collections method to sort
        //it and implements Deque, so we can still pass it around as a Deque. 
        LinkedList<EpmemLiteral>/*epmem_literal_deque*/ gm_ordering
            = new LinkedList<EpmemLiteral>();

        if (level > 1) 
        {
            // build the DNF graph while checking for leaf WMEs
            {
                //my_agent->epmem_timers->query_dnf->start();
                root_literal.id_sym = null;
                root_literal.value_sym = pos_query;
                root_literal.is_neg_q = EPMEM_NODE_POS;
                root_literal.value_is_id = EPMEM_RIT_STATE_EDGE;
                root_literal.is_leaf = false;
                root_literal.is_current = false;
                root_literal.w = EPMEM_NODEID_BAD;
                root_literal.q1 = EPMEM_NODEID_ROOT;
                root_literal.weight = 0.0;
                root_literal.parents = new HashSet<EpmemLiteral>();
                root_literal.children = new HashSet<EpmemLiteral>();
                /*
                #ifdef USE_MEM_POOL_ALLOCATORS
                            new(&(root_literal->matches)) epmem_node_pair_set(std::less<epmem_node_pair>(), soar_module::soar_memory_pool_allocator<epmem_node_pair>(my_agent));
                #else
                */
                root_literal.matches = new TreeSet<EpmemNodePair>();
                //#endif
                root_literal.values = new  HashMap<Long, Integer>();
                symbol_num_incoming.put(pos_query, 1);
                literal_cache.put(null, root_literal);

                Set<SymbolImpl>/*std::set<Symbol*>*/ visiting = new HashSet<SymbolImpl>();
                visiting.add(pos_query);
                visiting.add(neg_query);
                for (int query_type = EPMEM_NODE_POS; query_type <= EPMEM_NODE_NEG; query_type++) 
                {
                    SymbolImpl query_root = null;
                    switch (query_type) 
                    {
                        case EPMEM_NODE_POS:
                            query_root = pos_query;
                            break;
                        case EPMEM_NODE_NEG:
                            query_root = neg_query;
                            break;
                    }
                    if (query_root == null)
                    {
                        continue;
                    }
                    List<WmeImpl>/*epmem_wme_list**/ children = epmem_get_augs_of_id(query_root, DefaultMarker.create());//get_new_tc_number(my_agent));
                    // for each first level WME, build up a DNF
                    for (WmeImpl wme_iter: children) 
                    {
                        EpmemLiteral/*epmem_literal**/ child = epmem_build_dnf(
                                    wme_iter, 
                                    literal_cache, 
                                    leaf_literals, 
                                    symbol_num_incoming, 
                                    gm_ordering, 
                                    currents, 
                                    query_type, 
                                    visiting, 
                                    cue_wmes
                                );
                        if (child != null) 
                        {
                            // force all first level literals to have the same id symbol
                            child.id_sym = pos_query;
                            child.parents.add(root_literal);
                            root_literal.children.add(child);
                        }
                    }
                    //delete children;
                }
                //my_agent->epmem_timers->query_dnf->stop();
            }

            // calculate the highest possible score and cardinality score
            double perfect_score = 0;
            int perfect_cardinality = 0;
            for (EpmemLiteral iter: leaf_literals) 
            {
                if (iter.is_neg_q == 0) 
                {
                    perfect_score += iter.weight;
                    perfect_cardinality++;
                }
            }

            // set default values for before and after
            if (before == EPMEM_MEMID_NONE) {
                before = stats.time.get() - 1;//my_agent->epmem_stats->time->get_value() - 1;
            }
            else 
            {
                before = before - 1; // since before's are strict
            }
            /*  WAT.  They are uint64_t
            if (after == EPMEM_MEMID_NONE) {
                after = EPMEM_MEMID_NONE;
            }
            */
            long/*epmem_time_id*/ current_episode = before;
            long/*epmem_time_id*/ next_episode;

            
            // create dummy edges and intervals
            {
                // insert dummy unique edge and interval end point queries for DNF root
                // we make an SQL statement just so we don't have to do anything special at cleanup
                EpmemTriple triple = new EpmemTriple(EPMEM_NODEID_BAD, EPMEM_NODEID_BAD, EPMEM_NODEID_ROOT);
                EpmemPEdge root_pedge = new EpmemPEdge();
                //allocate_with_pool(my_agent, &(my_agent->epmem_pedge_pool), &root_pedge);
                root_pedge.triple = triple;
                root_pedge.value_is_id = EPMEM_RIT_STATE_EDGE;
                root_pedge.has_noncurrent = false;
                root_pedge.literals = new HashSet<EpmemLiteral>();
                root_pedge.literals.add(root_literal);
                root_pedge.sql = db.pool_dummy.request();//my_agent->epmem_stmts_graph->pool_dummy->request();
                root_pedge.sql.setLong(1, Long.MAX_VALUE/*LLONG_MAX*/);
                root_pedge.sqlResults = root_pedge.sql.executeQuery();
                root_pedge.time = Long.MAX_VALUE/*LLONG_MAX*/;
                pedge_pq.add(root_pedge);
                pedge_caches[EPMEM_RIT_STATE_EDGE].put(triple, root_pedge);
                
                EpmemUEdge root_uedge = new EpmemUEdge();
                //allocate_with_pool(my_agent, &(my_agent->epmem_uedge_pool), &root_uedge);
                root_uedge.triple = triple;
                root_uedge.value_is_id = EPMEM_RIT_STATE_EDGE;
                root_uedge.has_noncurrent = false;
                root_uedge.activation_count = 0;
                root_uedge.pedges = new HashSet<EpmemPEdge>();
                root_uedge.intervals = 1;
                root_uedge.activated = false;
                uedge_caches[EPMEM_RIT_STATE_EDGE].put(triple, root_uedge);

                EpmemInterval root_interval = new EpmemInterval();
                //allocate_with_pool(my_agent, &(my_agent->epmem_interval_pool), &root_interval);
                root_interval.uedge = root_uedge;
                root_interval.is_end_point = 1;//true;
                root_interval.sql = db.pool_dummy.request();//my_agent->epmem_stmts_graph->pool_dummy->request();
                //root_interval->sql->prepare();
                root_interval.sql.setLong(1, before);
                root_interval.sqlResult = root_interval.sql.executeQuery();
                root_interval.time = before;
                interval_pq.add(root_interval);
                interval_cleanup.add(root_interval);
            }

            if (logger.isDebugEnabled()) {
                logger.debug(epmem_print_retrieval_state(literal_cache, pedge_caches, uedge_caches));
            }

            /*
            #ifdef EPMEM_EXPERIMENT
                    epmem_episodes_searched = 0;
            #endif
            */
            // main loop of interval walk
            //my_agent->epmem_timers->query_walk->start();
            
            while (pedge_pq.size() != 0 && current_episode > after) 
            {
                long /*epmem_time_id*/ next_edge;
                long /*epmem_time_id*/ next_interval;

                boolean changed_score = false;

                //my_agent->epmem_timers->query_walk_edge->start();
                next_edge = pedge_pq.peek().time;
                // process all edges which were last used at this time point
                while ((pedge_pq.size() != 0) && (pedge_pq.peek().time == next_edge || pedge_pq.peek().time >= current_episode)) 
                {
                    EpmemPEdge pedge = pedge_pq.poll();
                    EpmemTriple triple = pedge.triple;
                    triple.q1 = pedge.sqlResults.getLong(1 + 1);
                    
                    if (logger.isDebugEnabled()) {
                        logger.debug("  EDGE " + triple.q0 + "-" + triple.w + "-" + triple.q1);
                    }

                    // create queries for the unique edge children of this partial edge
                    if (pedge.value_is_id != 0) 
                    {
                        boolean created = false;
                        //for (epmem_literal_set::iterator literal_iter = pedge->literals.begin(); literal_iter != pedge->literals.end(); literal_iter++) {
                        for(EpmemLiteral literal_iter: pedge.literals)
                        {
                            EpmemLiteral literal = literal_iter;
                            //for (epmem_literal_set::iterator child_iter = literal->children.begin(); child_iter != literal->children.end(); child_iter++) {
                            for(EpmemLiteral child_iter: literal.children)
                            {
                                created |= epmem_register_pedges(triple.q1, child_iter, pedge_pq, after, pedge_caches, uedge_caches);
                            }
                        }
                    }
                    // Left in from C: TODO what I want to do here is, if there is no children which leads to a leaf, retract everything
                    // I'm not sure how to properly test for this though

                    // look for uedge with triple; if none exist, create one
                    // otherwise, link up the uedge with the pedge and consider score changes
                    Map<EpmemTriple, EpmemUEdge>/*epmem_triple_uedge_map**/ uedge_cache = uedge_caches[pedge.value_is_id];
                    EpmemUEdge uedge_iter = uedge_cache.get(triple);
                    if (uedge_iter != null) 
                    {
                        // create a uedge for this
                        EpmemUEdge uedge = new EpmemUEdge();
                        //allocate_with_pool(my_agent, &(my_agent->epmem_uedge_pool), &uedge);
                        uedge.triple = triple;
                        uedge.value_is_id = pedge.value_is_id;
                        uedge.has_noncurrent = pedge.has_noncurrent;
                        uedge.activation_count = 0;
                        uedge.pedges  = new HashSet<EpmemPEdge>();
                        uedge.intervals = 0;
                        uedge.activated = false;
                        // create interval queries for this partial edge
                        boolean created = false;
                        long/*int64_t*/ edge_id = pedge.sqlResults.getLong(0 + 1);
                        long/*epmem_time_id*/ promo_time = EPMEM_MEMID_NONE;
                        boolean is_lti = (pedge.value_is_id != 0 && pedge.triple.q1 != EPMEM_NODEID_BAD && pedge.triple.q1 != EPMEM_NODEID_ROOT);
                        if (is_lti) 
                        {
                            // find the promotion time of the LTI
                            db.find_lti_promotion_time.setLong(1, triple.q1);
                            ResultSet results = db.find_lti_promotion_time.executeQuery();
                            try
                            {
                                promo_time = results.getLong(0 + 1);
                            }
                            finally
                            {
                                results.close();
                            }
                            
                            //my_agent->epmem_stmts_graph->find_lti_promotion_time->reinitialize();
                        }
                        for (int interval_type = EPMEM_RANGE_EP; interval_type <= EPMEM_RANGE_POINT; interval_type++) 
                        {
                            for (int point_type = EPMEM_RANGE_START; point_type <= EPMEM_RANGE_END; point_type++) 
                            {
                                /*
                                 * This appears to be selecting the timer to time the next queries -ACN
                                 * 
                                // pick a timer (any timer)
                                soar_module::timer* sql_timer = NULL;
                                switch (interval_type) {
                                    case EPMEM_RANGE_EP:
                                        if (point_type == EPMEM_RANGE_START) {
                                            sql_timer = my_agent->epmem_timers->query_sql_start_ep;
                                        } else {
                                            sql_timer = my_agent->epmem_timers->query_sql_end_ep;
                                        }
                                        break;
                                    case EPMEM_RANGE_NOW:
                                        if (point_type == EPMEM_RANGE_START) {
                                            sql_timer = my_agent->epmem_timers->query_sql_start_now;
                                        } else {
                                            sql_timer = my_agent->epmem_timers->query_sql_end_now;
                                        }
                                        break;
                                    case EPMEM_RANGE_POINT:
                                        if (point_type == EPMEM_RANGE_START) {
                                            sql_timer = my_agent->epmem_timers->query_sql_start_point;
                                        } else {
                                            sql_timer = my_agent->epmem_timers->query_sql_end_point;
                                        }
                                        break;
                                }
                                */
                                // create the SQL query and bind it
                                // try to find an existing query first; if none exist, allocate a new one from the memory pools
                                PreparedStatement/*soar_module::pooled_sqlite_statement**/ interval_sql = null;
                                if (is_lti) 
                                {
                                    interval_sql = db.pool_find_lti_queries[point_type][interval_type].request(/*sql_timer*/);
                                } 
                                else 
                                {
                                    interval_sql = db.pool_find_interval_queries[pedge.value_is_id][point_type][interval_type].request(/*sql_timer*/);
                                }
                                //This is setting the binding index for sql queries, which is 1 higher in jdbc vs. C
                                int bind_pos = 1 + 1;
                                if (point_type == EPMEM_RANGE_END && interval_type == EPMEM_RANGE_NOW) 
                                {
                                    interval_sql.setLong(bind_pos++, current_episode);
                                }
                                interval_sql.setLong(bind_pos++, edge_id);
                                if (is_lti) 
                                {
                                    // find the promotion time of the LTI, and use that as an after constraint
                                    interval_sql.setLong(bind_pos++, promo_time);
                                }
                                interval_sql.setLong(bind_pos++, current_episode);
                                ResultSet results = interval_sql.executeQuery();
                                if (results.next()) 
                                {
                                    EpmemInterval interval = new EpmemInterval();
                                    //allocate_with_pool(my_agent, &(my_agent->epmem_interval_pool), &interval);
                                    interval.is_end_point = point_type;
                                    interval.uedge = uedge;
                                    interval.time = results.getLong(0 + 1);
                                    interval.sql = interval_sql;
                                    //This logic does not allow us to free this result set here.
                                    //This means that we need ot close this by hand later on. -ACN
                                    interval.sqlResult = results;
                                    interval_pq.add(interval);
                                    interval_cleanup.add(interval);
                                    uedge.intervals++;
                                    created = true;
                                }
                                else 
                                {
                                    results.close();
                                }
                            }
                        }
                        if (created) 
                        {
                            if (is_lti) 
                            {
                                // insert a dummy promo time start for LTIs
                                EpmemInterval start_interval = new EpmemInterval();
                                //allocate_with_pool(my_agent, &(my_agent->epmem_interval_pool), &start_interval);
                                start_interval.uedge = uedge;
                                start_interval.is_end_point = EPMEM_RANGE_START;
                                start_interval.time = promo_time - 1;
                                start_interval.sql = null;
                                interval_pq.add(start_interval);
                                interval_cleanup.add(start_interval);
                            }
                            uedge.pedges.add(pedge);
                            uedge_cache.put(triple, uedge);
                        } 
                        else
                        { 
                            //uedge.pedges.~epmem_pedge_set();
                            uedge.pedges.clear();
                            uedge.pedges = null;
                            //free_with_pool(&(my_agent->epmem_uedge_pool), uedge);
                        }
                    }
                    else
                    {
                        EpmemUEdge uedge = uedge_iter;
                        uedge.pedges.add(pedge);
                        if (uedge.activated) {
                            for (EpmemLiteral lit_iter: pedge.literals) 
                            {
                                EpmemLiteral literal = lit_iter;
                                if (!literal.is_current || uedge.activation_count == 1) 
                                {
                                    ByRef<Double> curScoreRef = new ByRef<Double>(current_score);
                                    ByRef<Long> curCardinalityRef = new ByRef<Long>(current_cardinality);
                                    changed_score |= epmem_satisfy_literal(literal, triple.q0, triple.q1, curScoreRef, curCardinalityRef, symbol_node_count, uedge_caches, symbol_num_incoming);
                                    current_score = curScoreRef.value;
                                    current_cardinality = curCardinalityRef.value;
                                }
                            }
                        }
                    }

                    // put the partial edge query back into the queue if there's more
                    // otherwise, reinitialize the query and put it in a pool
                    if(pedge.sql != null)
                    {
                        ResultSet results = pedge.sql.executeQuery();
                        pedge.sqlResults = results;
                        if(results.next())
                        {
                            pedge.time = results.getLong(2 + 1);
                            pedge_pq.add(pedge);
                        }
                        else 
                        {
                            //pedge->sql->get_pool()->release(pedge->sql);
                            pedge.sqlResults.close();
                            pedge.sql = null;
                        }
                    }
                }
                next_edge = (pedge_pq.isEmpty() ? after : pedge_pq.peek().time);
                //my_agent->epmem_timers->query_walk_edge->stop();

                // process all intervals before the next edge arrives
                //my_agent->epmem_timers->query_walk_interval->start();
                
                while (interval_pq.size() != 0 && interval_pq.peek().time > next_edge && current_episode > after) 
                {
                    if (logger.isDebugEnabled()) {
                        logger.debug("EPISODE " + current_episode);
                    }

                    // process all interval endpoints at this time step
                    while (interval_pq.size() != 0 && interval_pq.peek().time >= current_episode) 
                    {
                        EpmemInterval interval = interval_pq.poll();
                        EpmemUEdge uedge = interval.uedge;
                        EpmemTriple triple = uedge.triple;

                        if (logger.isDebugEnabled()) {
                            logger.debug("  INTERVAL (" + (interval.is_end_point != 0 ? "end" : "start") + "): " + triple.q0 + "-" + triple.w + "-" + triple.q1);
                        }

                        if (interval.is_end_point != 0) 
                        {
                            uedge.activated = true;
                            uedge.activation_count++;
                            for (EpmemPEdge pedge_iter: uedge.pedges)
                            {
                                EpmemPEdge pedge = pedge_iter;
                                for (EpmemLiteral lit_iter: pedge.literals)
                                {
                                    EpmemLiteral literal = lit_iter;
                                    if (!literal.is_current || uedge.activation_count == 1) 
                                    {
                                        // TODO: Consider just using ByRef throughout this function.
                                        ByRef<Double> curScoreRef = new ByRef<Double>(current_score);
                                        ByRef<Long> curCardinalityRef = new ByRef<Long>(current_cardinality);
                                        changed_score |= epmem_satisfy_literal(literal, triple.q0, triple.q1, curScoreRef, curCardinalityRef, symbol_node_count, uedge_caches, symbol_num_incoming);
                                        current_score = curScoreRef.value;
                                        current_cardinality = curCardinalityRef.value;
                                    }
                                }
                            }
                        } 
                        else
                        {
                            uedge.activated = false;
                            for (EpmemPEdge pedge_iter: uedge.pedges)
                            {
                                EpmemPEdge pedge = pedge_iter;
                                for (EpmemLiteral lit_iter: pedge.literals)
                                {
                                    ByRef<Double> curScore = new ByRef<Double>(current_score);
                                    ByRef<Long> curCardinality = new ByRef<Long>(current_cardinality);
                                    changed_score |= epmem_unsatisfy_literal(lit_iter, triple.q0, triple.q1, curScore, curCardinality, symbol_node_count);
                                    current_score = curScore.value;
                                    current_cardinality = curCardinality.value;
                                }
                            }
                        }
                        // put the interval query back into the queue if there's more and some literal cares
                        // otherwise, reinitialize the query and put it in a pool
                        
                        if(interval.sql != null)
                        {
                            interval.sqlResult = interval.sql.executeQuery();
                            if (interval.uedge.has_noncurrent && interval.sqlResult.next()) 
                            {
                                interval.time = interval.sqlResult.getInt(0 + 1);
                                interval_pq.add(interval);
                            }
                            else if (interval.sql != null)
                            {
                                //interval->sql->get_pool()->release(interval->sql);
                                interval.sqlResult.close();
                                interval.sql = null;
                                uedge.intervals--;
                                if (uedge.intervals != 0)
                                {
                                    interval_cleanup.remove(interval);
                                    //free_with_pool(&(my_agent->epmem_interval_pool), interval);
                                } 
                                else 
                                {
                                    // TODO retract intervals
                                }
                            }
                        }
                    }
                    next_interval = (interval_pq.isEmpty() ? after : interval_pq.peek().time);
                    next_episode = (next_edge > next_interval ? next_edge : next_interval);

                    // update the prohibits list to catch up
                    while (prohibits.size() != 0 && prohibits.get(prohibits.size() - 1) > current_episode)
                    {
                        prohibits.remove(prohibits.size() - 1);
                    }
                    // ignore the episode if it is prohibited
                    while (prohibits.size() != 0 && current_episode > next_episode && current_episode == prohibits.get(prohibits.size() - 1))
                    {
                        current_episode--;
                        prohibits.remove(prohibits.size() - 1);
                    }

                    if (logger.isTraceEnabled()) {
                        logger.trace(epmem_print_retrieval_state(literal_cache, pedge_caches, uedge_caches));
                    }
                    
                    /*
                    if (my_agent->sysparams[TRACE_EPMEM_SYSPARAM]) {
                        char buf[256];
                        SNPRINTF(buf, 254, "CONSIDERING EPISODE (time, cardinality, score) (%lld, %ld, %f)\n", static_cast<long long int>(current_episode), current_cardinality, current_score);
                        print(my_agent, buf);
                        xml_generate_warning(my_agent, buf);
                    }
                    */
                    /*
                    #ifdef EPMEM_EXPERIMENT
                                    epmem_episodes_searched++;
                    #endif
                    */
                    // if
                    // * the current time is still before any new intervals
                    // * and the score was changed in this period
                    // * and the new score is higher than the best score
                    // then save the current time as the best one
                    if (
                            current_episode > next_episode && 
                            changed_score && 
                            (
                                    best_episode == EPMEM_MEMID_NONE || 
                                    current_score > best_score || 
                                    (
                                            do_graph_match && 
                                            current_score == best_score && !best_graph_matched
                                    )
                            )
                    ) {
                        boolean new_king = false;
                        if (best_episode == EPMEM_MEMID_NONE || current_score > best_score)
                        {
                            best_episode = current_episode;
                            best_score = current_score;
                            best_cardinality = current_cardinality;
                            new_king = true;
                        }
                        // we should graph match if the option is set and all leaf literals are satisfied
                        if (current_cardinality == perfect_cardinality) 
                        {
                            boolean graph_matched = false;
                            if (do_graph_match) 
                            {
                                if (gm_order == DefaultEpisodicMemoryParams.GmOrderingChoices.undefined)
                                {
                                    //TODO: This is probably sorting on pointer values.  Why is it here, and 
                                    //do we need to emulate it?
                                    //std::sort(gm_ordering.begin(), gm_ordering.end());
                                }
                                else if (gm_order == DefaultEpisodicMemoryParams.GmOrderingChoices.mcv)
                                {
                                    //std::sort(gm_ordering.begin(), gm_ordering.end(), epmem_gm_mcv_comparator);
                                    Collections.sort(
                                            gm_ordering,
                                            new Comparator<EpmemLiteral>()
                                            {
                                                //episodic_memory.cpp:3427
                                                @Override
                                                public int compare(EpmemLiteral a, EpmemLiteral b)
                                                {
                                                    // TODO Auto-generated method stub
                                                    return (a.matches.size() < b.matches.size()? -1: 1);
                                                }
                                            }
                                        );
                                }
                                //epmem_literal_deque::iterator begin = gm_ordering.begin();
                                //epmem_literal_deque::iterator end = gm_ordering.end();
                                best_bindings.clear();
                                //Java array do not get along well with paramatized types.  Maybe 
                                //we should make this a list. -ACN
                                Map<Long/*epmem_node_id*/, SymbolImpl>[]/*epmem_node_symbol_map*/ bound_nodes = (Map<Long, SymbolImpl>[])new Map[2];
                                for(int i = 0; i < bound_nodes.length; i++)
                                {
                                    bound_nodes[i] = new HashMap<Long, SymbolImpl>(); 
                                }
                                
                                if (logger.isDebugEnabled()) {
                                    logger.debug("  GRAPH MATCH");
                                    logger.debug(epmem_print_retrieval_state(literal_cache, pedge_caches, uedge_caches));
                                }
                                
                                //my_agent->epmem_timers->query_graph_match->start();
                                graph_matched = epmem_graph_match(gm_ordering, gm_ordering.listIterator(), best_bindings, bound_nodes, 2);
                                //my_agent->epmem_timers->query_graph_match->stop();
                            }
                            if (!do_graph_match || graph_matched) 
                            {
                                best_episode = current_episode;
                                best_graph_matched = true;
                                current_episode = EPMEM_MEMID_NONE;
                                new_king = true;
                            }
                        }
                        /*
                        if (new_king && my_agent->sysparams[TRACE_EPMEM_SYSPARAM]) {
                            char buf[256];
                            SNPRINTF(buf, 254, "NEW KING (perfect, graph-match): (%s, %s)\n", (current_cardinality == perfect_cardinality ? "true" : "false"), (best_graph_matched ? "true" : "false"));
                            print(my_agent, buf);
                            xml_generate_warning(my_agent, buf);
                        }
                        */
                    }

                    if (current_episode == EPMEM_MEMID_NONE)
                    {
                        break;
                    } 
                    else 
                    {
                        current_episode = next_episode;
                    }
                }
                //my_agent->epmem_timers->query_walk_interval->stop();
            }
            //my_agent->epmem_timers->query_walk->stop();

            // if the best episode is the default, fail
            // otherwise, put the episode in working memory
            if (best_episode == EPMEM_MEMID_NONE)
            {
                epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_failure, pos_query);
                if (neg_query != null) 
                {
                    epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_failure, neg_query);
                }
            }
            else 
            {
                //my_agent->epmem_timers->query_result->start();
                SymbolImpl temp_sym;
                Map<Long/*epmem_node_id*/, SymbolImpl>/*epmem_id_mapping*/ node_map_map = new HashMap<Long, SymbolImpl>();
                Map<Long/*epmem_node_id*/, SymbolImpl>/*epmem_id_mapping*/ node_mem_map = new HashMap<Long, SymbolImpl>();
                // cue size
                temp_sym = symbols.createInteger(leaf_literals.size());
                epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_cue_size, temp_sym);
                //symbol_remove_ref(my_agent, temp_sym);
                // match cardinality
                temp_sym = symbols.createInteger(best_cardinality);
                epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_match_cardinality, temp_sym);
                //symbol_remove_ref(my_agent, temp_sym);
                // match score
                temp_sym = symbols.createDouble(best_score);
                epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_match_score, temp_sym);
                //symbol_remove_ref(my_agent, temp_sym);
                // normalized match score
                temp_sym = symbols.createDouble(best_score / perfect_score);
                epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_normalized_match_score, temp_sym);
                //symbol_remove_ref(my_agent, temp_sym);
                // status
                epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_success, pos_query);
                if (neg_query != null) 
                {
                    epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_success, neg_query);
                }
                // give more metadata if graph match is turned on
                if (do_graph_match)
                {
                    // graph match
                    temp_sym = symbols.createInteger((best_graph_matched ? 1 : 0));
                    epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_graph_match, temp_sym);
                    //symbol_remove_ref(my_agent, temp_sym);

                    // mapping
                    if (best_graph_matched) 
                    {
                        //This instantiation of level is shadowing the function parameter, which java does not
                        //allow, so were going to have to rename it here.
                        int/*goal_stack_level*/ levelLocal = epmem_info(state).epmem_result_header.level;
                        // mapping identifier
                        SymbolImpl mapping = symbols.make_new_identifier('M', level);
                        epmem_buffer_add_wme(meta_wmes, epmem_info(state).epmem_result_header, predefinedSyms.epmem_sym_graph_match_mapping, mapping);
                        //symbol_remove_ref(my_agent, mapping);

                        //for (epmem_literal_node_pair_map::iterator iter = best_bindings.begin(); iter != best_bindings.end(); iter++) {
                        for(Entry<EpmemLiteral, EpmemNodePair> iter: best_bindings.entrySet())
                        {
                            if (iter.getKey().value_is_id != 0)
                            {
                                // create the node
                                temp_sym = symbols.make_new_identifier('N', level);
                                epmem_buffer_add_wme(meta_wmes, mapping, predefinedSyms.epmem_sym_graph_match_mapping_node, temp_sym);
                                //symbol_remove_ref(my_agent, temp_sym);
                                // point to the cue identifier
                                epmem_buffer_add_wme(meta_wmes, temp_sym, predefinedSyms.epmem_sym_graph_match_mapping_cue, iter.getKey().value_sym);
                                // save the mapping point for the episode
                                node_map_map.put(iter.getValue().second, temp_sym);
                                node_mem_map.put(iter.getValue().second, null);
                            }
                        }
                    }
                }
                // reconstruct the actual episode
                if (level > 2) 
                {
                    epmem_install_memory(state, best_episode, meta_wmes, retrieval_wmes, node_mem_map);
                }
                if (best_graph_matched)
                {
                    //for (epmem_id_mapping::iterator iter = node_mem_map.begin(); iter != node_mem_map.end(); iter++) {
                    for(Entry<Long, SymbolImpl> iter: node_mem_map.entrySet())
                    {
                        //epmem_id_mapping::iterator map_iter = node_map_map.find((*iter).first);
                        SymbolImpl map_iter = node_map_map.get(iter.getKey());
                        if (map_iter != null && iter.getValue() != null)
                        {
                            epmem_buffer_add_wme(meta_wmes, map_iter, predefinedSyms.epmem_sym_retrieved, iter.getValue());
                        }
                    }
                }
                //my_agent->epmem_timers->query_result->stop();
            }
        }

        // cleanup
        //my_agent->epmem_timers->query_cleanup->start();
        for (EpmemInterval interval: interval_cleanup) 
        {
            //epmem_interval* interval = *iter;
            if (interval.sqlResult != null) 
            {
                //interval->sql->get_pool()->release(interval->sql);
                interval.sqlResult.close();
            }
            //free_with_pool(&(my_agent->epmem_interval_pool), interval);
        }
        for (int type = EPMEM_RIT_STATE_NODE; type <= EPMEM_RIT_STATE_EDGE; type++) 
        {
            //for (epmem_triple_pedge_map::iterator iter = pedge_caches[type].begin(); iter != pedge_caches[type].end(); iter++) {
            for(EpmemPEdge pedge: pedge_caches[type].values())
            {    
                //epmem_pedge* pedge = (*iter).second;
                if (pedge.sqlResults != null) 
                {
                    //pedge->sql->get_pool()->release(pedge->sql);
                    pedge.sqlResults.close();
                }
                //In some places, we use clear to "destroy" containers, but this one is about to leave
                //scope so we dont need to bother. -ACN
                //pedge->literals.~epmem_literal_set();
                //free_with_pool(&(my_agent->epmem_pedge_pool), pedge);
            }
            /*
             * No queries to free, so we don't need to do this loop. -ACN
            for (epmem_triple_uedge_map::iterator iter = uedge_caches[type].begin(); iter != uedge_caches[type].end(); iter++) {
                epmem_uedge* uedge = (*iter).second;
                uedge->pedges.~epmem_pedge_set();
                free_with_pool(&(my_agent->epmem_uedge_pool), uedge);
            }
            */
        }
        /*
         * No queries to free, so we don't need to do this loop. -ACN
        for (epmem_wme_literal_map::iterator iter = literal_cache.begin(); iter != literal_cache.end(); iter++) {
            epmem_literal* literal = (*iter).second;
            literal->parents.~epmem_literal_set();
            literal->children.~epmem_literal_set();
            literal->matches.~epmem_node_pair_set();
            literal->values.~epmem_node_int_map();
            free_with_pool(&(my_agent->epmem_literal_pool), literal);
        }
        my_agent->epmem_timers->query_cleanup->stop();

        my_agent->epmem_timers->query->stop();
        */
    }
    
    /**
     * episodic_memory:3756
     * 
     * bool epmem_graph_match(
     *      epmem_literal_deque::iterator& dnf_iter, 
     *      epmem_literal_deque::iterator& iter_end, 
     *      epmem_literal_node_pair_map& bindings, 
     *      epmem_node_symbol_map bound_nodes[], 
     *      agent* my_agent, 
     *      int depth = 0
     *  )
     * 
     * @param gm_ordering
     * @param best_bindings
     * @param bound_nodes
     * @param i
     * @return
     */
    private boolean epmem_graph_match(
            LinkedList<EpmemLiteral> dnf_array,
            ListIterator<EpmemLiteral> dnf_iter,
            Map<EpmemLiteral, EpmemNodePair> bindings,
            Map<Long, SymbolImpl>[] bound_nodes, 
            int depth
    )
    {
        if (!dnf_iter.hasNext()) {
            return true;
        }
        
        // Doing a next to get the value then a previous so 
        // the cursor stays at its original position
        EpmemLiteral literal = dnf_iter.next();
        dnf_iter.previous();
        
        if (bindings.containsKey(literal)) {
            return false;
        }
        //epmem_literal_deque::iterator next_iter = dnf_iter;
        // TODO: This may be slow using linked lists but its the best out of the box option in Java
        ListIterator<EpmemLiteral> next_iter = dnf_array.listIterator(dnf_iter.nextIndex());
        //next_iter++;
        next_iter.next();

    //#ifdef USE_MEM_POOL_ALLOCATORS
    //    epmem_node_set failed_parents = epmem_node_set(std::less<epmem_node_id>(), soar_module::soar_memory_pool_allocator<epmem_node_id>(my_agent));
    //    epmem_node_set failed_children = epmem_node_set(std::less<epmem_node_id>(), soar_module::soar_memory_pool_allocator<epmem_node_id>(my_agent));
    //#else
        Set<Long> failed_parents = new HashSet<Long>();
        Set<Long> failed_children = new HashSet<Long>();
    //#endif
        // go through the list of matches, binding each one to this literal in turn
        for ( EpmemNodePair match : literal.matches ) {
            long q0 = match.first;
            long q1 = match.second;
            if (failed_parents.contains(q0)) {
                continue;
            }
            if (logger.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < depth; i++) {
                    sb.append("\t");
                }
                sb.append("TRYING ").append(literal).append(" ").append(q0);
                logger.trace(sb.toString());
            }
            boolean relations_okay = true;
            // for all parents
            for ( EpmemLiteral parent : literal.parents ) {
                EpmemNodePair bind = bindings.get(parent); 
                if (bind != null && bind.second != q0) {
                    relations_okay = false;
                    break;
                }
            }
            if (!relations_okay) {
                if ( logger.isTraceEnabled() ) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < depth; i++) {
                        sb.append("\t");
                    }
                    sb.append("PARENT CONSTRAINT FAIL");
                    logger.trace(sb.toString());
                }
                failed_parents.add(q0);
                continue;
            }
            // if the node has already been bound, make sure it's bound to the same thing
            SymbolImpl binder = bound_nodes[(int) literal.value_is_id].get(q1);
            if (binder != null && binder != literal.value_sym) {
                failed_children.add(q1);
                continue;
            }
            if ( logger.isTraceEnabled() ) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < depth; i++) {
                    sb.append("\t");
                }
                sb.append("TRYING ").append(literal).append(" ").append(q0).append(" ").append(q1);
                logger.trace(sb.toString());
            }
            if (literal.q1 != EPMEM_NODEID_BAD && literal.q1 != q1) {
                relations_okay = false;
            }
            // for all children
            for ( EpmemLiteral child : literal.children ) {
                EpmemNodePair bind = bindings.get(child);
                if ( bind != null && bind.first != q1) {
                    relations_okay = false;
                    break;
                }
            }
            if (!relations_okay) {
                StringBuilder sb = new StringBuilder();
                if (logger.isTraceEnabled()) {
                    for (int i = 0; i < depth; i++) {
                        sb.append("\t");
                    }
                    sb.append("CHILD CONSTRAINT FAIL");
                    logger.trace(sb.toString());
                }
                failed_children.add(q1);
                continue;
            }
            if (logger.isTraceEnabled()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < depth; i++) {
                    sb.append("\t");
                }
                sb.append(literal).append(" ").append(q0).append(" ").append(q1);
            }
            // temporarily modify the bindings and bound nodes
            bindings.put(literal, new EpmemNodePair(q0,q1));
            bound_nodes[(int) literal.value_is_id].put(q1, literal.value_sym);
            
            // recurse on the rest of the list
            boolean list_satisfied = epmem_graph_match(dnf_array, next_iter, bindings, bound_nodes, depth + 1);
            // if the rest of the list matched, we've succeeded
            // otherwise, undo the temporarily modifications and try again
            if (list_satisfied) {
                return true;
            } else {
                bindings.remove(literal);
                bound_nodes[(int) literal.value_is_id].remove(q1);
            }
        }
        // this means we've tried everything and this whole exercise was a waste of time
        // EPIC FAIL
        if ( logger.isTraceEnabled() ) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                sb.append("\t");
            }
            sb.append("EPIC FAIL");
            logger.trace(sb.toString());
        }
        return false;
    }

    /**
     * bool epmem_unsatisfy_literal(
     *      epmem_literal* literal, 
     *      epmem_node_id parent, 
     *      epmem_node_id child, 
     *      double& current_score, 
     *      long int& current_cardinality, 
     *      epmem_symbol_node_pair_int_map& symbol_node_count
     *   )
     * 
     * @param lit_iter
     * @param q0
     * @param q1
     * @param current_score
     * @param current_cardinality
     * @param symbol_node_count
     * @return
     */
    private boolean epmem_unsatisfy_literal(
            EpmemLiteral literal, 
            long /*epmem_node_id*/ parent,
            long /*epmem_node_id*/ child, 
            ByRef<Double> current_score, 
            ByRef<Long> current_cardinality,
            Map<EpmemSymbolNodePair, Integer> symbol_node_count
    )
    {
        if (literal.matches.size() == 0) {
            return false;
        }
        if ( logger.isDebugEnabled() ) {
            logger.debug("      RECURSING ON " + parent + " " + child + " " + literal);
        }
        // we only need things if this parent-child pair is matching the literal
        // epmem_node_pair_set::iterator lit_match_iter = literal->matches.find(std::make_pair(parent, child));
        EpmemNodePair enpair = new EpmemNodePair(parent,child);
        boolean removedMatch = literal.matches.remove(enpair);
        if ( removedMatch ) {
            // erase the edge from this literal's matches
            // literal->matches.erase(lit_match_iter);
            int value = literal.values.get(child);
            //epmem_node_int_map::iterator values_iter = literal->values.find(child);
            //(*values_iter).second--;
            value--;
            literal.values.put(child, value);
            if ( value == 0) {
                literal.values.remove(child);
                if (literal.is_leaf) {
                    if (literal.matches.size() == 0) {
                        current_score.value -= literal.weight;
                        current_cardinality.value -= (literal.is_neg_q != 0 ? -1 : 1);
                        if ( logger.isDebugEnabled() ) {
                            logger.debug("          NEW SCORE: " + current_score + ", " + current_cardinality);
                        }
                        return true;
                    }
                } else {
                    boolean changed_score = false;
                    EpmemSymbolNodePair match = new EpmemSymbolNodePair(literal.value_sym, child);
                    int match_value = symbol_node_count.get(match);
                    match_value--;
                    if (match_value == 0) {
                        symbol_node_count.remove(match);
                    } else {
                        symbol_node_count.put(match, match_value);
                    }
                    // if this literal is no longer satisfied, recurse on all children
                    // if this literal is still satisfied, recurse on children who is matching on descendants of this edge
                    if (literal.matches.size() == 0) {
                        for (EpmemLiteral child_lit : literal.children ) {
                            for (EpmemNodePair node : child_lit.matches ) {
                                changed_score |= epmem_unsatisfy_literal(child_lit, node.first, node.second, current_score, current_cardinality, symbol_node_count);
                            }
                        }
                    } else {
                        EpmemNodePair node_pair = new EpmemNodePair(child, EPMEM_NODEID_BAD);
                        for ( EpmemLiteral child_lit : literal.children ) {
                            EpmemNodePair node = child_lit.matches.ceiling(node_pair);
                            if (node != null && node.first == child) {
                                changed_score |= epmem_unsatisfy_literal(child_lit, node.first, node.second, current_score, current_cardinality, symbol_node_count);
                            }
                        }
                    }
                    return changed_score;
                }
            }
        }
        return false;
    }

    /**
     * bool epmem_satisfy_literal(
     *      epmem_literal* literal,     
     *      epmem_node_id parent, 
     *      epmem_node_id child, 
     *      double& current_score, 
     *      long int& current_cardinality, 
     *      epmem_symbol_node_pair_int_map& symbol_node_count, 
     *      epmem_triple_uedge_map uedge_caches[], 
     *      epmem_symbol_int_map& symbol_num_incoming) {
     * 
     * @param literal
     * @param parent
     * @param child
     * @param current_score
     * @param current_cardinality
     * @param symbol_node_count
     * @param uedge_caches
     * @param symbol_num_incoming
     * @return
     */
    private boolean epmem_satisfy_literal(
            EpmemLiteral literal, 
            long parent,
            long child, 
            ByRef<Double> current_score, 
            ByRef<Long> current_cardinality,
            Map<EpmemSymbolNodePair, Integer> symbol_node_count,
            SortedMap<EpmemTriple,EpmemUEdge>[] uedge_caches,
            Map<SymbolImpl, Integer> symbol_num_incoming)
    {
        if ( logger.isDebugEnabled() ) {
            logger.debug("      RECURSING ON " + parent + " " + child + " " + literal); 
        }
                
        // check if the ancestors of this literal are satisfied
        boolean parents_satisfied = (literal.id_sym == null);
        if (!parents_satisfied) {
            // ancestors are satisfied if:
            // 1. all incoming literals are satisfied
            // 2. all incoming literals have this particular node satisfying it
            
            // TODO: Check if this map retrieval should return 0 if key is missing
            Integer num_incoming = symbol_num_incoming.get(literal.id_sym);
            Integer match = symbol_node_count.get(new EpmemSymbolNodePair(literal.id_sym, parent));
            // since, by definition, if a node satisfies all incoming literals, all incoming literals are satisfied
            parents_satisfied = (match != null) && (match == num_incoming);
        }
        // if yes
        if ( parents_satisfied ) {
            // add the edge as a match
            literal.matches.add(new EpmemNodePair(parent, child));
            Integer value = literal.values.get(child);
            if ( value == null ) {
                literal.values.put(child, 1);
                if ( literal.is_leaf ) {
                    if (literal.matches.size() == 1) {
                        current_score.value += literal.weight;
                        current_cardinality.value += (literal.is_neg_q != 0 ? -1 : 1);
                        if ( logger.isDebugEnabled() ) {
                            logger.debug("          NEW SCORE: " + current_score + ", " + current_cardinality );
                        }
                        return true;
                    }
                } else {
                    boolean changed_score = false;
                    // change bookkeeping information about ancestry
                    EpmemSymbolNodePair matchKey = new EpmemSymbolNodePair(literal.value_sym, child);
                    Integer match = symbol_node_count.get(matchKey);
                    if ( match == null ) {
                        symbol_node_count.put(matchKey, 1);
                    } else {
                        symbol_node_count.put(matchKey, match+1);
                    }
                    // recurse over child literals
                    for ( EpmemLiteral child_lit : literal.children ) {
                        SortedMap<EpmemTriple, EpmemUEdge> uedge_cache = uedge_caches[(int) child_lit.value_is_id];
                        EpmemTriple child_triple = new EpmemTriple(child, child_lit.w, child_lit.q1);
                        EpmemUEdge child_uedge = null;
                        if (child_lit.q1 == EPMEM_NODEID_BAD) {
                            SortedMap<EpmemTriple, EpmemUEdge> tailMap = uedge_cache.tailMap(child_triple);
                            for ( Map.Entry<EpmemTriple, EpmemUEdge> entry : tailMap.entrySet()) {
                                child_triple = entry.getKey();
                                child_uedge = entry.getValue();
                                if (child_triple.q0 != child || child_triple.w != child_lit.w) {
                                    break;
                                }
                                if (child_uedge.activated && (!literal.is_current || child_uedge.activation_count == 1)) {
                                    changed_score |= epmem_satisfy_literal(child_lit, child_triple.q0, child_triple.q1, current_score, current_cardinality, symbol_node_count, uedge_caches, symbol_num_incoming);
                                }
                            }
                        } else {
                            child_uedge = uedge_cache.get(child_triple);
                            if (child_uedge != null ) {
                                if (child_uedge.activated && (!literal.is_current || child_uedge.activation_count == 1)) {
                                    changed_score |= epmem_satisfy_literal(child_lit, child_triple.q0, child_triple.q1, current_score, current_cardinality, symbol_node_count, uedge_caches, symbol_num_incoming);
                                }
                            }
                        }
                    }
                    return changed_score;
                }
            } else {
                literal.values.put(child, value+1);
            }
        }
        return false;
    }

    /**
     * episodic_memory.cpp:3539
     * bool epmem_register_pedges(
     *      epmem_node_id parent, 
     *      epmem_literal* literal, 
     *      epmem_pedge_pq& pedge_pq, 
     *      epmem_time_id after, 
     *      epmem_triple_pedge_map pedge_caches[], 
     *      epmem_triple_uedge_map uedge_caches[], 
     *      agent* my_agent
     *   )
     * 
     * @param q1
     * @param child_iter
     * @param pedge_pq
     * @param after
     * @param pedge_caches
     * @param uedge_caches
     * @return
     */
        
    private boolean epmem_register_pedges(long parent, EpmemLiteral literal,
            PriorityQueue<EpmemPEdge> pedge_pq, long after,
            Map<EpmemTriple, EpmemPEdge>[] pedge_caches,
            SortedMap<EpmemTriple, EpmemUEdge>[] uedge_caches
    ) throws SQLException
    {
        // we don't need to keep track of visited literals/nodes because the literals are guaranteed to be acyclic
        // that is, the expansion to the literal's children will eventually bottom out
        // select the query
        EpmemTriple triple = new EpmemTriple(parent, literal.w, literal.q1);
        int is_edge = (int) literal.value_is_id;
        if ( logger.isDebugEnabled() ) {
            logger.debug("      RECURSING ON " + parent + " " + literal);
        }
        // if the unique edge does not exist, create a new unique edge query
        // otherwse, if the pedge has not been registered with this literal
        Map<EpmemTriple, EpmemPEdge> pedge_cache = pedge_caches[is_edge];
        EpmemPEdge child_pedge = pedge_cache.get(triple);
        if ( child_pedge == null ) {
            int has_value = (literal.q1 != EPMEM_NODEID_BAD ? 1 : 0);
            //soar_module::pooled_sqlite_statement* pedge_sql = my_agent->epmem_stmts_graph->pool_find_edge_queries[is_edge][has_value]->request(my_agent->epmem_timers->query_sql_edge);

            PreparedStatement pedge_sql = db.pool_find_edge_queries[is_edge][has_value].request();
            int bind_pos = 1;
            if (is_edge == 0) {
                pedge_sql.setLong(bind_pos++, Long.MAX_VALUE);
            }
            pedge_sql.setLong(bind_pos++, triple.q0);
            pedge_sql.setLong(bind_pos++, triple.w);
            if (has_value != 0) {
                pedge_sql.setLong(bind_pos++, triple.q1);
            }
            if (is_edge != 0) {
                pedge_sql.setLong(bind_pos++, after);
            }
        
            ResultSet results = pedge_sql.executeQuery();
            try {
                if ( results.next() ) {
                    //allocate_with_pool(my_agent, &(my_agent->epmem_pedge_pool), &child_pedge);
                    child_pedge = new EpmemPEdge();
                    child_pedge.triple = triple;
                    child_pedge.value_is_id = (int) literal.value_is_id;
                    child_pedge.has_noncurrent = !literal.is_current;
                    child_pedge.sql = pedge_sql;
                    //new(&(child_pedge->literals)) epmem_literal_set();
                    child_pedge.literals = new HashSet<EpmemLiteral>();
                    child_pedge.literals.add(literal);
                    //child_pedge.time = child_pedge.sql.column_int(2);
                    child_pedge.time = results.getLong(2+1);
                    pedge_pq.add(child_pedge);
                    pedge_cache.put(triple,child_pedge);
                    return true;
                } else {
                    return false;
                }
            } finally {
                results.close();
            }

        } else {
            if (!child_pedge.literals.contains(literal)) {
                child_pedge.literals.add(literal);
                if (!literal.is_current) {
                    child_pedge.has_noncurrent = true;
                }
                // if the literal is an edge with no specified value, add the literal to all potential pedges
                if (!literal.is_leaf && literal.q1 == EPMEM_NODEID_BAD) {
                    boolean created = false;
                    SortedMap<EpmemTriple,EpmemUEdge> uedge_cache = uedge_caches[is_edge];
                    Map<EpmemTriple,EpmemUEdge> uedge_iter = uedge_cache.tailMap(triple);
                    for ( Map.Entry<EpmemTriple, EpmemUEdge> entry : uedge_iter.entrySet() ) {
                        EpmemTriple child_triple = entry.getKey();
                        // make sure we're still looking at the right edge(s)
                        if (child_triple.q0 != triple.q0 || child_triple.w != triple.w) {
                            break;
                        }
                        EpmemUEdge child_uedge = entry.getValue();
                        if (child_triple.q1 != EPMEM_NODEID_BAD && child_uedge.value_is_id != 0) {
                            for (EpmemLiteral child_iter : literal.children ) {
                                created |= epmem_register_pedges(child_triple.q1, child_iter, pedge_pq, after, pedge_caches, uedge_caches);
                            }
                        }
                    }
                    return created;
                }
            }
        }
        return true;
    }

    /**
     * episodic_memory.cpp:3318
     * 
     * void epmem_print_retrieval_state(epmem_wme_literal_map& literals, 
     *                                  epmem_triple_pedge_map pedge_caches[], 
     *                                  epmem_triple_uedge_map uedge_caches[]) {
     *   
     */
    private String epmem_print_retrieval_state(Map<WmeImpl,EpmemLiteral> literals, 
                                               Map<EpmemTriple,EpmemPEdge> pedge_caches[], 
                                               Map<EpmemTriple,EpmemUEdge> uedge_caches[]) {
        //std::map<epmem_node_id, std::string> tsh;
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("digraph {\n");
        sb.append("node [style=\"filled\"];\n");
        // LITERALS
        sb.append("subgraph cluster_literals {\n");
        sb.append("node [fillcolor=\"#0084D1\"];\n");
        for (Map.Entry<WmeImpl, EpmemLiteral> entry : literals.entrySet()) {
            EpmemLiteral literal = entry.getValue();
            if (literal.id_sym != null) {
                sb.append("\"" + literal.value_sym + "\" [");
                if (literal.q1 == EPMEM_NODEID_BAD) {
                    sb.append("label=\"" + literal.value_sym + "\"");
                } else {
                    sb.append("label=\"" + literal.q1 + "\"");
                }
                if (literal.value_is_id == 0) {
                    sb.append(", shape=\"rect\"");
                }
                if (literal.matches.isEmpty()) {
                    sb.append(", penwidth=\"2.0\"");
                }
                if (literal.is_neg_q != 0) {
                    sb.append(", fillcolor=\"#C5000B\"");
                }
                sb.append("];\n");
                sb.append("\"" + literal.id_sym + "\" -> \"" + literal.value_sym + "\" [label=\"");
                if (literal.w == EPMEM_NODEID_BAD) {
                    sb.append("?");
                } else {
                    sb.append(literal.w);
                }
                sb.append("\\n" + literal + "\"];\n");
            }
        }
        sb.append("};\n");
        // NODES / NODE->NODE
        sb.append("subgraph cluster_uedges{\n");
        sb.append("node [fillcolor=\"#FFD320\"];\n");
        for (int type = EPMEM_RIT_STATE_NODE; type <= EPMEM_RIT_STATE_EDGE; type++) {
            Map<EpmemTriple,EpmemUEdge> uedge_cache = uedge_caches[type];
            for (Map.Entry<EpmemTriple,EpmemUEdge> entry : uedge_cache.entrySet()) {
                EpmemTriple triple = entry.getKey();
                if (triple.q1 != EPMEM_NODEID_ROOT) {
                    if (type == EPMEM_RIT_STATE_NODE) {
                        sb.append("\"n" + triple.q1 + "\" [shape=\"rect\"];\n");
                    }
                    sb.append("\"e" + triple.q0 + "\" -> \"" + (type == EPMEM_RIT_STATE_NODE ? "n" : "e") + triple.q1 + "\" [label=\"" + triple.w + "\"];\n");
                }
            }
        }
        sb.append("};\n");
        // PEDGES / LITERAL->PEDGE
        sb.append("subgraph cluster_pedges {\n");
        sb.append("node [fillcolor=\"#008000\"];\n");
        HashMultimap<Long /*epmem_node_id*/, EpmemPEdge> parent_pedge_map = HashMultimap.create();
        for (int type = EPMEM_RIT_STATE_NODE; type <= EPMEM_RIT_STATE_EDGE; type++) {
            Map<EpmemTriple,EpmemPEdge> pedge_cache = pedge_caches[type];
            for (Map.Entry<EpmemTriple,EpmemPEdge> entry : pedge_cache.entrySet() ) {
                EpmemTriple triple = entry.getKey();
                EpmemPEdge pedge = entry.getValue();
                if (triple.w != EPMEM_NODEID_BAD) {
                    sb.append("\"" + pedge + "\" [label=\"" + pedge + "\\n(" + triple.q0 + ", " + triple.w + ", ");
                    if (triple.q1 == EPMEM_NODEID_BAD) {
                        sb.append("?");
                    } else {
                        sb.append(triple.q1);
                    }
                    sb.append(")\"");
                    if (pedge.value_is_id == 0) {
                        sb.append(", shape=\"rect\"");
                    }
                    sb.append("];\n");
                    for (EpmemLiteral literal : pedge.literals ) {
                        sb.append("\"" + literal.value_sym + "\" -> \"" + pedge + "\";\n");
                    }
                    parent_pedge_map.put(triple.q0, pedge);
                }
            }
        }
        sb.append("};\n");
        // PEDGE->PEDGE / PEDGE->NODE
        Set<EpmemPEdgeNodePair> drawn = new HashSet<EpmemPEdgeNodePair>();
        for (int type = EPMEM_RIT_STATE_NODE; type <= EPMEM_RIT_STATE_EDGE; type++) {
            Map<EpmemTriple,EpmemUEdge> uedge_cache = uedge_caches[type];
            for (Map.Entry<EpmemTriple, EpmemUEdge> entry : uedge_cache.entrySet()) {
                EpmemTriple triple = entry.getKey();
                EpmemUEdge uedge = entry.getValue();
                if (triple.w != EPMEM_NODEID_BAD) {
                    for (EpmemPEdge pedge : uedge.pedges ) {
                        EpmemPEdgeNodePair pair = new EpmemPEdgeNodePair(pedge, triple.q0);
                        if (!drawn.contains(pair)) {
                            drawn.add(pair);
                            sb.append("\"" + pedge + "\" -> \"e" + triple.q0 + "\";\n");
                        }
                        sb.append("\"" + pedge + "\" -> \"" + (pedge.value_is_id != 0 ? "e" : "n") + triple.q1 + "\" [style=\"dashed\"];\n");
                        /*
                        std::pair<std::multimap<epmem_node_id, epmem_pedge*>::iterator, std::multimap<epmem_node_id, epmem_pedge*>::iterator> pedge_iters = parent_pedge_map.equal_range(triple.q1);
                        for (std::multimap<epmem_node_id, epmem_pedge*>::iterator pedge_iter = pedge_iters.first; pedge_iter != pedge_iters.second; pedge_iter++) {
                        */
                        for ( EpmemPEdge pedge_iter : parent_pedge_map.get(triple.q1) ) {
                            sb.append("\"" + pedge + "\" -> \"" + pedge_iter + "\";\n");
                        }
                    }
                }
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    // TODO: There should be a decent generic pair type around somewhere
    private static class EpmemPEdgeNodePair {
        EpmemPEdge pedge;
        long node_id;
        
        EpmemPEdgeNodePair(EpmemPEdge pedge, long node_id) {
            this.pedge = pedge;
            this.node_id = node_id;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (node_id ^ (node_id >>> 32));
            result = prime * result + ((pedge == null) ? 0 : pedge.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EpmemPEdgeNodePair other = (EpmemPEdgeNodePair) obj;
            if (node_id != other.node_id)
                return false;
            if (pedge == null)
            {
                if (other.pedge != null)
                    return false;
            }
            else if (!pedge.equals(other.pedge))
                return false;
            return true;
        }
    }
    
    /**
     * episodic_memory.cpp:3431
     * epmem_literal* epmem_build_dnf(
     *      wme* cue_wme, 
     *      epmem_wme_literal_map& literal_cache, 
     *      epmem_literal_set& leaf_literals, 
     *      epmem_symbol_int_map& symbol_num_incoming, 
     *      epmem_literal_deque& gm_ordering, 
     *      epmem_symbol_set& currents, 
     *      int query_type, std::set<Symbol*>& visiting, 
     *      soar_module::wme_set& cue_wmes, 
     *      agent* my_agent
     *  )
     */
    private EpmemLiteral epmem_build_dnf(
            WmeImpl cue_wme,
            Map<WmeImpl, EpmemLiteral> literal_cache,
            Set<EpmemLiteral> leaf_literals,
            Map<SymbolImpl, Integer> symbol_num_incoming,
            Deque<EpmemLiteral> gm_ordering, 
            Set<SymbolImpl> currents,
            long query_type, 
            Set<SymbolImpl> visiting, 
            Set<WmeImpl> cue_wmes
    ) throws SQLException
    {
        // if the value is being visited, this is part of a loop; return NULL
        // remove this check (and in fact, the entire visiting parameter) if cyclic cues are allowed
        if (visiting.contains(cue_wme.value)) {
            return null;
        }
        // if the value is an identifier and we've been here before, we can return the previous literal
        if (literal_cache.containsKey(cue_wme)) {
            return literal_cache.get(cue_wme);
        }

        cue_wmes.add(cue_wme);
        SymbolImpl value = cue_wme.value;
        // epmem_literal* literal;
        // allocate_with_pool(my_agent, &(my_agent->epmem_literal_pool), &literal);
        EpmemLiteral literal = new EpmemLiteral();
        //new(&(literal->parents)) epmem_literal_set();
        //new(&(literal->children)) epmem_literal_set();
        literal.parents = new HashSet<EpmemLiteral>();
        literal.children = new HashSet<EpmemLiteral>();        

        IdentifierImpl identifier = value.asIdentifier();
        if ( identifier == null ) { // WME is a value
            literal.value_is_id = EPMEM_RIT_STATE_NODE;
            literal.is_leaf = true;
            literal.q1 = epmem_temporal_hash(value);
            leaf_literals.add(literal);
        } else if ( identifier.smem_lti != 0 ) { // WME is an LTI
            // if we can find the LTI node id, cache it; otherwise, return failure
            //my_agent->epmem_stmts_graph->find_lti->bind_int(1, identifier.getNameLetter());
            //my_agent->epmem_stmts_graph->find_lti->bind_int(2, identifier.getNameNumber());
            db.find_lti.setLong(1, identifier.getNameLetter());
            db.find_lti.setLong(2, identifier.getNameNumber());
            ResultSet results = db.find_lti.executeQuery();
            
            try {
                if ( results.next() ) {
                    literal.value_is_id = EPMEM_RIT_STATE_EDGE;
                    literal.is_leaf = true;
                    literal.q1 = results.getLong(0 + 1);
                    // my_agent->epmem_stmts_graph->find_lti->reinitialize();
                    leaf_literals.add(literal);
                } else {
                    // my_agent->epmem_stmts_graph->find_lti->reinitialize();
                    //literal->parents.~epmem_literal_set();
                    //literal->children.~epmem_literal_set();
                    literal.parents = null;
                    literal.children = null;
                    //free_with_pool(&(my_agent->epmem_literal_pool), literal);
                    literal = null;
                    return null;
                }
            } finally {
                results.close();
            }
            
        } else { // WME is a normal identifier
            // we determine whether it is a leaf by checking for children
            List<WmeImpl> children = epmem_get_augs_of_id(value, DefaultMarker.create());
            literal.value_is_id = EPMEM_RIT_STATE_EDGE;
            literal.q1 = EPMEM_NODEID_BAD;

            // if the WME has no children, then it's a leaf
            // otherwise, we recurse for all children
            if (children.isEmpty()) {
                literal.is_leaf = true;
                leaf_literals.add(literal);
                //delete children;
            } else {
                boolean cycle = false;
                visiting.add(cue_wme.value);
                for (WmeImpl wme_iter : children ) {
                    // check to see if this child forms a cycle
                    // if it does, we skip over it
                    EpmemLiteral child = epmem_build_dnf(wme_iter, literal_cache, leaf_literals, symbol_num_incoming, gm_ordering, currents, query_type, visiting, cue_wmes);
                    if (child != null) {
                        child.parents.add(literal);
                        literal.children.add(child);
                    } else {
                        cycle = true;
                    }
                }
                //delete children;
                visiting.remove(cue_wme.value);
                // if all children of this WME lead to cycles, then we don't need to walk this path
                // in essence, this forces the DNF graph to be acyclic
                // this results in savings in not walking edges and intervals
                if (cycle && literal.children.isEmpty()) {
                    //literal->parents.~epmem_literal_set();
                    //literal->children.~epmem_literal_set();
                    literal.parents = null;
                    literal.children = null;
                    //free_with_pool(&(my_agent->epmem_literal_pool), literal);
                    literal = null;
                    return null;
                }
                literal.is_leaf = false;
                Integer incomingCount = symbol_num_incoming.get(value);
                if (incomingCount == null) {
                    incomingCount = 1;
                } else {
                    incomingCount++;
                }
                symbol_num_incoming.put(value,  incomingCount);
            }
        }

        if ( query_type == 0 ) {
            gm_ordering.offerFirst(literal);
        }

        literal.id_sym = cue_wme.id;
        literal.value_sym = cue_wme.value;
        literal.is_current = currents.contains(value);
        literal.w = epmem_temporal_hash(cue_wme.attr);
        literal.is_neg_q = query_type;
        literal.weight = (literal.is_neg_q != 0 ? -1 : 1) * (params.balance.get() >= 1.0 - 1.0e-8 ? 1.0 : wma_get_wme_activation(cue_wme, true));
    //#ifdef USE_MEM_POOL_ALLOCATORS
    //    new(&(literal->matches)) epmem_node_pair_set(std::less<epmem_node_pair>(), soar_module::soar_memory_pool_allocator<epmem_node_pair>(my_agent));
    //#else
    //    new(&(literal->matches)) epmem_node_pair_set();
    //#endif
    //    new(&(literal->values)) epmem_node_int_map();
        literal.matches = new TreeSet<EpmemNodePair>();
        literal.values = new HashMap<Long,Integer>();

        literal_cache.put(cue_wme,literal);
        return literal;
    }
    
    // TODO: Unclear if this should even be here...
    /*
     * wma.cpp: 1212
     * double wma_get_wme_activation( 
     *              agent* my_agent, 
     *              wme* w, 
     *              bool log_result )
     *
     */
    private double wma_get_wme_activation(WmeImpl w, boolean log_result) 
    {
        return 0.0;
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
            List<SymbolTriple> my_list, 
            SymbolImpl id, 
            SymbolImpl attr, 
            SymbolImpl value)
    {
        my_list.add( new SymbolTriple( id, attr, value) );
        
        //In java, we don't care about reference counting
        //symbol_add_ref( id );
        //symbol_add_ref( attr );
        //symbol_add_ref( value );
        
    }

    /**
     * episodic_memory.cpp:3199:epmem_time_id epmem_previous_episode( 
     *      agent *my_agent, epmem_time_id memory_id )
     *      
     * Returns the last valid temporal id.  This is really
     * only an issue if you implement episode dynamics like
     * forgetting.
     * 
     * @param last_memory
     * @return
     * @throws SQLException 
     */
    private long epmem_previous_episode(long memory_id)
    {
        // //////////////////////////////////////////////////////////////////////////
        // my_agent->epmem_timers->prev->start();
        // //////////////////////////////////////////////////////////////////////////

        long return_val = EPMEM_MEMID_NONE;

        if (memory_id != EPMEM_MEMID_NONE)
        {
            // soar_module::sqlite_statement *my_q = my_agent->epmem_stmts_graph->prev_episode;
            final PreparedStatement myQuery = db.prev_episode;
            try
            {
                myQuery.setLong(1, memory_id);
                final ResultSet resultSet = myQuery.executeQuery();
                try
                {
                    if (resultSet.next())
                    {
                        return_val = resultSet.getLong(0 + 1);
                    }
                }
                finally
                {
                    resultSet.close();
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            /*
             * my_q->reinitialize();
             */
        }

        // //////////////////////////////////////////////////////////////////////////
        // my_agent->epmem_timers->prev->stop();
        // //////////////////////////////////////////////////////////////////////////

        return return_val;
    }

    /**
     * Returns the next valid temporal id.  This is really
     * only an issue if you implement episode dynamics like
     * forgetting.
     *                  
     * @param last_memory
     * @return
     */
    private long epmem_next_episode(long memory_id)
    {
        ////////////////////////////////////////////////////////////////////////////
        //my_agent->epmem_timers->next->start();
        ////////////////////////////////////////////////////////////////////////////
        
        long return_val = EPMEM_MEMID_NONE;

        if (memory_id != EPMEM_MEMID_NONE)
        {
            final PreparedStatement myQuery = db.next_episode;
            try
            {
                myQuery.setLong(1, memory_id);
                final ResultSet resultSet = myQuery.executeQuery();
                try
                {
                    if (resultSet.next())
                    {
                        return_val = resultSet.getLong(0 + 1);
                    }
                }
                finally
                {
                    resultSet.close();
                }

            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
        
        ////////////////////////////////////////////////////////////////////////////
        //my_agent->epmem_timers->next->stop();
        ////////////////////////////////////////////////////////////////////////////
        
        return return_val;
    }
    
    /**
     * This is being used to emulate the default parameter in C
     * 
     * @param state
     * @param memory_id
     * @param meta_wmes
     * @param retrieval_wmes
     * @throws SQLException 
     * @throws SoarException 
     */
    private void epmem_install_memory(
            IdentifierImpl state, 
            long /*epmem_time_id*/ memory_id, 
            List<SymbolTriple> meta_wmes, 
            List<SymbolTriple> retrieval_wmes
            /*Map<Long / *epmem_node_id* /, SymbolImpl> id_record = NULL*/
        ) throws SQLException, SoarException
    {
        epmem_install_memory(state, memory_id, meta_wmes, retrieval_wmes, null);
    }
    
    /**
     * This is used to emulate std::pair<Symbol, bool>
     * 
     * @author ACNickles
     *
     */
    private class SymbolBooleanPair
    {
        SymbolImpl first;
        boolean second;
        
        SymbolBooleanPair(SymbolImpl first, boolean second)
        {
            this.first = first;
            this.second = second;
        }
    }
    
    private class EpmemEdge
    {
        
        long /*epmem_node_id*/ q0;      // id
        SymbolImpl w;                   // attr
        long /*epmem_node_id*/ q1;      // value

        boolean val_is_short_term;
        char val_letter;
        long val_num;

    };
    
    /**
     * episodic_memory.cpp: 2847:
     * void epmem_install_memory( 
     *      agent *my_agent, 
     *      Symbol *state, 
     *      epmem_time_id memory_id, 
     *      soar_module::symbol_triple_list& meta_wmes, 
     *      soar_module::symbol_triple_list& retrieval_wmes, 
     *      epmem_id_mapping *id_record = NULL )
     * 
     * @param state
     * @param retrieve
     * @param meta_wmes
     * @param retrieval_wmes
     * @throws SQLException 
     * @throws SoarException 
     */
    private void epmem_install_memory(
            IdentifierImpl state, 
            long /*epmem_time_id*/ memory_id, 
            List<SymbolTriple> meta_wmes, 
            List<SymbolTriple> retrieval_wmes,
            Map<Long /*epmem_node_id*/, SymbolImpl>  id_record /*=NULL*/
        ) throws SQLException, SoarException
    {
        final EpisodicMemoryStateInfo epmemInfo = epmem_info(state);
        ////////////////////////////////////////////////////////////////////////////
        //my_agent->epmem_timers->ncb_retrieval->start();
        ////////////////////////////////////////////////////////////////////////////
        
        // get the ^result header for this state
        SymbolImpl result_header = epmemInfo.epmem_result_header;
        
        // initialize stat
        long num_wmes = 0;
        //my_agent->epmem_stats->ncb_wmes->set_value( num_wmes );
        this.stats.ncb_wmes.set(num_wmes);
        
        // if no memory, say so
        if ( 
                ( memory_id == EPMEM_MEMID_NONE ) ||
                !epmem_valid_episode( memory_id )
            )
        {
            epmem_buffer_add_wme( 
                    meta_wmes, 
                    result_header, 
                    predefinedSyms.epmem_sym_retrieved, 
                    predefinedSyms.epmem_sym_no_memory 
                );
            epmemInfo.last_memory = EPMEM_MEMID_NONE;
            
            ////////////////////////////////////////////////////////////////////////////
            //my_agent->epmem_timers->ncb_retrieval->stop();
            ////////////////////////////////////////////////////////////////////////////
            
            return;
        }
        // remember this as the last memory installed
        epmemInfo.last_memory = memory_id;
        
        // create a new ^retrieved header for this result
        SymbolImpl retrieved_header;
        retrieved_header = symbols.make_new_identifier( 'R', result_header.asIdentifier().level );
        //if ( id_record )
        if ( id_record != null )
        {
            id_record.put( EPMEM_NODEID_ROOT, retrieved_header);
        }
        
        epmem_buffer_add_wme( meta_wmes, result_header, predefinedSyms.epmem_sym_retrieved, retrieved_header );
        //Java doesn't care about reference counting
        //symbol_remove_ref( retrieved_header );
        
        // add *-id wme's
        {
            SymbolImpl my_meta;
            
            my_meta = symbols.createInteger( memory_id );
            epmem_buffer_add_wme( meta_wmes, result_header, predefinedSyms.epmem_sym_memory_id, my_meta );
            //symbol_remove_ref( my_agent, my_meta );
            
            my_meta = symbols.createInteger( stats.time.get() );
            epmem_buffer_add_wme( meta_wmes, result_header, predefinedSyms.epmem_sym_present_id, my_meta );
            //symbol_remove_ref( my_agent, my_meta );
        }
        
        // install memory
        {
            // Big picture: create identifier skeleton, then hang non-identifers
            //
            // Because of shared WMEs at different levels of the storage breadth-first search,
            // there is the possibility that the unique database id of an identifier can be
            // greater than that of its parent.  Because the retrieval query sorts by
            // unique id ascending, it is thus possible to have an "orphan" - a child with
            // no current parent.  We keep track of orphans and add them later, hoping their
            // parents have shown up.  I *suppose* there could be a really evil case in which
            // the ordering of the unique ids is exactly opposite of their insertion order.
            // I just hope this isn't a common case...
            
            // shared identifier lookup table
            //std::map< epmem_node_id, std::pair< Symbol*, bool > > ids;
            Map<Long /*epmem_node_id*/, SymbolBooleanPair> ids = new HashMap<Long, SymbolBooleanPair>();
            boolean dont_abide_by_ids_second = (params.merge.get() == MergeChoices.merge_add);
            
            // symbols used to create WMEs
            SymbolImpl attr = null;
            
            // lookup query
            //soar_module::sqlite_statement *my_q;
            
            // initialize the lookup table
            ids.put(EPMEM_NODEID_ROOT, new SymbolBooleanPair(retrieved_header, true));
            // first identifiers (i.e. reconstruct)
            PreparedStatement my_q = db.get_edges;
            {
                // relates to finite automata: q1 = d(q0, w)
                long /*epmem_node_id*/ q0; // id
                long /*epmem_node_id*/ q1; // attribute
                long /*int64_t*/ w_type; // we support any constant attribute symbol
                
                boolean val_is_short_term = false;
                char val_letter = 0;//NIL
                long /*int64_t*/ val_num = 0;//NIL
                
                // used to lookup shared identifiers
                // the bool in the pair refers to if children are allowed on this id (re: lti)
                //We dont need to use an iterator for lookups in Java, however the port will
                //match much closer if we use the variable. -ACN
                //std::map< epmem_node_id, std::pair< Symbol*, bool> >::iterator id_p;
                SymbolBooleanPair id_p;
                
                // orphaned children
                Queue<EpmemEdge> orphans = new LinkedList<EpmemEdge>();
                EpmemEdge orphan;
                
                epmem_rit_prep_left_right( memory_id, memory_id, epmem_rit_state_graph[ EPMEM_RIT_STATE_EDGE ]  );
                
                my_q.setLong(1, memory_id);
                my_q.setLong(2, memory_id);
                my_q.setLong(3, memory_id);
                my_q.setLong(4, memory_id);
                my_q.setLong(5, memory_id);
                ResultSet resultSet = my_q.executeQuery();
                while ( resultSet.next() )
                {
                    // q0, w, q1, w_type
                    //q0 = my_q->column_int( 0 );
                    q0 = resultSet.getLong(0 + 1);
                    //q1 = my_q->column_int( 2 );
                    q1 = resultSet.getLong(2 + 1);
                    //w_type = my_q->column_int( 3 );
                    w_type = resultSet.getLong( 3 + 1 );
                    
                    //All of the cases here are ints, so if this cast changes anything,
                    //we have bigger problems
                    switch ( (int)w_type )
                    {
                        case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                            //attr = make_int_constant( my_agent,my_q->column_int( 1 ) );
                            attr = symbols.createInteger(resultSet.getLong( 1 + 1 ) );
                            break;
                        
                        case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                            //attr = make_float_constant( my_agent, my_q->column_double( 1 ) );
                            attr = symbols.createDouble( resultSet.getDouble( 1 + 1 ) );
                            break;
                        
                        case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                            //attr = make_sym_constant( my_agent, const_cast<char *>( reinterpret_cast<const char *>( my_q->column_text( 1 ) ) ) );
                            attr = symbols.createString(resultSet.getString(1 + 1));
                            break;
                    }
                    
                    // short vs. long-term
                    //This is how Smem is doing this cast, but I'm not certain how 
                    char tempValLetter = (char)resultSet.getLong(4 + 1);
                    val_is_short_term = ( resultSet.wasNull() );
                    if ( !val_is_short_term )
                    {
                        val_letter = tempValLetter;
                        //val_num = static_cast<uint64_t>( my_q->column_int( 5 ) );
                        resultSet.getLong(5 + 1);
                    }
                    
                    // get a reference to the parent
                    id_p = ids.get( q0 );
                    if ( id_p != null )
                    {
                        // if existing lti with kids don't touch
                        if ( dont_abide_by_ids_second || id_p.second )
                        {
                            _epmem_install_id_wme( 
                                    id_p.first, 
                                    attr, 
                                    ids, 
                                    q1, 
                                    val_is_short_term, 
                                    val_letter, 
                                    val_num, 
                                    id_record, 
                                    retrieval_wmes 
                                );
                            num_wmes++;
                        }
                        //Ref counting doesn't matter in Java
                        //symbol_remove_ref( my_agent, attr );
                    }
                    else
                    {
                        // out of order
                        orphan = new EpmemEdge();
                        orphan.q0 = q0;
                        orphan.w = attr;
                        orphan.q1 = q1;
                        
                        orphan.val_letter = 0;//NIL;
                        orphan.val_num = 0;//NIL;
                        
                        orphan.val_is_short_term = val_is_short_term;
                        if ( !val_is_short_term )
                        {
                            orphan.val_letter = val_letter;
                            orphan.val_num = val_num;
                        }
                        
                        orphans.add( orphan );
                    }
                }
                //my_q->reinitialize();
                resultSet.close();
                
                epmem_rit_clear_left_right( );
                
                // take care of any orphans
                if ( !orphans.isEmpty() )
                {
                    int /*std::queue<epmem_edge *>::size_type*/ orphans_left;
                    Queue<EpmemEdge> still_orphans = new LinkedList<EpmemEdge>();
                    
                    do
                    {
                        orphans_left = orphans.size();
                        
                        while ( !orphans.isEmpty() )
                        {
                            orphan = orphans.poll();
                            
                            // get a reference to the parent
                            id_p = ids.get( orphan.q0 );
                            if ( id_p != null )
                            {
                                if ( dont_abide_by_ids_second || id_p.second )
                                {
                                    _epmem_install_id_wme( 
                                            id_p.first, 
                                            orphan.w, 
                                            ids, 
                                            orphan.q1, 
                                            orphan.val_is_short_term, 
                                            orphan.val_letter, 
                                            orphan.val_num, 
                                            id_record, 
                                            retrieval_wmes 
                                        );
                                    num_wmes++;
                                }
                                
                                //Java does this for us
                                //symbol_remove_ref( my_agent, orphan->w );
                                //delete orphan;
                            }
                            else
                            {
                                still_orphans.add( orphan );
                            }
                        }
                        
                        orphans.addAll(still_orphans);
                        still_orphans.clear();
                        /*
                        orphans = still_orphans;
                        while ( !still_orphans.isEmpty() )
                        {
                            still_orphans.pop();
                        }
                        */
                    } while ( ( !orphans.isEmpty() ) && ( orphans_left != orphans.size() ) );
                    
                    /*
                    while ( !orphans.empty() )
                    {
                        orphan = orphans.front();
                        orphans.pop();
                        
                        symbol_remove_ref( my_agent, orphan->w );
                        delete orphan;
                    }
                    */
                }
            }
            
            // then node_unique
            my_q = db.get_nodes;
            {
                long /*epmem_node_id*/ child_id;
                long /*epmem_node_id*/ parent_id;
                long attr_type;
                long value_type;
                
                SymbolBooleanPair /*std::pair< Symbol*, bool >*/ parent;
                SymbolImpl value = null;
                
                epmem_rit_prep_left_right( 
                        memory_id, 
                        memory_id, 
                        epmem_rit_state_graph[ EPMEM_RIT_STATE_NODE ] 
                    );
                
                my_q.setLong( 1, memory_id );
                my_q.setLong( 2, memory_id );
                my_q.setLong( 3, memory_id );
                my_q.setLong( 4, memory_id );
                
                ResultSet resultSet = my_q.executeQuery();
                while ( resultSet.next() )
                {
                    // f.child_id, f.parent_id, f.name, f.value, f.attr_type, f.value_type
                    //child_id = my_q->column_int( 0 );
                    child_id = resultSet.getLong(0 + 1);
                    //parent_id = my_q->column_int( 1 + 1 );
                    parent_id = resultSet.getLong( 1 + 1 );
                    //attr_type = my_q->column_int( 4 + 1 );
                    attr_type = resultSet.getLong( 4 + 1 );
                    //value_type = my_q->column_int( 5 + 1 );
                    value_type = resultSet.getLong( 5 + 1 );
                    
                    // get a reference to the parent
                    parent = ids.get( parent_id );
                    
                    if ( dont_abide_by_ids_second || parent.second )
                    {
                        // make a symbol to represent the attribute
                        switch ( (int)attr_type )
                        {
                            case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                            //attr = make_int_constant( my_agent, my_q->column_int( 2 ) );
                            attr = symbols.createInteger( resultSet.getInt( 2 + 1 ));
                            break;
                            
                            case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                            //attr = make_float_constant( my_agent, my_q->column_double( 2 ) );
                            attr = symbols.createDouble(resultSet.getDouble( 2 + 1 ));
                            break;
                            
                            case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                            //attr = make_sym_constant( my_agent, const_cast<char *>( reinterpret_cast<const char *>( my_q->column_text( 2 ) ) ) );
                            attr = symbols.createString(  resultSet.getString( 2 + 1 ) ) ;
                            break;
                        }
                        
                        // make a symbol to represent the value
                        switch ( (int)value_type )
                        {
                            case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                            //value = make_int_constant( my_agent,my_q->column_int( 3 ) );
                            value = symbols.createInteger( resultSet.getLong( 3 + 1 ) );
                            break;
                            
                            case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                            //value = make_float_constant( my_agent, my_q->column_double( 3 ) );
                            value = symbols.createDouble( resultSet.getDouble( 3 + 1 ) );
                            break;
                            
                            case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                            //value = make_sym_constant( my_agent, const_cast<char *>( (const char *) my_q->column_text( 3 ) ) );
                            value = symbols.createString( resultSet.getString( 3 + 1 ) );
                            break;
                        }
                        
                        epmem_buffer_add_wme( retrieval_wmes, parent.first, attr, value );
                        num_wmes++;
                        
                        //symbol_remove_ref( my_agent, attr );
                        //symbol_remove_ref( my_agent, value );
                    }
                }
                //my_q->reinitialize();
                resultSet.close();
                epmem_rit_clear_left_right();
            }
        }
        
        // adjust stat
        this.stats.ncb_wmes.set(num_wmes);
        
        ////////////////////////////////////////////////////////////////////////////
        //my_agent->epmem_timers->ncb_retrieval->stop();
        ////////////////////////////////////////////////////////////////////////////
         
    }
    
    /**
     * episodic_memory.cpp: 1121:
     * void epmem_rit_clear_left_right( agent *my_agent )
     * 
     * Clears the left/right relations populated during prep
     * @throws SQLException 
     */
    private void epmem_rit_clear_left_right() throws SQLException
    {
        db.rit_truncate_left.execute();
        db.rit_truncate_right.execute();
    }
    
    /**
     * episodic_memory.cpp: 2779:
     * inline void _epmem_install_id_wme( 
     *      agent* my_agent, 
     *      Symbol* parent, 
     *      Symbol* attr, 
     *      std::map< epmem_node_id, std::pair< Symbol*, bool > >* ids, 
     *      epmem_node_id q1, 
     *      bool val_is_short_term, 
     *      char val_letter, 
     *      uint64_t val_num, 
     *      epmem_id_mapping* id_record, 
     *      soar_module::symbol_triple_list& retrieval_wmes 
     *    )
     * @throws SoarException 
     * 
     */
    private void _epmem_install_id_wme(
            SymbolImpl parent, 
            SymbolImpl attr, 
            Map<Long /*epmem_node_id*/, SymbolBooleanPair> ids, 
            long /*epmem_node_id*/ q1, 
            boolean val_is_short_term, 
            char val_letter, 
            long val_num, 
            Map<Long, SymbolImpl>/*epmem_id_mapping*/ id_record, 
            List<SymbolTriple> /*soar_module::symbol_triple_list&*/ retrieval_wmes
        ) throws SoarException
    {
        //std::map< epmem_node_id, std::pair< Symbol*, bool > >::iterator id_p = ids->find( q1 );
        SymbolBooleanPair id_p = ids.get( q1 );
        boolean existing_identifier = ( id_p != null );

        if ( val_is_short_term )
        {
            if ( !existing_identifier )
            {
                id_p = ids.put(
                    q1, 
                    new SymbolBooleanPair(
                            symbols.make_new_identifier(
                                    ( (Symbols.getSymbolType(attr) == Symbols.SYM_CONSTANT_SYMBOL_TYPE )?( attr.getFirstLetter() ):('E') ), 
                                    parent.asIdentifier().level
                                ), 
                            true
                        )
                    );
                
                //if ( id_record )
                if ( id_record != null )
                {
                    //epmem_id_mapping::iterator rec_p = id_record->find( q1 );
                    //if ( rec_p != id_record->end() )
                    if ( id_record.containsKey(q1) )
                    {
                        //rec_p->second = id_p->second.first;
                        id_record.put(q1, id_p.first);
                    }
                }
            }

            epmem_buffer_add_wme( retrieval_wmes, parent, attr, id_p.first );

            //if ( !existing_identifier )
            //{
            //    symbol_remove_ref( my_agent, id_p->second.first );
            //}
        }
        else
        {
            if ( existing_identifier )
            {
                epmem_buffer_add_wme( retrieval_wmes, parent, attr, id_p.first );
            }
            else
            {
                //SymbolImpl value = smem_lti_soar_make( my_agent, smem_lti_get_id( my_agent, val_letter, val_num ), val_letter, val_num, parent->id.level );
                SymbolImpl value = smem.smem_lti_soar_make( 
                        smem.smem_lti_get_id( 
                                val_letter, 
                                val_num 
                            ), 
                        val_letter, 
                        val_num, 
                        parent.asIdentifier().level 
                    );

                //if ( id_record )
                if ( id_record != null )
                {
                    //epmem_id_mapping::iterator rec_p = id_record->find( q1 );
                    //if ( rec_p != id_record->end() )
                    if ( id_record.containsKey(q1) )
                    {
                        //rec_p->second = value;
                        id_record.put(q1, value);
                    }
                }

                epmem_buffer_add_wme( retrieval_wmes, parent, attr, value );
                //symbol_remove_ref( my_agent, value );

                ids.put(
                        q1, 
                        new SymbolBooleanPair(
                                value, 
                                !( 
                                    ( value.asIdentifier().goalInfo.getImpasseWmes() != null) || 
                                    ( value.asIdentifier().getInputWmes() != null) || 
                                    ( value.asIdentifier().slots != null) 
                                )
                            )
                    );
            }
        }
    }
    
    /**
     * episodic_memory.cpp: 1156:
     * void epmem_rit_prep_left_right( 
     *      agent *my_agent, 
     *      int64_t lower, 
     *      int64_t upper, 
     *      epmem_rit_state *rit_state 
     *    )
     * 
     * Implements the computational components of the RIT
     * @throws SQLException 
     */
    private void epmem_rit_prep_left_right(long lower, long upper, epmem_rit_state rit_state) throws SQLException
    {
        ////////////////////////////////////////////////////////////////////////////
        //rit_state->timer->start();
        ////////////////////////////////////////////////////////////////////////////
        
        long offset = rit_state.offset.stat;
        long node, step;
        long left_node, left_step;
        long right_node, right_step;
        
        lower = ( lower - offset );
        upper = ( upper - offset );
        
        // auto add good range
        epmem_rit_add_left( lower, upper );
        
        // go to fork
        node = EPMEM_RIT_ROOT;
        step = 0;
        if ( ( lower > node ) || (upper < node ) )
        {
            if ( lower > node )
            {
                node = rit_state.rightroot.stat;
                epmem_rit_add_left( EPMEM_RIT_ROOT, EPMEM_RIT_ROOT );
            }
            else
            {
                node = rit_state.leftroot.stat;
                epmem_rit_add_right( EPMEM_RIT_ROOT );
            }
            
            for ( step = ( ( ( node >= 0 )?( node ):( -1 * node ) ) / 2 ); step >= 1; step /= 2 )
            {
                if ( lower > node )
                {
                    epmem_rit_add_left( node, node );
                    node += step;
                }
                else if ( upper < node )
                {
                    epmem_rit_add_right( node );
                    node -= step;
                }
                else
                {
                    break;
                }
            }
        }
        
        // go left
        left_node = node - step;
        for ( left_step = ( step / 2 ); left_step >= 1; left_step /= 2 )
        {
            if ( lower == left_node )
            {
                break;
            }
            else if ( lower > left_node )
            {
                epmem_rit_add_left( left_node, left_node );
                left_node += left_step;
            }
            else
            {
                left_node -= left_step;
            }
        }
        
        // go right
        right_node = node + step;
        for ( right_step = ( step / 2 ); right_step >= 1; right_step /= 2 )
        {
            if ( upper == right_node )
            {
                break;
            }
            else if ( upper < right_node )
            {
                epmem_rit_add_right( right_node );
                right_node -= right_step;
            }
            else
            {
                right_node += right_step;
            }
        }
        ////////////////////////////////////////////////////////////////////////////
        //rit_state->timer->stop();
        ////////////////////////////////////////////////////////////////////////////
    }
    
    /**
     * episodic_memory.cpp: 1144:
     * void epmem_rit_add_right( agent *my_agent, epmem_time_id id )
     * @throws SQLException 
     */
    private void epmem_rit_add_right(long id) throws SQLException{
        //my_agent->epmem_stmts_common->rit_add_right->bind_int( 1, id );
        db.rit_add_right.setLong( 1, id );
        //my_agent->epmem_stmts_common->rit_add_right->execute( soar_module::op_reinit );
        db.rit_add_right.execute();
    }
    
    /**
     * episodic_memory.cpp: 1132:
     * void epmem_rit_add_left( agent *my_agent, epmem_time_id min, epmem_time_id max )
     * 
     * Adds a range to the left relation
     * @throws SQLException 
     */
    private void epmem_rit_add_left(long min, long max) throws SQLException
    {
        //my_agent->epmem_stmts_common->rit_add_left->bind_int( 1, min );
        db.rit_add_left.setLong( 1, min );
        //my_agent->epmem_stmts_common->rit_add_left->bind_int( 2, max );
        db.rit_add_left.setLong( 2, max );
        //my_agent->epmem_stmts_common->rit_add_left->execute( soar_module::op_reinit );
        db.rit_add_left.execute();
    }
    
    /**
     * episodic_memory.cpp: 2760:
     * bool epmem_valid_episode( agent *my_agent, epmem_time_id memory_id )
     * 
     * @param memory_id
     * @return Returns true if the temporal id is valid
     * @throws SQLException 
     */
    private boolean epmem_valid_episode(long /*epmem_time_id*/ memory_id) throws SQLException
    {
        boolean return_val = false;

        {
            PreparedStatement my_q = db.valid_episode;
            
            my_q.setLong( 1, memory_id );
            ResultSet resultSet = my_q.executeQuery();
            //return_val = ( my_q->column_int( 0 ) > 0 );
            return_val = ( resultSet.getLong( 0 + 1 ) > 0 );
            resultSet.close();
        }

        return return_val;
    }

    /**
     * <p>
     * episodic_memory.cpp:5063:
     * void inline _epmem_respond_to_cmd_parse( 
     *      agent*
     *      my_agent, 
     *      epmem_wme_list* cmds, 
     *      bool& good_cue, 
     *      int& path, 
     *      epmem_time_id& retrieve, 
     *      Symbol*& next, 
     *      Symbol*& previous, 
     *      Symbol*& query, 
     *      Symbol*& neg_query, 
     *      epmem_time_list& prohibit, 
     *      epmem_time_id& before, 
     *      epmem_time_id& after, 
     *      epmem_symbol_set& currents, 
     *      soar_module::wme_set& cue_wmes 
     *  )
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
            final ByRef<Boolean> good_cue, 
            final ByRef<Integer> path, 
            final ByRef<Long> retrieve, 
            SymbolImpl next, 
            SymbolImpl previous,
            SymbolImpl query, 
            SymbolImpl neg_query, 
            List<Long> prohibit, 
            final ByRef<Long> before, 
            final ByRef<Long> after, 
            Set<SymbolImpl> currents, 
            Set<WmeImpl> cue_wmes)
    {
        cue_wmes.clear();

        retrieve.value = EPMEM_MEMID_NONE;
        next = null;
        previous = null;
        query = null;
        neg_query = null;
        prohibit.clear();
        before.value = EPMEM_MEMID_NONE;
        after.value = EPMEM_MEMID_NONE;
        good_cue.value = true;
        path.value = 0;
        

        //for ( epmem_wme_list::iterator w_p=cmds->begin(); w_p!=cmds->end(); w_p++ )
        for(WmeImpl w_p : cmds)
        {
            cue_wmes.add( (w_p) );

            if ( good_cue.value )
            {
                // collect information about known commands
                if ( w_p.attr == predefinedSyms.epmem_sym_retrieve )
                {
                    //if ( ( (*w_p)->value->ic.common_symbol_info.symbol_type == INT_CONSTANT_SYMBOL_TYPE ) &&
                    if ( ( w_p.getValue().asInteger() != null ) &&
                            ( path.value == 0 ) &&
                            ( w_p.value.asInteger().getValue() > 0 ) )
                    {
                        retrieve.value = w_p.value.asInteger().getValue();
                        path.value = 1;
                    }
                    else
                    {
                        good_cue.value = false;
                    }
                }
                else if ( w_p.attr == predefinedSyms.epmem_sym_next )
                {
                    if ( ( w_p.getValue().asIdentifier() != null ) &&
                            ( path.value == 0 ) )
                    {
                        next = w_p.value;
                        path.value = 2;
                    }
                    else
                    {
                        good_cue.value = false;
                    }
                }
                else if ( w_p.attr == predefinedSyms.epmem_sym_prev )
                {
                    if ( ( w_p.getValue().asIdentifier() != null ) &&
                            ( path.value == 0 ) )
                    {
                        previous = w_p.value;
                        path.value = 2;
                    }
                    else
                    {
                        good_cue.value = false;
                    }
                }
                else if ( w_p.attr == predefinedSyms.epmem_sym_query )
                {
                    if ( ( w_p.getValue().asIdentifier() != null ) &&
                            ( ( path.value == 0 ) || ( path.value == 3 ) ) &&
                            ( query == null ) )

                    {
                        query = w_p.value;
                        path.value = 3;
                    }
                    else
                    {
                        good_cue.value = false;
                    }
                }
                else if ( w_p.attr == predefinedSyms.epmem_sym_negquery )
                {
                    if ( ( w_p.getValue().asIdentifier() != null ) &&
                            ( ( path.value == 0 ) || ( path.value == 3 ) ) &&
                            ( neg_query == null ) )

                    {
                        neg_query = w_p.value;
                        path.value = 3;
                    }
                    else
                    {
                        good_cue.value = false;
                    }
                }
                else if ( w_p.attr == predefinedSyms.epmem_sym_before )
                {
                    if ( ( w_p.getValue().asInteger() != null ) &&
                            ( ( path.value == 0 ) || ( path.value == 3 ) ) )
                    {
                        if ( ( before.value == EPMEM_MEMID_NONE ) || ( w_p.value.asInteger().getValue() < before.value ) )
                        {
                            before.value = w_p.value.asInteger().getValue();
                        }
                        path.value = 3;
                    }
                    else
                    {
                        good_cue.value = false;
                    }
                }
                else if ( w_p.attr == predefinedSyms.epmem_sym_after )
                {
                    if ( ( w_p.getValue().asInteger() != null ) &&
                            ( ( path.value == 0 ) || ( path.value == 3 ) ) )
                    {
                        if ( after.value < w_p.value.asInteger().getValue() )
                        {
                            after.value = w_p.value.asInteger().getValue();
                        }
                        path.value = 3;
                    }
                    else
                    {
                        good_cue.value = false;
                    }
                }
                else if ( w_p.attr == predefinedSyms.epmem_sym_prohibit )
                {
                    if ( ( w_p.getValue().asInteger() != null ) &&
                            ( ( path.value == 0 ) || ( path.value == 3 ) ) )
                    {
                        prohibit.add( w_p.value.asInteger().getValue() );
                        path.value = 3;
                    }
                    else
                    {
                        good_cue.value = false;
                    }
                }
                else if ( w_p.attr == predefinedSyms.epmem_sym_current )
                {
                    if ( ( w_p.getValue().asIdentifier() != null ) &&
                            ( ( path.value == 0 ) || ( path.value == 3 ) ) )
                    {
                        currents.add( w_p.value );
                        path.value = 3;
                    }
                    else
                    {
                        good_cue.value = false;
                    }
                }
                else
                {
                    good_cue.value = false;
                }
            }
        }
        
        // if on path 3 must have query
        if ( ( path.value == 3 ) && ( query == null ) )
        {
            good_cue.value = false;
        }

        // must be on a path
        if ( path.value == 0 )
        {
            good_cue.value = false;
        }
        
    }
    
    /**
     * Removes any WMEs produced by EpMem resulting from a command
     * 
     * <p>episodic_memory.cpp:1449:void epmem_clear_result( agent *my_agent, Symbol *state )
     * @param state
     */
    private void epmem_clear_result(IdentifierImpl state)
    {
        Preference pref;
        
        final Deque<Preference> wmes = epmem_info(state).epmem_wmes;
        
        //while ( !state->id.epmem_info->epmem_wmes->empty() ) -ACN
        while ( !wmes.isEmpty() )
        {
            pref = wmes.removeLast();
            if ( pref.isInTempMemory())
            {
                recognitionMemory.remove_preference_from_tm( pref );
            }
        }
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

    @Override
    public void removeWme(WmeImpl w)
    {
        boolean was_encoded = false;
        
        if(w.value.asIdentifier()!=null)
        {
            boolean lti = (w.value.asIdentifier().smem_lti!=0);

            if((w.epmem_id!=EPMEM_NODEID_BAD) && (w.epmem_valid==epmem_validation)) 
            {
                was_encoded = true;
         
                epmem_edge_removals.put(w.epmem_id, true);
              
                // return to the id pool
                if ( !lti )
                {
                    Map<Long,Long> p = epmem_id_replacement.get(w.epmem_id);
                    p.put(w.value.asIdentifier().epmem_id, w.epmem_id);
                    epmem_id_replacement.remove(p);
                }
            }
          
            // reduce the ref count on the value
            if(!lti && (w.value.asIdentifier().epmem_id!=EPMEM_NODEID_BAD) && (w.value.asIdentifier().epmem_valid==epmem_validation))
            {
                Set<WmeImpl> my_refs = epmem_id_ref_counts.get(w.value.asIdentifier().epmem_id);

                //This is to mimic the lazy insertion of entries from the stl map. -ACN
                if(my_refs == null){
                    my_refs = new HashSet<WmeImpl>();
                    epmem_id_ref_counts.put(w.value.asIdentifier().epmem_id, my_refs);
                }
                if(my_refs.contains(w))
                {
                    my_refs.remove(w);

                    // recurse if no incoming edges from top-state (i.e. not in transitive closure of top-state)
                    boolean recurse = true;
                    for(WmeImpl rc_it : my_refs)
                    {
                        if(rc_it.id.asIdentifier().level==decider.top_state.asIdentifier().level)
                        {
                            recurse = false;
                            break;
                        }
                    }

                    if ( recurse )
                    {
                        my_refs.clear();
                        epmem_id_removes.push(w.value);
                    }
                }
            }
        }
        else if((w.epmem_id!=EPMEM_NODEID_BAD) && (w.epmem_valid==epmem_validation))
        {
            was_encoded = true;
     
            epmem_node_removals.put(w.epmem_id, true);
        }

        if ( was_encoded )
        {
            w.epmem_id = EPMEM_NODEID_BAD;
            w.epmem_valid = 0;
        }
    }

    @Override
    public void processIds()
    {
        while(!epmem_id_removes.isEmpty())
        {
            IdentifierImpl id = epmem_id_removes.poll().asIdentifier();
            
            //assert( id->common.symbol_type == IDENTIFIER_SYMBOL_TYPE );   
            if(id==null) {
                logError("Expected identifier symbol type in epmem_id_removes queue");
                continue;
            }

            if((id.epmem_id != EPMEM_NODEID_BAD) && (id.epmem_valid == epmem_validation))
            {
                // invalidate identifier encoding
                id.epmem_id = EPMEM_NODEID_BAD;
                id.epmem_valid = 0;

                // impasse wmes
                for(WmeImpl w=id.goalInfo != null ? id.goalInfo.getImpasseWmes() : null; w!=null; w=w.next)
                {
                    removeWme(w);
                }

                // input wmes
                for(WmeImpl w=id.getInputWmes(); w!=null; w=w.next)
                {
                    removeWme(w);
                }

                // regular wmes
                for(Slot s=id.slots; s!=null; s=s.next)
                {
                    for(WmeImpl w=s.getWmes(); w!=null; w=w.next)
                    {
                        removeWme(w);
                    }
                    
                    for(WmeImpl w=s.getAcceptablePreferenceWmes(); w!=null; w=w.next)
                    {
                        removeWme(w);
                    }
                }
            }
        }
    }
    
    private void logError(String text) {
        logger.error(text);
        agent.getPrinter().error(text);
    }
}
