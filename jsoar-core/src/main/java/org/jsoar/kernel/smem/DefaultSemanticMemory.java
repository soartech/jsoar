/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 23, 2010
 */
package org.jsoar.kernel.smem;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoar.kernel.Agent;
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
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.ByRef;
import org.jsoar.util.JdbcTools;
import org.jsoar.util.markers.Marker;

/**
 * smem_slot == List<smem_chunk_value> 
 * smem_slot_map == Map<SymbolImpl, List<smem_chunk_value>>

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

    private final Agent agent;
    private RecognitionMemory recMem;
    private Chunker chunker;
    
    private SemanticMemoryDatabase db;
    
    /** agent.h:smem_validation */
    private /*uintptr_t*/ long smem_validation;
    /** agent.h:smem_first_switch */
    private boolean smem_first_switch = true;
    /** agent.h:smem_made_changes */
    private boolean smem_made_changes = false;
    /** agent.h:smem_max_cycle */
    private /*intptr_t*/ long smem_max_cycle;
    
    private final SemanticMemorySymbols syms;
    
    private Map<IdentifierImpl, SemanticMemoryStateInfo> stateInfos = new HashMap<IdentifierImpl, SemanticMemoryStateInfo>();
    
    public DefaultSemanticMemory(Agent agent)
    {
        this.agent = agent;
        
        this.syms = new SemanticMemorySymbols(agent.getSymbols());
    }

    private SemanticMemoryStateInfo smem_info(IdentifierImpl state)
    {
        return stateInfos.get(state);
    }
    
    @Override
    public void smem_attach()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void smem_close()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean smem_enabled()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void smem_go(boolean storeOnly)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void smem_reset(IdentifierImpl state)
    {
        // TODO Auto-generated method stub
        
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
    private Symbol smem_statement_to_symbol( ResultSet q, int type_field, int val_field ) throws SQLException
    {
        final SymbolFactory syms = agent.getSymbols();
        
        switch (q.getInt( type_field ) )
        {
            case Symbols.SYM_CONSTANT_SYMBOL_TYPE:
                return syms.createString(q.getString(val_field));

            case Symbols.INT_CONSTANT_SYMBOL_TYPE:
                return syms.createInteger(q.getInt(val_field));

            case Symbols.FLOAT_CONSTANT_SYMBOL_TYPE:
                return syms.createDouble(q.getDouble(val_field));

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
                variable_value.value = rs.getLong(0);
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
                        return_val = rs.getLong(0);
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
    private void smem_lti_activate(/*smem_lti_id*/ long lti ) throws SQLException
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
            lti_child_ct = rs.getLong(0);
        }
        finally { rs.close(); }

        if ( lti_child_ct >= 0 /* TODO SMEM params: my_agent->smem_params->thresh->get_value()*/ )
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

        //my_agent->smem_stmts->act_lti_child_ct_get->reinitialize();

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
                    return_val = rs.getLong(0);
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

        //my_agent->smem_stmts->lti_get->reinitialize();

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
    private /*smem_lti_id*/ long smem_lti_add_id(char name_letter, long name_number) throws SQLException
    {
        /*smem_lti_id*/ long return_val = 0;

        // create lti: letter, number
        db.lti_add.setLong( 1, (long)( name_letter ) );
        db.lti_add.setLong( 2, (long)( name_number ) );
        db.lti_add.setLong( 3, 0 );
        db.lti_add.setLong( 4, 0 );

        return_val = JdbcTools.insertAndGetRowId(db.lti_add);

        // increment stat
        // TODO SMEM Stats: my_agent->smem_stats->chunks->set_value( my_agent->smem_stats->chunks->get_value() + 1 );

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

                id.smem_time_id = 0; // TODO SMEM stats: my_agent->epmem_stats->time->get_value();
                id.id_smem_valid = 0; // TODO SMEM WAH? my_agent->epmem_validation;
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
        final SymbolFactoryImpl factory = (SymbolFactoryImpl) agent.getSymbols();

        // try to find existing
        IdentifierImpl return_val = factory.findIdentifier(name_letter, (int) name_number); // TODO SMEM make name_number long

        // otherwise create
        if ( return_val == null )
        {
            return_val = factory.make_new_identifier(name_letter, level);
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
            final SymbolFactoryImpl factory = (SymbolFactoryImpl) agent.getSymbols();
            
            try
            {
                final ResultSet rs = db.lti_max.executeQuery();
                try
                {
                    while (rs.next())
                    {
                        // letter, max
                        long name_letter = rs.getLong(0);
                        long letter_max = rs.getLong(1);
   
                        // shift to alphabet
                        name_letter -= (long)( 'A' );
   
                        factory.resetIdNumber((int) name_letter, letter_max);
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

            // my_agent->smem_stmts->lti_max->reinitialize();
        }
    }
    
    /**
     * <p>semantic_memory.cpp:1116:smem_make_slot
     * 
     * @param slots
     * @param attr
     * @return
     */
    private List<smem_chunk_value> smem_make_slot( Map<SymbolImpl, List<smem_chunk_value>> slots, SymbolImpl attr )
    {
        List<smem_chunk_value> s = slots.get(attr);
        if(s == null)
        {
            s = new LinkedList<smem_chunk_value>();
            slots.put(attr, s);
        }
        return s;
    }
}
