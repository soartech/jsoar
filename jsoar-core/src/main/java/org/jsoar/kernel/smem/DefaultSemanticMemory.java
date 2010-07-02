/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.jsoar.kernel.Decider;
import org.jsoar.kernel.SoarException;
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
import org.jsoar.kernel.modules.SoarModule;
import org.jsoar.kernel.parser.original.Lexeme;
import org.jsoar.kernel.parser.original.LexemeType;
import org.jsoar.kernel.parser.original.Lexer;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.smem.DefaultSemanticMemoryParams.Optimization;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.ByRef;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;
import org.jsoar.util.properties.PropertyManager;

/**
 * Default implementation of {@link SemanticMemory}
 * 
 * <h2>Variance from CSoar Implementation</h2>
 * <p>The smem_data_struct that was added to every identifier in CSoar is instead maintained 
 * in a map from id to {@link SemanticMemoryStateInfo} in this class. This structure is never
 * accessed outside of SMem or in a way that would make a map too slow.
 * 
 * <h2>Notes on soardb/sqlite to JDBC conversion</h2>
 * <ul>
 * <li>When retrieving column values (e.g. {@code sqlite3_column_int}), columns are 
 * 0-based. In JDBC, they are 1-based. So all column retrievals (in the initial port)
 * have the original index and {@code + 1}. For example, {@code rs.getLong(2 + 1)}.
 * <li>soardb tries to store ints as 32 or 64 bits depending on the platform. In this port,
 * we're just using long (64 bits) everywhere. So {@code column_int()} maps to 
 * {@code ResultSet.getLong()}.
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
 * <li>smem_slot == {@code List<smem_chunk_value> }
 * <li>smem_slot_map == {@code Map<SymbolImpl, List<smem_chunk_value>>}
 * <li>smem_str_to_chunk_map == {@code Map<String, smem_chunk_value>}
 * <li>smem_chunk_set == {@code Set<smem_chunk_value>}
 * <li>smem_sym_to_chunk_map = {@code Map<SymbolImpl, smem_chunk>}
 * <li>smem_lti_set = {@code Set<Long>}
 * <li>smem_weighted_cue_list = {@code LinkedList<WeightedCueElement>}
 * <li>smem_prioritized_activated_lti_queue = {@code PriorityQueue<ActivatedLti>}
 * <li>tc_number = {@code Marker}
 * </ul>
 * @author ray
 */
public class DefaultSemanticMemory implements SemanticMemory
{
    /**
     * semantic_memory.h:232:smem_variable_key
     */
    private static enum smem_variable_key
    {
        var_max_cycle, var_num_nodes, var_num_edges, var_act_thresh
    };
    
    /**
     * semantic_memory.h:260:smem_storage_type
     */
    private static enum smem_storage_type { store_level, store_recursive };
    
    /**
     * semantic_memory.h:367:smem_query_levels
     */
    private static enum smem_query_levels { qry_search, qry_full };

    /**
     * semantic_memory.h:237:SMEM_ACT_MAX
     */
    private static final long SMEM_ACT_MAX = (0-1)/ 2; // TODO???

    private Adaptable context;
    private DefaultSemanticMemoryParams params;
    private DefaultSemanticMemoryStats stats;
    private SymbolFactoryImpl symbols;
    private RecognitionMemory recMem;
    private Chunker chunker;
    private Decider decider;
    
    private SemanticMemoryDatabase db;
    
    /** agent.h:smem_validation */
    private /*uintptr_t*/ long smem_validation;
    /** agent.h:smem_first_switch */
    private boolean smem_first_switch = true;
    /** agent.h:smem_made_changes */
    private boolean smem_made_changes = false;
    /** agent.h:smem_max_cycle */
    private /*intptr_t*/ long smem_max_cycle;
    
    private SemanticMemorySymbols predefinedSyms;
    
    private Map<IdentifierImpl, SemanticMemoryStateInfo> stateInfos = new HashMap<IdentifierImpl, SemanticMemoryStateInfo>();
    
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
        
        final PropertyManager properties = Adaptables.require(DefaultSemanticMemory.class, context, PropertyManager.class);
        params = new DefaultSemanticMemoryParams(properties);
        stats = new DefaultSemanticMemoryStats(properties);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#resetStatistics()
     */
    @Override
    public void resetStatistics()
    {
        stats.reset();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#getCommand()
     */
    @Override
    public SoarCommand getCommand()
    {
        return new DefaultSemanticMemoryCommand(this);
    }

    SemanticMemoryDatabase getDatabase()
    {
        return db;
    }
    
    private SemanticMemoryStateInfo smem_info(IdentifierImpl state)
    {
        return stateInfos.get(state);
    }
    
    @Override
    public boolean smem_enabled()
    {
        // TODO Auto-generated method stub
        return false;
    }

    private List<WmeImpl> smem_get_direct_augs_of_id( SymbolImpl sym)
    {
        return smem_get_direct_augs_of_id(sym, null);
    }
    
    /**
     * semantic_memory.cpp:481:smem_get_direct_augs_of_id
     * 
     * @param sym
     * @param tc
     * @return
     */
    private List<WmeImpl> smem_get_direct_augs_of_id( SymbolImpl sym, Marker tc /*= NIL*/ )
    {
        final List<WmeImpl> return_val = new ArrayList<WmeImpl>();
        // augs only exist for identifiers
        final IdentifierImpl id = sym.asIdentifier();
        if ( id != null )
        {
            if ( tc != null )
            {
                if ( tc == id.tc_number )
                {
                    return return_val;
                }
                else
                {
                    id.tc_number = tc;
                }
            }

            // impasse wmes
            for (WmeImpl w=id.getImpasseWmes(); w!=null; w=w.next )
            {
                if ( !w.acceptable )
                {
                    return_val.add( w );
                }
            }

            // input wmes
            for (WmeImpl w=id.getInputWmes(); w!=null; w=w.next )
            {
                return_val.add( w );
            }

            // regular wmes
            for (Slot s=id.slots; s!=null; s=s.next )
            {
                for (WmeImpl w=s.getWmes(); w!=null; w=w.next )
                {
                    if ( !w.acceptable )
                    {
                        return_val.add( w );
                    }
                }
            }
        }

        return return_val;
    } 
    
    /**
     * semantic_memory.cpp:481:smem_symbol_is_constant
     * 
     * @param sym
     * @return
     */
    private static boolean smem_symbol_is_constant( Symbol sym )
    {
        return sym.asIdentifier() == null;
//        return ( ( sym->common.symbol_type == SYM_CONSTANT_SYMBOL_TYPE ) ||
//                 ( sym->common.symbol_type == INT_CONSTANT_SYMBOL_TYPE ) ||
//                 ( sym->common.symbol_type == FLOAT_CONSTANT_SYMBOL_TYPE ) );
    }
    
    /**
     * semantic_memory.cpp:542:smem_symbol_to_bind
     * 
     * @param sym
     * @param q
     * @param type_field
     * @param val_field
     * @throws SQLException
     */
    private void smem_symbol_to_bind( Symbol sym, PreparedStatement q, int type_field, int val_field ) throws SQLException
    {
        q.setInt( type_field, Symbols.getSymbolType(sym) );
        if(sym.asString() != null)
        {
            q.setString(val_field, sym.asString().getValue());
        }
        else if(sym.asInteger() != null)
        {
            q.setInt(val_field, sym.asInteger().getValue());
        }
        else if(sym.asDouble() != null)
        {
            q.setDouble(val_field, sym.asDouble().getValue());
        }
    }
    
    /**
     * semantic_memory.cpp:561:smem_statement_to_symbol
     * 
     * @param q
     * @param type_field
     * @param val_field
     * @return
     * @throws SQLException
     */
    private SymbolImpl smem_statement_to_symbol( ResultSet q, int type_field, int val_field ) throws SQLException
    {
        switch (q.getInt( type_field ) )
        {
            case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                return symbols.createString(q.getString(val_field + 1));

            case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                return symbols.createInteger(q.getInt(val_field + 1));

            case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                return symbols.createDouble(q.getDouble(val_field + 1));

            default:
                return null;
        }
    }

    
    /**
     * semantic_memory.cpp:589:_smem_add_wme
     * 
     * @param state
     * @param id
     * @param attr
     * @param value
     * @param meta
     */
    void _smem_add_wme(IdentifierImpl state, IdentifierImpl id, SymbolImpl attr, SymbolImpl value, boolean meta )
    {
        // this fake preference is just for this state.
        // it serves the purpose of simulating a completely
        // local production firing to provide backtracing
        // information, making the result wmes dependent
        // upon the cue wmes.
        final SemanticMemoryStateInfo smem_info = smem_info(state);
        final Preference pref = SoarModule.make_fake_preference( state, id, attr, value, smem_info.cue_wmes );

        // add the preference to temporary memory
        recMem.add_preference_to_tm( pref );

        // and add it to the list of preferences to be removed
        // when the goal is removed
        state.addGoalPreference(pref);
        pref.on_goal_list = true;


        if ( meta )
        {
            // if this is a meta wme, then it is completely local
            // to the state and thus we will manually remove it
            // (via preference removal) when the time comes
            smem_info.smem_wmes.push( pref );
        }
        else
        {
            // otherwise, we submit the fake instantiation to backtracing
            // such as to potentially produce justifications that can follow
            // it to future adventures (potentially on new states)

            final ByRef<Instantiation> my_justification_list = ByRef.create(null);
            chunker.chunk_instantiation( pref.inst, false, my_justification_list );

            // if any justifications are created, assert their preferences manually
            // (copied mainly from assert_new_preferences with respect to our circumstances)
            if ( my_justification_list.value != null)
            {
                Instantiation next_justification = null;
                
                for ( Instantiation my_justification=my_justification_list.value;
                      my_justification!=null;
                      my_justification=next_justification )
                {
                    next_justification = my_justification.nextInProdList;

                    if ( my_justification.in_ms )
                    {
                        my_justification.prod.instantiations = my_justification.insertAtHeadOfProdList(my_justification.prod.instantiations);
                    }

                    for (Preference just_pref=my_justification.preferences_generated; just_pref!=null; just_pref=just_pref.inst_next ) 
                    {
                        recMem.add_preference_to_tm( just_pref );                        
                        
                        // TODO SMEM WMA
//                        if ( wma_enabled( my_agent ) )
//                        {
//                            wma_activate_wmes_in_pref( my_agent, just_pref );
//                        }
                    }
                }
            }
        }
    }
    
    private void smem_add_retrieved_wme(IdentifierImpl state, IdentifierImpl id, SymbolImpl attr, SymbolImpl value )
    {
        _smem_add_wme( state, id, attr, value, false );
    }

    private void smem_add_meta_wme( IdentifierImpl state, IdentifierImpl id, SymbolImpl attr, SymbolImpl value )
    {
        _smem_add_wme( state, id, attr, value, true );
    }

    
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////
// Variable Functions (smem::var)
//
// Variables are key-value pairs stored in the database
// that are necessary to maintain a store between
// multiple runs of Soar.
//
//////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////

    /**
     * Gets an SMem variable from the database
     * 
     * semantic_memory.cpp:682:smem_variable_get
     * 
     * @param variable_id
     * @param variable_value
     * @return
     * @throws SQLException
     */
    private boolean smem_variable_get(smem_variable_key variable_id, ByRef<Long> variable_value ) throws SQLException
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
     * Sets an SMem variable in the database
     * 
     * semantic_memory.cpp:705:smem_variable_set
     * 
     * @param variable_id
     * @param variable_value
     * @throws SQLException
     */
    private void smem_variable_set(smem_variable_key variable_id, long variable_value ) throws SQLException
    {
        final PreparedStatement var_set = db.var_set;
    
        var_set.setInt( 1, variable_id.ordinal() );
        var_set.setLong( 2, variable_value );
    
        var_set.execute();
    }
    
    /**
     * semantic_memory.cpp:735:smem_temporal_hash
     * 
     * @param sym
     * @return
     * @throws SQLException
     */
    private /*smem_hash_id*/ long smem_temporal_hash(SymbolImpl sym) throws SQLException
    {
        return smem_temporal_hash(sym, true);
    }
    
    /**
     * semantic_memory.cpp:735:smem_temporal_hash
     * 
     * @param sym
     * @param add_on_fail
     * @return
     * @throws SQLException
     */
    private /*smem_hash_id*/ long smem_temporal_hash(SymbolImpl sym, boolean add_on_fail /*= true*/ ) throws SQLException
    {
        /*smem_hash_id*/ long return_val = 0;

        ////////////////////////////////////////////////////////////////////////////
        // TODO SMEM timers: my_agent->smem_timers->hash->start();
        ////////////////////////////////////////////////////////////////////////////

        if ( smem_symbol_is_constant( sym ) )
        {
            if ( ( sym.smem_hash == 0) || ( sym.common_smem_valid != smem_validation ) )
            {
                sym.smem_hash = 0;
                sym.common_smem_valid = smem_validation;

                // basic process:
                // - search
                // - if found, return
                // - else, add

                smem_symbol_to_bind( sym, db.hash_get, 1, 2 );
                final ResultSet rs = db.hash_get.executeQuery();
                try
                {
                    if (rs.next())
                    {
                        return_val = rs.getLong(0 + 1);
                    }
                }
                finally
                {
                    rs.close();
                }

                //

                if ( return_val == 0 && add_on_fail )
                {
                    smem_symbol_to_bind( sym, db.hash_add, 1, 2 );
                    return_val = JdbcTools.insertAndGetRowId(db.hash_add);
                }

                // cache results for later re-use
                sym.smem_hash = return_val;
                sym.common_smem_valid = smem_validation;
            }

            return_val = sym.smem_hash;
        }

        ////////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->hash->stop();
        ////////////////////////////////////////////////////////////////////////////

        return return_val;
    }
    
    /**
     * copied primarily from add_bound_variables_in_test
     * 
     * <p>semantic_memory.cpp:794:_smem_lti_from_test
     * 
     * @param t
     * @param valid_ltis
     */
    private void _smem_lti_from_test( Test t, Set<IdentifierImpl> valid_ltis )
    {
        if(Tests.isBlank(t)) return;
        
        final EqualityTest eq = t.asEqualityTest();
        if (eq != null)
        {
            final IdentifierImpl referent = eq.getReferent().asIdentifier();
            if (referent != null && referent.smem_lti != 0)
            {
                valid_ltis.add( referent );
            }
          
            return;
        }

        {    
            final ConjunctiveTest ct = t.asConjunctiveTest();

            if (ct != null) 
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
     * <p>semantic_memory.cpp:823:_smem_lti_from_rhs_value
     * 
     * @param rv
     * @param valid_ltis
     */
    private void _smem_lti_from_rhs_value( RhsValue rv, Set<IdentifierImpl> valid_ltis )
    {
        final RhsSymbolValue rsv = rv.asSymbolValue();
        if ( rsv != null )
        {
            final IdentifierImpl sym = rsv.getSym().asIdentifier();
            if (sym != null && sym.smem_lti != 0)
            {
                valid_ltis.add( sym );
            }
        }
        else
        {
            for(RhsValue c : rv.asFunctionCall().getArguments())
            {
                _smem_lti_from_rhs_value(c, valid_ltis );
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_valid_production(org.jsoar.kernel.lhs.Condition, org.jsoar.kernel.rhs.Action)
     */
    @Override
    public boolean smem_valid_production(Condition lhs_top, Action rhs_top)
    {
        // semantic_memory.cpp:844:smem_valid_production
        boolean return_val = true;
        
        final Set<IdentifierImpl> valid_ltis = new HashSet<IdentifierImpl>();
        
        // collect valid ltis
        for ( Condition c=lhs_top; c!=null; c=c.next )
        {
            final PositiveCondition pc = c.asPositiveCondition();
            if (pc != null)
            {
                _smem_lti_from_test( pc.attr_test, valid_ltis );
                _smem_lti_from_test( pc.value_test, valid_ltis );
            }
        }

        // validate ltis in actions
        // copied primarily from add_all_variables_in_action
        {
            int action_counter = 0;

            for (Action a=rhs_top; a!=null; a=a.next )
            {       
                a.already_in_tc = false;
                action_counter++;
            }

            // good_pass detects infinite loops
            boolean good_pass = true;
            boolean good_action = true;
            while ( good_pass && action_counter != 0)
            {
                good_pass = false;
                
                for (Action a=rhs_top; a!=null; a=a.next )
                {
                    if ( !a.already_in_tc )
                    {
                        good_action = false;
                        
                        final MakeAction ma = a.asMakeAction();
                        if ( ma != null )
                        {
                            final IdentifierImpl id = ma.id.asSymbolValue().getSym().asIdentifier();

                            // non-identifiers are ok
                            if ( id == null )
                            {
                                good_action = true;
                            }
                            // short-term identifiers are ok
                            else if ( id.smem_lti == 0 )
                            {
                                good_action = true;
                            }
                            // valid long-term identifiers are ok
                            else if ( valid_ltis.contains( id ) )
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
                        if ( good_action )
                        {
                            a.already_in_tc = true;

                            if(ma != null)
                            {
                                _smem_lti_from_rhs_value( ma.value, valid_ltis );
                                _smem_lti_from_rhs_value( ma.attr, valid_ltis );
                            }
                            else
                            {
                                _smem_lti_from_rhs_value( a.asFunctionAction().getCall(), valid_ltis );
                            }

                            // note that we've dealt with another action
                            action_counter--;
                            good_pass = true;
                        }
                    }
                }
            };

            return_val = ( action_counter == 0 );
        }

        return return_val;
    }
    
    /**
     * activates a new or existing long-term identifier
     * 
     * <p>semantic_memory.cpp:957:smem_lti_activate
     * 
     * @param lti
     * @throws SQLException
     */
    void smem_lti_activate(/*smem_lti_id*/ long lti ) throws SQLException
    {
        ////////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->act->start();
        ////////////////////////////////////////////////////////////////////////////

        db.act_lti_child_ct_get.setLong(1, lti);
        final ResultSet rs = db.act_lti_child_ct_get.executeQuery();
        long lti_child_ct = 0;
        try
        {
            rs.next();
            lti_child_ct = rs.getLong(0 + 1);
        }
        finally { rs.close(); }

        if ( lti_child_ct >= params.thresh.get())
        {
            // cycle=? WHERE lti=?
            db.act_lti_set.setLong( 1, smem_max_cycle++);
            db.act_lti_set.setLong( 2, lti );
            db.act_lti_set.execute( /*soar_module::op_reinit*/ );
        }
        else
        {
            // cycle=? WHERE lti=?
            db.act_set.setLong( 1, smem_max_cycle++);
            db.act_set.setLong( 2, lti );
            db.act_set.execute( /*soar_module::op_reinit*/ );
        }

        //db.act_lti_child_ct_get->reinitialize();

        ////////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->act->stop();
        ////////////////////////////////////////////////////////////////////////////
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_lti_get_id(char, long)
     */
    @Override
    public long /*smem_lti_id*/ smem_lti_get_id( char name_letter, long name_number ) throws SoarException
    {
        // semantic_memory.cpp:989:smem_lti_get_id
        
        /*smem_lti_id*/ long return_val = 0;

        // getting lti ids requires an open semantic database
        smem_attach();
        
        try
        {
            // letter=? AND number=?
            db.lti_get.setLong( 1, (long)( name_letter ) );
            db.lti_get.setLong( 2, (long)( name_number ) );

            final ResultSet rs = db.lti_get.executeQuery();
            try
            {
                if (rs.next())
                {
                    return_val = rs.getLong(0 + 1);
                }
            }
            finally
            {
                rs.close();
            }
        }
        catch (SQLException e)
        {
            throw new SoarException(e.getMessage(), e);
        }

        //db.lti_get->reinitialize();

        return return_val;
    }

    /**
     * adds a new lti id for a letter/number pair
     * 
     * <p>semantic_memory.cpp:1011:smem_lti_add_id
     * 
     * @param name_letter
     * @param name_number
     * @return
     * @throws SQLException
     */
    long /*smem_lti_id*/ smem_lti_add_id(char name_letter, long name_number) throws SQLException
    {
        /*smem_lti_id*/ long return_val = 0;

        // create lti: letter, number
        db.lti_add.setLong( 1, (long)( name_letter ) );
        db.lti_add.setLong( 2, (long)( name_number ) );
        db.lti_add.setLong( 3, 0 );
        db.lti_add.setLong( 4, 0 );

        return_val = JdbcTools.insertAndGetRowId(db.lti_add);

        // increment stat
        stats.nodes.set(stats.nodes.get() + 1); // smem_stats->chunks in CSoar

        return return_val;
    }
    
    /**
     * makes a non-long-term identifier into a long-term identifier
     * 
     * <p>semantic_memory.cpp:1031:smem_lti_soar_add
     * 
     * @param s
     * @return
     * @throws SoarException
     * @throws SQLException
     */
    private /*smem_lti_id*/ long smem_lti_soar_add(SymbolImpl s ) throws SoarException, SQLException
    {
        final IdentifierImpl id = s.asIdentifier();
        if ( ( id != null ) &&
             ( id.smem_lti == 0 ) )
        {
            // try to find existing lti
            id.smem_lti = smem_lti_get_id( id.getNameLetter(), id.getNameNumber() );

            // if doesn't exist, add
            if ( id.smem_lti == 0)
            {
                id.smem_lti = smem_lti_add_id( id.getNameLetter(), id.getNameNumber() );

                // TODO SMEM Uncomment and port these lines when epmem is working
                // id.smem_time_id = my_agent->epmem_stats->time->get_value();
                // id.id_smem_valid = my_agent->epmem_validation;
            }
        }

        return id.smem_lti;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_lti_soar_make(long, char, long, int)
     */
    public IdentifierImpl smem_lti_soar_make(/*smem_lti_id*/ long lti, char name_letter, long name_number, /*goal_stack_level*/ int level )
    {
        // semantic_memory.cpp:1053:smem_lti_soar_make

        // try to find existing
        IdentifierImpl return_val = symbols.findIdentifier(name_letter, (int) name_number); // TODO SMEM make name_number long

        // otherwise create
        if ( return_val == null )
        {
            return_val = symbols.make_new_identifier(name_letter, level);
        }
        else
        {
            if ( ( return_val.level == LTI_UNKNOWN_LEVEL ) && ( level != LTI_UNKNOWN_LEVEL ) )
            {
                return_val.level = level;
                return_val.promotion_level = level;
            }
        }

        // set lti field irrespective
        return_val.smem_lti = lti;

        return return_val;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_reset_id_counters()
     */
    @Override
    public void smem_reset_id_counters() throws SoarException
    {
        // semantic_memory.cpp:1082:smem_reset_id_counters
        
        if(db != null /*my_agent->smem_db->get_status() == soar_module::connected*/ )
        {
            try
            {
                final ResultSet rs = db.lti_max.executeQuery();
                try
                {
                    while (rs.next())
                    {
                        // letter, max
                        final long name_letter = rs.getLong(0 + 1);
                        final long letter_max = rs.getLong(1 + 1);
   
                        // shift to alphabet
                        // name_letter -= (long)( 'A' );
   
                        symbols.resetIdNumber((char) name_letter, letter_max);
                    }
                }
                finally
                {
                    rs.close();
                }
            }
            catch (SQLException e)
            {
                throw new SoarException(e.getMessage(), e);
            }

            // db.lti_max->reinitialize();
        }
    }
    
    /**
     * <p>semantic_memory.cpp:1116:smem_make_slot
     * 
     * @param slots
     * @param attr
     * @return
     */
    private static List<smem_chunk_value> smem_make_slot( Map<SymbolImpl, List<smem_chunk_value>> slots, SymbolImpl attr )
    {
        List<smem_chunk_value> s = slots.get(attr);
        if(s == null)
        {
            s = new LinkedList<smem_chunk_value>();
            slots.put(attr, s);
        }
        return s;
    }
    
    /**
     * <p>semantic_memory.cpp:1128:smem_disconnect_chunk
     * 
     * @param parent_id
     * @throws SQLException
     */
    void smem_disconnect_chunk(/*smem_lti_id*/ long parent_id ) throws SQLException
    {
        // adjust attribute counts
        {
            long counter = 0;
            
            // get all old counts
            db.web_attr_ct.setLong( 1, parent_id );
            final ResultSet webAttrCounts = db.web_attr_ct.executeQuery();
            try
            {
                while (webAttrCounts.next())
                {
                    counter += webAttrCounts.getLong( 1 + 1);
                    
                    // adjust in opposite direction ( adjust, attribute )
                    db.ct_attr_update.setLong( 1, -( webAttrCounts.getLong( 1 + 1) ) );
                    db.ct_attr_update.setLong( 2, webAttrCounts.getLong( 0 + 1 ) );
                    db.ct_attr_update.executeUpdate( /*soar_module::op_reinit*/ );
                }
            }
            finally
            {
                webAttrCounts.close();
            }
            //db.web_attr_ct->reinitialize();

            stats.edges.set(stats.edges.get() - counter); // smem_stats->slots in CSoar
        }

        // adjust const counts
        {
            // get all old counts
            db.web_const_ct.setLong( 1, parent_id );
            final ResultSet webConstCounts = db.web_const_ct.executeQuery();
            try
            {
                while ( webConstCounts.next() )
                {
                    // adjust in opposite direction ( adjust, attribute, const )
                    db.ct_const_update.setLong( 1, -( webConstCounts.getLong( 2 + 1 ) ) );
                    db.ct_const_update.setLong( 2, webConstCounts.getLong( 0 + 1 ) );
                    db.ct_const_update.setLong( 3, webConstCounts.getLong( 1 + 1 ) );
                    db.ct_const_update.executeUpdate( /*soar_module::op_reinit*/ );
                }
            }
            finally
            {
                webConstCounts.close();
            }
            //db.web_const_ct->reinitialize();
        }

        // adjust lti counts
        {
            // get all old counts
            db.web_lti_ct.setLong( 1, parent_id );
            final ResultSet webLtiCounts = db.web_lti_ct.executeQuery();
            try
            {
                while ( webLtiCounts.next() )
                {
                    // adjust in opposite direction ( adjust, attribute, lti )
                    db.ct_lti_update.setLong( 1, -( webLtiCounts.getLong( 2 + 1 ) ) );
                    db.ct_lti_update.setLong( 2, webLtiCounts.getLong( 0 + 1) );
                    db.ct_lti_update.setLong( 3, webLtiCounts.getLong( 1 + 1) );
                    db.ct_lti_update.executeUpdate( /*soar_module::op_reinit*/ );
                }
            }
            finally
            {
                webLtiCounts.close();
            }

            //db.web_lti_ct->reinitialize();
        }

        // disconnect
        {
            db.web_truncate.setLong( 1, parent_id );
            db.web_truncate.executeUpdate( /*soar_module::op_reinit*/ );
        }
    }

    /**
     * <p>semantic_memory.cpp:1187:smem_store_chunk
     * 
     * @param parent_id
     * @param children
     * @throws SQLException
     */
    void smem_store_chunk(/*smem_lti_id*/ long parent_id, Map<SymbolImpl, List<smem_chunk_value>> children) throws SQLException
    {
        smem_store_chunk(parent_id, children, true);
    }
    
    /**
     * <p>semantic_memory.cpp:1187:smem_store_chunk
     * 
     * @param parent_id
     * @param children
     * @param remove_old_children
     * @throws SQLException
     */
    void smem_store_chunk(/*smem_lti_id*/ long parent_id, Map<SymbolImpl, List<smem_chunk_value>> children, boolean remove_old_children /*= true*/ ) throws SQLException
    {
        long /*smem_hash_id*/ attr_hash = 0;
        long /*smem_hash_id*/ value_hash = 0;
        long /*smem_lti_id*/ value_lti = 0;

        Map</*smem_hash_id*/ Long, Long> attr_ct_adjust= new HashMap<Long, Long>();
        Map</*smem_hash_id*/ Long, Map</*smem_hash_id*/ Long, Long> > const_ct_adjust = new HashMap<Long, Map<Long,Long>>();
        Map</*smem_hash_id*/ Long, Map</*smem_lti_id*/ Long, Long> > lti_ct_adjust = new HashMap<Long, Map<Long,Long>>();
        long stat_adjust = 0;

        long next_act_cycle = smem_max_cycle++;
        
        // clear web, adjust counts
        long child_ct = 0;
        if ( remove_old_children )
        {
            smem_disconnect_chunk( parent_id );
        }
        else
        {
            db.act_lti_child_ct_get.setLong( 1, parent_id );
            final ResultSet rs = db.act_lti_child_ct_get.executeQuery();
            try
            {
                child_ct = rs.getLong(0 + 1);
            }
            finally
            {
                rs.close();
            }

            // db.act_lti_child_ct_get->reinitialize();
        }

        // already above threshold?
        long thresh = params.thresh.get();
        boolean before_above = ( child_ct >= thresh );

        // get final count
        {
            for(Map.Entry<SymbolImpl, List<smem_chunk_value>> s : children.entrySet())
            {
                for(smem_chunk_value v : s.getValue())
                {
                    child_ct++; // TODO SMEM Just add size()?
                }
            }
        }

        // above threshold now?
        boolean after_above = ( child_ct >= thresh );
        long web_act_cycle = ( ( after_above )?( SMEM_ACT_MAX ):( next_act_cycle ) );

        // if didn't clear and wasn't already above, need to update kids
        if ( ( !remove_old_children ) && ( !before_above ) )
        {
            db.act_set.setLong( 1, web_act_cycle );
            db.act_set.setLong( 2, parent_id );
            db.act_set.executeUpdate( /*soar_module::op_reinit*/ );
        }

        // if above threshold, update parent activation
        if ( after_above )
        {
            db.act_lti_set.setLong( 1, next_act_cycle );
            db.act_lti_set.setLong( 2, parent_id );
            db.act_lti_set.executeUpdate( /*soar_module::op_reinit*/ );
        }

        // for all slots
        for (Map.Entry<SymbolImpl, List<smem_chunk_value>> s : children.entrySet())
        {
            // get attribute hash and contribute to count adjustment
            attr_hash = smem_temporal_hash( s.getKey() );
            final Long countForAttrHash = attr_ct_adjust.get(attr_hash);
            attr_ct_adjust.put(attr_hash, countForAttrHash != null ? countForAttrHash + 1 : 0 + 1);
            stat_adjust++;

            // for all values in the slot
            for (smem_chunk_value v : s.getValue())
            {           
                // most handling is specific to constant vs. identifier
                final SymbolImpl constant = v.asConstant();
                if ( constant != null )
                {
                    value_hash = smem_temporal_hash( constant );

                    // parent_id, attr, val_const, val_lti, act_cycle
                    db.web_add.setLong( 1, parent_id );
                    db.web_add.setLong( 2, attr_hash );
                    db.web_add.setLong( 3, value_hash );
                    db.web_add.setNull( 4, java.sql.Types.NULL); //db.web_add->bind_null( 4 );
                    db.web_add.setLong( 5, web_act_cycle );
                    db.web_add.executeUpdate( /*soar_module::op_reinit*/ );

                    // TODO SMEM clean this up
                    Map<Long, Long> forHash = const_ct_adjust.get(attr_hash);
                    if(forHash == null)
                    {
                        forHash = new HashMap<Long, Long>();
                        const_ct_adjust.put(attr_hash, forHash);
                    }
                    final Long countForValueHash = forHash.get(value_hash);
                    forHash.put(value_hash, countForValueHash != null ? countForValueHash + 1 : 0 + 1);
                    //const_ct_adjust[ attr_hash ][ value_hash ]++;
                }
                else
                {
                    value_lti = v.asLti().lti_id; // (*v)->val_lti.val_value->lti_id;
                    if ( value_lti == 0 )
                    {
                        value_lti = smem_lti_add_id( v.asLti().lti_letter, v.asLti().lti_number );
                        v.asLti().lti_id = value_lti;

                        if ( v.asLti().soar_id != null )
                        {
                            v.asLti().soar_id.smem_lti = value_lti;

                            // TODO SMEM uncomment and implement when epmem is implemented
                            // v.asLti().soar_id.smem_time_id = my_agent->epmem_stats->time->get_value();
                            // v.asLti().soar_id.smem_valid = my_agent->epmem_validation;
                        }
                    }

                    // parent_id, attr, val_const, val_lti, act_cycle
                    db.web_add.setLong( 1, parent_id );
                    db.web_add.setLong( 2, attr_hash );
                    db.web_add.setNull(3, java.sql.Types.NULL); // db.web_add->bind_null( 3 );
                    db.web_add.setLong( 4, value_lti );
                    db.web_add.setLong( 5, web_act_cycle );
                    db.web_add.executeUpdate( /*soar_module::op_reinit*/ );

                    // add to counts
                    // TODO SMEM clean this up
                    Map<Long, Long> forHash = lti_ct_adjust.get(attr_hash);
                    if(forHash == null)
                    {
                        forHash = new HashMap<Long, Long>();
                        lti_ct_adjust.put(attr_hash, forHash);
                    }
                    final Long countForValueHash = forHash.get(value_lti);
                    forHash.put(value_lti, countForValueHash != null ? countForValueHash + 1 : 0 + 1);
                    
                    //lti_ct_adjust[ attr_hash ][ value_lti ]++;
                }
            }
        }

        // update stat
        {
            stats.edges.set(stats.edges.get() + stat_adjust); // smem_stats->slots in CSoar
        }

        // update attribute counts
        {
            for(Map.Entry<Long, Long> p : attr_ct_adjust.entrySet())
            {
                // make sure counter exists (attr)
                db.ct_attr_add.setLong( 1, p.getKey() );
                db.ct_attr_add.executeUpdate( /*soar_module::op_reinit*/ );

                // adjust count (adjustment, attr)
                db.ct_attr_update.setLong( 1, p.getValue() );
                db.ct_attr_update.setLong( 2, p.getKey() );
                db.ct_attr_update.executeUpdate( /*soar_module::op_reinit*/ );
            }
        }

        // update constant counts
        {
            for(Map.Entry<Long, Map<Long, Long>> p1 : const_ct_adjust.entrySet())
            {
                for(Map.Entry<Long, Long> p2 : p1.getValue().entrySet())
                {
                    // make sure counter exists (attr, val)
                    db.ct_const_add.setLong( 1, p1.getKey() );
                    db.ct_const_add.setLong( 2, p2.getKey() );
                    db.ct_const_add.executeUpdate( /*soar_module::op_reinit*/ );

                    // adjust count (adjustment, attr, val)
                    db.ct_const_update.setLong( 1, p2.getValue() );
                    db.ct_const_update.setLong( 2, p1.getKey() );
                    db.ct_const_update.setLong( 3, p2.getKey() );
                    db.ct_const_update.executeUpdate( /*soar_module::op_reinit*/ );
                }
            }
        }

        // update lti counts
        {
            for(Map.Entry<Long, Map<Long, Long>> p1 : lti_ct_adjust.entrySet())
            {
                for(Map.Entry<Long, Long> p2 : p1.getValue().entrySet())
                {
                    // make sure counter exists (attr, lti)
                    db.ct_lti_add.setLong( 1, p1.getKey() );
                    db.ct_lti_add.setLong( 2, p2.getKey() );
                    db.ct_lti_add.executeUpdate( /*soar_module::op_reinit*/ );

                    // adjust count (adjustment, attr, lti)
                    db.ct_lti_update.setLong( 1, p2.getValue() );
                    db.ct_lti_update.setLong( 2, p1.getKey() );
                    db.ct_lti_update.setLong( 3, p2.getKey() );
                    db.ct_lti_update.executeUpdate( /*soar_module::op_reinit*/ );
                }
            }
        }

        // update child count
        {
            db.act_lti_child_ct_set.setLong( 1, child_ct );
            db.act_lti_child_ct_set.setLong( 2, parent_id );
            db.act_lti_child_ct_set.executeUpdate( /*soar_module::op_reinit*/ );
        }
    }

    /**
     * <p>semantic_memory.cpp:1387:smem_soar_store
     * 
     * @param id
     * @throws SQLException
     * @throws SoarException
     */
    void smem_soar_store(IdentifierImpl id) throws SQLException, SoarException
    {
        smem_soar_store(id, smem_storage_type.store_level);
    }
    
    /**
     * <p>semantic_memory.cpp:1387:smem_soar_store
     * 
     * @param id
     * @param store_type
     * @throws SQLException
     * @throws SoarException
     */
    void smem_soar_store(IdentifierImpl id, smem_storage_type store_type) throws SQLException, SoarException
    {
        smem_soar_store(id, store_type, null);
    }

    /**
     * <p>semantic_memory.cpp:1387:smem_soar_store
     * 
     * @param id
     * @param store_type
     * @param tc
     * @throws SQLException
     * @throws SoarException
     */
    void smem_soar_store(IdentifierImpl id, smem_storage_type store_type /*= store_level*/, /*tc_number*/ Marker tc /*= null*/) throws SQLException, SoarException
    {
        // transitive closure only matters for recursive storage
        if ( ( store_type == smem_storage_type.store_recursive ) && ( tc == null ) )
        {
            tc = DefaultMarker.create();
        }

        // get level
        final List<WmeImpl> children = smem_get_direct_augs_of_id( id, tc );

        // encode this level
        {
            final Map<SymbolImpl, smem_chunk_value> sym_to_chunk = new HashMap<SymbolImpl, smem_chunk_value>();
            //smem_chunk **c;

            Map<SymbolImpl, List<smem_chunk_value>> slots = new HashMap<SymbolImpl, List<smem_chunk_value>>();

            for (WmeImpl w : children)
            {
                // get slot
                final List<smem_chunk_value> s = smem_make_slot( slots , w.attr );

                // create value, per type
                final smem_chunk_value v;
                if ( smem_symbol_is_constant( w.value ) )
                {
                    v = new smem_chunk_constant(w.value);
                }
                else
                {
                    // try to find existing chunk
                    smem_chunk_value c = sym_to_chunk.get(w.value);
                    // if doesn't exist, add; else use existing
                    if(c == null)
                    {
                        final smem_chunk_lti lti = new smem_chunk_lti();
                        lti.lti_id = w.value.asIdentifier().smem_lti;
                        lti.lti_letter = w.value.asIdentifier().getNameLetter();
                        lti.lti_number = w.value.asIdentifier().getNameNumber();
                        lti.slots = null;
                        lti.soar_id = w.value.asIdentifier();
                        
                        c = lti;
                    }
                    v = c;
                }

                // add value to slot
                s.add( v );
            }

            smem_store_chunk( smem_lti_soar_add( id ), slots);

            // clean up
            /*
            {
                // de-allocate slots
                for ( s_p=slots.begin(); s_p!=slots.end(); s_p++ )
                {
                    for ( v_p=s_p->second->begin(); v_p!=s_p->second->end(); v_p++ )
                    {
                        delete (*v_p);
                    }

                    delete s_p->second;
                }

                // de-allocate chunks
                for ( c_p=sym_to_chunk.begin(); c_p!=sym_to_chunk.end(); c_p++ )
                {
                    delete c_p->second;
                }
            }
            */
        }

        // recurse as necessary
        if ( store_type == smem_storage_type.store_recursive )
        {
            for (WmeImpl w : children)
            {
                if ( !smem_symbol_is_constant( w.value ) )
                {
                    smem_soar_store( w.value.asIdentifier(), store_type, tc );
                }
            }
        }

        // clean up child wme list
        //delete children;
    }
    
    /**
     * <p>semantic_memory.cpp:1494:smem_install_memory
     * 
     * @param state
     * @param parent_id
     * @throws SQLException
     */
    void smem_install_memory(IdentifierImpl state, long /*smem_lti_id*/ parent_id) throws SQLException
    {
        smem_install_memory(state, parent_id, null);
    }
    
    /**
     * <p>semantic_memory.cpp:1494:smem_install_memory
     * 
     * @param state
     * @param parent_id
     * @param lti
     * @throws SQLException
     */
    void smem_install_memory(IdentifierImpl state, long /*smem_lti_id*/ parent_id, IdentifierImpl lti /*= NIL*/ ) throws SQLException
    {
        ////////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->ncb_retrieval->start();
        ////////////////////////////////////////////////////////////////////////////

        // get the ^result header for this state
        final SemanticMemoryStateInfo info = smem_info(state);
        final IdentifierImpl result_header = info.smem_result_header;

        // get identifier if not known
        boolean lti_created_here = false;
        if ( lti == null )
        {
            db.lti_letter_num.setLong(1, parent_id);
            final ResultSet rs = db.lti_letter_num.executeQuery();
            try
            {
                lti = smem_lti_soar_make( parent_id, 
                        (char) rs.getLong(0 + 1), 
                        rs.getLong(1 + 1), 
                        result_header.level );
            }
            finally
            {
                rs.close();
            }
            lti_created_here = true;
        }

        // activate lti
        smem_lti_activate(parent_id);

        // point retrieved to lti
        smem_add_meta_wme(state, result_header, predefinedSyms.smem_sym_retrieved, lti );
        if ( lti_created_here )
        {
            // if the identifier was created above we need to
            // remove a single ref count AFTER the wme
            // is added (such as to not deallocate the symbol
            // prematurely)
            // Not needed in JSoar
            // symbol_remove_ref( my_agent, lti );
        }   

        // if no children, then retrieve children
        if ( ( lti.getImpasseWmes() == null ) &&
             ( lti.getInputWmes() == null ) &&
             ( lti.slots == null ) )
        {
            // get direct children: attr_const, attr_type, value_const, value_type, value_letter, value_num, value_lti
            db.web_expand.setLong( 1, parent_id );
            final ResultSet rs = db.web_expand.executeQuery();
            try
            {
                while (rs.next())
                {
                    // make the identifier symbol irrespective of value type
                    final SymbolImpl attr_sym = smem_statement_to_symbol( rs, 1, 0 );
    
                    // identifier vs. constant
                    final SymbolImpl value_sym;
                    if(rs.getMetaData().getColumnType(2 + 1) == java.sql.Types.NULL)
                    {
                        value_sym = smem_lti_soar_make(rs.getLong(6 + 1), 
                                                       (char) rs.getLong(4 + 1), 
                                                       rs.getLong(5 + 1), 
                                                       lti.level );
                    }
                    else
                    {
                        value_sym = smem_statement_to_symbol( rs, 3, 2 );
                    }
    
                    // add wme
                    smem_add_retrieved_wme( state, lti, attr_sym, value_sym );
    
                    // deal with ref counts - attribute/values are always created in this function
                    // (thus an extra ref count is set before adding a wme)
                    // Not needed in JSoar
                    //symbol_remove_ref( my_agent, attr_sym );
                    //symbol_remove_ref( my_agent, value_sym );
                }
            }
            finally
            {
                rs.close();
            }
            
        }

        ////////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->ncb_retrieval->stop();
        ////////////////////////////////////////////////////////////////////////////
    }

    /**
     * <p>semantic_memory.cpp:1582:smem_process_query
     * 
     * @param state
     * @param query
     * @param prohibit
     * @return
     * @throws SQLException
     */
    long /*smem_lti_id*/ smem_process_query(IdentifierImpl state, IdentifierImpl query, 
            Set<Long> /*smem_lti_set*/ prohibit) throws SQLException
    {
        return smem_process_query(state, query, prohibit, smem_query_levels.qry_full);
    }

    /**
     * <p>semantic_memory.cpp:1582:smem_process_query
     * 
     * @param state
     * @param query
     * @param prohibit
     * @param query_level
     * @return
     * @throws SQLException
     */
    long /*smem_lti_id*/ smem_process_query(IdentifierImpl state, IdentifierImpl query, 
            Set<Long> /*smem_lti_set*/ prohibit, 
            smem_query_levels query_level /*= qry_full*/ ) throws SQLException
    {   
        final SemanticMemoryStateInfo smem_info = smem_info(state);
        final LinkedList<WeightedCueElement> weighted_cue = new LinkedList<WeightedCueElement>();    
        boolean good_cue = true;

        long /*smem_lti_id*/ king_id = 0;

        ////////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->query->start();
        ////////////////////////////////////////////////////////////////////////////

        // prepare query stats
        {
            final PriorityQueue<WeightedCueElement> weighted_pq = WeightedCueElement.newPriorityQueue();
            
            final List<WmeImpl> cue = smem_get_direct_augs_of_id( query );

            smem_cue_element_type element_type = smem_cue_element_type.attr_t;

            for (WmeImpl w : cue)
            {
                smem_info.cue_wmes.add( w );

                if ( good_cue )
                {
                    // we only have to do hard work if
                    final long attr_hash = smem_temporal_hash( w.attr, false );
                    if ( attr_hash != 0 )
                    {
                        long value_lti = 0;
                        long value_hash = 0;
                        PreparedStatement q = null;
                        if ( smem_symbol_is_constant( w.value ) )
                        {
                            value_lti = 0;
                            value_hash = smem_temporal_hash( w.value, false );

                            if ( value_hash != 0 )
                            {
                                q = db.ct_const_get;
                                q.setLong( 1, attr_hash );
                                q.setLong( 2, value_hash );

                                element_type = smem_cue_element_type.value_const_t;
                            }
                            else
                            {
                                good_cue = false;
                            }
                        }
                        else
                        {
                            value_lti = w.value.asIdentifier().smem_lti;
                            value_hash = 0;

                            if ( value_lti == 0 )
                            {
                                q = db.ct_attr_get;
                                q.setLong( 1, attr_hash );

                                element_type = smem_cue_element_type.attr_t;
                            }
                            else
                            {
                                q = db.ct_lti_get;
                                q.setLong( 1, attr_hash );
                                q.setLong( 2, value_lti );

                                element_type = smem_cue_element_type.value_lti_t;
                            }
                        }

                        if ( good_cue )
                        {
                            final ResultSet rs = q.executeQuery();
                            try
                            {
                                if ( rs.next() )
                                {
                                    final WeightedCueElement new_cue_element = new WeightedCueElement();
    
                                    new_cue_element.weight = rs.getLong( 0 + 1);
                                    new_cue_element.attr_hash = attr_hash;
                                    new_cue_element.value_hash = value_hash;
                                    new_cue_element.value_lti = value_lti;
                                    new_cue_element.cue_element = w;
    
                                    new_cue_element.element_type = element_type;
    
                                    weighted_pq.add( new_cue_element );
                                }
                                else
                                {
                                    good_cue = false;
                                }
                            }
                            finally
                            {
                                rs.close();
                            }

                            //q->reinitialize();
                        }
                    }
                    else
                    {
                        good_cue = false;
                    }
                }
            }

            // if valid cue, transfer priority queue to list
            if ( good_cue )
            {
                while ( !weighted_pq.isEmpty() )
                {
                    weighted_cue.add( weighted_pq.remove() ); // top()/pop()
                }
            }
            // else deallocate priority queue contents
            else
            {
                while ( !weighted_pq.isEmpty() )
                {
                    weighted_pq.remove(); // top()/pop()
                }
            }

            // clean cue irrespective of validity
            //delete cue;
        }

        // only search if the cue was valid
        if ( good_cue && !weighted_cue.isEmpty() )
        {
            final WeightedCueElement first_element = weighted_cue.iterator().next();

            PreparedStatement q = null;
            PreparedStatement q2 = null;

            long /*smem_lti_id*/ cand;
            boolean good_cand;
            
            // setup first query, which is sorted on activation already
            {
                if ( first_element.element_type == smem_cue_element_type.attr_t )
                {
                    // attr=?
                    q = db.web_attr_all;
                }
                else if ( first_element.element_type == smem_cue_element_type.value_const_t )
                {
                    // attr=? AND val_const=?
                    q = db.web_const_all;
                    q.setLong( 2, first_element.value_hash );
                }
                else if ( first_element.element_type == smem_cue_element_type.value_lti_t )
                {
                    // attr=? AND val_lti=?
                    q = db.web_lti_all;
                    q.setLong( 2, first_element.value_lti );
                }

                // all require hash as first parameter
                q.setLong( 1, first_element.attr_hash );
            }

            final ResultSet qrs = q.executeQuery();
            try
            {
                if ( qrs.next() )
                {
                    final PriorityQueue<ActivatedLti> plentiful_parents = ActivatedLti.newPriorityQueue();
                    boolean more_rows = true;
                    boolean use_db = false;
    
                    while ( more_rows && ( qrs.getLong( 1 + 1 ) == SMEM_ACT_MAX ) )
                    {
                        db.act_lti_get.setLong( 1, qrs.getLong( 0 + 1 ) );
                        final ResultSet actLtiGetRs = db.act_lti_get.executeQuery();
                        try
                        {
                            plentiful_parents.add(new ActivatedLti(actLtiGetRs.getLong( 0 + 1 ), 
                                                                   qrs.getLong( 0 + 1 ) ) );
                        }
                        finally
                        {
                            actLtiGetRs.close();
                        }
                        //my_agent->smem_stmts->act_lti_get->reinitialize();
    
                        more_rows = qrs.next(); //( q->execute() == soar_module::row );
                    }
    
                    while ( ( king_id == 0 ) && ( ( more_rows ) || ( !plentiful_parents.isEmpty() ) ) )
                    {
                        // choose next candidate (db vs. priority queue)
                        {               
                            use_db = false;
                            
                            if ( !more_rows )
                            {
                                use_db = false;
                            }
                            else if ( plentiful_parents.isEmpty() )
                            {
                                use_db = true;
                            }
                            else
                            {
                                use_db = ( qrs.getLong( 1 + 1) >  plentiful_parents.peek().first );                       
                            }
    
                            if ( use_db )
                            {
                                cand = qrs.getLong( 0 + 1 );
                                more_rows = qrs.next(); // ( q->execute() == soar_module::row );
                            }
                            else
                            {
                                cand = plentiful_parents.remove().second; // top()/pop()
                            }
                        }
    
                        // if not prohibited, submit to the remaining cue elements
                        if (!prohibit.contains(cand))
                        {
                            good_cand = true;
    
                            final Iterator<WeightedCueElement> it = weighted_cue.iterator();
                            it.next(); // skip first element
                            for ( ; ( ( good_cand ) && ( it.hasNext() ) );  )
                            {
                                final WeightedCueElement next_element = it.next();
                                if ( next_element.element_type == smem_cue_element_type.attr_t )
                                {
                                    // parent=? AND attr=?
                                    q2 = db.web_attr_child;
                                }
                                else if ( next_element.element_type == smem_cue_element_type.value_const_t )
                                {
                                    // parent=? AND attr=? AND val_const=?
                                    q2 = db.web_const_child;
                                    q2.setLong( 3, next_element.value_hash );
                                }
                                else if ( next_element.element_type == smem_cue_element_type.value_lti_t )
                                {
                                    // parent=? AND attr=? AND val_lti=?
                                    q2 = db.web_lti_child;
                                    q2.setLong( 3, next_element.value_lti );
                                }
    
                                // all require own id, attribute
                                q2.setLong( 1, cand );
                                q2.setLong( 2, next_element.attr_hash );
    
                                final ResultSet q2rs = q2.executeQuery();
                                try
                                {
                                    good_cand = q2rs.next(); //( q2->execute( soar_module::op_reinit ) == soar_module::row );
                                }
                                finally
                                {
                                    q2rs.close();
                                }
                            }
    
                            if ( good_cand )
                            {
                                king_id = cand;
                            }
                        }
                    }
                }
            }
            finally
            {
                qrs.close();
            }
            //q->reinitialize();      

            // clean weighted cue
            // Not needed in JSoar
//            for ( next_element=weighted_cue.begin(); next_element!=weighted_cue.end(); next_element++ )
//            {
//                delete (*next_element);
//            }
        }

        // reconstruction depends upon level
        if ( query_level == smem_query_levels.qry_full )
        {
            // produce results
            if ( king_id != 0 )
            {
                // success!
                smem_add_meta_wme( state, smem_info.smem_result_header, predefinedSyms.smem_sym_success, query );

                ////////////////////////////////////////////////////////////////////////////
                // TODO SMEM Timers: my_agent->smem_timers->query->stop();
                ////////////////////////////////////////////////////////////////////////////

                smem_install_memory( state, king_id );
            }
            else
            {
                smem_add_meta_wme( state, smem_info.smem_result_header, predefinedSyms.smem_sym_failure, query );

                ////////////////////////////////////////////////////////////////////////////
                // TODO SMEM Timers: my_agent->smem_timers->query->stop();
                ////////////////////////////////////////////////////////////////////////////
            }
        }
        else
        {
            ////////////////////////////////////////////////////////////////////////////
            // TODO SMEM Timers: my_agent->smem_timers->query->stop();
            ////////////////////////////////////////////////////////////////////////////
        }

        return king_id;
    }
    
    /**
     * <p>semantic_memory.cpp:1892:smem_clear_result
     * 
     * @param state
     */
    void smem_clear_result( IdentifierImpl state )
    {
        final SemanticMemoryStateInfo smem_info = smem_info(state);
        while ( !smem_info.smem_wmes.isEmpty())
        {
            final Preference pref = smem_info.smem_wmes.remove(); // top()/pop()

            if ( pref.isInTempMemory() )
            {
                recMem.remove_preference_from_tm( pref );
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_reset(org.jsoar.kernel.symbols.IdentifierImpl)
     */
    @Override
    public void smem_reset(IdentifierImpl state )
    {
        // semantic_memory.cpp:1913:smem_reset
        if ( state == null )
        {
            state = decider.top_goal;
        }

        while( state != null )
        {
            final SemanticMemoryStateInfo data = smem_info(state);
            
            data.last_cmd_time[0] = 0;
            data.last_cmd_time[1] = 0;
            data.last_cmd_count[0] = 0;
            data.last_cmd_count[1] = 0;

            data.cue_wmes.clear();
            
            // this will be called after prefs from goal are already removed,
            // so just clear out result stack
            while ( !data.smem_wmes.isEmpty() )
            {
                data.smem_wmes.pop();
            }       

            state = state.lower_goal;
        }
    }
    
    /**
     * Opens the SQLite database and performs all initialization required for
     * the current mode
     * 
     * <p>semantic_memory.cpp:1952:smem_init_db
     * 
     * @throws SoarException
     * @throws SQLException
     * @throws IOException
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
     * <p>semantic_memory.cpp:1952:smem_init_db
     * 
     * @param readonly
     * @throws SoarException
     * @throws SQLException
     * @throws IOException
     */
    void smem_init_db(boolean readonly /*= false*/ ) throws SoarException, SQLException, IOException
    {
        if (db != null /* my_agent->smem_db->get_status() != soar_module::disconnected */ )
        {
            return;
        }

        ////////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers my_agent->smem_timers->init->start();
        ////////////////////////////////////////////////////////////////////////////
        
        // attempt connection
        final Connection connection = JdbcTools.connect(params.driver.get(), 
                                                        params.path.get());
        try
        {
            db = new SemanticMemoryDatabase(connection);
        }
        catch(SoarException e)
        {
            connection.close();
            throw e;
        }

        // temporary queries for one-time init actions

        // apply performance options
        {
            // cache
            {
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
                // synchronous - don't wait for writes to complete (can corrupt the db in case unexpected crash during transaction)
                final Statement s = db.getConnection().createStatement();
                try
                {
                    s.execute("PRAGMA synchronous = OFF" );
    
                    // journal_mode - no atomic transactions (can result in database corruption if crash during transaction)
                    s.execute("PRAGMA journal_mode = OFF" );
                    
                    // locking_mode - no one else can view the database after our first write
                    s.execute("PRAGMA locking_mode = EXCLUSIVE" );
                }
                finally
                {
                    s.close();
                }
            }
        }

        // update validation count
        smem_validation++;

        // setup common structures/queries
        db.structure();
        db.prepare();

        // reset identifier counters
        smem_reset_id_counters();

        db.begin.executeUpdate( /*soar_module::op_reinit*/ );

        if ( !readonly )
        {
            final ByRef<Long> tempMaxCycle = ByRef.create(smem_max_cycle);
            if ( !smem_variable_get( smem_variable_key.var_max_cycle, tempMaxCycle ) )
            {
                smem_max_cycle = 1;
            }
            else
            {
                smem_max_cycle = tempMaxCycle.value;
            }

            {
                ByRef<Long> temp = ByRef.create(0L);

                // threshold
                if ( smem_variable_get(smem_variable_key.var_act_thresh, temp ) )
                {
                    params.thresh.set(temp.value);
                }
                else
                {
                    smem_variable_set(smem_variable_key.var_act_thresh, params.thresh.get());
                }

                // nodes
                if ( smem_variable_get(smem_variable_key.var_num_nodes, temp ) )
                {
                    stats.nodes.set(temp.value);
                }
                else
                {
                    stats.nodes.set(0L);
                }

                // edges
                if ( smem_variable_get(smem_variable_key.var_num_edges, temp ) )
                {
                    stats.edges.set(temp.value);
                }
                else
                {
                    stats.edges.set(0L);
                }
            }
        }

        db.commit.executeUpdate( /*soar_module::op_reinit*/ );

        // if lazy commit, then we encapsulate the entire lifetime of the agent in a single transaction
        if (params.lazy_commit.get())
        {
            db.begin.executeUpdate( /*soar_module::op_reinit*/ );
        }

        ////////////////////////////////////////////////////////////////////////////
        // TODO SMEM Timers: my_agent->smem_timers->init->stop();
        // TODO SMEM Timers: do this in finally for exception handling above
        ////////////////////////////////////////////////////////////////////////////
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_attach()
     */
    @Override
    public void smem_attach() throws SoarException
    {
        // semantic_memory.cpp:2112:smem_attach
        if (db == null)
        {
            try
            {
                smem_init_db();
            }
            catch (SQLException e)
            {
                throw new SoarException("While attaching SMEM: " + e.getMessage(), e);
            }
            catch (IOException e)
            {
                throw new SoarException("While attaching SMEM: " + e.getMessage(), e);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_close()
     */
    @Override
    public void smem_close() throws SoarException
    {
        // semantic_memory.cpp:2126:smem_close
        if (db != null)
        {
            try
            {
                // store max cycle for future use of the smem database
                smem_variable_set( smem_variable_key.var_max_cycle, smem_max_cycle );

                // store num nodes/edges for future use of the smem database
                smem_variable_set( smem_variable_key.var_num_nodes, stats.nodes.get() );
                smem_variable_set( smem_variable_key.var_num_edges, stats.edges.get() );

                // if lazy, commit
                if (params.lazy_commit.get())
                {
                    db.commit.executeUpdate( /*soar_module::op_reinit*/ );
                }

                // close the database
                db.getConnection().close();
                db = null;
            }
            catch (SQLException e)
            {
                throw new SoarException("While closing SMEM: " + e.getMessage(), e);
            }
        }
    }

    /**
     * <p>semantic_memory.cpp:2158:smem_deallocate_chunk
     */
    static void smem_deallocate_chunk(smem_chunk_value chunk)
    {
        smem_deallocate_chunk(chunk, true);
    }

    /**
     * <p>semantic_memory.cpp:2158:smem_deallocate_chunk
     */
    static void smem_deallocate_chunk(smem_chunk_value chunk, boolean free_chunk /*= true*/ )
    {
        // Nothing to do in JSoar. Yay!
    }
    
    /**
     * <p>semantic_memory.cpp:2217:smem_parse_lti_name
     * 
     * @param lexeme
     * @return
     */
    static ParsedLtiName smem_parse_lti_name(Lexeme lexeme)
    {
        if ( lexeme.type == LexemeType.IDENTIFIER )
        {
            return new ParsedLtiName(String.format("%c%d", lexeme.id_letter, lexeme.id_number), 
                                     lexeme.id_letter, lexeme.id_number);
        }
        else
        {
            return new ParsedLtiName(lexeme.string, Character.toUpperCase(lexeme.string.charAt(1)), 0);
        }
    }
    
    /**
     * <p>semantic_memory.cpp:2243:smem_parse_constant_attr
     * 
     * @param syms
     * @param lexeme
     * @return
     */
    static SymbolImpl smem_parse_constant_attr( SymbolFactoryImpl syms, Lexeme lexeme )
    {
        final SymbolImpl return_val;

        if ( ( lexeme.type == LexemeType.SYM_CONSTANT ) )
        {
            return_val = syms.createString(lexeme.string);
        }
        else if ( lexeme.type == LexemeType.INTEGER)
        {
            return_val = syms.createInteger(lexeme.int_val);
        }
        else if ( lexeme.type == LexemeType.FLOAT)
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
     * <p>semantic_memory.cpp:2263:smem_parse_chunk
     * 
     * @param lexer
     * @param chunks
     * @param newbies
     * @return
     * @throws IOException
     */
    static boolean smem_parse_chunk( SymbolFactoryImpl symbols, Lexer lexer,  Map<String, smem_chunk_value> chunks, Set <smem_chunk_lti> newbies ) throws IOException
    {
        boolean return_val = false;
        smem_chunk_lti new_chunk = null;
        boolean good_at = false;
        ParsedLtiName chunk_name = null;
        //

        // consume left paren
        lexer.getNextLexeme();
        
        if ( ( lexer.getCurrentLexeme().type == LexemeType.AT ) || 
             ( lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER ) || 
             ( lexer.getCurrentLexeme().type == LexemeType.VARIABLE ) )
        {       
            good_at = true;
            
            if ( lexer.getCurrentLexeme().type == LexemeType.AT )
            {
                lexer.getNextLexeme();

                good_at = ( lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER );
            }
            
            if ( good_at )
            {
                // save identifier
                chunk_name = smem_parse_lti_name( lexer.getCurrentLexeme() );
                new_chunk = new smem_chunk_lti();
                new_chunk.lti_letter = chunk_name.id_letter;
                new_chunk.lti_number = chunk_name.id_number;
                new_chunk.lti_id = 0;
                new_chunk.soar_id = null;
                new_chunk.slots = new HashMap<SymbolImpl, List<smem_chunk_value>>();

                // consume id
                lexer.getNextLexeme();

                //

                long intermediate_counter = 1;
                smem_chunk_lti intermediate_parent;

                // populate slots
                while ( lexer.getCurrentLexeme().type == LexemeType.UP_ARROW )
                {
                    intermediate_parent = new_chunk;

                    // go on to attribute
                    lexer.getNextLexeme();

                    // get the appropriate constant type
                    SymbolImpl chunk_attr = smem_parse_constant_attr( symbols, lexer.getCurrentLexeme() );

                    // if constant attribute, proceed to value
                    if ( chunk_attr != null )
                    {
                        // consume attribute
                        lexer.getNextLexeme();

                        // support for dot notation:
                        // when we encounter a dot, instantiate
                        // the previous attribute as a temporary
                        // identifier and use that as the parent
                        while ( lexer.getCurrentLexeme().type == LexemeType.PERIOD )
                        {
                            // create a new chunk
                            final smem_chunk_lti temp_chunk = new smem_chunk_lti();
                            temp_chunk.lti_letter = chunk_attr.asString() != null ? chunk_attr.getFirstLetter() : 'X';
                            temp_chunk.lti_number = ( intermediate_counter++ );
                            temp_chunk.lti_id = 0;
                            temp_chunk.slots = new HashMap<SymbolImpl, List<smem_chunk_value>>();
                            temp_chunk.soar_id = null;

                            // add it as a child to the current parent
                            final List<smem_chunk_value> s = smem_make_slot( intermediate_parent.slots, chunk_attr );
                            s.add( temp_chunk );

                            // create a key guaranteed to be unique
                            final String temp_key = String.format("<%c#%d>", temp_chunk.lti_letter, temp_chunk.lti_number);
                            
                            // insert the new chunk
                            chunks.put(temp_key, temp_chunk);

                            // definitely a new chunk
                            newbies.add( temp_chunk );

                            // the new chunk is our parent for this set of values (or further dots)
                            intermediate_parent = temp_chunk;

                            // get the next attribute
                            lexer.getNextLexeme();
                            chunk_attr = smem_parse_constant_attr( symbols, lexer.getCurrentLexeme());

                            // consume attribute
                            lexer.getNextLexeme();
                        }

                        if ( chunk_attr != null )
                        {
                            smem_chunk_value chunk_value = null;
                            do
                            {
                                chunk_value = null;
                                // value by type
                                if ( ( lexer.getCurrentLexeme().type == LexemeType.SYM_CONSTANT ) )
                                {
                                    chunk_value = new smem_chunk_constant(symbols.createString(lexer.getCurrentLexeme().string));
                                }
                                else if ( ( lexer.getCurrentLexeme().type == LexemeType.INTEGER ) )
                                {
                                    chunk_value = new smem_chunk_constant(symbols.createInteger(lexer.getCurrentLexeme().int_val));
                                }
                                else if ( ( lexer.getCurrentLexeme().type == LexemeType.FLOAT) )
                                {
                                    chunk_value = new smem_chunk_constant(symbols.createDouble(lexer.getCurrentLexeme().float_val));
                                }
                                else if ( ( lexer.getCurrentLexeme().type == LexemeType.AT ) || 
                                          ( lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER ) || 
                                          ( lexer.getCurrentLexeme().type == LexemeType.VARIABLE ) )
                                {
                                    good_at = true;
                                    
                                    if ( lexer.getCurrentLexeme().type == LexemeType.AT )
                                    {
                                        lexer.getNextLexeme();

                                        good_at = ( lexer.getCurrentLexeme().type == LexemeType.IDENTIFIER );
                                    }

                                    if ( good_at )
                                    {                               
                                        // get key
                                        final ParsedLtiName temp_key2 = smem_parse_lti_name(lexer.getCurrentLexeme());

                                        // search for an existing chunk
                                        final smem_chunk_value p = chunks.get(temp_key2.value);

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

                                            chunk_value = temp_chunk;
                                            
                                            // add to chunks
                                            chunks.put(temp_key2.value, chunk_value);

                                            // possibly a newbie (could be a self-loop)
                                            newbies.add( chunk_value.asLti() );
                                        }
                                    }
                                }

                                if ( chunk_value != null )
                                {
                                    // consume
                                    lexer.getNextLexeme();

                                    // add to appropriate slot
                                    final List<smem_chunk_value> s = smem_make_slot( intermediate_parent.slots, chunk_attr );
                                    s.add( chunk_value );

                                    // if this was the last attribute
                                    if ( lexer.getCurrentLexeme().type == LexemeType.R_PAREN )
                                    {
                                        return_val = true;
                                        lexer.getNextLexeme();
                                        chunk_value = null;
                                    }
                                }
                            } while ( chunk_value != null );
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

        if ( return_val )
        {
            // search for an existing chunk (occurs if value comes before id)
            final smem_chunk_value p = chunks.get(chunk_name.value);

            if ( p == null)
            {
                chunks.put(chunk_name.value, new_chunk);

                // a newbie!
                newbies.add( new_chunk );
            }
            else
            {
                // transfer slots
                if ( p.asLti().slots == null )
                {
                    // if none previously, can just use
                    p.asLti().slots = new_chunk.slots;
                    new_chunk.slots = null;
                }
                else
                {
                    // otherwise, copy
                    for(Map.Entry<SymbolImpl, List<smem_chunk_value>> ss_p : new_chunk.slots.entrySet())
                    {
                        final List<smem_chunk_value> target_slot = smem_make_slot(p.asLti().slots, ss_p.getKey());
                        final List<smem_chunk_value> source_slot = ss_p.getValue();
                        
                        // for all values in the slot
                        for(smem_chunk_value s_p : source_slot)
                        {
                            // copy each value
                            target_slot.add(s_p);
                        }
                    }

                    // we no longer need the slots
                    new_chunk.slots = null;
                }

                // contents are new
                newbies.add(p.asLti());

                // deallocate
                smem_deallocate_chunk( new_chunk );
            }
        }
        else
        {
            newbies.clear();
        }

        return return_val;
    }
    
    /**
     * <p>semantic_memory.cpp:2564:smem_parse_chunks
     * 
     * @param chunkString
     * @return
     * @throws SoarException
     */
    boolean smem_parse_chunks( String chunkString ) throws SoarException
    {
        try
        {
            return smem_parse_chunks_safe(chunkString);
        }
        catch (IOException e)
        {
            throw new SoarException(e);
        }
        catch (SQLException e)
        {
            throw new SoarException(e);
        }
    }
    
    /**
     * <p>semantic_memory.cpp:2564:smem_parse_chunks
     * 
     * @param chunkString
     * @return
     * @throws SoarException
     * @throws IOException
     * @throws SQLException
     */
    private boolean smem_parse_chunks_safe( String chunkString ) throws SoarException, IOException, SQLException
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

        if ( lexer.getCurrentLexeme().type == LexemeType.L_BRACE )
        {
            boolean good_chunk = true;
            
            final Map<String, smem_chunk_value> chunks = new HashMap<String, smem_chunk_value>();
            //smem_str_to_chunk_map::iterator c_old;
            
            final Set<smem_chunk_lti> newbies = new HashSet<smem_chunk_lti>();
            //smem_chunk_set::iterator c_new;     

            // consume next token
            lexer.getNextLexeme();

            // while there are chunks to consume
            while ( ( lexer.getCurrentLexeme().type == LexemeType.L_PAREN ) && ( good_chunk ) )
            {
                good_chunk = smem_parse_chunk( symbols, lexer, chunks, newbies);

                if ( good_chunk )
                {
                    // add all newbie lti's as appropriate
                    for(smem_chunk_lti c_new : newbies)
                    {
                        if ( c_new.lti_id == 0 )
                        {                   
                            // deal differently with variable vs. lti
                            if ( c_new.lti_number == 0 )
                            {
                                // add a new lti id (we have a guarantee this won't be in Soar's WM)
                                c_new.lti_number = symbols.incrementIdNumber(c_new.lti_letter);
                                c_new.lti_id = smem_lti_add_id( c_new.lti_letter, c_new.lti_number );
                            }
                            else
                            {
                                // should ALWAYS be the case (it's a newbie and we've initialized lti_id to NIL)
                                if ( c_new.lti_id == 0 )
                                {
                                    // get existing
                                    c_new.lti_id = smem_lti_get_id(c_new.lti_letter, c_new.lti_number );

                                    // if doesn't exist, add it
                                    if ( c_new.lti_id == 0 )
                                    {
                                        c_new.lti_id = smem_lti_add_id( c_new.lti_letter, c_new.lti_number );

                                        // this could affect an existing identifier in Soar's WM
                                        final IdentifierImpl id_parent = symbols.findIdentifier( c_new.lti_letter, /* TODO SMEM long ids */(int) c_new.lti_number );
                                        if ( id_parent != null )
                                        {
                                            // if so we make it an lti manually
                                            id_parent.smem_lti = c_new.lti_id;

                                            // TODO SMEM Uncomment and port when epmem is ported
                                            // id_parent.smem_time_id = my_agent->epmem_stats->time->get_value();
                                            // id_parent.smem_valid = my_agent->epmem_validation;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // add all newbie contents (append, as opposed to replace, children)
                    for ( smem_chunk_lti c_new : newbies )
                    {
                        if ( c_new.slots != null )
                        {
                            smem_store_chunk( c_new.lti_id, c_new.slots, false );
                        }
                    }

                    // deallocate *contents* of all newbies (need to keep around name->id association for future chunks)
                    for ( smem_chunk_lti c_new : newbies )
                    {
                        smem_deallocate_chunk( c_new, false );
                    }

                    // increment clause counter
                    clause_count++;

                    // clear newbie list
                    newbies.clear();
                }
            };

            if ( good_chunk && ( lexer.getCurrentLexeme().type == LexemeType.R_BRACE ) )
            {
                // consume right brace
                lexer.getNextLexeme();

                // confirm (but don't consume) suffix
                return_val = ( lexer.getCurrentLexeme().type == LexemeType.EOF );       
            }

            // deallocate all chunks
            {
                for (smem_chunk_value c_old : chunks.values())
                {
                    smem_deallocate_chunk( c_old, true );
                }
            }
        }

        // produce error message on failure
        if ( !return_val )
        {
            throw new SoarException("Error parsing clause #" + clause_count);
        }

        return return_val;
    }

    private static enum path_type { blank_slate, cmd_bad, cmd_retrieve, cmd_query, cmd_store }
    
    /**
     * <p>semantic_memory.cpp:2702:smem_respond_to_cmd
     * 
     * @param store_only
     * @throws SQLException
     * @throws SoarException
     */
    void smem_respond_to_cmd( boolean store_only ) throws SQLException, SoarException
    {
        // start at the bottom and work our way up
        // (could go in the opposite direction as well)
        IdentifierImpl state = decider.bottom_goal;

        List<WmeImpl> wmes = new LinkedList<WmeImpl>();
        List<WmeImpl> cmds = null;

        IdentifierImpl query;
        IdentifierImpl retrieve;
        List<IdentifierImpl> prohibit = new LinkedList<IdentifierImpl>();
        List<IdentifierImpl> store = new LinkedList<IdentifierImpl>();

        path_type path = path_type.blank_slate;

        final int time_slot = ( ( store_only )?(1):(0) );
        long wme_count = 0;
        boolean new_cue = false;

        Marker tc; 

        IdentifierImpl parent_sym = null;
        final Queue<IdentifierImpl> syms = new ArrayDeque<IdentifierImpl>(); 

        int parent_level = 0;
        final Queue<Integer> levels = new ArrayDeque<Integer>();

        while ( state != null )
        {
            final SemanticMemoryStateInfo smem_info = smem_info(state);
            ////////////////////////////////////////////////////////////////////////////
            // TODO SMEM Timers: my_agent->smem_timers->api->start();
            ////////////////////////////////////////////////////////////////////////////

            // make sure this state has had some sort of change to the cmd
            // NOTE: we only care one-level deep!
            new_cue = false;
            wme_count = 0;
            cmds = null;
            {
                tc = DefaultMarker.create(); // get_new_tc_number( my_agent );

                // initialize BFS at command
                syms.add( smem_info.smem_cmd_header ); // push
                levels.add( 0 ); // push(0)           

                while ( !syms.isEmpty() )
                {
                    // get state
                    parent_sym = syms.remove(); // front()/pop()

                    parent_level = levels.remove(); // front()/pop()

                    // get children of the current identifier
                    wmes = smem_get_direct_augs_of_id( parent_sym, tc );
                    {
                        for (WmeImpl w_p : wmes)
                        {
                            if ( ( ( store_only ) && ( ( parent_level != 0 ) || ( ( w_p.attr != predefinedSyms.smem_sym_query ) && ( w_p.attr != predefinedSyms.smem_sym_retrieve ) ) ) ) || 
                                 ( ( !store_only ) && ( ( parent_level != 0 ) || ( w_p.attr != predefinedSyms.smem_sym_store ) ) ) )
                            {                       
                                wme_count++;

                                if ( w_p.timetag > smem_info.last_cmd_time[ time_slot ] )
                                {
                                    new_cue = true;
                                    smem_info.last_cmd_time[ time_slot ] = w_p.timetag;
                                }

                                if ( ( w_p.value.asIdentifier() != null ) &&
                                     ( parent_level == 0 ) &&
                                     ( ( w_p.attr == predefinedSyms.smem_sym_query ) || ( w_p.attr == predefinedSyms.smem_sym_store ) ) )                              
                                {
                                    syms.add( w_p.value.asIdentifier() ); // push
                                    levels.add( parent_level + 1 ); // push
                                }
                            }
                        }

                        // free space from aug list
                        if ( cmds == null )
                        {
                            cmds = wmes;
                        }
                        else
                        {
                            wmes = null; // delete wmes;
                        }
                    }
                }

                // see if any WMEs were removed         
                if ( smem_info.last_cmd_count[ time_slot ] != wme_count )
                {
                    new_cue = true;
                    smem_info.last_cmd_count[ time_slot ] = wme_count;
                }
                

                if ( new_cue )
                {
                    // clear old cue
                    smem_info.cue_wmes.clear();

                    // clear old results
                    smem_clear_result( state );

                    // change is afoot!
                    smem_made_changes = true;
                }
            }       

            // a command is issued if the cue is new
            // and there is something on the cue
            if ( new_cue && wme_count != 0)
            {
                // initialize command vars
                retrieve = null;
                query = null;
                store.clear();
                prohibit.clear();
                path = path_type.blank_slate;

                // process top-level symbols
                for (WmeImpl w_p : cmds )
                {
                    smem_info.cue_wmes.add( w_p );

                    if ( path != path_type.cmd_bad )
                    {
                        // collect information about known commands
                        if ( w_p.attr == predefinedSyms.smem_sym_retrieve )
                        {
                            if ( ( w_p.value.asIdentifier() != null ) &&
                                 ( path == path_type.blank_slate ) )
                            {
                                retrieve = w_p.value.asIdentifier();
                                path = path_type.cmd_retrieve;
                            }
                            else
                            {
                                path = path_type.cmd_bad;
                            }
                        }
                        else if ( w_p.attr == predefinedSyms.smem_sym_query )
                        {
                            if ( ( w_p.value.asIdentifier() != null ) &&
                                 ( ( path == path_type.blank_slate ) || ( path == path_type.cmd_query ) ) &&
                                 ( query == null ) )

                            {
                                query = w_p.value.asIdentifier();
                                path = path_type.cmd_query;
                            }
                            else
                            {
                                path = path_type.cmd_bad;
                            }
                        }
                        else if ( w_p.attr == predefinedSyms.smem_sym_prohibit )
                        {
                            if ( ( w_p.value.asIdentifier() != null ) &&
                                 ( ( path == path_type.blank_slate ) || ( path == path_type.cmd_query ) ) &&
                                 ( w_p.value.asIdentifier().smem_lti != 0 ) )
                            {
                                prohibit.add( w_p.value.asIdentifier() ); //push_back
                                path = path_type.cmd_query;
                            }
                            else
                            {
                                path = path_type.cmd_bad;
                            }
                        }
                        else if ( w_p.attr == predefinedSyms.smem_sym_store )
                        {
                            if ( ( w_p.value.asIdentifier() != null ) &&
                                 ( ( path == path_type.blank_slate ) || ( path == path_type.cmd_store ) ) )
                            {
                                store.add( w_p.value.asIdentifier() );
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
                if ( ( path == path_type.cmd_query ) && ( query == null ) )
                {
                    path = path_type.cmd_bad;
                }

                // must be on a path
                if ( path == path_type.blank_slate )
                {
                    path = path_type.cmd_bad;
                }

                ////////////////////////////////////////////////////////////////////////////
                // TODO SMEM Timers: my_agent->smem_timers->api->stop();
                ////////////////////////////////////////////////////////////////////////////

                // process command
                if ( path != path_type.cmd_bad )
                {
                    // performing any command requires an initialized database
                    smem_attach( );

                    // retrieve
                    if ( path == path_type.cmd_retrieve )
                    {
                        if ( retrieve.smem_lti == 0 )
                        {
                            // retrieve is not pointing to an lti!
                            smem_add_meta_wme( state, smem_info.smem_result_header, predefinedSyms.smem_sym_failure, retrieve );
                        }
                        else
                        {
                            // status: success
                            smem_add_meta_wme( state, smem_info.smem_result_header, predefinedSyms.smem_sym_success, retrieve );

                            // install memory directly onto the retrieve identifier
                            smem_install_memory( state, retrieve.smem_lti, retrieve );

                            // add one to the expansions stat
                            stats.retrieves.set(stats.retrieves.get() + 1);
                        }
                    }
                    // query
                    else if ( path == path_type.cmd_query )
                    {
                        final Set<Long> /*smem_lti_set*/ prohibit_lti = new HashSet<Long>();

                        for (IdentifierImpl sym_p : prohibit)
                        {
                            prohibit_lti.add( sym_p.smem_lti );
                        }

                        smem_process_query( state, query, prohibit_lti );

                        // add one to the cbr stat
                        stats.queries.set(stats.queries.get() + 1);
                    }
                    else if ( path == path_type.cmd_store )
                    {
                        ////////////////////////////////////////////////////////////////////////////
                        // TODO SMEM Timers: my_agent->smem_timers->storage->start();
                        ////////////////////////////////////////////////////////////////////////////

                        // start transaction (if not lazy)
                        if (!params.lazy_commit.get())
                        {
                            db.begin.executeUpdate( /*soar_module::op_reinit*/ );
                        }

                        for (IdentifierImpl sym_p : store )
                        {
                            smem_soar_store( sym_p );

                            // status: success
                            smem_add_meta_wme( state, smem_info.smem_result_header, predefinedSyms.smem_sym_success, sym_p );

                            // add one to the store stat
                            stats.stores.set(stats.stores.get() + 1);
                        }

                        // commit transaction (if not lazy)
                        if (!params.lazy_commit.get())
                        {
                            db.commit.executeUpdate( /*soar_module::op_reinit*/ );
                        }

                        ////////////////////////////////////////////////////////////////////////////
                        // TODO SMEM Timers: my_agent->smem_timers->storage->stop();
                        ////////////////////////////////////////////////////////////////////////////
                    }
                }
                else
                {
                    smem_add_meta_wme( state, smem_info.smem_result_header, predefinedSyms.smem_sym_bad_cmd, smem_info.smem_cmd_header );
                }
            }
            else
            {
                ////////////////////////////////////////////////////////////////////////////
                // TODO SMEM Timers: my_agent->smem_timers->api->stop();
                ////////////////////////////////////////////////////////////////////////////
            }

            // free space from aug list
            cmds = null; //delete cmds;

            state = state.higher_goal;
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.smem.SemanticMemory#smem_go(boolean)
     */
    @Override
    public void smem_go( boolean store_only )
    {
        // semantic_memory.cpp:3011:smem_go
        
        // after we are done we will perform a wm phase
        // if any adds/removes
        smem_made_changes = false;
        
        // TODO SMEM Timers: my_agent->smem_timers->total->start();

//    #ifndef SMEM_EXPERIMENT

        try
        {
            smem_respond_to_cmd( store_only );
        }
        catch (SQLException e)
        {
            // TODO SMEM error
            throw new RuntimeException(e);
        }
        catch (SoarException e)
        {
            // TODO SMEM error
            throw new RuntimeException(e);
        }

//    #else // SMEM_EXPERIMENT

//    #endif // SMEM_EXPERIMENT

        // TODO SMEM Timers: my_agent->smem_timers->total->stop();

        if ( smem_made_changes )
        {
            decider.do_working_memory_phase( );
        }
    }
    
    /**
     * <p>semantic_memory.cpp:3042:smem_visualize_store
     * 
     * @param return_val
     * @throws SoarException
     * @throws SQLException
     */
    void smem_visualize_store( PrintWriter return_val ) throws SoarException
    {
        try
        {
            smem_visualize_store_safe(return_val);
        }
        catch (SQLException e)
        {
            throw new SoarException(e);
        }
    }
    
    /**
     * <p>semantic_memory.cpp:3042:smem_visualize_store
     * 
     * @param return_val
     * @throws SoarException
     * @throws SQLException
     */
    private void smem_visualize_store_safe( PrintWriter return_val ) throws SoarException, SQLException
    {
        // vizualizing the store requires an open semantic database
        smem_attach( );

        // header
        return_val.append( "digraph smem {" );
        return_val.append( "\n" );

        // LTIs
        return_val.append( "node [ shape = doublecircle ];" );
        return_val.append( "\n" );

        final Map< Long, String > lti_names = new HashMap<Long, String>();
        {
            // id, letter, number
            {
                final ResultSet q = db.vis_lti.executeQuery();
                try
                {
                    while ( q.next() )
                    {
                        final long lti_id = q.getLong( 0 + 1 );
                        final char lti_letter = (char) q.getLong( 1 + 1);
                        final long lti_number = q.getLong( 2 + 1 );
        
                        final String lti_name = String.format("%c%d", lti_letter, lti_number);
                        lti_names.put(lti_id, lti_name);
        
                        return_val.append( lti_name );
                        return_val.append( " " );
                    }
                }
                finally
                {
                    q.close();
                }
            }

            if ( !lti_names.isEmpty() )
            {
                // terminal nodes first
                {
                    final Map< Long, List<String> > lti_terminals = new HashMap<Long, List<String>>();

                    List<String> my_terminals = null;

                    return_val.append( ";" );
                    return_val.append( "\n" );

                    // proceed to terminal nodes
                    return_val.append( "node [ shape = plaintext ];" );
                    return_val.append( "\n" );

                    {
                        // parent_id, attr_type, attr_val, val_type, val_val
                        final ResultSet q = db.vis_value_const.executeQuery();
                        try
                        {
                            while ( q.next() )
                            {
                                final long lti_id = q.getLong( 0 + 1 );
                                my_terminals = lti_terminals.get(lti_id);
                                if(my_terminals == null)
                                {
                                    lti_terminals.put(lti_id, my_terminals = new ArrayList<String>());
                                }
                                
                                final String lti_name = lti_names.get(lti_id); // TODO is this safe?
        
                                // parent prefix
                                return_val.append( lti_name );
                                return_val.append( "_" );
        
                                // terminal count
                                final int terminal_num = my_terminals.size();
                                return_val.append( Integer.toString(terminal_num) );
        
                                // prepare for value
                                return_val.append( " [ label = \"" );
        
                                // output value
                                {
                                    switch ( (int) q.getLong( 3 + 1 ) )
                                    {
                                        case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                            return_val.append( q.getString( 4 + 1 ) );
                                            break;
        
                                        case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                            return_val.append(Long.toString(q.getLong( 4 + 1 )));
                                            break;
        
                                        case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                            return_val.append(Double.toString(q.getDouble( 4 + 1 )));
                                            break;
        
                                        default:
                                            // print nothing
                                            break;
                                    }
                                }
        
                                // store terminal (attribute for edge label)
                                {
                                    switch ( (int) q.getLong( 1 + 1 ) )
                                    {
                                        case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                            my_terminals.add( q.getString( 2 + 1 ) );
                                            break;
        
                                        case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                            my_terminals.add(Long.toString(q.getLong( 2 + 1 )));
                                            break;
        
                                        case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                            my_terminals.add(Double.toString(q.getDouble( 2 + 1 )));
                                            break;
        
                                        default:
                                            my_terminals.add(""); // temp_str.clear();
                                            break;
                                    }
                                }
        
                                // footer
                                return_val.append( "\" ];" );
                                return_val.append( "\n" );
                            }
                        }
                        finally
                        {
                            q.close();
                        }
                    }

                    // output edges
                    {
                        for (Map.Entry<Long, String> n_p : lti_names.entrySet())
                        {
                            final List<String> t_p = lti_terminals.get( n_p.getKey() );

                            if ( t_p != null )
                            {
                                int terminal_counter = 0;

                                for (String a_p : t_p)
                                {
                                    return_val.append( n_p.getValue() );
                                    return_val.append( " -> " );
                                    return_val.append( n_p.getValue() );
                                    return_val.append( "_" );

                                    return_val.append( Integer.toString(terminal_counter) );
                                    return_val.append( " [ label=\"" );

                                    return_val.append( a_p );

                                    return_val.append( "\" ];" );
                                    return_val.append( "\n" );

                                    terminal_counter++;
                                }
                            }
                        }
                    }
                }

                // then links to other LTIs
                {
                    // parent_id, attr_type, attr_val, val_lti
                    {
                    final ResultSet q = db.vis_value_lti.executeQuery();
                    try
                    {
                        while ( q.next() )
                        {
                            // source
                            long lti_id = q.getLong( 0 + 1 );
                            String lti_name = lti_names.get(lti_id); // TODO SMEM is this safe?
                            return_val.append( lti_name );
                            return_val.append( " -> " );
    
                            // destination
                            lti_id = q.getLong( 3 + 1 );
                            lti_name = lti_names.get(lti_id); // TODO SMEM is this safe?
                            return_val.append( lti_name );
                            return_val.append( " [ label =\"" );
    
                            // output attribute
                            {
                                switch ( (int) q.getLong( 1 + 1) )
                                {
                                    case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                        return_val.append( q.getString( 2 + 1 ) );
                                        break;
    
                                    case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                        return_val.append(Long.toString(q.getLong( 2 + 1 )));
                                        break;
    
                                    case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                        return_val.append(Double.toString(q.getDouble( 2 + 1 )));
                                        break;
    
                                    default:
                                        return_val.append("");
                                        break;
                                }
                            }
    
                            // footer
                            return_val.append( "\" ];" );
                            return_val.append( "\n" );
                        }
                    }
                    finally
                    {
                        q.close();
                    }
                    }
                }
            }
        }

        // footer
        return_val.append( "}" );
        return_val.append( "\n" );
    }

    /**
     * <p>semantic_memory.cpp:3277:smem_visualize_lti
     * 
     * @param lti_id
     * @param depth
     * @param return_val
     * @throws SQLException
     */
    void smem_visualize_lti( long /*smem_lti_id*/ lti_id, long depth, PrintWriter return_val ) throws SoarException
    {
        try
        {
            smem_visualize_lti_safe(lti_id, depth, return_val);
        }
        catch (SQLException e)
        {
            throw new SoarException(e);
        }
    }
    
    /**
     * <p>semantic_memory.cpp:3277:smem_visualize_lti
     * 
     * @param lti_id
     * @param depth
     * @param return_val
     * @throws SQLException
     */
    private void smem_visualize_lti_safe( long /*smem_lti_id*/ lti_id, long depth, PrintWriter return_val ) throws SQLException
    {
        final Queue<smem_vis_lti> bfs = new ArrayDeque<smem_vis_lti>();

        final Set<Long /*smem_lti_id*/> close_list = new HashSet<Long>();

        // header
        return_val.append( "digraph smem_lti {" );
        return_val.append( "\n" );

        // root
        {
            final smem_vis_lti new_lti = new smem_vis_lti();
            new_lti.lti_id = lti_id;
            new_lti.level = 0;

            // fake former linkage
            {
                // get just this lti
                db.lti_letter_num.setLong( 1, lti_id );
                final ResultSet lti_q = db.lti_letter_num.executeQuery();

                try
                {
                    // letter
                    new_lti.lti_name = String.format("%c%d", (char) lti_q.getLong(0 + 1), lti_q.getLong(1 + 1));
                }
                finally
                {
                    lti_q.close();
                }

                // output without linkage
                return_val.append( "node [ shape = doublecircle ];" );
                return_val.append( "\n" );

                return_val.append( new_lti.lti_name );
                return_val.append( ";" );
                return_val.append( "\n" );
            }

            bfs.add( new_lti );

            close_list.add( lti_id );
        }

        // optionally depth-limited breadth-first-search of children
        while ( !bfs.isEmpty() )
        {
            final smem_vis_lti parent_lti = bfs.remove(); // front()/pop()

            long child_counter = 0;

            // get direct children: attr_const, attr_type, value_const, value_type, value_letter, value_num, value_lti
            db.web_expand.setLong( 1, parent_lti.lti_id );
            final ResultSet expand_q = db.web_expand.executeQuery();
            try
            {
                while ( expand_q.next() )
                {
                    // identifier vs. constant
                    if ( expand_q.getMetaData().getColumnType(2 + 1) == java.sql.Types.NULL )
                    {
                        final smem_vis_lti new_lti = new smem_vis_lti();
                        new_lti.lti_id = expand_q.getLong( 6 );
                        new_lti.level = ( parent_lti.level + 1 );
    
                        // add node
                        {
                            // letter
                            new_lti.lti_name = String.format("%c%d", (char) expand_q.getLong(4 + 1), expand_q.getLong(5 + 1));
    
                            // output node
                            return_val.append( "node [ shape = doublecircle ];" );
                            return_val.append( "\n" );
    
                            return_val.append( new_lti.lti_name );
                            return_val.append( ";" );
                            return_val.append( "\n" );
                        }
    
    
                        // add linkage
                        {
                            // get attribute
                            final String temp_str;
                            switch ( (int) expand_q.getLong( 1 + 1 ) )
                            {
                                case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                    temp_str = expand_q.getString( 0 + 1 );
                                    break;
    
                                case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                    temp_str = Long.toString(expand_q.getLong(0 + 1));
                                    break;
    
                                case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                    temp_str = Double.toString(expand_q.getDouble(0 + 1));
                                    break;
    
                                default:
                                    temp_str = "";
                                    break;
                            }
    
                            // output linkage
                            return_val.append( parent_lti.lti_name );
                            return_val.append( " -> " );
                            return_val.append( new_lti.lti_name );
                            return_val.append( " [ label = \"" );
                            return_val.append( temp_str );
                            return_val.append( "\" ];" );
                            return_val.append( "\n" );
                        }
    
                        // add to bfs (if still in depth limit)
                        if ( ( depth == 0 ) || ( new_lti.level < depth ) )
                        {
                            // prevent looping
                            if ( !close_list.contains(new_lti.lti_id) )
                            {
                                close_list.add( new_lti.lti_id );                       
                                bfs.add( new_lti );
                            }
                            else
                            {
                                // delete new_lti;
                            }               
                        }
                        else
                        {
                            // delete new_lti;
                        }
                    }
                    else
                    {
                        // get node name
                        final String node_name = String.format("%s_%d", parent_lti.lti_name, child_counter);
                        // add value node
                        {
    
                            // get value
                            final String temp_str;
                            switch ( (int) expand_q.getLong( 3 + 1 ) )
                            {
                                case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                    temp_str = expand_q.getString( 2 + 1 );
                                    break;
    
                                case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                    temp_str = Long.toString(expand_q.getLong( 2 + 1 ));
                                    break;
    
                                case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                    temp_str = Double.toString(expand_q.getDouble( 2 + 1 ));
                                    break;
    
                                default:
                                    temp_str = "";
                                    break;
                            }
    
                            // output node
                            return_val.append( "node [ shape = plaintext ];" );
                            return_val.append( "\n" );
                            return_val.append( node_name );
                            return_val.append( " [ label=\"" );
                            return_val.append( temp_str );
                            return_val.append( "\" ];" );
                            return_val.append( "\n" );
                        }
    
                        // add linkage
                        {
                            // get attribute
                            final String temp_str;
                            switch ( (int) expand_q.getLong( 1 + 1 ) )
                            {
                                case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                                    temp_str = expand_q.getString( 0 + 1 );
                                    break;
    
                                case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                                    temp_str = Long.toString(expand_q.getLong( 0 + 1 ));
                                    break;
    
                                case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                                    temp_str = Double.toString(expand_q.getDouble( 0 + 1));
                                    break;
    
                                default:
                                    temp_str = "";
                                    break;
                            }
    
                            // output linkage
                            return_val.append( parent_lti.lti_name );
                            return_val.append( " -> " );
                            return_val.append( node_name );
                            return_val.append( " [ label = \"" );
                            return_val.append( temp_str );
                            return_val.append( "\" ];" );
                            return_val.append( "\n" );
                        }
    
                        child_counter++;
                    }
                }
            }
            finally
            {
                expand_q.close();
            }
        }

        // footer
        return_val.append( "}" );
        return_val.append( "\n" );
    }
}
