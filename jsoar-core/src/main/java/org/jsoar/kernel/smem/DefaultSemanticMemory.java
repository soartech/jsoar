/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.epmem.DefaultEpisodicMemory;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.ConjunctiveTest;
import org.jsoar.kernel.lhs.EqualityTest;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.lhs.Test;
import org.jsoar.kernel.lhs.Tests;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WmeImpl.SymbolTriple;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.modules.SoarModule;
import org.jsoar.kernel.parser.original.Lexeme;
import org.jsoar.kernel.parser.original.LexemeType;
import org.jsoar.kernel.parser.original.Lexer;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.ActivateOnQueryChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.ActivationChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.AppendDatabaseChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.BaseUpdateChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.LazyCommitChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.LearningChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.MergeChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.MirroringChoices;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.Optimization;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.PageChoices;
import org.jsoar.kernel.smem.math.MathQuery;
import org.jsoar.kernel.smem.math.MathQueryGreater;
import org.jsoar.kernel.smem.math.MathQueryGreaterOrEqual;
import org.jsoar.kernel.smem.math.MathQueryLess;
import org.jsoar.kernel.smem.math.MathQueryLessOrEqual;
import org.jsoar.kernel.smem.math.MathQueryMax;
import org.jsoar.kernel.smem.math.MathQueryMin;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.kernel.wma.WorkingMemoryActivation;
import org.jsoar.util.ByRef;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;
import org.jsoar.util.properties.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link SemanticMemory}
 * 
 * <h2>Variance from CSoar Implementation</h2>
 * <p>
 * The smem_data_struct that was added to every identifier in CSoar is instead
 * maintained in a map from id to {@link SemanticMemoryStateInfo} in this class.
 * This structure is never accessed outside of SMem or in a way that would make
 * a map too slow.
 * 
 * <h2>Notes on soardb/sqlite to JDBC conversion</h2>
 * <ul>
 * <li>When retrieving column values (e.g. {@code sqlite3_column_int}), columns
 * are 0-based. In JDBC, they are 1-based. So all column retrievals (in the
 * initial port) have the original index and {@code + 1}. For example,
 * {@code rs.getLong(2 + 1)}.
 * <li>soardb tries to store ints as 32 or 64 bits depending on the platform. In
 * this port, we're just using long (64 bits) everywhere. So
 * {@code column_int()} maps to {@code ResultSet.getLong()}.
 * </ul>
 * <h2>Typedef mappings</h2>
 * <ul>
 * <li>uintptr_t == long
 * <li>intptr_t == long
 * <li>smem_hash_id == long
 * <li>smem_lti_id == long
 * <li>goal_stack_level == int
 * <li>smem_lti_set = {@code Set<Long>}
 * <li>smem_wme_list == {@code List<WmeImpl>}
 * <li>smem_pooled_symbol_set == {@code Set<SymbolImpl>}
 * <li>smem_wme_stack == {@code Deque<Preference>}
 * <li>smem_slot == {@code List<smem_chunk_value> }
 * <li>smem_slot_map == {@code Map<SymbolImpl, List<smem_chunk_value>>}
 * <li>smem_str_to_chunk_map == {@code Map<String, smem_chunk_value>}
 * <li>smem_chunk_set == {@code Set<smem_chunk_value>}
 * <li>smem_sym_to_chunk_map = {@code Map<SymbolImpl, smem_chunk>}
 * <li>smem_lti_set = {@code Set<Long>}
 * <li>smem_weighted_cue_list = {@code LinkedList<WeightedCueElement>}
 * <li>smem_prioritized_activated_lti_queue =
 * {@code PriorityQueue<ActivatedLti>}
 * <li>tc_number = {@code Marker}
 * </ul>
 * 
 * @author ray
 */
public class DefaultSemanticMemory implements SemanticMemory
{
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSemanticMemory.class);
    
    /**
     * semantic_memory.h:232:smem_variable_key
     */
    private enum smem_variable_key
    {
        var_max_cycle, var_num_nodes, var_num_edges, var_act_thresh, var_act_mode
    }
    
    /**
     * semantic_memory.h:260:smem_storage_type
     */
    private enum smem_storage_type
    {
        store_level, store_recursive
    }
    
    /**
     * semantic_memory.h:367:smem_query_levels
     */
    private enum smem_query_levels
    {
        qry_search, qry_full
    }
    
    /**
     * semantic_memory.h:237:SMEM_ACT_MAX
     */
    private static final long SMEM_ACT_MAX = (0 - 1) / 2; // TODO???
    
    // Not ever used so commented to prevent warnings.
    // private static final long SMEM_LTI_UNKNOWN_LEVEL = 0L;
    
    private static final long SMEM_AUGMENTATIONS_NULL = 0L;
    
    // Not ever used so commented to prevent warnings.
    // private static final String SMEM_AUGMENTATIONS_NULL_STR = "0";
    
    private static final long SMEM_ACT_HISTORY_ENTRIES = 10L;
    
    private static final long SMEM_ACT_LOW = -1000000000L;
    
    private Adaptable context;
    
    private DefaultSemanticMemoryParams params;
    
    private DefaultSemanticMemoryStats stats;
    
    /* private */SymbolFactoryImpl symbols;
    
    private RecognitionMemory recMem;
    
    private Chunker chunker;
    
    private Decider decider;
    
    private DefaultEpisodicMemory epmem;
    
    private WorkingMemoryActivation wma;
    
    private Trace trace;
    
    private SemanticMemoryDatabase db;
    
    /** agent.h:smem_validation */
    private/* uintptr_t */long smem_validation;
    
    /** agent.h:smem_max_cycle */
    private/* intptr_t */long smem_max_cycle;
    
    /* private */SemanticMemorySymbols predefinedSyms;
    
    private Map<IdentifierImpl, SemanticMemoryStateInfo> stateInfos = new LinkedHashMap<>();
    
    private Set<IdentifierImpl> smem_changed_ids = new LinkedHashSet<>();
    
    private boolean smem_ignore_changes;
    
    /**
     * This section regarding the lastCue member has no equivalent in CSoar. It was
     * added to allow unit tests to make sure the proper cue is being used. It could
     * also be exposed somewhere on the debugger to assist in implementing efficient
     * SMem agents.
     */
    private BasicWeightedCue lastCue;
    
    public BasicWeightedCue getLastCue()
    {
        return lastCue;
    }
    
    public static class BasicWeightedCue
    {
        public final WmeImpl cue;
        public final long weight;
        
        public BasicWeightedCue(WmeImpl cue, long weight)
        {
            this.cue = cue;
            this.weight = weight;
        }
    }
    
    public DefaultSemanticMemory(Adaptable context)
    {
        this(context, null);
    }
    
    public DefaultSemanticMemory(Adaptable context, SemanticMemoryDatabase db)
    {
        this.context = context;
        this.db = db;
    }
    
    public void initialize()
    {
        this.symbols = Adaptables.require(DefaultSemanticMemory.class, context, SymbolFactoryImpl.class);
        this.predefinedSyms = new SemanticMemorySymbols(this.symbols);
        
        this.chunker = Adaptables.adapt(context, Chunker.class);
        this.decider = Adaptables.adapt(context, Decider.class);
        this.recMem = Adaptables.adapt(context, RecognitionMemory.class);
        
        this.epmem = Adaptables.require(DefaultSemanticMemory.class, context, DefaultEpisodicMemory.class);
        this.wma = Adaptables.require(DefaultSemanticMemory.class, context, WorkingMemoryActivation.class);
        
        Agent agent = Adaptables.adapt(context, Agent.class);
        this.trace = agent.getTrace();
        
        final PropertyManager properties = Adaptables.require(DefaultSemanticMemory.class, context, PropertyManager.class);
        params = new DefaultSemanticMemoryParams(properties);
        stats = new DefaultSemanticMemoryStats(properties);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.smem.SemanticMemory#resetStatistics()
     */
    @Override
    public void resetStatistics()
    {
        stats.reset();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.smem.SemanticMemory#getStatistics()
     */
    @Override
    public SemanticMemoryStatistics getStatistics()
    {
        return stats;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jsoar.kernel.smem.SemanticMemory#attachToNewContext(org.jsoar.kernel
     * .symbols.IdentifierImpl)
     */
    @Override
    public void initializeNewContext(WorkingMemory wm, IdentifierImpl id)
    {
        stateInfos.put(id, new SemanticMemoryStateInfo(this, wm, id));
    }
    
    public SemanticMemoryDatabase getDatabase()
    {
        return db;
    }
    
    DefaultSemanticMemoryParams getParams()
    {
        return params;
    }
    
    DefaultSemanticMemoryStats getStats()
    {
        return stats;
    }
    
    private SemanticMemoryStateInfo smem_info(IdentifierImpl state)
    {
        return stateInfos.get(state);
    }
    
    @Override
    public boolean smem_enabled()
    {
        return params.learning.get() == LearningChoices.on;
    }
    
    private List<WmeImpl> smem_get_direct_augs_of_id(SymbolImpl sym)
    {
        return smem_get_direct_augs_of_id(sym, null);
    }
    
    /**
     * semantic_memory.cpp:481:smem_get_direct_augs_of_id
     */
    private List<WmeImpl> smem_get_direct_augs_of_id(SymbolImpl sym, Marker tc /*
                                                                                * =
                                                                                * NIL
                                                                                */)
    {
        final List<WmeImpl> return_val = new ArrayList<>();
        // augs only exist for identifiers
        final IdentifierImpl id = sym.asIdentifier();
        if(id != null)
        {
            if(tc != null)
            {
                if(tc == id.tc_number)
                {
                    return return_val;
                }
                else
                {
                    id.tc_number = tc;
                }
            }
            
            // impasse wmes
            for(WmeImpl w = id.goalInfo != null ? id.goalInfo.getImpasseWmes() : null; w != null; w = w.next)
            {
                if(!w.acceptable)
                {
                    return_val.add(w);
                }
            }
            
            // input wmes
            for(WmeImpl w = id.getInputWmes(); w != null; w = w.next)
            {
                return_val.add(w);
            }
            
            // regular wmes
            for(Slot s = id.slots; s != null; s = s.next)
            {
                for(WmeImpl w = s.getWmes(); w != null; w = w.next)
                {
                    if(!w.acceptable)
                    {
                        return_val.add(w);
                    }
                }
            }
        }
        
        return return_val;
    }
    
    /**
     * semantic_memory.cpp:481:smem_symbol_is_constant
     * 
     */
    private static boolean smem_symbol_is_constant(Symbol sym)
    {
        return ((Symbols.getSymbolType(sym) == Symbols.SYM_CONSTANT_SYMBOL_TYPE) || (Symbols.getSymbolType(sym) == Symbols.INT_CONSTANT_SYMBOL_TYPE)
                || (Symbols.getSymbolType(sym) == Symbols.FLOAT_CONSTANT_SYMBOL_TYPE));
    }
    
    private long /* smem_hash_id */ smem_temporal_hash_add_type(int symbol_type) throws SQLException
    {
        db.hash_add_type.setInt(1, symbol_type);
        return JdbcTools.insertAndGetRowId(db.hash_add_type);
    }
    
    private long /* smem_hash_id */ smem_temporal_hash_int(long val, boolean add_on_fail /*
                                                                                          * =
                                                                                          * true
                                                                                          */) throws SQLException
    {
        long /* smem_hash_id */ return_val = 0;
        
        // search first
        db.hash_get_int.setLong(1, val);
        try(ResultSet rs = db.hash_get_int.executeQuery())
        {
            if(rs.next())
            {
                return_val = rs.getLong(0 + 1);
            }
        }
        
        // if fail and supposed to add
        if(return_val == 0 && add_on_fail)
        {
            // type first
            return_val = smem_temporal_hash_add_type(Symbols.INT_CONSTANT_SYMBOL_TYPE);
            
            // then content
            db.hash_add_int.setLong(1, return_val);
            db.hash_add_int.setLong(2, val);
            db.hash_add_int.executeUpdate(/* soar_module::op_reinit */);
        }
        
        return return_val;
    }
    
    private long /* smem_hash_id */ smem_temporal_hash_float(double val, boolean add_on_fail /*
                                                                                              * =
                                                                                              * true
                                                                                              */) throws SQLException
    {
        long /* smem_hash_id */ return_val = 0;
        
        // search first
        // search first
        db.hash_get_float.setDouble(1, val);
        try(ResultSet rs = db.hash_get_float.executeQuery())
        {
            if(rs.next())
            {
                return_val = rs.getLong(0 + 1);
            }
        }
        
        // if fail and supposed to add
        if(return_val == 0 && add_on_fail)
        {
            // type first
            return_val = smem_temporal_hash_add_type(Symbols.FLOAT_CONSTANT_SYMBOL_TYPE);
            
            // then content
            db.hash_add_float.setLong(1, return_val);
            db.hash_add_float.setDouble(2, val);
            db.hash_add_float.executeUpdate(/* soar_module::op_reinit */);
        }
        
        return return_val;
    }
    
    private long /* smem_hash_id */ smem_temporal_hash_str(String val, boolean add_on_fail /*
                                                                                            * =
                                                                                            * true
                                                                                            */) throws SQLException
    {
        long /* smem_hash_id */ return_val = 0;
        
        // search first
        // search first
        db.hash_get_str.setString(1, val);
        try(ResultSet rs = db.hash_get_str.executeQuery())
        {
            if(rs.next())
            {
                return_val = rs.getLong(0 + 1);
            }
        }
        
        // if fail and supposed to add
        if(return_val == 0 && add_on_fail)
        {
            // type first
            return_val = smem_temporal_hash_add_type(Symbols.SYM_CONSTANT_SYMBOL_TYPE);
            
            // then content
            db.hash_add_str.setLong(1, return_val);
            db.hash_add_str.setString(2, val);
            db.hash_add_str.executeUpdate(/* soar_module::op_reinit */);
        }
        
        return return_val;
    }
    
    /**
     * semantic_memory.cpp:589:_smem_add_wme
     */
    void _smem_process_buffered_wme_list(IdentifierImpl state, Set<WmeImpl> cue_wmes, List<SymbolTriple> my_list, boolean meta)
    {
        if(my_list.isEmpty())
        {
            return;
        }
        
        Instantiation inst = SoarModule.make_fake_instantiation(state, cue_wmes, my_list);
        
        for(Preference pref = inst.preferences_generated; pref != null; pref = pref.inst_next)
        {
            // add the preference to temporary memory
            recMem.add_preference_to_tm(pref);
            // and add it to the list of preferences to be removed
            // when the goal is removed
            state.goalInfo.addGoalPreference(pref);
            pref.on_goal_list = true;
            
            if(meta)
            {
                // if this is a meta wme, then it is completely local
                // to the state and thus we will manually remove it
                // (via preference removal) when the time comes
                smem_info(state).smem_wmes.push(pref);
            }
        }
        
        if(!meta)
        {
            // otherwise, we submit the fake instantiation to backtracing
            // such as to potentially produce justifications that can follow
            // it to future adventures (potentially on new states)
            
            final ByRef<Instantiation> my_justification_list = ByRef.create(null);
            chunker.chunk_instantiation(inst, false, my_justification_list);
            
            // if any justifications are created, assert their preferences
            // manually
            // (copied mainly from assert_new_preferences with respect to our
            // circumstances)
            if(my_justification_list.value != null)
            {
                Instantiation next_justification = null;
                
                for(Instantiation my_justification = my_justification_list.value; my_justification != null; my_justification = next_justification)
                {
                    next_justification = my_justification.nextInProdList;
                    
                    if(my_justification.in_ms)
                    {
                        my_justification.prod.instantiations = my_justification.insertAtHeadOfProdList(my_justification.prod.instantiations);
                    }
                    
                    for(Preference just_pref = my_justification.preferences_generated; just_pref != null; just_pref = just_pref.inst_next)
                    {
                        recMem.add_preference_to_tm(just_pref);
                        if(wma.wma_enabled())
                        {
                            wma.wma_activate_wmes_in_pref(just_pref);
                        }
                    }
                }
            }
        }
    }
    
    private void smem_process_buffered_wmes(IdentifierImpl state, Set<WmeImpl> cue_wmes, List<SymbolTriple> meta_wmes, List<SymbolTriple> retrieval_wmes)
    {
        _smem_process_buffered_wme_list(state, cue_wmes, meta_wmes, true);
        _smem_process_buffered_wme_list(state, cue_wmes, retrieval_wmes, false);
    }
    
    private void smem_buffer_add_wme(List<SymbolTriple> my_list, IdentifierImpl id, SymbolImpl attr, SymbolImpl value)
    {
        my_list.add(new SymbolTriple(id, attr, value));
        
        // Don't need since this is JSoar? -ALT
        // symbol_add_ref( id );
        // symbol_add_ref( attr );
        // symbol_add_ref( value );
    }
    
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    // Variable Functions (smem::var)
    //
    // Variables are key-value pairs stored in the database
    // that are necessary to maintain a store between
    // multiple runs of Soar.
    //
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    
    /**
     * Gets an SMem variable from the database
     * 
     * <p>
     * semantic_memory.cpp:682:smem_variable_get
     * 
     */
    private boolean smem_variable_get(smem_variable_key variable_id, ByRef<Long> variable_value) throws SQLException
    {
        final PreparedStatement var_get = db.var_get;
        
        var_get.setInt(1, variable_id.ordinal());
        try(ResultSet rs = var_get.executeQuery())
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
    }
    
    /**
     * Sets an SMem variable in the database
     * 
     * semantic_memory.cpp:705:smem_variable_set
     * 
     */
    private void smem_variable_set(smem_variable_key variable_id, long variable_value) throws SQLException
    {
        final PreparedStatement var_set = db.var_set;
        
        var_set.setLong(1, variable_value);
        var_set.setInt(2, variable_id.ordinal());
        
        var_set.execute();
    }
    
    /**
     * Create a new SMem variable in the database
     * 
     * semantic_memory.cpp:705:smem_variable_set
     * 
     */
    private void smem_variable_create(smem_variable_key variable_id, long variable_value) throws SQLException
    {
        final PreparedStatement var_create = db.var_create;
        
        var_create.setInt(1, variable_id.ordinal());
        var_create.setLong(2, variable_value);
        
        var_create.execute();
    }
    
    /**
     * semantic_memory.cpp:735:smem_temporal_hash
     * 
     */
    private/* smem_hash_id */long smem_temporal_hash(SymbolImpl sym) throws SQLException
    {
        return smem_temporal_hash(sym, true);
    }
    
    /**
     * Returns a temporally unique integer representing a symbol constant
     * 
     * <p>
     * semantic_memory.cpp:735:smem_temporal_hash
     * 
     */
    private/* smem_hash_id */long smem_temporal_hash(SymbolImpl sym, boolean add_on_fail /*
                                                                                          * =
                                                                                          * true
                                                                                          */) throws SQLException
    {
        /* smem_hash_id */long return_val = 0;
        
        // //////////////////////////////////////////////////////////////////////////
        // TODO SMEM timers: my_agent->smem_timers->hash->start();
        // //////////////////////////////////////////////////////////////////////////
        
        if(smem_symbol_is_constant(sym))
        {
            if((sym.smem_hash == 0) || (sym.common_smem_valid != smem_validation))
            {
                sym.smem_hash = 0;
                sym.common_smem_valid = smem_validation;
                
                // basic process:
                // - search
                // - if found, return
                // - else, add
                
                switch(Symbols.getSymbolType(sym))
                {
                case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                    return_val = smem_temporal_hash_str(sym.asString().getValue(), add_on_fail);
                    break;
                
                case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                    return_val = smem_temporal_hash_int(sym.asInteger().getValue(), add_on_fail);
                    break;
                
                case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                    return_val = smem_temporal_hash_float(sym.asDouble().getValue(), add_on_fail);
                    break;
                }
                
                // cache results for later re-use
                sym.smem_hash = return_val;
                sym.common_smem_valid = smem_validation;
            }
            
            return_val = sym.smem_hash;
        }
        
        // //////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->hash->stop();
        // //////////////////////////////////////////////////////////////////////////
        
        return return_val;
    }
    
    private int /* long? */ smem_reverse_hash_int(long /* smem_hash_id */ hash_value) throws SQLException
    {
        db.hash_rev_int.setLong(1, hash_value);
        try(ResultSet rs = db.hash_rev_int.executeQuery())
        {
            if(!rs.next())
            {
                throw new IllegalStateException("Expected non-empty result");
            }
            int toReturn = rs.getInt(0 + 1);
            return toReturn;
        }
    }
    
    private double smem_reverse_hash_float(long /* smem_hash_id */ hash_value) throws SQLException
    {
        db.hash_rev_float.setLong(1, hash_value);
        try(ResultSet rs = db.hash_rev_float.executeQuery())
        {
            rs.next();
            double toReturn = rs.getDouble(0 + 1);
            return toReturn;
        }
    }
    
    private String smem_reverse_hash_str(long /* smem_hash_id */ hash_value) throws SQLException
    {
        db.hash_rev_str.setLong(1, hash_value);
        try(ResultSet rs = db.hash_rev_str.executeQuery())
        {
            rs.next();
            String toReturn = rs.getString(0 + 1);
            return toReturn;
        }
    }
    
    private SymbolImpl smem_reverse_hash(int symbol_type, long /* smem_hash_id */ hash_value) throws SQLException
    {
        switch(symbol_type)
        {
        case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
            return symbols.createString(smem_reverse_hash_str(hash_value));
        
        case Symbols.INT_CONSTANT_SYMBOL_TYPE:
            return symbols.createInteger(smem_reverse_hash_int(hash_value));
        
        case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
            return symbols.createDouble(smem_reverse_hash_float(hash_value));
        
        default:
            return null;
        }
    }
    
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    // Activation Functions (smem::act)
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    
    public double smem_lti_calc_base(long lti, long time_now)
    {
        return smem_lti_calc_base(lti, time_now);
    }
    
    public double smem_lti_calc_base(long lti, long time_now, long n) throws SQLException
    {
        return smem_lti_calc_base(lti, time_now, n, 0);
    }
    
    public double smem_lti_calc_base(long lti, long time_now, long n, long activations_first) throws SQLException
    {
        double sum = 0.0;
        double d = this.params.base_decay.get();
        long t_k;
        long t_n = (time_now - activations_first);
        
        if(n == 0)
        {
            db.lti_access_get.setLong(1, lti);
            try(ResultSet rs = db.lti_access_get.executeQuery())
            {
                n = rs.getLong(1 + 1);
                activations_first = rs.getLong(2 + 1);
            }
        }
        
        // get all history
        db.history_get.setLong(1, lti);
        try(ResultSet rs = db.history_get.executeQuery())
        {
            rs.next();
            int available_history = (int) (SMEM_ACT_HISTORY_ENTRIES < n ? (SMEM_ACT_HISTORY_ENTRIES) : (n));
            t_k = time_now - rs.getLong(available_history - 1 + 1);
            
            for(int i = 0; i < available_history; i++)
            {
                sum += Math.pow(time_now - rs.getLong(i + 1), -d);
            }
        }
        
        // if available history was insufficient, approximate rest
        if(n > SMEM_ACT_HISTORY_ENTRIES)
        {
            double apx_numerator = (n - SMEM_ACT_HISTORY_ENTRIES) * (Math.pow(t_n, 1.0 - d) - Math.pow(t_k, 1.0 - d));
            double apx_denominator = ((1.0 - d) * (t_n - t_k));
            
            sum += (apx_numerator / apx_denominator);
        }
        
        return ((sum > 0) ? (Math.log(sum)) : (SMEM_ACT_LOW));
    }
    
    double smem_lti_activate(long lti, boolean add_access) throws SQLException
    {
        return smem_lti_activate(lti, add_access, SMEM_ACT_MAX);
    }
    
    /**
     * activates a new or existing long-term identifier note: optional num_edges
     * parameter saves us a lookup just when storing a new chunk (default is a
     * big number that should never come up naturally and if it does, satisfies
     * thresholding behavior).
     * 
     */
    double smem_lti_activate(long lti, boolean add_access, long num_edges) throws SQLException
    {
        // TODO: SMem Timers
        // //////////////////////////////////////////////////////////////////////////
        // my_agent->smem_timers->act->start();
        // //////////////////////////////////////////////////////////////////////////
        
        long time_now;
        if(add_access)
        {
            time_now = this.smem_max_cycle++;
            
            if((this.params.activation_mode.get() == DefaultSemanticMemoryParams.ActivationChoices.base_level)
                    && (this.params.base_update.get() == DefaultSemanticMemoryParams.BaseUpdateChoices.incremental))
            {
                long time_diff;
                
                // for ( std::set< int64_t >::iterator
                // b=my_agent->smem_params->base_incremental_threshes->set_begin();
                // b!=my_agent->smem_params->base_incremental_threshes->set_end();
                // b++ )
                for(Iterator<Long> b = this.params.base_incremental_threshes.get().iterator(); b.hasNext();)
                {
                    Long next = b.next();
                    
                    if(next > 0)
                    {
                        time_diff = (time_now - next);
                        
                        if(time_diff > 0)
                        {
                            List<Long> to_update = new ArrayList<>();
                            
                            db.lti_get_t.setLong(1, time_diff);
                            try(ResultSet rs = db.lti_get_t.executeQuery();)
                            {
                                
                                // while (
                                // my_agent->smem_stmts->lti_get_t->execute() ==
                                // soar_module::row )
                                
                                while(rs.next())
                                {
                                    to_update.add(rs.getLong(0 + 1));
                                }
                            }
                            
                            // for ( std::list< smem_lti_id >::iterator
                            // it=to_update.begin(); it!=to_update.end(); it++ )
                            for(Long l : to_update)
                            {
                                smem_lti_activate(l, false);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            time_now = smem_max_cycle;
            
            this.stats.act_updates.set(this.stats.act_updates.get() + 1);
        }
        
        // access information
        long prev_access_n = 0;
        @SuppressWarnings("unused")
        long prev_access_t = 0;
        long prev_access_1 = 0;
        {
            // get old (potentially useful below)
            db.lti_access_get.setLong(1, lti);
            try(ResultSet rs = db.lti_access_get.executeQuery())
            {
                rs.next();
                prev_access_n = rs.getLong(0 + 1);
                prev_access_t = rs.getLong(1 + 1);
                prev_access_1 = rs.getLong(2 + 1);
            }
            
            // set new
            if(add_access)
            {
                db.lti_access_set.setLong(1, (prev_access_n + 1));
                db.lti_access_set.setLong(2, time_now);
                db.lti_access_set.setLong(3, ((prev_access_n == 0) ? (time_now) : (prev_access_1)));
                
                db.lti_access_set.setLong(4, lti);
                db.lti_access_set.executeUpdate();
            }
        }
        
        // get new activation value (depends upon bias)
        double new_activation = 0.0;
        ActivationChoices act_mode = this.params.activation_mode.get();
        if(act_mode == ActivationChoices.recency)
        {
            new_activation = time_now;
        }
        else if(act_mode == ActivationChoices.frequency)
        {
            new_activation = prev_access_n + ((add_access) ? (1) : (0));
        }
        else if(act_mode == ActivationChoices.base_level)
        {
            if(prev_access_n == 0)
            {
                if(add_access)
                {
                    db.history_add.setLong(1, lti);
                    db.history_add.setLong(2, time_now);
                    db.history_add.executeUpdate();
                }
                
                new_activation = 0;
            }
            else
            {
                if(add_access)
                {
                    db.history_push.setLong(1, time_now);
                    db.history_push.setLong(2, lti);
                    db.history_push.executeUpdate();
                }
                
                new_activation = smem_lti_calc_base(lti, time_now + ((add_access) ? (1) : (0)), prev_access_n + ((add_access) ? (1) : (0)), prev_access_1);
            }
        }
        
        // get number of augmentations (if not supplied)
        if(num_edges == SMEM_ACT_MAX)
        {
            ResultSet rs = null;
            try
            {
                db.act_lti_child_ct_get.setLong(1, lti);
                rs = db.act_lti_child_ct_get.executeQuery();
                rs.next();
                num_edges = rs.getLong(0 + 1);
            }
            finally
            {
                rs.close();
            }
        }
        
        // only if augmentation count is less than threshold do we associate
        // with edges
        if(num_edges < this.params.thresh.get())
        {
            // activation_value=? WHERE lti=?
            db.act_set.setDouble(1, new_activation);
            db.act_set.setLong(2, lti);
            db.act_set.executeUpdate();
        }
        
        // always associate activation with lti
        {
            // activation_value=? WHERE lti=?
            db.act_lti_set.setDouble(1, new_activation);
            db.act_lti_set.setLong(2, lti);
            db.act_lti_set.executeUpdate();
        }
        
        // TODO: SMem Timers
        // //////////////////////////////////////////////////////////////////////////
        // my_agent->smem_timers->act->stop();
        // //////////////////////////////////////////////////////////////////////////
        
        return new_activation;
    }
    
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    // Long-Term Identifier Functions (smem::lti)
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    
    /**
     * copied primarily from add_bound_variables_in_test
     * 
     * <p>
     * semantic_memory.cpp:794:_smem_lti_from_test
     * 
     */
    private static void _smem_lti_from_test(Test t, Set<IdentifierImpl> valid_ltis)
    {
        if(Tests.isBlank(t))
        {
            return;
        }
        
        final EqualityTest eq = t.asEqualityTest();
        if(eq != null)
        {
            final IdentifierImpl referent = eq.getReferent().asIdentifier();
            if(referent != null && referent.smem_lti != 0)
            {
                valid_ltis.add(referent);
            }
            
            return;
        }
        
        {
            final ConjunctiveTest ct = t.asConjunctiveTest();
            
            if(ct != null)
            {
                for(Test c : ct.conjunct_list)
                {
                    _smem_lti_from_test(c, valid_ltis);
                }
            }
        }
    }
    
    /**
     * copied primarily from add_all_variables_in_rhs_value
     * 
     * <p>
     * semantic_memory.cpp:823:_smem_lti_from_rhs_value
     * 
     */
    private static void _smem_lti_from_rhs_value(RhsValue rv, Set<IdentifierImpl> valid_ltis)
    {
        final RhsSymbolValue rsv = rv.asSymbolValue();
        if(rsv != null)
        {
            final IdentifierImpl sym = rsv.getSym().asIdentifier();
            if(sym != null && sym.smem_lti != 0)
            {
                valid_ltis.add(sym);
            }
        }
        else
        {
            for(RhsValue c : rv.asFunctionCall().getArguments())
            {
                _smem_lti_from_rhs_value(c, valid_ltis);
            }
        }
    }
    
    /**
     * make sure ltis in actions are grounded
     * 
     * <p>
     * semantic_memory.h:smem_valid_production
     * <p>
     * semantic_memory.cpp:844:smem_valid_production
     */
    public static boolean smem_valid_production(Condition lhs_top, Action rhs_top)
    {
        final Set<IdentifierImpl> valid_ltis = new LinkedHashSet<>();
        
        // collect valid ltis
        for(Condition c = lhs_top; c != null; c = c.next)
        {
            final PositiveCondition pc = c.asPositiveCondition();
            if(pc != null)
            {
                _smem_lti_from_test(pc.attr_test, valid_ltis);
                _smem_lti_from_test(pc.value_test, valid_ltis);
            }
        }
        
        // validate ltis in actions
        // copied primarily from add_all_variables_in_action
        int action_counter = 0;
        
        for(Action a = rhs_top; a != null; a = a.next)
        {
            a.already_in_tc = false;
            action_counter++;
        }
        
        // good_pass detects infinite loops
        boolean good_pass = true;
        boolean good_action = true;
        while(good_pass && action_counter != 0)
        {
            good_pass = false;
            
            for(Action a = rhs_top; a != null; a = a.next)
            {
                if(!a.already_in_tc)
                {
                    good_action = false;
                    
                    final MakeAction ma = a.asMakeAction();
                    if(ma != null)
                    {
                        final IdentifierImpl id = ma.id.asSymbolValue().getSym().asIdentifier();
                        
                        // non-identifiers are ok
                        if(id == null)
                        {
                            good_action = true;
                        }
                        // short-term identifiers are ok
                        else if(id.smem_lti == 0)
                        {
                            good_action = true;
                        }
                        // valid long-term identifiers are ok
                        else if(valid_ltis.contains(id))
                        {
                            good_action = true;
                        }
                    }
                    else
                    {
                        good_action = true;
                    }
                    
                    // we've found a new good action
                    // mark as good, collect all goodies
                    if(good_action)
                    {
                        a.already_in_tc = true;
                        
                        if(ma != null)
                        {
                            _smem_lti_from_rhs_value(ma.value, valid_ltis);
                            _smem_lti_from_rhs_value(ma.attr, valid_ltis);
                        }
                        else
                        {
                            _smem_lti_from_rhs_value(a.asFunctionAction().getCall(), valid_ltis);
                        }
                        
                        // note that we've dealt with another action
                        action_counter--;
                        good_pass = true;
                    }
                }
            }
        }
        
        return action_counter == 0;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_lti_get_id(char, long)
     */
    @Override
    public long /* smem_lti_id */ smem_lti_get_id(char name_letter, long name_number) throws SoarException
    {
        // semantic_memory.cpp:989:smem_lti_get_id
        
        /* smem_lti_id */long return_val = 0;
        
        // getting lti ids requires an open semantic database
        smem_attach();
        
        try
        {
            // soar_letter=? AND number=?
            db.lti_get.setLong(1, name_letter);
            db.lti_get.setLong(2, name_number);
            
            try(ResultSet rs = db.lti_get.executeQuery())
            {
                if(rs.next())
                {
                    return_val = rs.getLong(0 + 1);
                }
            }
        }
        catch(SQLException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
        
        // db.lti_get->reinitialize();
        
        return return_val;
    }
    
    /**
     * adds a new lti id for a soar_letter/number pair
     * 
     * <p>
     * semantic_memory.cpp:1011:smem_lti_add_id
     * 
     */
    long /* smem_lti_id */ smem_lti_add_id(char name_letter, long name_number) throws SQLException
    {
        /* smem_lti_id */long return_val = 0;
        
        // create lti: soar_letter, number, total_augmentations,
        // activation_value, activations_total, activations_last,
        // activations_first
        db.lti_add.setLong(1, name_letter);
        db.lti_add.setLong(2, name_number);
        db.lti_add.setLong(3, 0);
        db.lti_add.setDouble(4, 0);
        db.lti_add.setLong(5, 0);
        db.lti_add.setLong(6, 0);
        db.lti_add.setLong(7, 0);
        
        return_val = JdbcTools.insertAndGetRowId(db.lti_add);
        
        // increment stat
        stats.nodes.set(stats.nodes.get() + 1); // smem_stats->chunks in CSoar
        
        return return_val;
    }
    
    /**
     * makes a non-long-term identifier into a long-term identifier
     * 
     * <p>
     * semantic_memory.cpp:1031:smem_lti_soar_add
     * 
     */
    private/* smem_lti_id */long smem_lti_soar_add(SymbolImpl s) throws SoarException, SQLException
    {
        final IdentifierImpl id = s.asIdentifier();
        if((id != null) && (id.smem_lti == 0))
        {
            // try to find existing lti
            id.smem_lti = smem_lti_get_id(id.getNameLetter(), id.getNameNumber());
            
            // if doesn't exist, add
            if(id.smem_lti == 0)
            {
                id.smem_lti = smem_lti_add_id(id.getNameLetter(), id.getNameNumber());
                
                id.smem_time_id = epmem.getStats().getTime();
                id.id_smem_valid = epmem.epmem_validation();
                
                epmem.epmem_schedule_promotion(id);
            }
        }
        
        return id.smem_lti;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_lti_soar_make(long, char,
     * long, int)
     */
    public IdentifierImpl smem_lti_soar_make(/* smem_lti_id */long lti, char name_letter, long name_number, /* goal_stack_level */int level)
    {
        // semantic_memory.cpp:1053:smem_lti_soar_make
        
        // try to find existing
        IdentifierImpl return_val = symbols.findIdentifier(name_letter, name_number);
        
        // otherwise create
        if(return_val == null)
        {
            return_val = symbols.make_new_identifier(name_letter, name_number, level);
        }
        else
        {
            if((return_val.level == LTI_UNKNOWN_LEVEL) && (level != LTI_UNKNOWN_LEVEL))
            {
                return_val.level = level;
                return_val.promotion_level = level;
            }
        }
        
        // set lti field irrespective
        return_val.smem_lti = lti;
        
        return return_val;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_reset_id_counters()
     */
    @Override
    public void smem_reset_id_counters() throws SoarException
    {
        // semantic_memory.cpp:1082:smem_reset_id_counters
        
        if(db != null /*
                       * my_agent->smem_db->get_status() ==
                       * soar_module::connected
                       */)
        {
            try(ResultSet rs = db.lti_max.executeQuery())
            {
                while(rs.next())
                {
                    // soar_letter, max
                    final long name_letter = rs.getLong(0 + 1);
                    final long letter_max = rs.getLong(1 + 1);
                    
                    // shift to alphabet
                    // name_letter -= (long)( 'A' );
                    
                    symbols.resetIdNumber((char) name_letter, letter_max);
                }
            }
            catch(SQLException e)
            {
                throw new SoarException(e.getMessage(), e);
            }
            
            // db.lti_max->reinitialize();
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:1128:smem_disconnect_chunk
     * 
     */
    void smem_disconnect_chunk(/* smem_lti_id */long lti_id) throws SQLException
    {
        // adjust attr, attr/value counts
        {
            long pair_count = 0;
            
            long child_attr = 0;
            Set<Long> distinct_attr = new LinkedHashSet<>();
            
            // pairs first, accumulate distinct attributes and pair count
            db.web_all.setLong(1, lti_id);
            
            try(ResultSet webAllCounts = db.web_all.executeQuery())
            {
                while(webAllCounts.next())
                {
                    pair_count++;
                    
                    child_attr = webAllCounts.getLong(0 + 1);
                    distinct_attr.add(child_attr);
                    
                    // null -> attr/lti
                    if(webAllCounts.getLong(0 + 1) != SMEM_AUGMENTATIONS_NULL)
                    {
                        // adjust in opposite direction ( adjust, attribute,
                        // const )
                        db.wmes_constant_frequency_update.setInt(1, -1);
                        db.wmes_constant_frequency_update.setLong(2, child_attr);
                        db.wmes_constant_frequency_update.setInt(3, webAllCounts.getInt(1 + 1));
                        
                        db.wmes_constant_frequency_update.executeUpdate();
                    }
                    else
                    {
                        // adjust in opposite direction ( adjust, attribute, lti
                        // )
                        db.wmes_constant_frequency_update.setInt(1, -1);
                        db.wmes_constant_frequency_update.setLong(2, child_attr);
                        db.wmes_constant_frequency_update.setInt(3, webAllCounts.getInt(2 + 1));
                        
                        db.wmes_constant_frequency_update.executeUpdate();
                    }
                }
            }
            
            // now attributes
            // for (std::set<smem_lti_id>::iterator a=distinct_attr.begin();
            // a!=distinct_attr.end(); a++)
            for(Long a : distinct_attr)
            {
                // adjust in opposite direction ( adjust, attribute )
                db.attribute_frequency_update.setInt(1, -1);
                db.attribute_frequency_update.setLong(2, a);
                
                db.attribute_frequency_update.executeUpdate();
            }
            
            // update local statistic
            // my_agent->smem_stats->slots->set_value(
            // my_agent->smem_stats->slots->get_value() - pair_count );
            stats.edges.set(stats.edges.get() - pair_count);
        }
        
        // disconnect
        {
            db.web_truncate.setLong(1, lti_id);
            db.web_truncate.executeUpdate( /* soar_module::op_reinit */);
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:1617:smem_store_chunk
     * 
     */
    void smem_store_chunk(/* smem_lti_id */long lti_id, Map<SymbolImpl, List<Object>> children) throws SQLException
    {
        smem_store_chunk(lti_id, children, true, null);
    }
    
    /**
     * <p>
     * semantic_memory.cpp:1617:smem_store_chunk
     * 
     */
    void smem_store_chunk(/* smem_lti_id */long lti_id, Map<SymbolImpl, List<Object>> children, boolean remove_old_children) throws SQLException
    {
        smem_store_chunk(lti_id, children, remove_old_children, null);
    }
    
    private static class SmemHashIdLongPair
    {
        private final long /* smem_hash_id */ hash_id;
        
        private final long second;
        
        SmemHashIdLongPair(long hash_id, long second)
        {
            this.hash_id = hash_id;
            this.second = second;
        }
        
        public long getHashID()
        {
            return hash_id;
        }
        
        public long getSecond()
        {
            return second;
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hash(hash_id, second);
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if(this == obj)
            {
                return true;
            }
            if(obj == null)
            {
                return false;
            }
            if(getClass() != obj.getClass())
            {
                return false;
            }
            SmemHashIdLongPair other = (SmemHashIdLongPair) obj;
            if(hash_id != other.hash_id)
            {
                return false;
            }
            if(second != other.second)
            {
                return false;
            }
            return true;
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:1187:smem_store_chunk
     * 
     */
    void smem_store_chunk(/* smem_lti_id */long lti_id, Map<SymbolImpl, List<Object>> children, boolean remove_old_children /*
                                                                                                                             * =
                                                                                                                             * true
                                                                                                                             */, IdentifierImpl print_id /*
                                                                                                                                                          * =
                                                                                                                                                          * NULL
                                                                                                                                                          */) throws SQLException
    {
        // if remove children, disconnect chunk -> no existing edges
        // else, need to query number of existing edges
        long existing_edges = 0;
        if(remove_old_children)
        {
            smem_disconnect_chunk(lti_id);
            
            // provide trace output
            // if ( my_agent->sysparams[ TRACE_SMEM_SYSPARAM ] && ( print_id ) )
            // {
            // char buf[256];
            //
            // snprintf_with_symbols( my_agent, buf, 256, "<=SMEM: (%y ^* *)\n",
            // print_id );
            //
            // print( my_agent, buf );
            // xml_generate_warning( my_agent, buf );
            // }
            if(print_id != null)
            {
                trace.startNewLine().print(Category.SMEM, "<=SMEM: (%s ^* *)", print_id);
            }
        }
        else
        {
            db.act_lti_child_ct_get.setLong(1, lti_id);
            
            try(ResultSet rs = db.act_lti_child_ct_get.executeQuery())
            {
                rs.next();
                existing_edges = rs.getLong(0 + 1);
            }
        }
        
        // get new edges
        // if didn't disconnect, entails lookups in existing edges
        Set<Long /* smem_hash_id */> attr_new = new LinkedHashSet<>();
        Set<SmemHashIdLongPair /* smem_hash_id->smem_hash_id */> const_new = new LinkedHashSet<>();
        Set<SmemHashIdLongPair /* smem_hash_id->smem_lti_id */> lti_new = new LinkedHashSet<>();
        {
            long /* smem_hash_id */ attr_hash = 0;
            long /* smem_hash_id */ value_hash = 0;
            long /* smem_lti_id */ value_lti = 0;
            
            for(Map.Entry<SymbolImpl, List<Object>> s : children.entrySet())
            {
                attr_hash = smem_temporal_hash(s.getKey());
                if(remove_old_children)
                {
                    attr_new.add(attr_hash);
                }
                else
                {
                    // lti_id, attribute_s_id
                    db.web_attr_child.setLong(1, lti_id);
                    db.web_attr_child.setLong(2, attr_hash);
                    
                    try(ResultSet rs = db.web_attr_child.executeQuery())
                    {
                        if(!rs.next())
                        {
                            attr_new.add(attr_hash);
                        }
                    }
                }
                
                for(Object v : s.getValue())
                {
                    final SymbolImpl constant = Adaptables.adapt(v, SymbolImpl.class);
                    if(constant != null)
                    {
                        value_hash = smem_temporal_hash(constant);
                        
                        if(remove_old_children)
                        {
                            const_new.add(new SmemHashIdLongPair(attr_hash, value_hash));
                        }
                        else
                        {
                            // lti_id, attribute_s_id, val_const
                            db.web_const_child.setLong(1, lti_id);
                            db.web_const_child.setLong(2, attr_hash);
                            db.web_const_child.setLong(3, value_hash);
                            
                            try(ResultSet rs = db.web_const_child.executeQuery())
                            {
                                if(!rs.next())
                                {
                                    const_new.add(new SmemHashIdLongPair(attr_hash, value_hash));
                                }
                            }
                        }
                        
                        // provide trace output
                        // snprintf_with_symbols( my_agent, buf, 256,
                        // "=>SMEM: (%y ^%y %y)\n", print_id, s->first,
                        // (*v)->val_const.val_value );
                        trace.startNewLine().print(Category.SMEM, "=>SMEM: (%s ^%s %s)", print_id, s.getKey(), constant);
                    }
                    else
                    {
                        final smem_chunk_lti vAsLti = (smem_chunk_lti) v;
                        value_lti = vAsLti.lti_id;
                        if(value_lti == 0)
                        {
                            value_lti = smem_lti_add_id(vAsLti.lti_letter, vAsLti.lti_number);
                            vAsLti.lti_id = value_lti;
                            
                            if(vAsLti.soar_id != null)
                            {
                                vAsLti.soar_id.smem_lti = value_lti;
                                vAsLti.soar_id.smem_time_id = epmem.getStats().getTime();
                                vAsLti.soar_id.id_smem_valid = epmem.epmem_validation();
                                
                                epmem.epmem_schedule_promotion(vAsLti.soar_id);
                            }
                        }
                        
                        if(remove_old_children)
                        {
                            lti_new.add(new SmemHashIdLongPair(attr_hash, value_lti));
                        }
                        else
                        {
                            // lti_id, attribute_s_id, val_lti
                            db.web_lti_child.setLong(1, lti_id);
                            db.web_lti_child.setLong(2, attr_hash);
                            db.web_lti_child.setLong(3, value_lti);
                            
                            try(ResultSet rs = db.web_lti_child.executeQuery())
                            {
                                if(!rs.next())
                                {
                                    lti_new.add(new SmemHashIdLongPair(attr_hash, value_lti));
                                }
                            }
                        }
                        
                        // provide trace output
                        // snprintf_with_symbols( my_agent, buf, 256,
                        // "=>SMEM: (%y ^%y %y)\n", print_id, s->first,
                        // (*v)->val_lti.val_value->soar_id );
                        trace.startNewLine().print(Category.SMEM, "=>SMEM: (%s ^%s %s)", print_id, s.getKey(), vAsLti.soar_id);
                    }
                }
            }
        }
        
        // activation function assumes proper thresholding state
        // thus, consider four cases of augmentation counts (w.r.t. thresh)
        // 1. before=below, after=below: good (activation will update
        // smem_augmentations)
        // 2. before=below, after=above: need to update smem_augmentations->inf
        // 3. before=after, after=below: good (activation will update
        // smem_augmentations, free transition)
        // 4. before=after, after=after: good (activation won't touch
        // smem_augmentations)
        //
        // hence, we detect + handle case #2 here
        long new_edges = (existing_edges + const_new.size() + lti_new.size());
        boolean after_above;
        double web_act = SMEM_ACT_MAX;
        {
            long thresh = this.params.thresh.get();
            after_above = (new_edges >= thresh);
            
            // if before below
            if(existing_edges < thresh)
            {
                if(after_above)
                {
                    // update smem_augmentations to inf
                    db.act_set.setDouble(1, web_act);
                    db.act_set.setLong(2, lti_id);
                    db.act_set.executeUpdate();
                }
            }
        }
        
        // update edge counter
        {
            db.act_lti_child_ct_set.setLong(1, new_edges);
            db.act_lti_child_ct_set.setLong(2, lti_id);
            db.act_lti_child_ct_set.executeUpdate();
        }
        
        // now we can safely activate the lti
        {
            double lti_act = smem_lti_activate(lti_id, true, new_edges);
            
            if(!after_above)
            {
                web_act = lti_act;
            }
        }
        
        // insert new edges, update counters
        {
            // attr/const pairs
            {
                for(SmemHashIdLongPair pair : const_new)
                {
                    // insert
                    {
                        // lti_id, attribute_s_id, val_const, value_lti_id,
                        // activation_value
                        db.web_add.setLong(1, lti_id);
                        db.web_add.setLong(2, pair.getHashID());
                        db.web_add.setLong(3, pair.getSecond());
                        db.web_add.setLong(4, SMEM_AUGMENTATIONS_NULL);
                        db.web_add.setDouble(5, web_act);
                        
                        db.web_add.executeUpdate();
                    }
                    
                    // update counter
                    {
                        // check if counter exists (and add if does not):
                        // attribute_s_id, val
                        db.wmes_constant_frequency_check.setLong(1, pair.getHashID());
                        db.wmes_constant_frequency_check.setLong(2, pair.getSecond());
                        
                        try(ResultSet rs = db.wmes_constant_frequency_check.executeQuery())
                        {
                            if(!rs.next())
                            {
                                db.wmes_constant_frequency_add.setLong(1, pair.getHashID());
                                db.wmes_constant_frequency_add.setLong(2, pair.getSecond());
                                
                                db.wmes_constant_frequency_add.executeUpdate();
                            }
                            else
                            {
                                // adjust count (adjustment, attribute_s_id,
                                // val)
                                db.wmes_constant_frequency_update.setLong(1, 1);
                                db.wmes_constant_frequency_update.setLong(2, pair.getHashID());
                                db.wmes_constant_frequency_update.setLong(3, pair.getSecond());
                                
                                db.wmes_constant_frequency_update.executeUpdate();
                            }
                        }
                    }
                }
            }
            
            // attr/lti pairs
            {
                for(SmemHashIdLongPair pair : lti_new)
                {
                    // insert
                    {
                        // lti_id, attribute_s_id, val_const, value_lti_id,
                        // activation_value
                        db.web_add.setLong(1, lti_id);
                        db.web_add.setLong(2, pair.getHashID());
                        db.web_add.setLong(3, SMEM_AUGMENTATIONS_NULL);
                        db.web_add.setLong(4, pair.getSecond());
                        db.web_add.setDouble(5, web_act);
                        
                        db.web_add.executeUpdate();
                    }
                    
                    // update counter
                    {
                        // check if counter exists (and add if does not):
                        // attribute_s_id, val
                        db.wmes_lti_frequency_check.setLong(1, pair.getHashID());
                        db.wmes_lti_frequency_check.setLong(2, pair.getSecond());
                        
                        try(ResultSet rs = db.wmes_lti_frequency_check.executeQuery())
                        {
                            if(!rs.next())
                            {
                                db.wmes_lti_frequency_add.setLong(1, pair.getHashID());
                                db.wmes_lti_frequency_add.setLong(2, pair.getSecond());
                                
                                db.wmes_lti_frequency_add.executeUpdate();
                            }
                            else
                            {
                                // adjust count (adjustment, attribute_s_id,
                                // lti)
                                db.wmes_lti_frequency_update.setLong(1, 1);
                                db.wmes_lti_frequency_update.setLong(2, pair.getHashID());
                                db.wmes_lti_frequency_update.setLong(3, pair.getSecond());
                                
                                db.wmes_lti_frequency_update.executeUpdate();
                            }
                        }
                    }
                }
            }
            
            // update attribute count
            {
                for(Long a : attr_new)
                {
                    // check if counter exists (and add if does not):
                    // attribute_s_id
                    db.attribute_frequency_check.setLong(1, a);
                    
                    try(ResultSet rs = db.attribute_frequency_check.executeQuery())
                    {
                        if(!rs.next())
                        {
                            db.attribute_frequency_add.setLong(1, a);
                            
                            db.attribute_frequency_add.executeUpdate();
                        }
                        else
                        {
                            db.attribute_frequency_update.setLong(1, 1);
                            db.attribute_frequency_update.setLong(2, a);
                            
                            db.attribute_frequency_update.executeUpdate();
                        }
                    }
                }
            }
            
            // update local edge count
            {
                this.stats.edges.set(this.stats.edges.get() + (const_new.size() + lti_new.size()));
            }
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:1387:smem_soar_store
     * 
     */
    void smem_soar_store(IdentifierImpl id) throws SQLException, SoarException
    {
        smem_soar_store(id, smem_storage_type.store_level);
    }
    
    /**
     * <p>
     * semantic_memory.cpp:1387:smem_soar_store
     * 
     */
    void smem_soar_store(IdentifierImpl id, smem_storage_type store_type) throws SQLException, SoarException
    {
        smem_soar_store(id, store_type, null);
    }
    
    /**
     * <p>
     * semantic_memory.cpp:1387:smem_soar_store
     * 
     */
    void smem_soar_store(IdentifierImpl id, smem_storage_type store_type /*
                                                                          * =
                                                                          * store_level
                                                                          */, /* tc_number */Marker tc /*
                                                                                                        * =
                                                                                                        * null
                                                                                                        */) throws SQLException, SoarException
    {
        // transitive closure only matters for recursive storage
        if((store_type == smem_storage_type.store_recursive) && (tc == null))
        {
            tc = DefaultMarker.create();
        }
        List<SymbolImpl> shorties = new ArrayList<>();
        
        // get level
        final List<WmeImpl> children = smem_get_direct_augs_of_id(id, tc);
        
        // make the target an lti, so intermediary data structure has lti_id
        // (takes care of short-term id self-referencing)
        smem_lti_soar_add(id);
        
        // encode this level
        {
            final Map<IdentifierImpl, smem_chunk_lti> sym_to_chunk = new LinkedHashMap<>();
            
            final Map<SymbolImpl, List<Object>> slots = smem_chunk_lti.newSlotMap();
            
            for(WmeImpl w : children)
            {
                // get slot
                final List<Object> s = smem_chunk_lti.smem_make_slot(slots, w.attr);
                
                // create value, per type
                final Object v;
                if(w.value.symbol_is_constant())
                {
                    v = w.value;
                }
                else
                {
                    final IdentifierImpl valueId = w.value.asIdentifier();
                    assert valueId != null;
                    
                    // try to find existing chunk
                    smem_chunk_lti c = sym_to_chunk.get(valueId);
                    
                    // if doesn't exist, add; else use existing
                    if(c == null)
                    {
                        final smem_chunk_lti lti = new smem_chunk_lti();
                        lti.lti_id = valueId.smem_lti;
                        lti.lti_letter = valueId.getNameLetter();
                        lti.lti_number = valueId.getNameNumber();
                        lti.slots = null;
                        lti.soar_id = valueId;
                        
                        sym_to_chunk.put(valueId, lti);
                        
                        c = lti;
                        
                        // only traverse to short-term identifiers
                        if((store_type == smem_storage_type.store_recursive) && (c.lti_id == 0))
                        {
                            shorties.add(c.soar_id);
                        }
                    }
                    
                    v = c;
                }
                
                // add value to slot
                s.add(v);
            }
            
            smem_store_chunk(id.smem_lti, slots, true, id);
            
            // clean up
            // Nothing to do in JSoar
        }
        
        // recurse as necessary
        for(SymbolImpl shorty : shorties)
        {
            smem_soar_store(shorty.asIdentifier(), smem_storage_type.store_recursive, tc);
        }
        
        // clean up child wme list
        // delete children;
    }
    
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    // Non-Cue-Based Retrieval Functions (smem::ncb)
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    
    /**
     * <p>
     * semantic_memory.cpp:1494:smem_install_memory
     */
    void smem_install_memory(IdentifierImpl state, long /* smem_lti_id */ lti_id, IdentifierImpl lti, boolean activate_lti, List<SymbolTriple> meta_wmes, List<SymbolTriple> retrieval_wmes)
            throws SQLException
    {
        // //////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->ncb_retrieval->start();
        // //////////////////////////////////////////////////////////////////////////
        
        // get the ^result header for this state
        final SemanticMemoryStateInfo info = smem_info(state);
        final IdentifierImpl result_header = info.smem_result_header;
        
        // get identifier if not known
        boolean lti_created_here = false;
        if(lti == null)
        {
            db.lti_letter_num.setLong(1, lti_id);
            try(ResultSet rs = db.lti_letter_num.executeQuery())
            {
                if(!rs.next())
                {
                    throw new IllegalStateException("Expected non-empty result");
                }
                lti = smem_lti_soar_make(lti_id, (char) rs.getLong(0 + 1), rs.getLong(1 + 1), result_header.level);
            }
            lti_created_here = true;
        }
        
        // activate lti
        if(activate_lti)
        {
            smem_lti_activate(lti_id, true);
        }
        
        // point retrieved to lti
        smem_buffer_add_wme(meta_wmes, result_header, predefinedSyms.smem_sym_retrieved, lti);
        if(lti_created_here)
        {
            // if the identifier was created above we need to remove a single
            // ref count AFTER the wme is added (such as to not deallocate the
            // symbol prematurely)
            // Not needed in JSoar
            // symbol_remove_ref( my_agent, lti );
        }
        
        // if no children, then retrieve children
        // merge may override this behavior
        if((this.params.merge.get() == MergeChoices.add) || ((lti.goalInfo == null || lti.goalInfo.getImpasseWmes() == null) && (lti.getInputWmes() == null) && (lti.slots == null)))
        {
            // get direct children: attr_type, attr_hash, value_type,
            // value_hash, value_letter, value_num, value_lti
            db.web_expand.setLong(1, lti_id);
            try(ResultSet rs = db.web_expand.executeQuery())
            {
                while(rs.next())
                {
                    // make the identifier symbol irrespective of value type
                    final SymbolImpl attr_sym = smem_reverse_hash(rs.getInt(0 + 1), rs.getLong(1 + 1));
                    
                    // identifier vs. constant
                    final SymbolImpl value_sym;
                    final long lti_rs = rs.getLong(6 + 1);
                    if(lti_rs != SMEM_AUGMENTATIONS_NULL)
                    {
                        value_sym = smem_lti_soar_make(lti_rs, (char) rs.getLong(4 + 1), rs.getLong(5 + 1), lti.level);
                    }
                    else
                    {
                        value_sym = smem_reverse_hash(rs.getInt(2 + 1), rs.getLong(3 + 1));
                    }
                    
                    // add wme
                    smem_buffer_add_wme(retrieval_wmes, lti, attr_sym, value_sym);
                    
                    // deal with ref counts - attribute/values are always
                    // created in this function
                    // (thus an extra ref count is set before adding a wme)
                    // Not needed in JSoar
                    // symbol_remove_ref( my_agent, attr_sym );
                    // symbol_remove_ref( my_agent, value_sym );
                }
            }
            
        }
        
        // //////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->ncb_retrieval->stop();
        // //////////////////////////////////////////////////////////////////////////
    }
    
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    // Cue-Based Retrieval Functions (smem::cbr)
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    
    PreparedStatement smem_setup_web_crawl(WeightedCueElement el) throws SQLException
    {
        PreparedStatement q = null;
        
        // first, point to correct query and setup
        // query-specific parameters
        if(el.element_type == smem_cue_element_type.attr_t)
        {
            // attribute_s_id=?
            q = db.web_attr_all;
        }
        else if(el.element_type == smem_cue_element_type.value_const_t)
        {
            // attribute_s_id=? AND value_constant_s_id=?
            q = db.web_const_all;
            q.setLong(2, el.value_hash);
        }
        else if(el.element_type == smem_cue_element_type.value_lti_t)
        {
            q = db.web_lti_all;
            q.setLong(2, el.value_lti);
        }
        
        // all require hash as first parameter
        q.setLong(1, el.attr_hash);
        
        return q;
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2145:_smem_process_cue_wme
     */
    boolean _smem_process_cue_wme(WmeImpl w, boolean pos_cue, PriorityQueue<WeightedCueElement> weighted_pq, MathQuery mathQuery) throws SQLException
    {
        boolean good_wme = true;
        WeightedCueElement new_cue_element;
        
        long /* smem_hash_id */ attr_hash;
        long /* smem_hash_id */ value_hash;
        long /* smem_lti_id */ value_lti;
        smem_cue_element_type element_type = null;
        
        PreparedStatement q = null;
        
        {
            // we only have to do the hard work if
            attr_hash = smem_temporal_hash(w.attr, false);
            if(attr_hash != 0)
            {
                // If there is a math query, we don't want to treat this as a direct match search
                if(w.value.symbol_is_constant() && mathQuery == null)
                {
                    value_lti = 0;
                    value_hash = smem_temporal_hash(w.value, false);
                    element_type = smem_cue_element_type.value_const_t;
                    
                    if(value_hash != 0)
                    {
                        q = db.wmes_constant_frequency_get;
                        q.setLong(1, attr_hash);
                        q.setLong(2, value_hash);
                        
                    }
                    else if(pos_cue)
                    {
                        good_wme = false;
                    }
                    else
                    {
                        // This would be a negative query that smem has no hash for. This means that
                        // there is no way it could be in any of the results, and we don't
                        // need to continue processing it, let alone use it in the search. --ACN
                        return true;
                    }
                }
                else
                {
                    // If we get here on a math query, the value may not be an identifier
                    if(w.value.asIdentifier() != null)
                    {
                        value_lti = w.value.asIdentifier().smem_lti;
                    }
                    else
                    {
                        value_lti = 0;
                    }
                    value_hash = 0;
                    
                    if(value_lti == 0)
                    {
                        q = db.attribute_frequency_get;
                        q.setLong(1, attr_hash);
                        
                        element_type = smem_cue_element_type.attr_t;
                    }
                    else
                    {
                        q = db.wmes_lti_frequency_get;
                        q.setLong(1, attr_hash);
                        q.setLong(2, value_lti);
                        
                        element_type = smem_cue_element_type.value_lti_t;
                    }
                }
                
                if(good_wme)
                {
                    try(ResultSet rs = q.executeQuery())
                    {
                        if(rs.next())
                        {
                            new_cue_element = new WeightedCueElement();
                            
                            new_cue_element.weight = rs.getLong(0 + 1);
                            new_cue_element.attr_hash = attr_hash;
                            new_cue_element.value_hash = value_hash;
                            new_cue_element.value_lti = value_lti;
                            new_cue_element.cue_element = w;
                            
                            new_cue_element.element_type = element_type;
                            new_cue_element.pos_element = pos_cue;
                            new_cue_element.mathElement = mathQuery;
                            
                            weighted_pq.add(new_cue_element);
                            new_cue_element = null;
                        }
                        else
                        {
                            if(pos_cue)
                            {
                                good_wme = false;
                            }
                        }
                    }
                }
            }
            else
            {
                if(pos_cue)
                {
                    good_wme = false;
                }
            }
        }
        
        return good_wme;
    }
    
    private static class MathQueryProcessResults
    {
        public boolean needFullSearch;
        public boolean goodCue;
        
        public MathQueryProcessResults(boolean needFullSearch, boolean goodCue)
        {
            this.needFullSearch = needFullSearch;
            this.goodCue = goodCue;
        }
    }
    
    private MathQueryProcessResults processMathQuery(IdentifierImpl mathQuery, PriorityQueue<WeightedCueElement> weighted_pq) throws SQLException
    {
        boolean needFullSearch = false;
        // Use this set to track when certain elements have been added, so we don't add them twice
        Set<Symbol> uniqueMathQueryElements = new HashSet<>();
        List<WmeImpl> cue = smem_get_direct_augs_of_id(mathQuery);
        for(WmeImpl cue_p : cue)
        {
            List<WmeImpl> cueTypes = smem_get_direct_augs_of_id(cue_p.value);
            if(cueTypes.isEmpty())
            {
                // This would be an attribute without a query type attached
                return new MathQueryProcessResults(false, false);
            }
            
            for(WmeImpl cueType : cueTypes)
            {
                if(cueType.attr == predefinedSyms.smem_sym_max)
                {
                    if(uniqueMathQueryElements.contains(predefinedSyms.smem_sym_max))
                    {
                        return new MathQueryProcessResults(false, false);
                    }
                    else
                    {
                        uniqueMathQueryElements.add(predefinedSyms.smem_sym_max);
                    }
                    needFullSearch = true;
                    _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryMax());
                    
                }
                else if(cueType.attr == predefinedSyms.smem_sym_min)
                {
                    if(uniqueMathQueryElements.contains(predefinedSyms.smem_sym_min))
                    {
                        return new MathQueryProcessResults(false, false);
                    }
                    else
                    {
                        uniqueMathQueryElements.add(predefinedSyms.smem_sym_min);
                    }
                    needFullSearch = true;
                    _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryMin());
                    
                }
                else if(cueType.attr == predefinedSyms.smem_sym_less)
                {
                    if(cueType.value.asDouble() != null)
                    {
                        _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryLess(cueType.value.asDouble().getValue()));
                    }
                    else if(cueType.value.asInteger() != null)
                    {
                        _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryLess(cueType.value.asInteger().getValue()));
                    }
                    else
                    {
                        // There isn't a valid value to compare against
                        return new MathQueryProcessResults(false, false);
                    }
                }
                else if(cueType.attr == predefinedSyms.smem_sym_greater)
                {
                    if(cueType.value.asDouble() != null)
                    {
                        _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryGreater(cueType.value.asDouble().getValue()));
                    }
                    else if(cueType.value.asInteger() != null)
                    {
                        _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryGreater(cueType.value.asInteger().getValue()));
                    }
                    else
                    {
                        // There isn't a valid value to compare against
                        return new MathQueryProcessResults(false, false);
                    }
                }
                else if(cueType.attr == predefinedSyms.smem_sym_less_or_equal)
                {
                    if(cueType.value.asDouble() != null)
                    {
                        _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryLessOrEqual(cueType.value.asDouble().getValue()));
                    }
                    else if(cueType.value.asInteger() != null)
                    {
                        _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryLessOrEqual(cueType.value.asInteger().getValue()));
                    }
                    else
                    {
                        // There isn't a valid value to compare against
                        return new MathQueryProcessResults(false, false);
                    }
                }
                else if(cueType.attr == predefinedSyms.smem_sym_greater_or_equal)
                {
                    if(cueType.value.asDouble() != null)
                    {
                        _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryGreaterOrEqual(cueType.value.asDouble().getValue()));
                    }
                    else if(cueType.value.asInteger() != null)
                    {
                        _smem_process_cue_wme(cue_p, true, weighted_pq, new MathQueryGreaterOrEqual(cueType.value.asInteger().getValue()));
                    }
                    else
                    {
                        // There isn't a valid value to compare against
                        return new MathQueryProcessResults(false, false);
                    }
                }
            }
            
        }
        return new MathQueryProcessResults(needFullSearch, true);
    }
    
    long /* smem_lti_id */ smem_process_query(IdentifierImpl state, IdentifierImpl query, IdentifierImpl negquery, IdentifierImpl math, Set<Long> /* smem_lti_set */ prohibit, Set<WmeImpl> cue_wmes,
            List<SymbolTriple> meta_wmes, List<SymbolTriple> retrieval_wmes)
            throws SQLException
    {
        return smem_process_query(state, query, negquery, math, prohibit, cue_wmes, meta_wmes, retrieval_wmes, smem_query_levels.qry_full);
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2246:smem_process_query
     */
    long /* smem_lti_id */ smem_process_query(IdentifierImpl state, IdentifierImpl query, IdentifierImpl negquery, IdentifierImpl mathQuery, Set<Long> /* smem_lti_set */ prohibit,
            Set<WmeImpl> cue_wmes, List<SymbolTriple> meta_wmes, List<SymbolTriple> retrieval_wmes,
            smem_query_levels query_level) throws SQLException
    {
        final SemanticMemoryStateInfo smem_info = smem_info(state);
        final List<WeightedCueElement> weighted_cue = new ArrayList<>();
        boolean good_cue = true;
        
        // This is used when doing math queries that need to look at more that just the first valid element
        boolean needFullSearch = false;
        
        long /* smem_lti_id */ king_id = 0;
        
        // //////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->query->start();
        // //////////////////////////////////////////////////////////////////////////
        
        // prepare query stats
        {
            final PriorityQueue<WeightedCueElement> weighted_pq = WeightedCueElement.newPriorityQueue();
            
            // positive que - always
            {
                List<WmeImpl> cue = smem_get_direct_augs_of_id(query);
                if(cue.isEmpty())
                {
                    good_cue = false;
                }
                
                // for ( smem_wme_list::iterator cue_p=cue->begin();
                // cue_p!=cue->end(); cue_p++ )
                for(WmeImpl cue_p : cue)
                {
                    cue_wmes.add(cue_p);
                    
                    if(good_cue)
                    {
                        good_cue = _smem_process_cue_wme(cue_p, true, weighted_pq, null);
                    }
                }
            }
            
            // Look through while were here, so that we can make sure the attributes we need are in the results
            if(mathQuery != null && good_cue)
            {
                MathQueryProcessResults mpr = processMathQuery(mathQuery, weighted_pq);
                good_cue = mpr.goodCue;
                needFullSearch = mpr.needFullSearch;
            }
            
            // negative que - if present
            if(negquery != null)
            {
                List<WmeImpl> cue = smem_get_direct_augs_of_id(negquery);
                
                // for ( smem_wme_list::iterator cue_p=cue->begin();
                // cue_p!=cue->end(); cue_p++ )
                for(WmeImpl cue_p : cue)
                {
                    cue_wmes.add(cue_p);
                    
                    if(good_cue)
                    {
                        good_cue = _smem_process_cue_wme(cue_p, false, weighted_pq, null);
                    }
                }
            }
            
            // if valid cue, transfer priority queue to list
            if(good_cue)
            {
                while(!weighted_pq.isEmpty())
                {
                    weighted_cue.add(weighted_pq.remove()); // top()/pop()
                }
            }
            // else deallocate priority queue contents
            else
            {
                while(!weighted_pq.isEmpty())
                {
                    weighted_pq.remove(); // top()/pop()
                }
            }
            
            // clean cue irrespective of validity
            // delete cue;
        }
        
        // only search if the cue was valid
        if(good_cue && !weighted_cue.isEmpty())
        {
            // by definition, the first positive-cue element dictates the
            // candidate set
            WeightedCueElement cand_set = null;
            
            for(WeightedCueElement next_element : weighted_cue)
            {
                if(next_element.pos_element)
                {
                    cand_set = next_element;
                    break;
                }
            }
            
            PreparedStatement q = null;
            PreparedStatement q2 = null;
            
            long /* smem_lti_id */ cand;
            boolean good_cand;
            
            if(params.activation_mode.get() == ActivationChoices.base_level)
            {
                // naive base-level updates means update activation of
                // every candidate in the minimal list before the
                // confirmation walk
                if(params.base_update.get() == BaseUpdateChoices.naive)
                {
                    q = smem_setup_web_crawl(cand_set);
                    
                    // queue up distinct lti's to update
                    // - set because queries could contain wilds
                    // - not in loop because the effects of activation may
                    // actually
                    // alter the resultset of the query (isolation???)
                    Set<Long /* smem_lti_id */> to_update = new LinkedHashSet<>();
                    
                    try(ResultSet rs = q.executeQuery())
                    {
                        while(rs.next())
                        {
                            to_update.add(rs.getLong(0 + 1));
                        }
                    }
                    
                    for(Long lti : to_update)
                    {
                        smem_lti_activate(lti, false);
                    }
                }
            }
            
            // setup first query, which is sorted on activation already
            q = smem_setup_web_crawl(cand_set);
            lastCue = new BasicWeightedCue(cand_set.cue_element, cand_set.weight);
            
            // this becomes the minimal set to walk (till match or fail)
            try(ResultSet qrs = q.executeQuery())
            {
                if(qrs.next())
                {
                    final PriorityQueue<ActivatedLti> plentiful_parents = ActivatedLti.newPriorityQueue();
                    boolean more_rows = true;
                    boolean use_db = false;
                    boolean has_feature = false;
                    
                    while(more_rows && (qrs.getDouble(1 + 1) == SMEM_ACT_MAX))
                    {
                        db.act_lti_get.setLong(1, qrs.getLong(0 + 1));
                        try(ResultSet actLtiGetRs = db.act_lti_get.executeQuery())
                        {
                            if(!actLtiGetRs.next())
                            {
                                throw new IllegalStateException("act_lti_get did not return a result");
                            }
                            plentiful_parents.add(new ActivatedLti(actLtiGetRs.getLong(0 + 1), qrs.getLong(0 + 1)));
                        }
                        // my_agent->smem_stmts->act_lti_get->reinitialize();
                        
                        more_rows = qrs.next(); // ( q->execute() ==
                                                // soar_module::row );
                    }
                    
                    while(((king_id == 0) || (needFullSearch)) && ((more_rows) || (!plentiful_parents.isEmpty())))
                    {
                        // choose next candidate (db vs. priority queue)
                        {
                            use_db = false;
                            
                            if(!more_rows)
                            {
                                use_db = false;
                            }
                            else if(plentiful_parents.isEmpty())
                            {
                                use_db = true;
                            }
                            else
                            {
                                use_db = (qrs.getDouble(1 + 1) > plentiful_parents.peek().first);
                            }
                            
                            if(use_db)
                            {
                                cand = qrs.getLong(0 + 1);
                                more_rows = qrs.next(); // ( q->execute() ==
                                                        // soar_module::row );
                            }
                            else
                            {
                                cand = plentiful_parents.remove().second; // top()/pop()
                            }
                        }
                        
                        // if not prohibited, submit to the remaining cue
                        // elements
                        if(!prohibit.contains(cand))
                        {
                            good_cand = true;
                            
                            final Iterator<WeightedCueElement> it = weighted_cue.iterator();
                            for(; ((good_cand) && (it.hasNext()));)
                            {
                                final WeightedCueElement next_element = it.next();
                                
                                // If the cand_set is a math query, we care about more than its existence
                                if(next_element == cand_set && next_element.mathElement == null)
                                {
                                    continue;
                                }
                                
                                if(next_element.element_type == smem_cue_element_type.attr_t)
                                {
                                    // parent=? AND attribute_s_id=?
                                    q2 = db.web_attr_child;
                                }
                                else if(next_element.element_type == smem_cue_element_type.value_const_t)
                                {
                                    // parent=? AND attribute_s_id=? AND
                                    // value_constant_s_id=?
                                    q2 = db.web_const_child;
                                    q2.setLong(3, next_element.value_hash);
                                }
                                else if(next_element.element_type == smem_cue_element_type.value_lti_t)
                                {
                                    // parent=? AND attribute_s_id=? AND
                                    // value_lti_id=?
                                    q2 = db.web_lti_child;
                                    q2.setLong(3, next_element.value_lti);
                                }
                                
                                // all require own id, attribute
                                q2.setLong(1, cand);
                                q2.setLong(2, next_element.attr_hash);
                                
                                try(ResultSet q2rs = q2.executeQuery())
                                {
                                    has_feature = q2rs.next();
                                    boolean mathQueryMet = false;
                                    if(next_element.mathElement != null && has_feature)
                                    {
                                        do
                                        {
                                            long valueHash = q2rs.getLong(2);
                                            db.hash_rev_type.setLong(1, valueHash);
                                            try(ResultSet rs = db.hash_rev_type.executeQuery())
                                            {
                                                // If this hash wasn't in the table, there isn't a value to work with
                                                if(!rs.next())
                                                {
                                                    good_cand = false;
                                                }
                                                else
                                                {
                                                    int valueType = rs.getInt(1);
                                                    // Not using the built in hash reverse because I want the raw type, not the symbol
                                                    switch(valueType)
                                                    {
                                                    case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                                        mathQueryMet |= next_element.mathElement.valueIsAcceptable(smem_reverse_hash_float(valueHash));
                                                        break;
                                                    case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                                        mathQueryMet |= next_element.mathElement.valueIsAcceptable(smem_reverse_hash_int(valueHash));
                                                        break;
                                                    }
                                                }
                                            }
                                            // Go through the all the attribute records, to find the best match for a math query
                                        } while(q2rs.next());
                                        good_cand = mathQueryMet;
                                    }
                                    else
                                    {
                                        good_cand = ((next_element.pos_element) ? (has_feature) : (!has_feature));
                                    }
                                    if(!good_cand)
                                    {
                                        break;
                                    }
                                }
                            }
                            
                            if(good_cand)
                            {
                                king_id = cand;
                                for(WeightedCueElement wce : weighted_cue)
                                {
                                    if(wce.mathElement != null)
                                    {
                                        wce.mathElement.commit();
                                    }
                                }
                            }
                            else
                            {
                                for(WeightedCueElement wce : weighted_cue)
                                {
                                    if(wce.mathElement != null)
                                    {
                                        wce.mathElement.rollback();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // q->reinitialize();
            
            // clean weighted cue
            // Not needed in JSoar
            // for ( next_element=weighted_cue.begin();
            // next_element!=weighted_cue.end(); next_element++ )
            // {
            // delete (*next_element);
            // }
        }
        
        // reconstruction depends upon level
        if(query_level == smem_query_levels.qry_full)
        {
            // produce results
            if(king_id != 0)
            {
                // success!
                smem_buffer_add_wme(meta_wmes, smem_info.smem_result_header, predefinedSyms.smem_sym_success, query);
                
                if(negquery != null)
                {
                    smem_buffer_add_wme(meta_wmes, smem_info.smem_result_header, predefinedSyms.smem_sym_success, negquery);
                }
                
                // //////////////////////////////////////////////////////////////////////////
                // TODO SMEM Timers: my_agent->smem_timers->query->stop();
                // //////////////////////////////////////////////////////////////////////////
                
                smem_install_memory(state, king_id, null, params.activate_on_query.get() == ActivateOnQueryChoices.on, meta_wmes, retrieval_wmes);
            }
            else
            {
                smem_buffer_add_wme(meta_wmes, smem_info.smem_result_header, predefinedSyms.smem_sym_failure, query);
                
                if(negquery != null)
                {
                    smem_buffer_add_wme(meta_wmes, smem_info.smem_result_header, predefinedSyms.smem_sym_failure, negquery);
                }
                
                // //////////////////////////////////////////////////////////////////////////
                // TODO SMEM Timers: my_agent->smem_timers->query->stop();
                // //////////////////////////////////////////////////////////////////////////
            }
        }
        else
        {
            // //////////////////////////////////////////////////////////////////////////
            // TODO SMEM Timers: my_agent->smem_timers->query->stop();
            // //////////////////////////////////////////////////////////////////////////
        }
        
        return king_id;
    }
    
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    // Initialization (smem::init)
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    
    /**
     * <p>
     * semantic_memory.cpp:1892:smem_clear_result
     */
    void smem_clear_result(IdentifierImpl state)
    {
        final SemanticMemoryStateInfo smem_info = smem_info(state);
        while(!smem_info.smem_wmes.isEmpty())
        {
            final Preference pref = smem_info.smem_wmes.remove(); // top()/pop()
            
            if(pref.isInTempMemory())
            {
                recMem.remove_preference_from_tm(pref);
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jsoar.kernel.smem.SemanticMemory#smem_reset(org.jsoar.kernel.symbols
     * .IdentifierImpl)
     */
    @Override
    public void smem_reset(IdentifierImpl state)
    {
        // semantic_memory.cpp:1913:smem_reset
        if(state == null)
        {
            state = decider.top_goal;
        }
        
        while(state != null)
        {
            final SemanticMemoryStateInfo data = stateInfos.remove(state);
            
            // TODO The line above should be sufficient. Nothing else should be
            // necessary.
            data.last_cmd_time[0] = 0;
            data.last_cmd_time[1] = 0;
            data.last_cmd_count[0] = 0;
            data.last_cmd_count[1] = 0;
            
            // this will be called after prefs from goal are already removed,
            // so just clear out result stack
            data.smem_wmes.clear();
            
            state = state.goalInfo.lower_goal;
        }
    }
    
    void smem_switch_to_memory_db(String buf) throws SoarException, SQLException, IOException
    {
        trace.print(buf);
        params.path.set(SemanticMemoryDatabase.IN_MEMORY_PATH);
        db.getConnection().close();
        db = null;
        smem_init_db(false);
    }
    
    /**
     * Opens the SQLite database and performs all initialization required for
     * the current mode
     * 
     * <p>
     * semantic_memory.cpp:1952:smem_init_db
     */
    void smem_init_db() throws SoarException, SQLException, IOException
    {
        smem_init_db(false);
    }
    
    /**
     * Opens the SQLite database and performs all initialization required for
     * the current mode
     * 
     * The readonly param should only be used in experimentation where you don't
     * want to alter previous database state.
     * 
     * <p>
     * semantic_memory.cpp:1952:smem_init_db
     * 
     */
    void smem_init_db(boolean readonly /* = false */) throws SoarException, SQLException, IOException
    {
        if(db != null /*
                       * my_agent->smem_db->get_status() !=
                       * soar_module::disconnected
                       */)
        {
            return;
        }
        
        // //////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers my_agent->smem_timers->init->start();
        // //////////////////////////////////////////////////////////////////////////
        
        // attempt connection
        final String jdbcUrl = URLDecoder.decode(params.protocol.get() + ":" + params.path.get(), "UTF-8");
        final Connection connection = JdbcTools.connect(params.driver.get(), jdbcUrl);
        final DatabaseMetaData meta = connection.getMetaData();
        
        LOG.info("Opened database '{}' with {}:{},", jdbcUrl, meta.getDriverName(), meta.getDriverVersion());
        
        if(params.path.get().equals(SemanticMemoryDatabase.IN_MEMORY_PATH))
        {
            trace.print(Category.SMEM, "SMem| Initializing semantic memory database in cpu memory.\n");
        }
        else
        {
            trace.print(Category.SMEM, "SMem| Initializing semantic memory memory database at %s\n", params.path.get());
        }
        
        db = new SemanticMemoryDatabase(params.driver.get(), connection);
        
        // temporary queries for one-time init actions
        
        applyDatabasePerformanceOptions();
        
        // update validation count
        smem_validation++;
        
        // setup common structures/queries
        final boolean tabula_rasa = db.structure();
        db.prepare();
        
        // Make sure we do not have an incorrect database version
        if(!SemanticMemoryDatabase.IN_MEMORY_PATH.equals(params.path.get()))
        {
            try(ResultSet result = db.get_schema_version.executeQuery())
            {
                if(result.next())
                {
                    String schemaVersion = result.getString(1);
                    if(!SemanticMemoryDatabase.SMEM_SCHEMA_VERSION.equals(schemaVersion))
                    {
                        LOG.error("Incorrect database version, switching to memory.  Found version: {}", schemaVersion);
                        params.path.set(SemanticMemoryDatabase.IN_MEMORY_PATH);
                        // Switch to memory
                        // Undo what was done so far
                        connection.close();
                        db = null;
                        // This will only recurse once, because the path is
                        // guaranteed to be memory for the second call
                        smem_init_db(readonly);
                    }
                }
                else
                {
                    if(params.append_db.get() == AppendDatabaseChoices.on)
                    {
                        LOG.info("The selected database contained no data to append on.  New tables created.");
                    }
                }
            }
        }
        db.set_schema_version.setString(1, SemanticMemoryDatabase.SMEM_SCHEMA_VERSION);
        db.set_schema_version.execute();
        /*
         * This is used to rebuild ONLY the epmem tables. Unfortunately we cannot build the
         * prepared statements without making sure the tables exist, but we cannot drop the new
         * tables without first building the prepared statements.
         * TODO: Maybe we should bypass the reflected query structure so this can be done in
         * one statement, instead of building the tables and immediately dropping them. -ACN
         */
        if(params.append_db.get() == AppendDatabaseChoices.off)
        {
            db.dropSmemTables();
            db.structure();
            db.prepare();
        }
        
        if(tabula_rasa)
        {
            db.beginExecuteUpdate( /* soar_module::op_reinit */);
            {
                smem_max_cycle = 1;
                smem_variable_create(smem_variable_key.var_max_cycle, smem_max_cycle);
                
                stats.nodes.set(0L);
                smem_variable_create(smem_variable_key.var_num_nodes, stats.nodes.get());
                
                stats.edges.set(0L);
                smem_variable_create(smem_variable_key.var_num_edges, stats.edges.get());
                
                smem_variable_create(smem_variable_key.var_act_thresh, params.thresh.get());
                
                smem_variable_create(smem_variable_key.var_act_mode, params.activation_mode.get().ordinal());
            }
            db.commitExecuteUpdate();
        }
        else
        {
            final ByRef<Long> tempMaxCycle = ByRef.create(smem_max_cycle);
            smem_variable_get(smem_variable_key.var_max_cycle, tempMaxCycle);
            smem_max_cycle = tempMaxCycle.value;
            
            final ByRef<Long> temp = ByRef.create(0L);
            
            // threshold
            smem_variable_get(smem_variable_key.var_act_thresh, temp);
            params.thresh.set(temp.value);
            
            // nodes
            smem_variable_get(smem_variable_key.var_num_nodes, temp);
            stats.nodes.set(temp.value);
            
            // edges
            smem_variable_get(smem_variable_key.var_num_edges, temp);
            stats.edges.set(temp.value);
            
            // activiation mode
            smem_variable_get(smem_variable_key.var_act_mode, temp);
            params.activation_mode.set(ActivationChoices.values()[Integer.parseInt(temp.value.toString())]);
        }
        
        // reset identifier counters
        smem_reset_id_counters();
        
        // if lazy commit, then we encapsulate the entire lifetime of the agent
        // in a single transaction
        if(params.lazy_commit.get() == LazyCommitChoices.on)
        {
            db.beginExecuteUpdate( /* soar_module::op_reinit */);
        }
        
        // //////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->init->stop();
        // TODO SMEM Timers: do this in finally for exception handling above
        // //////////////////////////////////////////////////////////////////////////
    }
    
    /**
     * Extracted from smem_init_db(). Take performance settings and apply then
     * to the current database.
     * 
     * <p>
     * semantic_memory.cpp:1952:smem_init_db
     * 
     */
    private void applyDatabasePerformanceOptions() throws SQLException, SoarException, IOException
    {
        // cache
        if(params.driver.get().equals("org.sqlite.JDBC"))
        {
            // TODO: Generalize this. Move to a resource somehow.
            final long cacheSize = params.cache_size.get();
            
            try(Statement s = db.getConnection().createStatement())
            {
                s.execute("PRAGMA cache_size = " + cacheSize);
            }
        }
        
        // optimization
        if(params.optimization.get() == Optimization.performance)
        {
            // If /org/jsoar/kernel/smem/<driver>.performance.sql is found on
            // the class path, execute the statements in it.
            final String perfResource = params.driver.get() + ".performance.sql";
            final String fullPath = "/" + getClass().getCanonicalName().replace('.', '/') + "/" + perfResource;
            
            LOG.info("Applying performance settings from '{}'.", fullPath);
            try(InputStream perfStream = getClass().getResourceAsStream(perfResource);)
            {
                if(perfStream != null)
                {
                    JdbcTools.executeSql(db.getConnection(), perfStream, null /*
                                                                               * no
                                                                               * filter
                                                                               */);
                }
                else
                {
                    LOG.warn("Could not find performance resource at '{}'. No performance settings applied.", fullPath);
                }
            }
        }
        
        // page_size
        if(params.driver.get().equals("org.sqlite.JDBC"))
        {
            final PageChoices pageSize = params.page_size.get();
            
            long pageSizeLong = 0;
            
            switch(pageSize)
            {
            case page_16k:
                pageSizeLong = 16 * 1024L;
                break;
            case page_1k:
                pageSizeLong = 1 * 1024L;
                break;
            case page_2k:
                pageSizeLong = 2 * 1024L;
                break;
            case page_32k:
                pageSizeLong = 32 * 1024L;
                break;
            case page_4k:
                pageSizeLong = 4 * 1024L;
                break;
            case page_64k:
                pageSizeLong = 64 * 1024L;
                break;
            case page_8k:
                pageSizeLong = 8 * 1024L;
                break;
            default:
                break;
            }
            
            try(Statement s = db.getConnection().createStatement())
            {
                s.execute("PRAGMA page_size = " + pageSizeLong);
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_attach()
     */
    @Override
    public void smem_attach() throws SoarException
    {
        // semantic_memory.cpp:2112:smem_attach
        if(db == null)
        {
            try
            {
                smem_init_db();
            }
            catch(SQLException | IOException e)
            {
                throw new SoarException("While attaching SMEM: " + e.getMessage(), e);
            }
        }
    }
    
    void _smem_close_vars() throws SQLException
    {
        // store max cycle for future use of the smem database
        smem_variable_set(smem_variable_key.var_max_cycle, this.smem_max_cycle);
        
        // store num nodes/edges for future use of the smem database
        smem_variable_set(smem_variable_key.var_num_nodes, stats.nodes.get());
        smem_variable_set(smem_variable_key.var_num_edges, stats.edges.get());
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_close()
     */
    @Override
    public void smem_close() throws SoarException
    {
        // semantic_memory.cpp:2126:smem_close
        if(db != null)
        {
            try
            {
                _smem_close_vars();
                
                // if lazy, commit
                if(params.lazy_commit.get() == LazyCommitChoices.on)
                {
                    db.commitExecuteUpdate( /* soar_module::op_reinit */);
                }
                
                // close the database
                db.getConnection().close();
                db = null;
            }
            catch(SQLException e)
            {
                throw new SoarException("While closing SMEM: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2158:smem_deallocate_chunk
     */
    static void smem_deallocate_chunk(smem_chunk_lti chunk)
    {
        smem_deallocate_chunk(chunk, true);
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2158:smem_deallocate_chunk
     */
    static void smem_deallocate_chunk(smem_chunk_lti chunk, boolean free_chunk /*
                                                                                * =
                                                                                * true
                                                                                */)
    {
        // Nothing to do in JSoar. Yay!
        if(chunk != null)
        {
            chunk.slots = null;
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2217:smem_parse_lti_name
     * 
     */
    static ParsedLtiName smem_parse_lti_name(Lexeme lexeme)
    {
        if(lexeme.type == LexemeType.IDENTIFIER)
        {
            return new ParsedLtiName(String.format("%c%d", lexeme.id_letter, lexeme.id_number), lexeme.id_letter, lexeme.id_number);
        }
        else
        {
            return new ParsedLtiName(lexeme.string, Character.toUpperCase(lexeme.string.charAt(1)), 0);
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2243:smem_parse_constant_attr
     * 
     */
    static SymbolImpl smem_parse_constant_attr(SymbolFactoryImpl syms, Lexeme lexeme)
    {
        final SymbolImpl return_val;
        
        if((lexeme.type == LexemeType.SYM_CONSTANT))
        {
            return_val = syms.createString(lexeme.string);
        }
        else if(lexeme.type == LexemeType.INTEGER)
        {
            return_val = syms.createInteger(lexeme.int_val);
        }
        else if(lexeme.type == LexemeType.FLOAT)
        {
            return_val = syms.createDouble(lexeme.float_val);
        }
        else
        {
            return_val = null;
        }
        
        return return_val;
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2263:smem_parse_chunk
     * 
     */
    static boolean smem_parse_chunk(SymbolFactoryImpl symbols, Lexer lexer, Map<String, smem_chunk_lti> chunks, Set<smem_chunk_lti> newbies) throws IOException
    {
        boolean return_val = false;
        smem_chunk_lti new_chunk = null;
        boolean good_at = false;
        ParsedLtiName chunk_name = null;
        //
        
        // consume left paren
        lexer.getNextLexeme();
        
        if((lexer.getCurrentLexeme().type == LexemeType.AT) || (lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER) || (lexer.getCurrentLexeme().type == LexemeType.VARIABLE))
        {
            good_at = true;
            
            if(lexer.getCurrentLexeme().type == LexemeType.AT)
            {
                lexer.getNextLexeme();
                
                good_at = (lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER);
            }
            
            if(good_at)
            {
                // save identifier
                chunk_name = smem_parse_lti_name(lexer.getCurrentLexeme());
                new_chunk = new smem_chunk_lti();
                new_chunk.lti_letter = chunk_name.id_letter;
                new_chunk.lti_number = chunk_name.id_number;
                new_chunk.lti_id = 0;
                new_chunk.soar_id = null;
                new_chunk.slots = smem_chunk_lti.newSlotMap();
                
                // consume id
                lexer.getNextLexeme();
                
                //
                
                long intermediate_counter = 1;
                smem_chunk_lti intermediate_parent;
                
                // populate slots
                while(lexer.getCurrentLexeme().type == LexemeType.UP_ARROW)
                {
                    intermediate_parent = new_chunk;
                    
                    // go on to attribute
                    lexer.getNextLexeme();
                    
                    // get the appropriate constant type
                    SymbolImpl chunk_attr = smem_parse_constant_attr(symbols, lexer.getCurrentLexeme());
                    
                    // if constant attribute, proceed to value
                    if(chunk_attr != null)
                    {
                        // consume attribute
                        lexer.getNextLexeme();
                        
                        // support for dot notation:
                        // when we encounter a dot, instantiate
                        // the previous attribute as a temporary
                        // identifier and use that as the parent
                        while(lexer.getCurrentLexeme().type == LexemeType.PERIOD)
                        {
                            // create a new chunk
                            final smem_chunk_lti temp_chunk = new smem_chunk_lti();
                            temp_chunk.lti_letter = chunk_attr.asString() != null ? chunk_attr.getFirstLetter() : 'X';
                            temp_chunk.lti_number = (intermediate_counter++);
                            temp_chunk.lti_id = 0;
                            temp_chunk.slots = smem_chunk_lti.newSlotMap();
                            temp_chunk.soar_id = null;
                            
                            // add it as a child to the current parent
                            final List<Object> s = smem_chunk_lti.smem_make_slot(intermediate_parent.slots, chunk_attr);
                            s.add(temp_chunk);
                            
                            // create a key guaranteed to be unique
                            final String temp_key = String.format("<%c#%d>", temp_chunk.lti_letter, temp_chunk.lti_number);
                            
                            // insert the new chunk
                            chunks.put(temp_key, temp_chunk);
                            
                            // definitely a new chunk
                            newbies.add(temp_chunk);
                            
                            // the new chunk is our parent for this set of
                            // values (or further dots)
                            intermediate_parent = temp_chunk;
                            
                            // get the next attribute
                            lexer.getNextLexeme();
                            chunk_attr = smem_parse_constant_attr(symbols, lexer.getCurrentLexeme());
                            
                            // consume attribute
                            lexer.getNextLexeme();
                        }
                        
                        if(chunk_attr != null)
                        {
                            Object chunk_value = null;
                            do
                            {
                                chunk_value = null;
                                // value by type
                                if(lexer.getCurrentLexeme().type == LexemeType.SYM_CONSTANT)
                                {
                                    chunk_value = symbols.createString(lexer.getCurrentLexeme().string);
                                }
                                else if(lexer.getCurrentLexeme().type == LexemeType.INTEGER)
                                {
                                    chunk_value = symbols.createInteger(lexer.getCurrentLexeme().int_val);
                                }
                                else if(lexer.getCurrentLexeme().type == LexemeType.FLOAT)
                                {
                                    chunk_value = symbols.createDouble(lexer.getCurrentLexeme().float_val);
                                }
                                else if((lexer.getCurrentLexeme().type == LexemeType.AT) || (lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER)
                                        || (lexer.getCurrentLexeme().type == LexemeType.VARIABLE))
                                {
                                    // @ must be followed by an identifier
                                    good_at = true;
                                    if(lexer.getCurrentLexeme().type == LexemeType.AT)
                                    {
                                        lexer.getNextLexeme();
                                        
                                        good_at = (lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER);
                                    }
                                    
                                    if(good_at)
                                    {
                                        // get key
                                        final ParsedLtiName temp_key2 = smem_parse_lti_name(lexer.getCurrentLexeme());
                                        
                                        // search for an existing chunk
                                        final smem_chunk_lti p = chunks.get(temp_key2.value);
                                        
                                        // if exists, point; else create new
                                        if(p != null)
                                        {
                                            chunk_value = p;
                                        }
                                        else
                                        {
                                            // create new chunk
                                            final smem_chunk_lti temp_chunk = new smem_chunk_lti();
                                            temp_chunk.lti_letter = temp_key2.id_letter;
                                            temp_chunk.lti_number = temp_key2.id_number;
                                            temp_chunk.lti_id = 0;
                                            temp_chunk.slots = null;
                                            temp_chunk.soar_id = null;
                                            
                                            // add to chunks
                                            chunks.put(temp_key2.value, temp_chunk);
                                            
                                            // possibly a newbie (could be a
                                            // self-loop)
                                            newbies.add(temp_chunk);
                                            
                                            chunk_value = temp_chunk;
                                        }
                                    }
                                }
                                
                                if(chunk_value != null)
                                {
                                    // consume
                                    lexer.getNextLexeme();
                                    
                                    // add to appropriate slot
                                    final List<Object> s = smem_chunk_lti.smem_make_slot(intermediate_parent.slots, chunk_attr);
                                    s.add(chunk_value);
                                    
                                    // if this was the last attribute
                                    if(lexer.getCurrentLexeme().type == LexemeType.R_PAREN)
                                    {
                                        return_val = true;
                                        lexer.getNextLexeme();
                                        chunk_value = null;
                                    }
                                }
                            } while(chunk_value != null);
                        }
                    }
                }
            }
            else
            {
                // delete new_chunk;
            }
        }
        else
        {
            // delete new_chunk;
        }
        
        if(return_val)
        {
            // search for an existing chunk (occurs if value comes before id)
            final smem_chunk_lti p = chunks.get(chunk_name.value);
            
            if(p == null)
            {
                chunks.put(chunk_name.value, new_chunk);
                
                // a newbie!
                newbies.add(new_chunk);
            }
            else
            {
                // transfer slots
                if(p.slots == null)
                {
                    // if none previously, can just use
                    p.slots = new_chunk.slots;
                }
                else
                {
                    // otherwise, copy
                    for(Map.Entry<SymbolImpl, List<Object>> ss_p : new_chunk.slots.entrySet())
                    {
                        final List<Object> target_slot = smem_chunk_lti.smem_make_slot(p.slots, ss_p.getKey());
                        final List<Object> source_slot = ss_p.getValue();
                        
                        // Copy from source to target
                        target_slot.addAll(source_slot);
                        // for all values in the slot
                        // for(smem_chunk_value s_p : source_slot)
                        // {
                        // // copy each value
                        // target_slot.add(s_p);
                        // }
                    }
                    
                }
                
                // we no longer need the slots
                new_chunk.slots = null;
                
                // contents are new
                newbies.add(p);
                
                // deallocate
                smem_deallocate_chunk(new_chunk);
            }
        }
        else
        {
            newbies.clear();
        }
        
        return return_val;
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2564:smem_parse_chunks
     * 
     */
    boolean smem_parse_chunks(String chunkString) throws SoarException
    {
        try
        {
            return smem_parse_chunks_safe(chunkString);
        }
        catch(IOException | SQLException e)
        {
            throw new SoarException(e);
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2564:smem_parse_chunks
     * 
     */
    private boolean smem_parse_chunks_safe(String chunkString) throws SoarException, IOException, SQLException
    {
        boolean return_val = false;
        long clause_count = 0;
        
        // parsing chunks requires an open semantic database
        smem_attach();
        
        // copied primarily from cli_sp
        final StringWriter errorWriter = new StringWriter();
        final Lexer lexer = new Lexer(new Printer(errorWriter), new StringReader(chunkString));
        lexer.setAllowIds(true);
        lexer.getNextLexeme();
        
        boolean good_chunk = true;
        
        final Map<String, smem_chunk_lti> chunks = new LinkedHashMap<>();
        // smem_str_to_chunk_map::iterator c_old;
        
        final Set<smem_chunk_lti> newbies = new LinkedHashSet<>();
        // smem_chunk_set::iterator c_new;
        
        // consume next token
        lexer.getNextLexeme();
        
        if(lexer.getCurrentLexeme().type == LexemeType.L_BRACE)
        {
            good_chunk = false;
        }
        
        // while there are chunks to consume
        while((lexer.getCurrentLexeme().type == LexemeType.L_PAREN) && (good_chunk))
        {
            good_chunk = smem_parse_chunk(symbols, lexer, chunks, newbies);
            
            if(good_chunk)
            {
                // add all newbie lti's as appropriate
                for(smem_chunk_lti c_new : newbies)
                {
                    if(c_new.lti_id == 0)
                    {
                        // deal differently with variable vs. lti
                        if(c_new.lti_number == 0)
                        {
                            // add a new lti id (we have a guarantee this won't
                            // be in Soar's WM)
                            c_new.lti_number = symbols.incrementIdNumber(c_new.lti_letter);
                            c_new.lti_id = smem_lti_add_id(c_new.lti_letter, c_new.lti_number);
                        }
                        else
                        {
                            // should ALWAYS be the case (it's a newbie and
                            // we've initialized lti_id to NIL)
                            if(c_new.lti_id == 0)
                            {
                                // get existing
                                c_new.lti_id = smem_lti_get_id(c_new.lti_letter, c_new.lti_number);
                                
                                // if doesn't exist, add it
                                if(c_new.lti_id == 0)
                                {
                                    c_new.lti_id = smem_lti_add_id(c_new.lti_letter, c_new.lti_number);
                                    
                                    // this could affect an existing identifier
                                    // in Soar's WM
                                    final IdentifierImpl id_parent = symbols.findIdentifier(c_new.lti_letter, c_new.lti_number);
                                    if(id_parent != null)
                                    {
                                        // if so we make it an lti manually
                                        id_parent.smem_lti = c_new.lti_id;
                                        
                                        id_parent.smem_time_id = epmem.getStats().getTime();
                                    }
                                }
                            }
                        }
                    }
                }
                
                // add all newbie contents (append, as opposed to replace,
                // children)
                for(smem_chunk_lti c_new : newbies)
                {
                    if(c_new.slots != null)
                    {
                        smem_store_chunk(c_new.lti_id, c_new.slots, false);
                    }
                }
                
                // deallocate *contents* of all newbies (need to keep around
                // name->id association for future chunks)
                for(smem_chunk_lti c_new : newbies)
                {
                    smem_deallocate_chunk(c_new, false);
                }
                // clear newbie list
                newbies.clear();
                
                // increment clause counter
                clause_count++;
                
            }
        }
        
        return_val = good_chunk;
        
        // deallocate all chunks
        for(smem_chunk_lti c_old : chunks.values())
        {
            smem_deallocate_chunk(c_old, true);
        }
        chunks.clear();
        
        // produce error message on failure
        if(!return_val)
        {
            throw new SoarException("Error parsing clause #" + clause_count);
        }
        
        return return_val;
    }
    
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    // API Implementation (smem::api)
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    
    private enum path_type
    {
        blank_slate, cmd_bad, cmd_retrieve, cmd_query, cmd_store
    }
    
    /**
     * <p>
     * semantic_memory.cpp:2702:smem_respond_to_cmd
     * 
     */
    void smem_respond_to_cmd(boolean store_only) throws SQLException, SoarException
    {
        // start at the bottom and work our way up
        // (could go in the opposite direction as well)
        IdentifierImpl state = decider.bottom_goal;
        
        List<SymbolTriple> meta_wmes = new ArrayList<>();
        List<SymbolTriple> retrieval_wmes = new ArrayList<>();
        Set<WmeImpl> cue_wmes = new LinkedHashSet<>();
        
        final List<IdentifierImpl> prohibit = new ArrayList<>();
        final List<IdentifierImpl> store = new ArrayList<>();
        
        final int time_slot = store_only ? 1 : 0;
        final Queue<IdentifierImpl> syms = new ArrayDeque<>();
        final Queue<Integer> levels = new ArrayDeque<>();
        
        boolean do_wm_phase = false;
        boolean mirroring_on = params.mirroring.get() == MirroringChoices.on;
        
        // Free this up as soon as we start a phase that allows queries
        if(!store_only)
        {
            lastCue = null;
        }
        
        while(state != null)
        {
            final SemanticMemoryStateInfo smem_info = smem_info(state);
            // //////////////////////////////////////////////////////////////////////////
            // TODO SMEM Timers: my_agent->smem_timers->api->start();
            // //////////////////////////////////////////////////////////////////////////
            
            // make sure this state has had some sort of change to the cmd
            // NOTE: we only care one-level deep!
            boolean new_cue = false;
            long wme_count = 0;
            List<WmeImpl> cmds = null;
            {
                final Marker tc = DefaultMarker.create(); // get_new_tc_number(
                                                          // my_agent );
                
                // initialize BFS at command
                syms.add(smem_info.smem_cmd_header); // push
                levels.add(0); // push(0)
                
                while(!syms.isEmpty())
                {
                    // get state
                    final IdentifierImpl parent_sym = syms.remove(); // front()/pop()
                    
                    final int parent_level = levels.remove(); // front()/pop()
                    
                    {
                        // get children of the current identifier
                        final List<WmeImpl> wmes = smem_get_direct_augs_of_id(parent_sym, tc);
                        for(WmeImpl w_p : wmes)
                        {
                            if(((store_only) && ((parent_level != 0) || (w_p.attr == predefinedSyms.smem_sym_store)))
                                    || ((!store_only) && ((parent_level != 0) || (w_p.attr != predefinedSyms.smem_sym_store))))
                            {
                                wme_count++;
                                
                                if(w_p.timetag > smem_info.last_cmd_time[time_slot])
                                {
                                    new_cue = true;
                                    smem_info.last_cmd_time[time_slot] = w_p.timetag;
                                }
                                
                                if((w_p.value.asIdentifier() != null) && (parent_level == 0) && ((w_p.attr == predefinedSyms.smem_sym_query) || (w_p.attr == predefinedSyms.smem_sym_store)))
                                {
                                    syms.add(w_p.value.asIdentifier()); // push
                                    levels.add(parent_level + 1); // push
                                }
                            }
                        }
                        
                        // free space from aug list
                        if(cmds == null)
                        {
                            cmds = wmes;
                        }
                        else
                        {
                            // wmes = null; delete wmes;
                        }
                    }
                }
                
                // see if any WMEs were removed
                if(smem_info.last_cmd_count[time_slot] != wme_count)
                {
                    new_cue = true;
                    smem_info.last_cmd_count[time_slot] = wme_count;
                }
                
                if(new_cue)
                {
                    // clear old results
                    smem_clear_result(state);
                    
                    do_wm_phase = true;
                }
            }
            
            // a command is issued if the cue is new
            // and there is something on the cue
            if(new_cue && wme_count != 0)
            {
                cue_wmes.clear();
                meta_wmes.clear();
                retrieval_wmes.clear();
                
                // initialize command vars
                IdentifierImpl retrieve = null;
                IdentifierImpl query = null;
                IdentifierImpl negquery = null;
                IdentifierImpl math = null;
                store.clear();
                prohibit.clear();
                path_type path = path_type.blank_slate;
                
                // process top-level symbols
                for(WmeImpl w_p : cmds)
                {
                    cue_wmes.add(w_p);
                    
                    if(path != path_type.cmd_bad)
                    {
                        // collect information about known commands
                        if(w_p.attr == predefinedSyms.smem_sym_retrieve)
                        {
                            if((w_p.value.asIdentifier() != null) && (path == path_type.blank_slate))
                            {
                                retrieve = w_p.value.asIdentifier();
                                path = path_type.cmd_retrieve;
                            }
                            else
                            {
                                path = path_type.cmd_bad;
                            }
                        }
                        else if(w_p.attr == predefinedSyms.smem_sym_query)
                        {
                            if((w_p.value.asIdentifier() != null) && ((path == path_type.blank_slate) || (path == path_type.cmd_query)) && (query == null))
                            
                            {
                                query = w_p.value.asIdentifier();
                                path = path_type.cmd_query;
                            }
                            else
                            {
                                path = path_type.cmd_bad;
                            }
                        }
                        else if(w_p.attr == predefinedSyms.smem_sym_negquery)
                        {
                            if((w_p.value.asIdentifier() != null) && ((path == path_type.blank_slate) || (path == path_type.cmd_query)) && (negquery == null))
                            {
                                negquery = w_p.value.asIdentifier();
                                path = path_type.cmd_query;
                            }
                            else
                            {
                                path = path_type.cmd_bad;
                            }
                        }
                        else if(w_p.attr == predefinedSyms.smem_sym_prohibit)
                        {
                            if((w_p.value.asIdentifier() != null) && ((path == path_type.blank_slate) || (path == path_type.cmd_query)) && (w_p.value.asIdentifier().smem_lti != 0))
                            {
                                prohibit.add(w_p.value.asIdentifier()); // push_back
                                path = path_type.cmd_query;
                            }
                            else
                            {
                                path = path_type.cmd_bad;
                            }
                        }
                        else if(w_p.attr == predefinedSyms.smem_sym_math_query)
                        {
                            if((w_p.value.asIdentifier() != null) && ((path == path_type.blank_slate) || (path == path_type.cmd_query)) && (math == null))
                            {
                                math = w_p.value.asIdentifier();
                                path = path_type.cmd_query;
                            }
                            else
                            {
                                path = path_type.cmd_bad;
                            }
                        }
                        else if(w_p.attr == predefinedSyms.smem_sym_store)
                        {
                            if((w_p.value.asIdentifier() != null) && ((path == path_type.blank_slate) || (path == path_type.cmd_store)))
                            {
                                store.add(w_p.value.asIdentifier());
                                path = path_type.cmd_store;
                            }
                            else
                            {
                                path = path_type.cmd_bad;
                            }
                        }
                        else
                        {
                            path = path_type.cmd_bad;
                        }
                    }
                }
                
                // if on path 3 must have query/neg-query
                if((path == path_type.cmd_query) && (query == null))
                {
                    path = path_type.cmd_bad;
                }
                
                // must be on a path
                if(path == path_type.blank_slate)
                {
                    path = path_type.cmd_bad;
                }
                
                // //////////////////////////////////////////////////////////////////////////
                // TODO SMEM Timers: my_agent->smem_timers->api->stop();
                // //////////////////////////////////////////////////////////////////////////
                
                // process command
                if(path != path_type.cmd_bad)
                {
                    // performing any command requires an initialized database
                    smem_attach();
                    
                    // retrieve
                    if(path == path_type.cmd_retrieve)
                    {
                        if(retrieve.smem_lti == 0)
                        {
                            // retrieve is not pointing to an lti!
                            smem_buffer_add_wme(meta_wmes, smem_info.smem_result_header, predefinedSyms.smem_sym_failure, retrieve);
                        }
                        else
                        {
                            // status: success
                            smem_buffer_add_wme(meta_wmes, smem_info.smem_result_header, predefinedSyms.smem_sym_success, retrieve);
                            
                            // install memory directly onto the retrieve
                            // identifier
                            smem_install_memory(state, retrieve.smem_lti, retrieve, true, meta_wmes, retrieval_wmes);
                            
                            // add one to the expansions stat
                            stats.retrieves.set(stats.retrieves.get() + 1);
                        }
                    }
                    // query
                    else if(path == path_type.cmd_query)
                    {
                        final Set<Long> /* smem_lti_set */ prohibit_lti = new LinkedHashSet<>();
                        
                        for(IdentifierImpl sym_p : prohibit)
                        {
                            prohibit_lti.add(sym_p.smem_lti);
                        }
                        
                        smem_process_query(state, query, negquery, math, prohibit_lti, cue_wmes, meta_wmes, retrieval_wmes);
                        
                        // add one to the cbr stat
                        stats.queries.set(stats.queries.get() + 1);
                    }
                    else if(path == path_type.cmd_store)
                    {
                        // //////////////////////////////////////////////////////////////////////////
                        // TODO SMEM Timers:
                        // my_agent->smem_timers->storage->start();
                        // //////////////////////////////////////////////////////////////////////////
                        
                        // start transaction (if not lazy)
                        if(params.lazy_commit.get() == LazyCommitChoices.off)
                        {
                            db.beginExecuteUpdate( /* soar_module::op_reinit */);
                        }
                        
                        for(IdentifierImpl sym_p : store)
                        {
                            smem_soar_store(sym_p, ((mirroring_on) ? (smem_storage_type.store_recursive) : (smem_storage_type.store_level)));
                            
                            // status: success
                            smem_buffer_add_wme(meta_wmes, smem_info.smem_result_header, predefinedSyms.smem_sym_success, sym_p);
                            
                            // add one to the store stat
                            stats.stores.set(stats.stores.get() + 1);
                        }
                        
                        // commit transaction (if not lazy)
                        if(params.lazy_commit.get() == LazyCommitChoices.off)
                        {
                            db.commitExecuteUpdate( /* soar_module::op_reinit */);
                        }
                        
                        // //////////////////////////////////////////////////////////////////////////
                        // TODO SMEM Timers:
                        // my_agent->smem_timers->storage->stop();
                        // //////////////////////////////////////////////////////////////////////////
                    }
                }
                else
                {
                    smem_buffer_add_wme(meta_wmes, smem_info.smem_result_header, predefinedSyms.smem_sym_bad_cmd, smem_info.smem_cmd_header);
                }
                
                if(!meta_wmes.isEmpty() || !retrieval_wmes.isEmpty())
                {
                    // process preference assertion en masse
                    smem_process_buffered_wmes(state, cue_wmes, meta_wmes, retrieval_wmes);
                    
                    // clear cache
                    {
                        retrieval_wmes.clear();
                        meta_wmes.clear();
                    }
                    
                    // process wm changes on state
                    do_wm_phase = true;
                }
                
                // clear cue wmes
                cue_wmes.clear();
            }
            else
            {
                // //////////////////////////////////////////////////////////////////////////
                // TODO SMEM Timers: my_agent->smem_timers->api->stop();
                // //////////////////////////////////////////////////////////////////////////
            }
            
            state = state.goalInfo.higher_goal;
        }
        
        if(store_only && mirroring_on && (!smem_changed_ids.isEmpty()))
        {
            // //////////////////////////////////////////////////////////////////////////
            // TODO SMEM Timers: my_agent->smem_timers->storage->start();
            // //////////////////////////////////////////////////////////////////////////
            
            // start transaction (if not lazy)
            if(params.lazy_commit.get() == LazyCommitChoices.off)
            {
                db.beginExecuteUpdate();
            }
            
            for(SymbolImpl it : smem_changed_ids)
            {
                // require that the lti has at least one augmentation
                if(it.asIdentifier().slots != null)
                {
                    smem_soar_store(it.asIdentifier(), smem_storage_type.store_recursive);
                    
                    // add one to the mirrors stat
                    stats.mirrors.set(stats.mirrors.get() + 1);
                }
            }
            
            // commit transaction (if not lazy)
            if(params.lazy_commit.get() == LazyCommitChoices.off)
            {
                db.commitExecuteUpdate();
            }
            
            smem_changed_ids.clear();
            
            // //////////////////////////////////////////////////////////////////////////
            // TODO SMEM Timers: my_agent->smem_timers->storage->stop();
            // //////////////////////////////////////////////////////////////////////////
        }
        
        if(do_wm_phase)
        {
            smem_ignore_changes = true;
            
            decider.do_working_memory_phase();
            
            smem_ignore_changes = false;
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_go(boolean)
     */
    @Override
    public void smem_go(boolean store_only)
    {
        // TODO SMEM Timers: my_agent->smem_timers->total->start();
        
        // #ifndef SMEM_EXPERIMENT
        
        try
        {
            smem_respond_to_cmd(store_only);
        }
        catch(SQLException | SoarException e)
        {
            // TODO SMEM error
            throw new RuntimeException(e);
        }
        
        // #else // SMEM_EXPERIMENT
        
        // #endif // SMEM_EXPERIMENT
        
        // TODO SMEM Timers: my_agent->smem_timers->total->stop();
    }
    
    boolean smem_backup_db(String file_name, ByRef<String> err) throws SQLException
    {
        boolean return_val = false;
        
        if(db != null)
        {
            _smem_close_vars();
            
            return db.backupDb(file_name);
        }
        else
        {
            err.value = "Semantic database is not currently connected.";
        }
        
        return return_val;
    }
    
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    // Visualization (smem::viz)
    // ////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////
    
    /**
     * <p>
     * semantic_memory.cpp:3042:smem_visualize_store
     * 
     */
    void smem_visualize_store(PrintWriter return_val) throws SoarException
    {
        try
        {
            smem_visualize_store_safe(return_val);
        }
        catch(SQLException e)
        {
            throw new SoarException(e);
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:3042:smem_visualize_store
     * 
     */
    private void smem_visualize_store_safe(PrintWriter return_val) throws SoarException, SQLException
    {
        // vizualizing the store requires an open semantic database
        smem_attach();
        
        // header
        return_val.append("digraph smem {");
        return_val.append("\n");
        
        // LTIs
        return_val.append("node [ shape = doublecircle ];");
        return_val.append("\n");
        
        final Map<Long, String> lti_names = new LinkedHashMap<>();
        {
            // id, soar_letter, number
            {
                try(ResultSet q = db.vis_lti.executeQuery())
                {
                    while(q.next())
                    {
                        final long lti_id = q.getLong(0 + 1);
                        final char lti_letter = (char) q.getLong(1 + 1);
                        final long lti_number = q.getLong(2 + 1);
                        
                        final String lti_name = String.format("%c%d", lti_letter, lti_number);
                        lti_names.put(lti_id, lti_name);
                        
                        return_val.append(lti_name);
                        return_val.append(" [ label=\"");
                        return_val.append(lti_name);
                        return_val.append("\\n[");
                        
                        Double temp_double = q.getDouble(3 + 1);
                        if(temp_double >= 0)
                        {
                            return_val.append("+");
                        }
                        return_val.append(temp_double.toString());
                        
                        return_val.append("]\" ];\n");
                    }
                }
            }
            
            if(!lti_names.isEmpty())
            {
                // terminal nodes first
                {
                    final Map<Long, List<String>> lti_terminals = new LinkedHashMap<>();
                    
                    List<String> my_terminals = null;
                    
                    return_val.append("\n");
                    
                    // proceed to terminal nodes
                    return_val.append("node [ shape = plaintext ];");
                    return_val.append("\n");
                    
                    {
                        // lti_id, attr_type, attr_hash, val_type, val_hash
                        try(ResultSet q = db.vis_value_const.executeQuery())
                        {
                            while(q.next())
                            {
                                final long lti_id = q.getLong(0 + 1);
                                my_terminals = lti_terminals.get(lti_id);
                                if(my_terminals == null)
                                {
                                    lti_terminals.put(lti_id, my_terminals = new ArrayList<String>());
                                }
                                
                                final String lti_name = lti_names.get(lti_id); // TODO
                                                                               // is
                                                                               // this
                                                                               // safe?
                                
                                // parent prefix
                                return_val.append(lti_name);
                                return_val.append("_");
                                
                                // terminal count
                                final int terminal_num = my_terminals.size();
                                return_val.append(Integer.toString(terminal_num));
                                
                                // prepare for value
                                return_val.append(" [ label = \"");
                                
                                // output value
                                {
                                    switch((int) q.getLong(3 + 1))
                                    {
                                    case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                        return_val.append(smem_reverse_hash_str(q.getLong(4 + 1)));
                                        break;
                                    
                                    case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                        return_val.append(Integer.toString(smem_reverse_hash_int(q.getLong(4 + 1))));
                                        break;
                                    
                                    case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                        return_val.append(Double.toString(smem_reverse_hash_float(q.getLong(4 + 1))));
                                        break;
                                    
                                    default:
                                        // print nothing
                                        break;
                                    }
                                }
                                
                                // store terminal (attribute for edge label)
                                {
                                    switch((int) q.getLong(1 + 1))
                                    {
                                    case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                        my_terminals.add(smem_reverse_hash_str(q.getLong(2 + 1)));
                                        break;
                                    
                                    case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                        my_terminals.add(Integer.toString(smem_reverse_hash_int(q.getLong(2 + 1))));
                                        break;
                                    
                                    case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                        my_terminals.add(Double.toString(smem_reverse_hash_float(q.getLong(2 + 1))));
                                        break;
                                    
                                    default:
                                        my_terminals.add(""); // temp_str.clear();
                                        break;
                                    }
                                }
                                
                                // footer
                                return_val.append("\" ];");
                                return_val.append("\n");
                            }
                        }
                    }
                    
                    // output edges
                    {
                        for(Map.Entry<Long, String> n_p : lti_names.entrySet())
                        {
                            final List<String> t_p = lti_terminals.get(n_p.getKey());
                            
                            if(t_p != null)
                            {
                                int terminal_counter = 0;
                                
                                for(String a_p : t_p)
                                {
                                    return_val.append(n_p.getValue());
                                    return_val.append(" -> ");
                                    return_val.append(n_p.getValue());
                                    return_val.append("_");
                                    
                                    return_val.append(Integer.toString(terminal_counter));
                                    return_val.append(" [ label=\"");
                                    
                                    return_val.append(a_p);
                                    
                                    return_val.append("\" ];");
                                    return_val.append("\n");
                                    
                                    terminal_counter++;
                                }
                            }
                        }
                    }
                }
                
                // then links to other LTIs
                {
                    // lti_id, attr_type, attr_hash, value_lti_id
                    {
                        try(ResultSet q = db.vis_value_lti.executeQuery())
                        {
                            while(q.next())
                            {
                                // source
                                long lti_id = q.getLong(0 + 1);
                                String lti_name = lti_names.get(lti_id); // TODO
                                                                         // SMEM
                                                                         // is
                                                                         // this
                                                                         // safe?
                                return_val.append(lti_name);
                                return_val.append(" -> ");
                                
                                // destination
                                lti_id = q.getLong(3 + 1);
                                lti_name = lti_names.get(lti_id); // TODO SMEM
                                                                  // is this
                                                                  // safe?
                                return_val.append(lti_name);
                                return_val.append(" [ label =\"");
                                
                                // output attribute
                                {
                                    switch((int) q.getLong(1 + 1))
                                    {
                                    case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                        return_val.append(smem_reverse_hash_str(q.getLong(2 + 1)));
                                        break;
                                    
                                    case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                        return_val.append(Integer.toString(smem_reverse_hash_int(q.getLong(2 + 1))));
                                        break;
                                    
                                    case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                        return_val.append(Double.toString(smem_reverse_hash_float(q.getLong(2 + 1))));
                                        break;
                                    
                                    default:
                                        // print nothing
                                        break;
                                    }
                                }
                                
                                // footer
                                return_val.append("\" ];");
                                return_val.append("\n");
                            }
                        }
                    }
                }
            }
        }
        
        // footer
        return_val.append("}");
        return_val.append("\n");
    }
    
    /**
     * <p>
     * semantic_memory.cpp:3277:smem_visualize_lti
     * 
     */
    void smem_visualize_lti(long /* smem_lti_id */ lti_id, int depth, PrintWriter return_val) throws SoarException
    {
        try
        {
            smem_visualize_lti_safe(lti_id, depth, return_val);
        }
        catch(SQLException e)
        {
            throw new SoarException(e);
        }
    }
    
    /**
     * <p>
     * semantic_memory.cpp:3277:smem_visualize_lti
     * 
     */
    private void smem_visualize_lti_safe(long /* smem_lti_id */ lti_id, int depth, PrintWriter return_val) throws SQLException
    {
        // buffer
        StringWriter return_val2 = new StringWriter();
        
        final Queue<smem_vis_lti> bfs = new ArrayDeque<>();
        
        final Map<Long /* smem_lti_id */, smem_vis_lti> close_list = new LinkedHashMap<>();
        
        // header
        return_val.append("digraph smem_lti {");
        return_val.append("\n");
        
        // root
        {
            final smem_vis_lti new_lti = new smem_vis_lti();
            new_lti.lti_id = lti_id;
            new_lti.level = 0;
            
            // fake former linkage
            {
                // get just this lti
                db.lti_letter_num.setLong(1, lti_id);
                
                try(ResultSet lti_q = db.lti_letter_num.executeQuery())
                {
                    // soar_letter
                    new_lti.lti_name = String.format("%c%d", (char) lti_q.getLong(0 + 1), lti_q.getLong(1 + 1));
                }
            }
            
            bfs.add(new_lti);
            
            close_list.put(lti_id, new_lti);
        }
        
        // optionally depth-limited breadth-first-search of children
        while(!bfs.isEmpty())
        {
            final smem_vis_lti parent_lti = bfs.remove(); // front()/pop()
            
            long child_counter = 0;
            
            // get direct children: attr_type, attr_hash, value_type,
            // value_hash, value_letter, value_num, value_lti
            db.web_expand.setLong(1, parent_lti.lti_id);
            
            try(ResultSet expand_q = db.web_expand.executeQuery())
            {
                while(expand_q.next())
                {
                    // identifier vs. constant
                    final long check_lti_id = expand_q.getLong(6 + 1);
                    if(check_lti_id != SMEM_AUGMENTATIONS_NULL)
                    {
                        final smem_vis_lti new_lti = new smem_vis_lti();
                        new_lti.lti_id = check_lti_id;
                        new_lti.level = (parent_lti.level + 1);
                        
                        // add node
                        {
                            // soar_letter
                            new_lti.lti_name = String.format("%c%d", (char) expand_q.getLong(4 + 1), expand_q.getLong(5 + 1));
                        }
                        
                        // add linkage
                        {
                            // output linkage
                            return_val2.append(parent_lti.lti_name);
                            return_val2.append(" -> ");
                            return_val2.append(new_lti.lti_name);
                            return_val2.append(" [ label = \"");
                            
                            // get attribute
                            switch((int) expand_q.getLong(0 + 1))
                            {
                            case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                return_val2.append(smem_reverse_hash_str(expand_q.getLong(1 + 1)));
                                break;
                            
                            case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                return_val2.append(Integer.toString(smem_reverse_hash_int(expand_q.getLong(1 + 1))));
                                break;
                            
                            case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                return_val2.append(Double.toString(smem_reverse_hash_float(expand_q.getLong(1 + 1))));
                                break;
                            
                            default:
                                // print nothing
                                break;
                            }
                            
                            return_val2.append("\" ];");
                            return_val2.append("\n");
                        }
                        
                        // prevent looping
                        {
                            // prevent looping
                            if(!close_list.containsKey(new_lti.lti_id))
                            {
                                close_list.put(new_lti.lti_id, new_lti);
                                
                                if((depth == 0) || (new_lti.level < depth))
                                {
                                    bfs.add(new_lti);
                                }
                            }
                            else
                            {
                                // delete new_lti;
                            }
                        }
                    }
                    else
                    {
                        // get node name
                        final String node_name = String.format("%s_%d", parent_lti.lti_name, child_counter);
                        // add value node
                        {
                            // output node
                            return_val2.append("node [ shape = plaintext ];");
                            return_val2.append("\n");
                            return_val2.append(node_name);
                            return_val2.append(" [ label=\"");
                            
                            // get value
                            switch((int) expand_q.getLong(2 + 1))
                            {
                            case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                return_val2.append(smem_reverse_hash_str(expand_q.getLong(3 + 1)));
                                break;
                            
                            case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                return_val2.append(Integer.toString(smem_reverse_hash_int(expand_q.getLong(3 + 1))));
                                break;
                            
                            case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                return_val2.append(Double.toString(smem_reverse_hash_float(expand_q.getLong(3 + 1))));
                                break;
                            
                            default:
                                // print nothing
                                break;
                            }
                            
                            return_val2.append("\" ];");
                            return_val2.append("\n");
                        }
                        
                        // add linkage
                        {
                            // output linkage
                            return_val2.append(parent_lti.lti_name);
                            return_val2.append(" -> ");
                            return_val2.append(node_name);
                            return_val2.append(" [ label = \"");
                            
                            // get attribute
                            switch((int) expand_q.getLong(0 + 1))
                            {
                            case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                return_val.append(smem_reverse_hash_str(expand_q.getLong(1 + 1)));
                                break;
                            
                            case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                return_val.append(Integer.toString(smem_reverse_hash_int(expand_q.getLong(1 + 1))));
                                break;
                            
                            case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                return_val.append(Double.toString(smem_reverse_hash_float(expand_q.getLong(1 + 1))));
                                break;
                            
                            default:
                                // print nothing
                                break;
                            }
                            
                            return_val2.append("\" ];");
                            return_val2.append("\n");
                        }
                        
                        child_counter++;
                    }
                }
            }
        }
        
        // footer
        return_val2.append("}");
        return_val2.append("\n");
        
        // handle lti nodes at once
        {
            PreparedStatement act_q = db.vis_lti_act;
            
            return_val.append("node [ shape = doublecircle ];");
            return_val.append("\n");
            
            for(Map.Entry<Long, smem_vis_lti> e : close_list.entrySet())
            {
                return_val.append(e.getValue().lti_name);
                return_val.append(" [ label=\"");
                return_val.append(e.getValue().lti_name);
                return_val.append("\\n[");
                
                act_q.setLong(1, e.getKey());
                
                try(ResultSet rs = act_q.executeQuery())
                {
                    if(rs.next())
                    {
                        Double temp_double = rs.getDouble(0 + 1);
                        if(temp_double >= 0)
                        {
                            return_val.append("+");
                        }
                        return_val.append(temp_double.toString());
                    }
                }
                
                return_val.append("]\"");
                return_val.append(" ];");
                return_val.append("\n");
            }
        }
        
        // transfer buffer after nodes
        return_val.append(return_val2.toString());
    }
    
    Set<Long /* smem_lti_id */> _smem_print_lti(long /* smem_lti_id */ lti_id, char lti_letter, long lti_number, double lti_act, StringBuilder return_val) throws SQLException
    {
        Set<Long /* smem_lti_id */> next = new LinkedHashSet<>();
        
        String temp_str;
        StringBuilder temp_str2 = null;
        
        Map<String, List<String>> augmentations = new LinkedHashMap<>();
        
        PreparedStatement expand_q = db.web_expand;
        
        return_val.append("(@");
        return_val.append(lti_letter);
        return_val.append(lti_number);
        
        expand_q.setLong(1, lti_id);
        
        try(ResultSet rs = expand_q.executeQuery())
        {
            while(rs.next())
            {
                switch(rs.getInt(0 + 1))
                {
                case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                    temp_str = smem_reverse_hash_str(rs.getLong(1 + 1));
                    break;
                case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                    temp_str = String.valueOf(smem_reverse_hash_int(rs.getLong(1 + 1)));
                    break;
                case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                    temp_str = String.valueOf(smem_reverse_hash_float(rs.getLong(1 + 1)));
                    break;
                default:
                    temp_str = null;
                    break;
                }
                
                // identifier vs. constant
                if(rs.getLong(6 + 1) != SMEM_AUGMENTATIONS_NULL)
                {
                    temp_str2 = new StringBuilder("@");
                    
                    // soar letter
                    temp_str2.append((char) rs.getInt(4 + 1));
                    
                    // number
                    temp_str2.append(rs.getLong(5 + 1));
                    
                    // add to next
                    next.add(rs.getLong(6 + 1));
                }
                else
                {
                    switch(rs.getInt(2 + 1))
                    {
                    case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                        temp_str2 = new StringBuilder(smem_reverse_hash_str(rs.getLong(3 + 1)));
                        temp_str2.insert(0, "|");
                        temp_str2.append("|");
                        break;
                    case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                        temp_str2 = new StringBuilder(String.valueOf(smem_reverse_hash_int(rs.getLong(3 + 1))));
                        break;
                    case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                        temp_str2 = new StringBuilder(String.valueOf(smem_reverse_hash_float(rs.getLong(3 + 1))));
                        break;
                    
                    default:
                        temp_str2 = null;
                        break;
                    }
                }
                
                if(!augmentations.containsKey(temp_str))
                {
                    augmentations.put(temp_str, new ArrayList<String>());
                }
                if(temp_str2 != null)
                {
                    augmentations.get(temp_str).add(temp_str2.toString());
                }
                else
                {
                    augmentations.get(temp_str).add(temp_str);
                }
            }
        }
        
        // output augmentations nicely
        {
            for(Map.Entry<String, List<String>> lti_slot : augmentations.entrySet())
            {
                return_val.append(" ^");
                return_val.append(lti_slot.getKey());
                
                for(String slot_val : lti_slot.getValue())
                {
                    return_val.append(" ");
                    return_val.append(slot_val);
                }
            }
        }
        augmentations.clear();
        
        return_val.append(" [");
        if(lti_act >= 0)
        {
            return_val.append("+");
        }
        return_val.append(lti_act);
        return_val.append("]");
        return_val.append(")\n");
        
        return next;
    }
    
    public void smem_print_store(StringBuilder return_val) throws SoarException
    {
        // vizualizing the store requires an open semantic database
        smem_attach();
        
        // id, soar_letter, number
        PreparedStatement q = db.vis_lti;
        
        try(ResultSet rs = q.executeQuery())
        {
            while(rs.next())
            {
                _smem_print_lti(rs.getLong(0 + 1), (char) rs.getInt(1 + 1), rs.getLong(2 + 1), rs.getDouble(3 + 1), return_val);
            }
        }
        catch(SQLException e)
        {
            throw new SoarException(e);
        }
    }
    
    private static class SmemLTIidDepthPair
    {
        private final long /* smem_lti_id */ lti_id;
        
        private final int depth;
        
        public SmemLTIidDepthPair(long /* smem_lti_id */ lti_id, int depth)
        {
            this.lti_id = lti_id;
            this.depth = depth;
        }
        
        public long /* smem_lti_id */ getLTIid()
        {
            return lti_id;
        }
        
        public int getDepth()
        {
            return depth;
        }
        
        @Override
        public int hashCode()
        {
            return Objects.hash(depth, lti_id);
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if(this == obj)
            {
                return true;
            }
            if(obj == null)
            {
                return false;
            }
            if(getClass() != obj.getClass())
            {
                return false;
            }
            SmemLTIidDepthPair other = (SmemLTIidDepthPair) obj;
            if(depth != other.depth)
            {
                return false;
            }
            if(lti_id != other.lti_id)
            {
                return false;
            }
            return true;
        }
    }
    
    public void smem_print_lti(long /* smem_lti_id */ lti_id, int depth, StringBuilder return_val) throws SoarException
    {
        Set<Long /* smem_lti_id */> visited = new LinkedHashSet<>();
        
        Queue<SmemLTIidDepthPair> to_visit = new ArrayDeque<>();
        SmemLTIidDepthPair c = null;
        
        Set<Long /* smem_lti_id */> next = new LinkedHashSet<>();
        
        PreparedStatement lti_q = db.lti_letter_num;
        PreparedStatement act_q = db.vis_lti_act;
        
        int i;
        
        // vizualizing the store requires an open semantic database
        smem_attach();
        
        // initialize queue/set
        to_visit.add(new SmemLTIidDepthPair(lti_id, 1));
        visited.add(lti_id);
        
        while(!to_visit.isEmpty())
        {
            c = to_visit.remove();
            
            // output leading spaces ala depth
            for(i = 1; i < c.getDepth(); i++)
            {
                return_val.append(" ");
            }
            
            // get lti info
            try
            {
                lti_q.setLong(1, c.getLTIid());
                act_q.setLong(1, c.getLTIid());
                
                try(ResultSet ltiRS = lti_q.executeQuery(); ResultSet actRS = act_q.executeQuery())
                {
                    next = _smem_print_lti(c.getLTIid(), (char) ltiRS.getLong(0 + 1), ltiRS.getLong(1 + 1), actRS.getDouble(0 + 1), return_val);
                    
                    // done with lookup
                    
                    // consider further depth
                    if(c.getDepth() < depth)
                    {
                        for(Long next_it : next)
                        {
                            boolean successfullyInserted = visited.add(next_it);
                            if(successfullyInserted)
                            {
                                to_visit.add(new SmemLTIidDepthPair(next_it, c.getDepth() + 1));
                            }
                        }
                    }
                }
            }
            catch(SQLException e)
            {
                throw new SoarException(e);
            }
        }
    }
    
    /**
     * If db is open and in lazy-commit mode, do a commit. This will force all
     * data to the database. Otherwise, this method is a no-op.
     */
    void commit() throws SoarException
    {
        // if lazy, commit
        if(db != null && params.lazy_commit.get() == LazyCommitChoices.on)
        {
            // Commit and then start next lazy-commit transaction
            try
            {
                db.commitExecuteUpdate( /* soar_module::op_reinit */);
                db.beginExecuteUpdate( /* soar_module::op_reinit */);
            }
            catch(SQLException e)
            {
                throw new SoarException("Error while forcing commit: " + e.getMessage(), e);
            }
        }
        
    }
    
    @Override
    public boolean isMirroringEnabled()
    {
        return params.mirroring.get() == MirroringChoices.on;
    }
    
    @Override
    public Set<IdentifierImpl> smem_changed_ids()
    {
        return smem_changed_ids;
    }
    
    @Override
    public boolean smem_ignore_changes()
    {
        return smem_ignore_changes;
    }
}
